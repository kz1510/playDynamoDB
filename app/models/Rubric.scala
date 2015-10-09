package models

import play.api.libs.functional.syntax._
import play.api.libs.json._


case class Rubric(
  id: Option[String],
  publisher: String,
  token: String,
  name: String,
  indicators: Option[Vector[String]]
) {
  def simplify: Rubric = {
    this.copy(indicators = indicators.filter(_.nonEmpty))
  }
}

object Rubric {

  implicit val format: OFormat[Rubric] = {
    (
      (__ \ 'id).formatNullable[String] and
      (__ \ 'publisher).format[String] and
      (__ \ 'token).format[String] and
      (__ \ 'name).format[String] and
      (__ \ 'indicators).formatNullable[Vector[String]].inmap[Option[Vector[String]]](
        _.filter(_.nonEmpty), _.filter(_.nonEmpty)
      )
    )(Rubric.apply, unlift(Rubric.unapply))
  }

}