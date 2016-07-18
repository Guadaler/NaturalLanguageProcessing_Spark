package util

import java.io.{FileNotFoundException, IOException}

import com.ibm.icu.text.CharsetDetector
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.SparkContext

import scala.xml.{Elem, XML}

/**
  * Created by li on 16/7/7.
  */
object HBaseUtil {


  /**
    * 识别字符编码
    *
    * @param html 地址编码
    * @return 字符编码
    */
  def judgeChaser(html: Array[Byte]): String = {

    val icu4j = new CharsetDetector()
    icu4j.setText(html)
    val encoding = icu4j.detect()

    encoding.getName
  }

//  /**
//    * 设置时间范围
//    *
//    * @return 时间范围
//    * @author yangshuai
//    */
//  def setTimeRange(): String = {
//
//    val scan = new Scan()
//    val date = new Date(new Date().getTime - 1 * 60 * 60 * 1000)
//    val format = new SimpleDateFormat("yyyy-MM-dd HH")
//    val time = format.format(date)
//    val time1 = format.format(new Date().getTime)
//    val startTime = time + "-00-00"
//    val stopTime = time1 + "-00-00"
//    val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss")
//    val startRow: Long = sdf.parse(startTime).getTime
//    val stopRow: Long = sdf.parse(stopTime).getTime
//
//    scan.setTimeRange(startRow, stopRow)
//    val proto: ClientProtos.Scan = ProtobufUtil.toScan(scan)
//
//    Base64.encodeBytes(proto.toByteArray)
//  }


  /**
    * 获取xml格式的配置文件
    *
    * @param dir 配置文件所在的文件目录
    * @return
    */
  def readConfigFile(dir: String): Elem = {

    val configFile = XML.loadFile(dir)

    configFile
  }

  /**
    * 获取hbase配置内容,并且初始化hbase配置
    * @param configFile hbase配置文件
    * @return
    */
  def getHBaseConfigure(configFile: Elem): Configuration = {

    val rootDir = (configFile \ "hbase" \ "rootDir").text
    val ip = (configFile \ "hbase" \ "ip").text

    // 初始化配置
    val configuration = HBaseConfiguration.create()
    //    configuration.set("hbase.zookeeper.property.cilentport", "2181") //设置zookeeper client 端口configuration
    //    configuration.set("hbase.zookeeper.quorum", "localhost") //设置zookeeper quorum
    //    configuration.set("hbasemaster", "localhost:60000") //设置hbase master
    configuration.set("hbase.rootdir", rootDir)
    configuration.set("hbase.zookeeper.quorum", ip)

    configuration
  }

  /**
    * 获取hbase中的内容
    * @param sc SparkContext
    * @param dir 配置文件所在的文件夹
    */
  def readFromHBase(sc: SparkContext, dir: String) {

    val configFile = readConfigFile(dir)
    val configuration = getHBaseConfigure(configFile)

    // 读取hbase中的文件
    try {

      val tableName = "wk_detail"  // 表名
      val timeRange = TimeUtil.setTimeRange() //扫描格式
      configuration.set(TableInputFormat.INPUT_TABLE, tableName)
      configuration.set(TableInputFormat.SCAN, timeRange)

      // 使用Hadoop api来创建一个RDD
      val hBaseRDD = sc.newAPIHadoopRDD(configuration,
        classOf[TableInputFormat],
        classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
        classOf[org.apache.hadoop.hbase.client.Result])

      /**
        * todo 对hBaseRDD操作
        */
      println(hBaseRDD.count())

      val news = hBaseRDD.map( x => {

        val a = x._2.getValue(Bytes.toBytes("basic"), Bytes.toBytes("url"))
        val b = x._2.getValue(Bytes.toBytes("basic"), Bytes.toBytes("title"))
        val c = x._2.getValue(Bytes.toBytes("basic"), Bytes.toBytes("content"))
        val aFormat = judgeChaser(a)
        val bFormat = judgeChaser(b)
        val cFormat = judgeChaser(c)
        new String(a, aFormat) + "\n\t" + new String(b, bFormat) + "\n\t" + new String(c, cFormat)

      }).cache()

      news.foreach(x => println(x))
//      hBaseRDD.saveAsTextFile("")

    } catch {

      case e: FileNotFoundException => println("Missing file exception")
      case e: IOException => println("IO Exception")

    } finally {

      println{"Exiting finally ......"}

    }
  }

  def write2HBase: Unit ={

  }

}