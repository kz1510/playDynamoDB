import models.Rubric
import org.specs2.mutable.Specification
import play.api.libs.json._
import _root_.util.Converters

class RubricSerializerSpec extends Specification {

  val rubric1 = Rubric(Some("test1"), "aa", "aa", "aa", Some(Vector("abc", "abc")))
  val rubric2 = Rubric(Some("test2"), "aa", "aa", "aa", Some(Vector.empty))
  val rubric3 = Rubric(Some("test3"), "aa", "aa", "aa", None)
  val rubric4 = Rubric(None, "aa", "aa", "aa", None)

  "Rubric serializer" should {
    "convert to and from json with format" in {
      val sample = Rubric(Some("testId"), "aa", "aa", "aa", Some(Vector("abc", "abc")))
      val json = Json.toJson(sample).asInstanceOf[JsObject]
      json.fields.size === 5
      json.validate[Rubric] === JsSuccess(sample)

      Rubric.format.reads(Rubric.format.writes(rubric2)) === JsSuccess(rubric2.simplify)
    }

    "convert to and from an Item" in {
      val item = Converters.modelToItem(rubric1)
      Json.parse(item.toJSON).validate[Rubric] === JsSuccess(rubric1.simplify)

      val json = Json.toJson(rubric1).asInstanceOf[JsObject]
      Converters.jsonObjectToItem(json).asMap() === item.asMap()
    }

    "convert Some(Vector.empty) to None" in {
      val json = Json.toJson(rubric2)
      (json \ "indicators").toOption === None

      (Json.toJson(rubric1) \ "indicators").validateOpt[Vector[String]] === JsSuccess(rubric1.indicators)
      (Json.toJson(rubric3) \ "indicators").validateOpt[Vector[String]] === JsSuccess(None)
      (Json.toJson(rubric4) \ "indicators").validateOpt[Vector[String]] === JsSuccess(None)
    }
  }

}
