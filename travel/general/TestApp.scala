package ru.yandex.tours.spark

import org.apache.spark.{SparkConf, SparkContext}
import ru.yandex.tours.util.Logging

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 23.02.16
 */
object TestApp extends App with Logging{

  val conf = new SparkConf()
    .setAppName("test-app (ya.travel)")
    .setMaster("yarn-client")
    .setJars(Classpath.listJars.map(_.getAbsolutePath))
    .set("spark.yarn.jar", "hdfs:///user/darl/spark-assembly-1.6.0-hadoop2.6.0-cdh5.4.7.jar")

  val sc = new SparkContext(conf)

  val rdd = sc.parallelize(1 to 1000000)
  log.info("Calculated sum: " + rdd.sum())
}
