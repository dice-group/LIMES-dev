package org.aksw.limes.core.measures.measure.string.bilang;

import java.util.ArrayList;
import java.util.HashMap;
import org.aksw.limes.core.measures.measure.AMeasure;
import org.aksw.limes.core.measures.measure.string.AStringMeasure;
import org.aksw.limes.core.measures.measure.string.SimpleEditDistanceMeasure;

public class SimpleDictionaryMeasure extends AStringMeasure {


  private Dictionary dictionary;
  private AMeasure innerMeasure = new SimpleEditDistanceMeasure();

  public SimpleDictionaryMeasure(Dictionary dictionary) {
    this.dictionary = dictionary;
  }

  @Override
  public int getPrefixLength(int tokensNumber, double threshold) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int getMidLength(int tokensNumber, double threshold) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public double getSizeFilteringThreshold(int tokensNumber, double threshold) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int getAlpha(int xTokensNumber, int yTokensNumber, double threshold) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public double getSimilarity(int overlap, int lengthA, int lengthB) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean computableViaOverlap() {
    return false;
  }

  /**
   * iterates through all possible translations and uses innerMeasure to determine the similarity
   * as the product of the inner similarity from the input word to the e.g. english word,
   * multiplied by the inner similarity of the e.g. german translation of that word to the output word
   * @param object1,
   *            the source object (a string)
   * @param object2,
   *            the target object (a string)
   *
   * @return best translation similarity
   */
  @Override
  public double getSimilarity(Object object1, Object object2) {
    String a = ("" + object1).toLowerCase();
    String b = ("" + object2).toLowerCase();
    double bestSimilarity = 0.0;
    HashMap<String, ArrayList<String>> map = dictionary.getSource2targetMap();
    for (String sourceWord : map.keySet()) {
      for (String targetWord : map.get(sourceWord)) {
        double similarity = innerMeasure.getSimilarity(a, sourceWord) *
            innerMeasure.getSimilarity(targetWord, b);
        if (similarity > bestSimilarity) {
          bestSimilarity = similarity;
        }
      }
    }
    return bestSimilarity;
  }

  @Override
  public double getRuntimeApproximation(double mappingSize) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getName() {
    return "simpleDictionary";
  }
}
