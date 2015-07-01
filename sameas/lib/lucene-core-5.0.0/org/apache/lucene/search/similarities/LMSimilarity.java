package org.apache.lucene.search.similarities;

/*
 *
 * Copyright(c) 2015, Samsung Electronics Co., Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.util.Locale;

import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;

/**
 * Abstract superclass for language modeling Similarities. The following inner
 * types are introduced:
 * <ul>
 *   <li>{@link LMStats}, which defines a new statistic, the probability that
 *   the collection language model generates the current term;</li>
 *   <li>{@link CollectionModel}, which is a strategy interface for object that
 *   compute the collection language model {@code p(w|C)};</li>
 *   <li>{@link DefaultCollectionModel}, an implementation of the former, that
 *   computes the term probability as the number of occurrences of the term in the
 *   collection, divided by the total number of tokens.</li>
 * </ul> 
 * 
 * @lucene.experimental
 */
public abstract class LMSimilarity extends SimilarityBase {
  /** The collection model. */
  protected final CollectionModel collectionModel;
  
  /** Creates a new instance with the specified collection language model. */
  public LMSimilarity(CollectionModel collectionModel) {
    this.collectionModel = collectionModel;
  }
  
  /** Creates a new instance with the default collection language model. */
  public LMSimilarity() {
    this(new DefaultCollectionModel());
  }
  
  @Override
  protected BasicStats newStats(String field, float queryBoost) {
    return new LMStats(field, queryBoost);
  }

  /**
   * Computes the collection probability of the current term in addition to the
   * usual statistics.
   */
  @Override
  protected void fillBasicStats(BasicStats stats, CollectionStatistics collectionStats, TermStatistics termStats) {
    super.fillBasicStats(stats, collectionStats, termStats);
    LMStats lmStats = (LMStats) stats;
    lmStats.setCollectionProbability(collectionModel.computeProbability(stats));
  }

  @Override
  protected void explain(Explanation expl, BasicStats stats, int doc,
      float freq, float docLen) {
    expl.addDetail(new Explanation(collectionModel.computeProbability(stats),
                                   "collection probability"));
  }
  
  /**
   * Returns the name of the LM method. The values of the parameters should be
   * included as well.
   * <p>Used in {@link #toString()}</p>.
   */
  public abstract String getName();
  
  /**
   * Returns the name of the LM method. If a custom collection model strategy is
   * used, its name is included as well.
   * @see #getName()
   * @see CollectionModel#getName()
   * @see DefaultCollectionModel 
   */
  @Override
  public String toString() {
    String coll = collectionModel.getName();
    if (coll != null) {
      return String.format(Locale.ROOT, "LM %s - %s", getName(), coll);
    } else {
      return String.format(Locale.ROOT, "LM %s", getName());
    }
  }

  /** Stores the collection distribution of the current term. */
  public static class LMStats extends BasicStats {
    /** The probability that the current term is generated by the collection. */
    private float collectionProbability;
    
    /**
     * Creates LMStats for the provided field and query-time boost
     */
    public LMStats(String field, float queryBoost) {
      super(field, queryBoost);
    }
    
    /**
     * Returns the probability that the current term is generated by the
     * collection.
     */
    public final float getCollectionProbability() {
      return collectionProbability;
    }
    
    /**
     * Sets the probability that the current term is generated by the
     * collection.
     */
    public final void setCollectionProbability(float collectionProbability) {
      this.collectionProbability = collectionProbability;
    } 
  }
  
  /** A strategy for computing the collection language model. */
  public static interface CollectionModel {
    /**
     * Computes the probability {@code p(w|C)} according to the language model
     * strategy for the current term.
     */
    public float computeProbability(BasicStats stats);
    
    /** The name of the collection model strategy. */
    public String getName();
  }
  
  /**
   * Models {@code p(w|C)} as the number of occurrences of the term in the
   * collection, divided by the total number of tokens {@code + 1}.
   */
  public static class DefaultCollectionModel implements CollectionModel {

    /** Sole constructor: parameter-free */
    public DefaultCollectionModel() {}

    @Override
    public float computeProbability(BasicStats stats) {
      return (stats.getTotalTermFreq()+1F) / (stats.getNumberOfFieldTokens()+1F);
    }
    
    @Override
    public String getName() {
      return null;
    }
  }
}