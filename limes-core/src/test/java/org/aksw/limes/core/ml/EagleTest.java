package org.aksw.limes.core.ml;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.aksw.limes.core.evaluation.qualititativeMeasures.PseudoFMeasure;
import org.aksw.limes.core.exceptions.UnsupportedMLImplementationException;
import org.aksw.limes.core.io.cache.ACache;
import org.aksw.limes.core.io.cache.Instance;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.io.config.Configuration;
import org.aksw.limes.core.io.config.KBInfo;
import org.aksw.limes.core.io.mapping.AMapping;
import org.aksw.limes.core.io.mapping.MappingFactory;
import org.aksw.limes.core.ml.algorithm.ActiveMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.Eagle;
import org.aksw.limes.core.ml.algorithm.MLAlgorithmFactory;
import org.aksw.limes.core.ml.algorithm.MLImplementationType;
import org.aksw.limes.core.ml.algorithm.MLResults;
import org.aksw.limes.core.ml.algorithm.SupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.UnsupervisedMLAlgorithm;
import org.aksw.limes.core.ml.algorithm.eagle.util.PropertyMapping;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.MappingJsonFactory;

/**
 * @author Tommaso Soru (tsoru@informatik.uni-leipzig.de)
 * @author Klaus Lyko (lyko@informatik.uni-leipzig.de)
 *
 */
public class EagleTest {

	protected static Logger logger = Logger.getLogger(EagleTest.class);
    ACache sc = new MemoryCache();
    ACache tc = new MemoryCache();

    AMapping trainingMap, extendedTrainingMap, refMap;
    
    Configuration config = new Configuration();
    PropertyMapping pm = new PropertyMapping();

    @Before
    public void init() {
        List<String> props = new LinkedList<String>();
        props.add("name");
        props.add("surname");

        Instance i1 = new Instance("ex:i1");
        i1.addProperty("name", "Klaus");
        i1.addProperty("surname", "Lyko");
        Instance i2 = new Instance("ex:i2");
        i2.addProperty("name", "John");
        i2.addProperty("surname", "Doe");
        Instance i3 = new Instance("ex:i3");
        i3.addProperty("name", "Claus");
        i3.addProperty("surname", "Stadler");
        
        Instance i4 = new Instance("ex:i4");
        i4.addProperty("name", "Claus");
        i4.addProperty("surname", "Lyko");        
        Instance i5 = new Instance("ex:i5");
        i5.addProperty("name", "J.");
        i5.addProperty("surname", "Doe");
        
        Instance i6 = new Instance("ex:i6");
        i6.addProperty("name", "Donald");
        i6.addProperty("surname", "Trump");


        sc.addInstance(i1);
        sc.addInstance(i3);
        sc.addInstance(i6);

        tc.addInstance(i1);
        tc.addInstance(i2);
        tc.addInstance(i3);
        tc.addInstance(i4);
        tc.addInstance(i5);
        tc.addInstance(i6);
        
        
        trainingMap = MappingFactory.createDefaultMapping();
        trainingMap.add("ex:i1", "ex:i1", 1d);

        refMap = MappingFactory.createDefaultMapping();
        refMap.add("ex:i1", "ex:i1", 1d);
        refMap.add("ex:i3", "ex:i3", 1d);
        refMap.add("ex:i6", "ex:i6", 1d);

        extendedTrainingMap = MappingFactory.createDefaultMapping();
        for(Entry<String, HashMap<String, Double>> e : refMap.getMap().entrySet()) {
        	for(String t : e.getValue().keySet())
        	extendedTrainingMap.add(e.getKey(), t, refMap.getConfidence(e.getKey(), t));
        }
        extendedTrainingMap.add("ex:i1", "ex:i4", 0.8d);
        extendedTrainingMap.add("ex:i2", "ex:i5", 1d);
        
        KBInfo si = new KBInfo();
        si.setVar("?x");
        si.setProperties(props);

        KBInfo ti = new KBInfo();
        ti.setVar("?y");
        ti.setProperties(props);

        config.setSourceInfo(si);
        config.setTargetInfo(ti);

        pm.addStringPropertyMatch("name", "name");
        pm.addStringPropertyMatch("surname", "surname");

    }

