package org.aksw.limes.core.execution.rewriter;

import java.util.ArrayList;
import java.util.List;

import org.aksw.limes.core.io.ls.LinkSpecification;
import org.aksw.limes.core.io.parser.Parser;
import org.aksw.limes.core.measures.mapper.SetOperations.Operator;
import org.apache.log4j.Logger;



public class AlgebraicRewriter implements IRewriter {


    
    static Logger logger = Logger.getLogger("LIMES");

    @Override
    public LinkSpecification rewrite(LinkSpecification spec) {
	// rewrite only non-atomic specs
	// if(spec.size() <= 1) return spec;
	int oldSize;
	int newSize = spec.size();
	int counter = 0;
	LinkSpecification result = spec;
	try {
	    do {
		counter++;
		// System.out.println(counter + " -> " + spec);
		oldSize = newSize;
		spec = updateThresholds(spec);
		spec = computeAllDependencies(spec);
		spec = collapseSpec(spec);
		spec = removeUnaryOperators(spec);
		newSize = spec.size();
		result = spec;
	    } while (newSize < oldSize);
	} catch (Exception e) {
	    logger.error(e.getMessage());
	    e.printStackTrace();
	}
	return result;
    }

    /**
     * Updates the thresholds within the input spec
     *
     * @param spec
     * @return Return spec with updated weights
     */
    public LinkSpecification updateThresholds(LinkSpecification spec) {
	// should not happen
	if (spec.isEmpty()) {
	    return spec;
	}
	// does not work for atomic specs
	if (!spec.isAtomic()) {
	    // only works for null filters
	    if (spec.getFilterExpression() == null) {
		double min = 1f;
		// get minimum over all children
		for (LinkSpecification child : spec.children) {
		    if (child.threshold < min) {
			min = child.threshold;
		    }
		}
		// if spec threshold smaller than miminum then set to 0
		if (spec.threshold <= min) {
		    spec.threshold = 0;
		}
	    }
	    // if spec has children then run update for children as well
	    for (LinkSpecification child : spec.children) {
		child = updateThresholds(child);
	    }
	}
	return spec;
    }

    /**
     * Removes unary operators from a spec
     *
     * @param spec
     *            Input
     * @return Cleaned up spec
     */
    public LinkSpecification removeUnaryOperators(LinkSpecification spec) {
	if (!spec.isAtomic() && !spec.isEmpty()) {
	    if (spec.getFilterExpression() == null && spec.children.size() == 1) {
		// don't forget to update the threshold while lifting the branch
		double theta = Math.max(spec.threshold, spec.children.get(0).threshold);
		System.out.print("Old spec = " + spec + "\t");

		spec = spec.children.get(0);
		spec.threshold = theta;
		System.out.println("New spec = " + spec + "\t");
	    }
	    if (!spec.isAtomic()) {
		List<LinkSpecification> newChildren = new ArrayList<LinkSpecification>();
		for (LinkSpecification child : spec.children) {
		    newChildren.add(removeUnaryOperators(child));
		}
		spec.children = newChildren;
	    }
	}
	return spec;
    }

    /**
     * Checks whether source depends on target, i.e., whether the mapping
     * generated by source is a subset of the mapping generated by target.
     * Returns 0 if no dependency is found, 1 if target is included in source,
     * -1 if source is included in target and +2 if they are equivalent
     *
     * @param source
     *            Source link spec
     * @param target
     *            Target link spec
     * @return -1, 0, +1 or +2
     */
    public LinkSpecification computeAtomicDependency(LinkSpecification source, LinkSpecification target) {
	// only works for atomic properties
	if (!source.isAtomic() || !target.isAtomic()) {
	    return source;
	}
	// compute the source and target properties used. Only works if the
	// properties
	// used by the spec are the same
	if (getProperties(source).equals(getProperties(target))) {
	    String measure1 = getMeasure(source);
	    String measure2 = getMeasure(target);
	    if (measure1.equals(measure2)) {
		if (source.threshold <= target.threshold) {
		    source.addDependency(target);
		} else {

		    double t1 = source.threshold;
		    double t2 = target.threshold;
		    if (measure1.equals("trigrams")) {
			// works for jaro vs. jaro-winkler
			//
		    } else if (measure2.equals("overlap")) {
			if (measure2.equals("jaccard") && t2 <= 2 * t1 / (1 + t1)) {
			    source.addDependency(target);
			}
		    }
		}
	    }
	}
	return source; // nothing found
    }

    /**
     * Returns the properties that are used for the comparison Only works for
     * atomatic specs
     *
     * @param spec
     *            Input spec
     * @return List of properties used in the spec
     */
    public List<String> getProperties(LinkSpecification spec) {
	List<String> result = new ArrayList<String>();
	if (spec.isAtomic()) {
	    Parser p = new Parser(spec.getFilterExpression(), spec.threshold);
	    result.add(p.getTerm1());
	    result.add(p.getTerm2());
	}
	return result;
    }

