package measures.measure;

import org.aksw.limes.core.io.cache.Cache;
import org.aksw.limes.core.io.cache.MemoryCache;
import org.aksw.limes.core.measures.measure.MeasureProcessor;

public class MeasureProcessorTest {
    public static void main(String args[]) {
	Cache source = new MemoryCache();
	Cache target = new MemoryCache();
	source.addTriple("S1", "pub", "test");
	source.addTriple("S1", "conf", "conf one");
	source.addTriple("S2", "pub", "test2");
	source.addTriple("S2", "conf", "conf2");

	target.addTriple("S1", "pub", "test");
	target.addTriple("S1", "conf", "conf one");
	target.addTriple("S3", "pub", "test1");
	target.addTriple("S3", "conf", "conf three");

	System.out.println(MeasureProcessor.getSimilarity(source.getInstance("S1"), target.getInstance("S3"),
		"ADD(0.5*trigram(x.conf, y.conf),0.5*cosine(y.conf, x.conf))", "?x", "?y"));

	System.out.println(MeasureProcessor
		.getMeasures("AND(jaccard(x.authors,y.authors)|0.4278,overlap(x.authors,y.authors)|0.4278)"));
	System.out.println(MeasureProcessor.getMeasures("trigrams(x.conf, y.conf)"));

    }
}