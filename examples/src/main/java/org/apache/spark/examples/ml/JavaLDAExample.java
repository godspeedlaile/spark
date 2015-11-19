/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ML;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.ml.clustering.LDA;
import org.apache.spark.ml.clustering.LDAModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.VectorUDT;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.catalyst.expressions.GenericRow;
import org.apache.spark.sql.types.*;

import java.util.regex.Pattern;

/**
 * An example demonstrating LDA
 * Run with
 * <pre>
 * bin/run-example ml.JavaLDAExample <file> <k>
 * </pre>
 */
public class JavaLDAExample {

  private static class ParseVector implements Function<String, Row> {
    private static final Pattern separator = Pattern.compile(" ");

    @Override
    public Row call(String line) {
      String[] tok = separator.split(line);
      double[] point = new double[tok.length];
      for (int i = 0; i < tok.length; ++i) {
        point[i] = Double.parseDouble(tok[i]);
      }
      Vector[] points = {Vectors.dense(point)};
      return new GenericRow(points);
    }
  }

  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println("Usage: ml.JavaLDAExample <file> <k>");
      System.exit(1);
    }
    String inputFile = args[0];
    int k = Integer.parseInt(args[1]);

    // Parses the arguments
    SparkConf conf = new SparkConf().setAppName("JavaLDAExample");
    JavaSparkContext jsc = new JavaSparkContext(conf);
    SQLContext sqlContext = new SQLContext(jsc);

    // Loads data
    JavaRDD<Row> points = jsc.textFile(inputFile).map(new ParseVector());
    StructField[] fields = {new StructField("features", new VectorUDT(), false, Metadata.empty())};
    StructType schema = new StructType(fields);
    DataFrame dataset = sqlContext.createDataFrame(points, schema);

    // Trains a LDA model
    LDA lda = new LDA()
      .setK(k)
      .setMaxIter(10);
    LDAModel model = lda.fit(dataset);

    System.out.println(model.logLikelihood(dataset));
    System.out.println(model.logPerplexity(dataset));

    // Shows the result
    DataFrame topics = model.describeTopics(3);
    topics.show(false);

    jsc.stop();
  }

  /**
   * Load documents, tokenize them, create vocabulary, and prepare documents as term count vectors.
   * @return (corpus, vocabulary as array)
   */
  private DataFrame preprocess(JavaSparkContext jsc, String path) {

    SQLContext sqlContext = new SQLContext(jsc);

    JavaRDD<Row> docs = jsc.textFile(path).map(doc => new GenericRow(doc));
    StructField[] fields = {new StructField("features", new VectorUDT(), false, Metadata.empty())};
    StructType schema = new StructType(fields);
    DataFrame dataset = sqlContext.createDataFrame(points, schema);


    .toDF("docs")
    val tokenizer = new RegexTokenizer()
      .setInputCol("docs")
      .setOutputCol("rawTokens")
    val stopWordsRemover = new StopWordsRemover()
      .setInputCol("rawTokens")
      .setOutputCol("tokens")
    val countVectorizer = new CountVectorizer()
      .setInputCol("tokens")
      .setOutputCol("features")
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, stopWordsRemover, countVectorizer))

    val model = pipeline.fit(df)

    (model.transform(df),
      model.stages(2).asInstanceOf[CountVectorizerModel].vocabulary)
  }


}
