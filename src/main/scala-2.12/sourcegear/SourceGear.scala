package sourcegear

import better.files.File
import cognitro.parsers.GraphUtils.BaseNode
import cognitro.parsers.ParserBase
import sourcegear.accumulate.FileAccumulator
import sourceparsers.SourceParserManager

import scalax.collection.edge.LkDiEdge
import scalax.collection.mutable.Graph

abstract class SourceGear {
  val parser: Set[ParserBase]
  val gearSet: GearSet = new GearSet

  def parseFile(file: File) = {
    val fileContents = file.contentAsString
    //@todo connect to parser list
    val parsedOption = SourceParserManager.parseFile(file.toJava)
    if (parsedOption.isDefined) {
      val parsed = parsedOption.get
      val astGraph = parsed.graph
      gearSet.parseFromGraph(fileContents, astGraph)
    } else {
      Vector()
    }

  }

}
