package com.opticdev.core.sourcegear

import better.files.File
import com.opticdev.core.sourcegear.graph.ProjectGraph
import com.opticdev.core.sourcegear.graph.edges.DerivedFrom
import play.api.libs.json._
import scalax.collection.edge.LkDiEdge
import com.opticdev.core.sourcegear.astDebugLocationFormats
import com.opticdev.common.fileFormat

package object sync {

  case class SyncSubGraph(sources: Int, targets: Int, warnings: Vector[SyncWarning], syncGraph: ProjectGraph) {
    def noWarnings : Boolean = warnings.isEmpty
  }

  sealed trait SyncWarning { def asJson: JsValue }
  case class DuplicateSourceName(name: String, locations: Vector[AstDebugLocation]) extends SyncWarning {
    override def asJson: JsValue = JsObject(Seq(
      "message" -> JsString(s"Source name '$name' is defined in multiple locations. All instances will be ignored"),
      "locations" -> JsArray(locations.map(Json.toJson[AstDebugLocation]))
    ))
  }

  case class SourceDoesNotExist(missingSource: String, location: AstDebugLocation) extends SyncWarning {
    override def asJson: JsValue = JsObject(Seq(
      "message" -> JsString(s"Source '${missingSource}' was not found."),
      "locations" -> JsArray(Seq(Json.toJson[AstDebugLocation](location)))
    ))
  }

  case class CircularDependency(targeting: String, location: AstDebugLocation) extends SyncWarning {
    override def asJson: JsValue = JsObject(Seq(
      "message" -> JsString(s"Using '${targeting}' as a source would lead to a circular dependency. This instance will be ignored."),
      "locations" -> JsArray(Seq(Json.toJson[AstDebugLocation](location)))
    ))
  }


  case class RangePatch(range: Range, newRaw: String, file: File, fileContents: String)
  trait FilePatchTrait {
    def file: File
    def originalFileContents: String
    def newFileContents: String
  }

  implicit val filePatchFormat = Json.format[FilePatch]
  case class FilePatch(file: File, originalFileContents: String, newFileContents: String) extends FilePatchTrait


  sealed trait SyncDiff {
    val edge: DerivedFrom
    def newValue : Option[JsObject] = None
    def isError : Boolean = false
  }
  case class NoChange(edge: DerivedFrom) extends SyncDiff
  case class Replace(edge: DerivedFrom, before: JsObject, after: JsObject, rangePatch: RangePatch) extends SyncDiff { override def newValue = Some(after) }
  case class ErrorEvaluating(edge: DerivedFrom, error: String, location: AstDebugLocation) extends SyncDiff {
    override def isError: Boolean = true
    private def message = s""""${error}" encountered when calculating patch"""
    override def toString: String = s""""${message}. Check location $location"""
    def asJson: JsValue = JsObject(Seq(
      "message" -> JsString(message),
      "locations" -> JsArray(Seq(Json.toJson[AstDebugLocation](location)))
    ))

  }
}