    @Test
    public void testSupervisedBatch() throws UnsupportedMLImplementationException {
        SupervisedMLAlgorithm eagleSup = null;
        try {
            eagleSup = MLAlgorithmFactory.createMLAlgorithm(Eagle.class,
                    MLImplementationType.SUPERVISED_BATCH).asSupervised();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
            fail();
        }
        assert (eagleSup.getClass().equals(SupervisedMLAlgorithm.class));
        eagleSup.init(null, sc, tc);
        eagleSup.getMl().setConfiguration(config);
        eagleSup.setParameter(Eagle.INQUIRY_SIZE, 2);
        eagleSup.setParameter(Eagle.PROPERTY_MAPPING, pm);

        MLResults mlModel = eagleSup.learn(trainingMap);
        AMapping resultMap = eagleSup.predict(sc, tc, mlModel);
        logger.info("Predicted links:"+resultMap);
//        assert (resultMap.getSize() > 0);  
    }

    @Test
    public void testUnsupervised() throws UnsupportedMLImplementationException {
        UnsupervisedMLAlgorithm eagleUnsup = null;
        try {
            eagleUnsup = MLAlgorithmFactory.createMLAlgorithm(Eagle.class,
                    MLImplementationType.UNSUPERVISED).asUnsupervised();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
            fail();
        }
        assert (eagleUnsup.getClass().equals(UnsupervisedMLAlgorithm.class));
        trainingMap = null;
        eagleUnsup.init(null, sc, tc);
        eagleUnsup.getMl().setConfiguration(config);
        eagleUnsup.setParameter(Eagle.PROPERTY_MAPPING, pm);

        MLResults mlModel = eagleUnsup.learn(new PseudoFMeasure());
        AMapping resultMap = eagleUnsup.predict(sc, tc, mlModel);

//        assert (resultMap.getSize() > 0);     
    }
    
    @Test
    public void testSupervisedActive() throws UnsupportedMLImplementationException {
    	ActiveMLAlgorithm eagleSup = null;
        try {
            eagleSup = MLAlgorithmFactory.createMLAlgorithm(Eagle.class,
                    MLImplementationType.SUPERVISED_ACTIVE).asActive();
        } catch (UnsupportedMLImplementationException e) {
            e.printStackTrace();
            fail();
        }
        assert (eagleSup.getClass().equals(ActiveMLAlgorithm.class));
        eagleSup.init(null, sc, tc);
        eagleSup.getMl().setConfiguration(config);
        eagleSup.setParameter(Eagle.INQUIRY_SIZE, 2);
        eagleSup.setParameter(Eagle.PROPERTY_MAPPING, pm);
        eagleSup.activeLearn(trainingMap);
       
        AMapping toAnnotate = eagleSup.getNextExamples(2);
        AMapping newOracle = MappingFactory.createDefaultMapping();
        for(String sKey : toAnnotate.getMap().keySet())
        	for(String tKey : toAnnotate.getMap().get(sKey).keySet()) {
        		logger.info("Asking Oracle about "+sKey+" - "+tKey+" ("+toAnnotate.getConfidence(sKey, tKey)+")");
        		if(trainingMap.contains(sKey, tKey) || extendedTrainingMap.contains(sKey, tKey)) {
        			newOracle.add(sKey, tKey, 1d);
        		} else {
        			newOracle.add(sKey, tKey, -1d);
        		}
        	}
        logger.info("new Oracle: "+newOracle.size()+": "+newOracle);
        MLResults resultMap = eagleSup.activeLearn(newOracle);
        logger.info("new resultMap: "+resultMap);
        assert (resultMap.getMapping() != null);  
    }

}
