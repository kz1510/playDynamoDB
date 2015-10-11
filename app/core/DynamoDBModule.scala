package core

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Table}
import com.amazonaws.services.dynamodbv2.model._
import com.google.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import play.api.{Application, Logger}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.util.Try



@Singleton
class DynamoDBModule @Inject() (app: Application, applicationLifecycle: ApplicationLifecycle)() {
  Logger.info("Creating DynamoDBModule")

  applicationLifecycle.addStopHook { () =>
    Logger.info("Shutting down dynamoDB client")
    Future.successful(this.shutdown())
  }

  private val client: AmazonDynamoDBClient = {
    val endpoint = app.configuration.getString("dynamodb.endpoint").getOrElse(throw new Exception("Please specify dynamodb.endpoint in conf file"))
    val c = new AmazonDynamoDBClient()
    c.setEndpoint(endpoint)
    c
  }

  def shutdown(): Unit = {
    client.shutdown()
  }

  val dynamoDB: DynamoDB = new DynamoDB(client)

  private val rubricsTableName: String = "Rubrics"

  val rubricsTable: Table = {
    if (Try(client.describeTable(rubricsTableName)).isSuccess) {
      dynamoDB.getTable(rubricsTableName)
    } else {
      createRubricsTable()
    }
  }

  def createRubricsTable(): Table = {
    val tableName = rubricsTableName
    val table = dynamoDB.createTable(
      tableName,
      List(
        new KeySchemaElement("id", KeyType.HASH)),
      List(
        new AttributeDefinition("id", ScalarAttributeType.S)),
      new ProvisionedThroughput(10000L, 10000L) // interesting, but beyond the scope of this exercise (getting started)
    )
    table
  }

  def deleteRubricsTable(): Boolean = {
     Try(dynamoDB.getTable(rubricsTableName).delete()).isSuccess
  }


}
