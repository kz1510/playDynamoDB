import core.DynamoDBModule
import models.Rubric
import play.api.libs.json.{JsObject, Json}
import play.api.test._
import play.api.{Logger, Play}

class RubricEndpointSpec extends PlaySpecification {
  // Set sequential execution
  sequential

  def createDummyRubricJson(name: String, indicators: Vector[String]): JsObject = {
    Json.obj(
      "publisher" -> s"p${name}",
      "token" -> s"t${name}",
      "name" -> s"n${name}"
    ) ++ {
      if (indicators.nonEmpty) Json.obj("indicators" -> indicators)
      else Json.obj()
    }
  }

  val app = new FakeApplication()
  step {
    Logger.info("Starting Play")
    Play.start(app)
    val dynamoDBModule: DynamoDBModule = app.injector.instanceOf[DynamoDBModule]
    Logger.info("Resetting rubrics table")
    dynamoDBModule.deleteRubricsTable()
    dynamoDBModule.createRubricsTable()
  }

  "RubricEndpointSpec" should {
    val controller = controllers.routes.RubricController

    "start with an empty table" in {
      // List empty table
      val Some(listResult) = route(FakeRequest(controller.list()))
      contentAsJson(listResult).as[Set[Rubric]].size === 0
    }

    "create a rubric" in {
      // Create one rubric
      val json1 = createDummyRubricJson("1", Vector.empty)
      val Some(createResult) = route(FakeRequest(controller.create).withJsonBody(json1))
      val savedRubric = contentAsJson(createResult).validate[Rubric].get
      savedRubric.copy(id = None) === Json.fromJson[Rubric](json1).get

      // Create second rubric
      val json2 = createDummyRubricJson("1", Vector("2", "2"))
      val Some(createResult2) = route(FakeRequest(controller.create).withJsonBody(json2))
      val savedRubric2 = contentAsJson(createResult2).as[Rubric]
      savedRubric2.id should beSome[String]
      savedRubric2.indicators === Some(Vector("2", "2"))
    }

    "handle create failures" in {
      val Some(createResult) = route(FakeRequest(controller.create)
        .withJsonBody(createDummyRubricJson("createFailure", Vector("aa", "bb")) ++ Json.obj("id" -> "dangerousId")))
      status(createResult) === BAD_REQUEST
      contentAsString(createResult) must contain("Rubric cannot contain id in Create")

      val Some(createResult2) = route(FakeRequest(controller.create)
        .withJsonBody(createDummyRubricJson("createFailure", Vector("aa", "bb")) - "name"))
      status(createResult2) === BAD_REQUEST
      contentAsString(createResult2) must contain("Invalid rubric json")
    }

    "update a rubric" in {
      val json = createDummyRubricJson("updateTest", Vector("aa", "bb", "cc"))
      val Some(createResult) = route(FakeRequest(controller.create()).withJsonBody(json))
      status(createResult) === OK
      val savedRubric = contentAsJson(createResult).as[Rubric]

      // Update rubric
      val updatedRubric = savedRubric.copy(publisher = "updatedPublisher", indicators = Some(Vector("Updated")))
      val Some(updateResult) = route(FakeRequest(controller.update(updatedRubric.id.get))
        .withJsonBody(Json.toJson(updatedRubric)))
      contentAsJson(updateResult).as[Rubric] === updatedRubric
    }

    "handle update failures" in {
      val Some(createResult) = route(FakeRequest(controller.create)
        .withJsonBody(createDummyRubricJson("updateFailure", Vector("aa", "bb"))))
      status(createResult) === OK
      val savedRubric = contentAsJson(createResult).as[Rubric]

      val Some(updateResult) = route(FakeRequest(controller.update(savedRubric.id.get))
        .withJsonBody(Json.toJson(savedRubric.copy(id = Some("otherId")))))
      status(updateResult) === BAD_REQUEST
      contentAsString(updateResult) must contain("Rubric id is different than url id")

      {
        val Some(updateResult) = route(FakeRequest(controller.update(savedRubric.id.get + "1"))
          .withJsonBody(Json.toJson(savedRubric.copy(id = Some(savedRubric.id.get + "1")))))
        status(updateResult) === BAD_REQUEST
        contentAsString(updateResult) must contain("Existing item not found while updating")
      }

      {
        val Some(updateResult) = route(FakeRequest(controller.update(savedRubric.id.get + "1"))
          .withJsonBody(Json.toJson(savedRubric.copy(id = Some(savedRubric.id.get + "1"))).asInstanceOf[JsObject] - "name"))
        status(updateResult) === BAD_REQUEST
        contentAsString(updateResult) must contain("Invalid rubric json")
      }

      {
        val Some(updateResult) = route(FakeRequest(controller.update(savedRubric.id.get)).withJsonBody(Json.toJson(savedRubric.copy(id = None))))
        status(updateResult) === BAD_REQUEST
        contentAsString(updateResult) must contain("Rubric should contain id in Update")
      }
    }

    "retrieve a rubric and handle failure" in {
      val json = createDummyRubricJson("retrieveTest", Vector("aa", "bb", "cc"))
      val Some(createResult) = route(FakeRequest(controller.create()).withJsonBody(json))
      status(createResult) === OK
      val savedRubric = contentAsJson(createResult).as[Rubric]

      val Some(retrieveResult) = route(FakeRequest(controller.retrieve(savedRubric.id.get)))
      status(retrieveResult) === OK
      contentAsJson(retrieveResult).as[Rubric] === savedRubric

      {
        val Some(retrieveResult) = route(FakeRequest(controller.retrieve("testtesttest")))
        status(retrieveResult) === NOT_FOUND
      }

    }

    "list rubrics" in {
      val json = createDummyRubricJson("listTest", Vector("aa", "bb", "cc"))
      val Some(createResult) = route(FakeRequest(controller.create()).withJsonBody(json))
      status(createResult) === OK
      val savedRubric = contentAsJson(createResult).as[Rubric]

      val Some(listResult) = route(FakeRequest(controller.list()))
      contentAsJson(listResult).as[List[Rubric]] must contain(savedRubric)
    }

    "delete rubric and handle failures" in {
      val json = createDummyRubricJson("retrieveTest", Vector("aa", "bb", "cc"))
      val Some(createResult) = route(FakeRequest(controller.create()).withJsonBody(json))
      status(createResult) === OK
      val savedRubric = contentAsJson(createResult).as[Rubric]

      // Delete first rubric
      val Some(deleteResult) = route(FakeRequest(controller.delete(savedRubric.id.get)))
      status(deleteResult) === OK
      val Some(retrieveResult) = route(FakeRequest(controller.retrieve(savedRubric.id.get)))
      status(retrieveResult) === NOT_FOUND

      {
        val Some(deleteResult) = route(FakeRequest(controller.delete("dangerousId")))
        status(deleteResult) === OK
      }
    }
  }

  step {
    Logger.info("Cleaning up")
    val dynamoDBModule: DynamoDBModule = app.injector.instanceOf[DynamoDBModule]
    dynamoDBModule.deleteRubricsTable()
    Logger.info("Stopping Play")
    Play.stop(app)
  }
}