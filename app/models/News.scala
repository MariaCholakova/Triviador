package models
import play.api.libs.json._
import org.joda.time.{DateTime}
import org.joda.time.format.DateTimeFormat
import java.sql.Timestamp

case class News (user: String, text: String, date: JsObject) {
    // date: JsObject with a $timestamp nested object having a t and a i JsNumber fields
    implicit val dateReads: Reads[Timestamp] =  (JsPath ).read[Timestamp]

    def getDate = {
        date
        // val dateMillisec = date.validate[Timestamp](dateReads)
        // dateMillisec match {
        //     case date: JsSuccess[Timestamp] => date.get
        //     case err: JsError => err
        // }
    }
}