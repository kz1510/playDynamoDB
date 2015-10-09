package util

import com.amazonaws.services.dynamodbv2.document.Item
import play.api.libs.json._

object Converters {

  def jsonObjectToItem(jsonObject: JsObject): Item = {
    Item.fromJSON(Json.stringify(jsonObject))
  }

  def modelToItem[Model:Writes](m: Model): Item = {
    val jsonString = Json.stringify(Json.toJson(m))
    Item.fromJSON(jsonString)
  }
}
