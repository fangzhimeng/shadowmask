/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.shadowmask.engine.spark.autosearch.pso;

import java.io.Serializable;
import java.util.Map;
import org.apache.spark.SparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.rdd.RDD;
import org.shadowmask.core.mask.rules.generalizer.actor.GeneralizerActor;
import org.shadowmask.engine.spark.ClassUtil;
import org.shadowmask.engine.spark.FileUtil;
import org.shadowmask.engine.spark.autosearch.pso.DataAnonymizePsoSearch;
import org.shadowmask.engine.spark.autosearch.pso.MkFitness;
import org.shadowmask.engine.spark.autosearch.pso.MkFitnessCalculator;
import org.shadowmask.engine.spark.autosearch.pso.MkParticle;
import org.shadowmask.engine.spark.autosearch.pso.MkParticleDriver;
import org.shadowmask.engine.spark.autosearch.pso.cluster.TaxTreeClusterMkParticleDriver;
import org.shadowmask.engine.spark.functions.DataAnoymizeFunction;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple5;

public class SparkDrivedDataAnoymizePSOSearch
    extends DataAnonymizePsoSearch<RDD<String>> implements Serializable {

  SparkDrivedFitnessCalculator calculator;
  MkParticleDriver driver = new TaxTreeClusterMkParticleDriver();
  private int threadNum;
  private String knFile = "/Users/liyh/Desktop/kn.txt";
  private String objectFile = "/Users/liyh/Desktop/particle.object";
  private String tableFile = "/Users/liyh/Desktop/table.txt";

  public SparkDrivedDataAnoymizePSOSearch(int threadNum) {
    this.threadNum = threadNum;
    calculator = new SparkDrivedFitnessCalculator(threadNum);
  }

  public RDD<String> currentBestMaskResult() {
    GeneralizerActor<?, ?>[] actors =
        this.globalBestParticle().historyBestPosition().getGeneralizerActors();
    return DataAnoymizeFunction
        .anoymizeWithActors(calculator.sc, calculator.dataSet, actors,
            calculator.separator, calculator.dataGeneralizerIndex);
  }

  @Override protected MkFitnessCalculator<RDD<String>> mkFitnessCalculator() {
    return calculator;
  }

  @Override protected MkParticleDriver velocityDriver() {
    return driver;
  }

  public void setSc(SparkContext sc) {
    this.calculator.sc = sc;
  }

  public void setSeparator(Broadcast<String> separator) {
    this.calculator.separator = separator;
  }

  public void setDataGeneralizerIndex(
      Broadcast<Map<Integer, Integer>> dataGeneralizerIndex) {
    this.calculator.dataGeneralizerIndex = dataGeneralizerIndex;
  }

  public void setTargetK(int targetK) {
    this.calculator.targetK = targetK;
  }

  public void setOutlierRate(Double outlierRate) {
    this.calculator.outlierRate = outlierRate;
  }

  public void setDataSize(Long dataSize) {
    this.calculator.dataSize = dataSize;
  }

  public void setQusiIndexes(int[] qusiIndexes) {
    this.calculator.qusiIndexes = qusiIndexes;
  }

  @Override public void updateGlobalBestParticle(MkParticle particle) {
    super.updateGlobalBestParticle(particle);
    System.out.println(particle.currentFitness());
    System.out.println(particle.currentPosition());
    FileUtil.append(knFile,
        particle.currentPosition().toString() + "#" + particle.currentFitness()
            .toString() + "\n");
    ClassUtil.seriObject(particle.currentPosition(), objectFile);
    super.updateGlobalBestParticle(particle);
  }

  @Override public void getBetterStep(int i) {
    FileUtil.append(knFile, i + "#");
  }

  @Override public void init() throws ClassNotFoundException {
    super.init();
    calculator.dataSet = this.privateTable;
    calculator.init();
  }

  @Override public void atLast() {
    calculator.sc.stop();
    calculator.shutdownExecutor();
  }

  @Override public void finished() {
    RDD<String> table = this.currentBestMaskResult();
    DataAnoymizeFunction.persistMaskResult(this.tableFile, table);
  }

  @Override public void foundABetterParticle(MkParticle particle) {
    RDD<String> table = this.currentBestMaskResult();
    DataAnoymizeFunction.persistMaskResult(this.tableFile, table);
  }

  public static class SparkDrivedFitnessCalculator
      extends MkFitnessCalculator<RDD<String>> implements Serializable {

    private SparkContext sc;

    private Broadcast<String> separator;

    private Broadcast<Map<Integer, Integer>> dataGeneralizerIndex;

    private int targetK;

    private Double outlierRate;

    private Long dataSize;

    private int[] qusiIndexes;

    private RDD<String> dataSet;

    private Double totalEntropy;
    private Integer originBadKSum;

    public SparkDrivedFitnessCalculator(int threadNums) {
      super(threadNums);
    }

    public void init() {
      Tuple3<Double, Integer, String[]> ek = DataAnoymizeFunction
          .utilityWithEntropy(dataSet, targetK, dataSize, ",", qusiIndexes);
      this.totalEntropy = ek._1();
      this.originBadKSum = ek._2();
    }

    //    @Override public MkFitness calculateOne(MkParticle particle,
    //        RDD<String> dataSet) {
    //
    //      RDD<String> anoymizedTable = DataAnoymizeFunction
    //          .anoymize(sc, dataSet, particle, separator, dataGeneralizerIndex);
    //
    //      anoymizedTable.cache();
    //      //      Object samples = anoymizedTable.sample(false,0.01,System.currentTimeMillis()).collect();
    //      //      String [] sampleArr = (String[]) samples;
    //      //      for (String s : sampleArr) {
    //      //        System.out.println(s);
    //      //      }
    //      //
    //      Tuple3<Object, Object, int[]> utility = DataAnoymizeFunction
    //          .utility(anoymizedTable, targetK, dataSize, outlierRate, ",",
    //              qusiIndexes, new int[] {}, new int[] {});
    //      MkFitness fitness = new MkFitness();
    //      fitness.setFitnessValue(Long.valueOf(utility._2().toString()));
    //      fitness.setOutlierSize(java.lang.Double.valueOf(utility._1().toString()));
    //      fitness.setKs(utility._3());
    //      // update particle fitness
    //      particle.setCurrentFitness(fitness);
    //
    //      return fitness;
    //    }

    @Override public MkFitness calculateOne(MkParticle particle,
        RDD<String> dataSet) {

      RDD<Tuple2<String, Tuple2<Integer, Integer>>> anoymizedTable =
          DataAnoymizeFunction
              .anoymizeWithRecordDepth(sc, dataSet, particle, separator,
                  dataGeneralizerIndex);

      anoymizedTable.cache();
      //      Object samples = anoymizedTable.sample(false,0.01,System.currentTimeMillis()).collect();
      //      String [] sampleArr = (String[]) samples;
      //      for (String s : sampleArr) {
      //        System.out.println(s);
      //      }
      //

      Tuple5<Double, Integer, Integer, Integer, String[]> ek =
          DataAnoymizeFunction
              .utilityWithEntropyTableWithDepth(anoymizedTable, targetK,
                  dataSize, ",", this.qusiIndexes);

      //      Tuple3<Object, Object, int[]> utility = DataAnoymizeFunction
      //          .utilityWithRecordDepth(anoymizedTable, targetK, dataSize,
      //              outlierRate, ",", qusiIndexes, new int[] {}, new int[] {});

      Double badKRate = (double) ek._2() / this.originBadKSum;
      Double eLoss = 1 - ek._1() / this.totalEntropy;
      Double depthLoss = ek._3().doubleValue() / ek._4();
      MkFitness fitness = new MkFitness();
      fitness.setFitnessValue(
          (badKRate + 0.5) * (eLoss + 0.5) * (depthLoss + 2));
      fitness.setOutlierSize(java.lang.Double.valueOf(ek._1().toString()));
      fitness.setOutlierSize(badKRate);
      fitness.setLossRate(eLoss);
      fitness.setDepthRate(depthLoss);
      fitness.setKcs(ek._5());
      // update particle fitness
      particle.setCurrentFitness(fitness);

      return fitness;
    }
  }
}