    /**
     * Returns the measure used in the spec
     *
     * @param spec
     *            Specification
     * @return Measure used in the spec, null if the spec is not atomic
     */
    public String getMeasure(LinkSpecification spec) {
	if (spec.isAtomic()) {
	    return spec.getFilterExpression().substring(0, spec.getFilterExpression().indexOf("("));
	} else {
	    return null;
	}
    }

    /**
     * Updates all dependencies within a spec
     *
     * @param spec
     *            Input spec
     * @return Spec with all dependencies updated
     */
    public LinkSpecification computeAllDependencies(LinkSpecification spec) {
	spec = computeAtomicDependencies(spec);
	spec = computeNonAtomicDependencies(spec);
	return spec;
    }

    /**
     * Updates the non-atomic dependencies of a link spec
     *
     * @param spec
     * @return
     */
    public LinkSpecification computeNonAtomicDependencies(LinkSpecification spec) {
	if (!spec.isAtomic()) {
	    List<LinkSpecification> newDependencies = null;
	    List<LinkSpecification> newChildren = new ArrayList<LinkSpecification>();
	    // first update dependencies of children
	    for (LinkSpecification child : spec.children) {
		newChildren.add(computeNonAtomicDependencies(child));
	    }
	    spec.children = newChildren;
	    // then update spec itself
	    // if operator = AND, then dependency is intersection of all
	    // dependencies
	    if (spec.operator == Operator.AND && spec.children.get(0).hasDependencies()) {
		newDependencies = spec.children.get(0).dependencies;
		for (int i = 1; i < spec.children.size(); i++) {
		    if (!spec.children.get(i).hasDependencies()) {
			break;
		    } else {
			newDependencies.retainAll(spec.children.get(i).dependencies);
		    }
		}
	    }
	    // if operator = OR, then merge all
	    if (spec.operator == Operator.OR) {
		newDependencies = new ArrayList<LinkSpecification>();
		for (LinkSpecification child : spec.children) {
		    if (child.hasDependencies()) {
			newDependencies.addAll(child.dependencies);
		    }
		}
	    }
	    spec.dependencies = null;

	    if (newDependencies != null) {
		for (LinkSpecification d : newDependencies) {
		    if (d.threshold > spec.threshold || spec.threshold == 0) {
			spec.addDependency(d);
		    }
		}
	    }
	}
	return spec;
    }

    /**
     * Computes all dependencies within a link specification
     *
     * @param spec
     *            Input spec
     * @return spec with all dependencies computed
     */
    public LinkSpecification computeAtomicDependencies(LinkSpecification spec) {
	List<LinkSpecification> leaves = spec.getAllLeaves();
	// compute the dependency between leaves
	for (int i = 0; i < leaves.size(); i++) {
	    // reset dependencies
	    leaves.get(i).dependencies = new ArrayList<LinkSpecification>();
	    for (int j = 0; j < leaves.size(); j++) {
		if (i != j) {
		    leaves.set(i, computeAtomicDependency(leaves.get(i), leaves.get(j)));
		}
	    }
	}
	return spec;
    }

    /**
     * Collapses a spec by making use of the dependencies within the spec
     *
     * @param spec
     *            Input spec
     * @return Collapsed spec, i.e., spec where dependencies have been removed
     */
    public LinkSpecification collapseSpec(LinkSpecification spec) {
	if (spec.isAtomic() || spec.isEmpty()) {
	    return spec;
	}
	// first collapse children which depend on each other
	if (spec.operator == Operator.AND) {
	    List<LinkSpecification> newChildren = new ArrayList<LinkSpecification>();
	    newChildren.addAll(spec.children);
	    // child is a superset of its dependencies, thus
	    // if one of its dependency is a child of the current node, then
	    // no need to compute child
	    for (LinkSpecification child : spec.children) {
		if (child.hasDependencies()) {
		    for (LinkSpecification dependency : child.dependencies) {
			if (newChildren.contains(dependency)) {
			    // ensures that at least one child is kept, in case
			    // of cyclic dependencies
			    // quick fix. Might not work
			    if (newChildren.size() > 1) {
				newChildren.remove(child);
			    }
			}
		    }
		}
	    }
	    spec.children = newChildren;
	} else if (spec.operator == Operator.OR) {
	    List<LinkSpecification> newChildren = new ArrayList<LinkSpecification>();
	    newChildren.addAll(spec.children);
	    for (LinkSpecification child : spec.children) {
		if (child.hasDependencies()) {
		    for (LinkSpecification dependency : child.dependencies) {
			// all entries of dependency contained in child, so
			// no need to compute it
			if (newChildren.contains(dependency)) {
			    // ensures that at least one child is kept, in case
			    // of cyclic dependencies
			    // quick fix. Might not work
			    if (newChildren.size() > 1) {
				newChildren.remove(dependency);
			    }
			}
		    }
		}
	    }
	    spec.children = newChildren;
	}
	List<LinkSpecification> newChildren = new ArrayList<LinkSpecification>();
	// now collapse remaining children
	for (LinkSpecification child : spec.children) {
	    newChildren.add(collapseSpec(child));
	}
	spec.children = newChildren;
	return spec;
    }

    
}
