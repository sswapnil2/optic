package sdk.descriptions

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import play.api.libs.json._

object Schema extends Description[Schema] {

  implicit val schemaIdReads: Reads[SchemaId] = (json: JsValue) => {
    if (json.isInstanceOf[JsString]) {
      JsSuccess(SchemaId(json.as[JsString].value))
    } else {
      JsError(error = "SchemaId must be a string")
    }
  }

  private val validatorFactory = JsonSchemaFactory.newBuilder().freeze()

  def schemaObjectfromJson(schema: JsObject): JsonSchema = {
    if (validatorFactory.getSyntaxValidator.schemaIsValid(schema.as[JsonNode])) {
      validatorFactory.getJsonSchema(schema.as[JsonNode])
    } else throw new Error("Invalid Schema "+ validatorFactory.getSyntaxValidator.validateSchema(schema.as[JsonNode]).toString)
  }

  override def fromJson(jsValue: JsValue): Schema = Schema(jsValue.as[JsObject])
}

case class Schema(schema: JsObject) {
  private def getValue(key: String) = {
    val valueOption = (schema \ key)
    if (valueOption.isDefined) {
      valueOption.get.as[JsString].value
    } else {
      throw new Error("Invalid Schema No field "+key+" defined.")
    }
  }

  val name : String = getValue("title")
  val version : String = getValue("version")
  val slug : String = getValue("slug")

  private val jsonSchema : JsonSchema = Schema.schemaObjectfromJson(schema)

  val identifier = name+"^"+version

  def validate(jsValue: JsValue): Boolean = jsonSchema.validate(jsValue.as[JsonNode]).isSuccess

}

case class SchemaId(id: String) {
  //@todo impliment provider lookup
  def resolve : Schema = null
}