package models
import play.api.libs.json.JsObject

case class News (user_id: JsObject, text: String, date: JsObject)
// user_id: JsObject with a $oid JsString field with the stringified ID as value
// date: JsObject with a $timestamp nested object having a t and a i JsNumber fields