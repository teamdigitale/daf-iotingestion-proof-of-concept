package it.teamDigitale.kafkaProducers.eventConverters
import java.text.SimpleDateFormat

import it.teamDigitale.avro.{AvroConverter, DataPoint}

import scala.collection.immutable.Seq
import scala.xml.{NodeSeq, XML}

/**
  * Created by fabiana on 14/03/17.
  */
class TorinoParkingConverter extends EventConverter {

  import TorinoParkingConverter._

  def convert(time: Long): (Long, Option[Seq[Array[Byte]]]) = {
    val xml = XML.load(url)

    val traffic_data: NodeSeq = xml \\ "traffic_data"
    val pk_data = traffic_data \\ "PK_data"
    val generationTimeString = (traffic_data \\ "@generation_time").text
    val generationTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(generationTimeString).getTime

    if (generationTimestamp > time) {
      val tags = for {
        tag <- pk_data
      } yield convertDataPoint(tag, generationTimestamp)

      val avro = tags.map(x => AvroConverter.convertDataPoint(x))
      avro.foreach(println(_))
      (generationTimestamp, Some(avro))
    } else {
      (time, None)
    }

  }

  private def convertDataPoint(ftd_data: NodeSeq, generationTimestamp: Long): DataPoint = {

    val name = (ftd_data \ "@Name").text
    val status = (ftd_data \ "@status").text
    val free1 = (ftd_data \ "@Free").text
    val free = free1.toDouble
    val tendence = (ftd_data \ "@tendence").text
    val lat = (ftd_data \ "@lat").text
    val lon = (ftd_data \ "@lng").text
    val latLon = s"$lat-$lon"

    val tags: Map[String, String] = Map(
      att_name -> name,
      att_status -> status,
      att_tendence -> tendence
    )

    val values = Map(
      att_free -> free
    )

    val point = new DataPoint(
      version = 0L,
      id = Some(measure),
      ts = generationTimestamp,
      event_type_id = measure.hashCode,
      location = latLon,
      host = host,
      service = url,
      body = Some(ftd_data.toString().getBytes()),
      tags = tags,
      values = values
    )

    point
  }


}
object TorinoParkingConverter {

  val url = "http://opendata.5t.torino.it/get_pk"
  val host = "http://opendata.5t.torino.it"

  val att_name = "name"
  val att_status = "status"
  val att_free = "free"
  val att_tendence = "tendence"
  val att_latLon = "coordinate"

  val measure = "opendata.torino.get_pk"

}
