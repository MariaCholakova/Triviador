package models
import play.api.libs.json._
import java.time.Instant

case class News (user: String, text: String, date: Long) {

    def getDate = Instant.ofEpochMilli(date)
}