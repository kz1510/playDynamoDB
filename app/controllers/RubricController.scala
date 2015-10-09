package controllers

import java.util.UUID

import _root_.util.Converters
import com.amazonaws.services.dynamodbv2.document.spec.{DeleteItemSpec, PutItemSpec, ScanSpec}
import com.amazonaws.services.dynamodbv2.document.{Expected, PrimaryKey, Table}
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException
import com.google.inject.Inject
import core.DynamoDBModule
import models.Rubric
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._


class RubricController @Inject() (val dynamoDBModule: DynamoDBModule) extends Controller {

  val table: Table = dynamoDBModule.rubricsTable

  def create: Action[JsValue] = Action(parse.json) { implicit req =>
    req.body.validate[Rubric].map { case rubric =>
        if (rubric.id.nonEmpty) {
          handleError("Rubric cannot contain id in Create")
        } else {
          val item = Converters.modelToItem(rubric.copy(id = Some(UUID.randomUUID().toString)))
          table.putItem(item)
          val savedRubric = Json.parse(item.toJSON).as[Rubric]
          Ok(Json.toJson(savedRubric))
        }
    }.recoverTotal { case err =>
      handleError("Invalid rubric json", err)
    }
  }

  private def handleError(err: String): Result = {
    BadRequest(Json.obj("error" -> err))
  }

  private def handleError(message: String, err: JsError): Result = {
    BadRequest(Json.obj("error" -> message, "details" -> JsError.toJson(err)))
  }

  def update(id: String): Action[JsValue] = Action(parse.json) { implicit req =>
    req.body.validate[Rubric].map { case rubric =>
      if (rubric.id.isEmpty) {
        handleError("Rubric should contain id in Update")
      } else if (rubric.id.get != id) {
        handleError("Rubric id is different than url id")
      } else {
        val item = Converters.modelToItem(rubric.copy(id = Some(id)))
        val putItemSpec = new PutItemSpec()
          .withItem(item)
          .withExpected(new Expected("id").exists())
        try {
          table.putItem(putItemSpec)
          val savedRubric = Json.parse(item.toJSON).as[Rubric]
          Ok(Json.toJson(savedRubric))
        } catch {
          case e: ConditionalCheckFailedException =>
            handleError("Existing item not found while updating")
        }
      }
    }.recoverTotal { case err =>
      handleError("Invalid rubric json", err)
    }
  }

  def list: Action[AnyContent] = Action { implicit req =>
    val scanResult = table.scan(new ScanSpec())
    // Get all items from table
    val allRubrics = scanResult.pages()
      .iterator()
      .flatMap(_.iterator().map(item => jsonStringToRubric(item.toJSON)))
      .collect { case JsSuccess(rubric, _) =>
        rubric
      }
      .toList
    Ok(Json.toJson(allRubrics))
  }

  def retrieve(id: String): Action[AnyContent] = Action { implicit req =>
    table.getItem("id", id) match {
      case null =>
        NotFound(Json.obj("error" -> "Not Found"))
      case item =>
        jsonStringToRubric(item.toJSON) match {
          case err: JsError =>
            Logger.error(s"Error reading stored json: ${JsError.toJson(err)}")
            InternalServerError("Error reading stored json")
          case succ: JsSuccess[Rubric] =>
            Ok(Json.toJson(succ.get))
        }
    }
  }

  def delete(id: String): Action[AnyContent] = Action { implicit req =>
    val deleteItemSpec = new DeleteItemSpec()
      .withPrimaryKey(new PrimaryKey("id", id))
    table.deleteItem(deleteItemSpec)
    Ok(Json.obj("status" -> "OK"))
  }

  private def jsonStringToRubric(jsonString: String): JsResult[Rubric] = {
    Json.parse(jsonString).validate[Rubric]
  }

}