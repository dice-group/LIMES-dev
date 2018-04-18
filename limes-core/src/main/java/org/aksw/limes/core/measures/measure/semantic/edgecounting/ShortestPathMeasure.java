package org.aksw.limes.core.measures.measure.semantic.edgecounting;

import java.util.List;

import org.aksw.limes.core.measures.measure.semantic.edgecounting.utils.ShortestPathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.mit.jwi.item.ISynset;

public class ShortestPathMeasure extends AEdgeCountingSemanticMeasure {
    private static final Logger logger = LoggerFactory.getLogger(ShortestPathMeasure.class);

    public ShortestPathMeasure() {
        super();
    }

    double maxValue = 1;

    @Override
    public double getSimilarity(ISynset synset1, List<List<ISynset>> synset1Tree, ISynset synset2,
            List<List<ISynset>> synset2Tree) {

        if (synset1.getType() != synset2.getType()) {
            logger.info(synset1.getType() + " " + synset2.getType());
            return 0;
        }

        if (synset1Tree.isEmpty() == true || synset2Tree.isEmpty() == true) {
            logger.info("Empty trees");
            return 0;
        }

        if (synset1.getOffset() == synset2.getOffset()) {
            logger.info("Max value: " + maxValue);
            return maxValue;
        }

        int shortestPath = ShortestPathFinder.shortestPath(synset1Tree, synset2Tree);
        if (shortestPath == -1) {
            logger.error("Error finding shortest path");
            return 0;
        }

        logger.info("Synsets distance " + shortestPath);

        double D = (double) getHierarchyDepth(synset1.getType());
        double sim = (double) (2.0 * D) - (double) (shortestPath);
        // normalize
        sim /= (double) (2.0 * D);
        logger.info("Similarity: " + sim);
        return sim;
    }

    @Override
    public double getRuntimeApproximation(double mappingSize) {
        return mappingSize / 1000d;

    }

    @Override
    public String getName() {
        return "shortestPath";
    }

    @Override
    public String getType() {
        return "semantic";
    }

}