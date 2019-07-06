package models
import play.api.libs.json.JsNumber

case class LoginUser (username: String, password: String)

case class User (username: String, password: String, points: Int)

