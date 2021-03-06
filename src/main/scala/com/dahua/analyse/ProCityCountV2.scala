package com.dahua.analyse

import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

object ProCityCountV2 {

  def main(args: Array[String]): Unit = {
    // 判断参数。
    if (args.length != 1) {
        println(
          """
            |com.dahua.analyse.ProCityCount
            |缺少参数
            |inputPath
          """.stripMargin)
      sys.exit()
    }

    // 接收参数
    val Array(inputPath) = args
    // 获取SparkSession
    val spark = SparkSession.builder().appName(this.getClass.getSimpleName).master("local[*]").getOrCreate()

    val sc: SparkContext = spark.sparkContext

    // 读取数据源
    val df: DataFrame = spark.read.parquet(inputPath)
    // 创建临时视图
    df.createTempView("log")
    // 编写sql语句
    val sql = "select provincename,cityname,count(*) as pcsum from log  group by provincename,cityname";
    val procityCount: DataFrame = spark.sql(sql)
    // 输出到mysql
    val load: Config = ConfigFactory.load()
    val properties = new Properties()
    properties.setProperty("user",load.getString("jdbc.user"))
    properties.setProperty("driver",load.getString("jdbc.driver"))
    properties.setProperty("password",load.getString("jdbc.password"))
    procityCount.write.mode(SaveMode.Overwrite).jdbc(load.getString("jdbc.url"),load.getString("jdbc.tableName"),properties)
    // 关闭对象。
    spark.stop()


  }

}
