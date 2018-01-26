package com.opticdev.core.sourcegear.project

import java.nio.file.{StandardWatchEventKinds => EventType}

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import better.files.FileWatcher._
import better.files.{File, _}
import com.opticdev.core.actorSystem
import com.opticdev.core.sourcegear.project.status._
import com.opticdev.core.sourcegear.SourceGear
import com.opticdev.core.sourcegear.actors.ActorCluster
import com.opticdev.core.sourcegear.graph.{ProjectGraph, ProjectGraphWrapper}
import com.opticdev.core.sourcegear.project.config.ProjectFile
import com.opticdev.core.sourcegear.project.monitoring.{FileStateMonitor}
import com.opticdev.core.sourcegear.project.status.ProjectStatus
import com.opticdev.opm.providers.ProjectKnowledgeSearchPaths
import net.jcazevedo.moultingyaml.YamlString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

abstract class OpticProject(val name: String, val baseDirectory: File)(implicit actorCluster: ActorCluster) {

  import com.opticdev.core.sourcegear.actors._

  /* Project Actor setup */

  val projectActor: ActorRef = actorCluster.newProjectActor

  /* Private & public declarations of the project status & info */

  protected val projectStatusInstance: ProjectStatus = new ProjectStatus()
  val projectStatus = projectStatusInstance.immutable
  def projectInfo : ProjectInfo = ProjectInfo(name, baseDirectory.pathAsString, projectStatus)


  /* Normal Disk Monitoring */

  val watcher: ActorRef = baseDirectory.newWatcher(recursive = true)

  def projectFileChanged(newPf: ProjectFile) : Unit = {
    projectStatusInstance.touch
    if (newPf.interface.isSuccess) {
      projectStatusInstance.configStatus = ValidConfig
    } else {
      projectStatusInstance.configStatus = InvalidConfig(newPf.interface.failed.get.getMessage)
    }
  }
  val projectFile = new ProjectFile(baseDirectory / "optic.yaml", createIfDoesNotExist = true, onChanged = projectFileChanged)

  def projectSourcegear : SourceGear

  def watch = {
    rereadAll
    watcher ! when(events = EventType.ENTRY_CREATE, EventType.ENTRY_MODIFY, EventType.ENTRY_DELETE)(handleFileChange)
    projectStatusInstance.monitoringStatus = Watching
  }

  def rereadAll = {
    implicit val timeout: akka.util.Timeout = Timeout(5 minutes)
    implicit val sourceGear = projectSourcegear
    //should delete all
    ProjectActorSyncAccess.clearGraph(projectActor)
    ParseSupervisorSyncAccess.clearCache()

    projectStatusInstance.firstPassStatus = InProgress

    val futures = filesToWatch.toSeq.map(i=> {
      projectActor ? FileCreated(i, this)
    }).map(
      _.map(Success(_)).recover { case t => Failure(t) }
    )

    Future.sequence(futures).onComplete(i=> {
      projectStatusInstance.firstPassStatus = Complete
      projectStatusInstance.touch
    })
  }

  val handleFileChange : better.files.FileWatcher.Callback = {
    case (EventType.ENTRY_CREATE, file) => {
      implicit val sourceGear = projectSourcegear
      projectStatusInstance.touch
      filesStateMonitor.markUpdated(file)
      if (shouldWatchFile(file)) projectActor ! FileCreated(file, this)
    }
    case (EventType.ENTRY_MODIFY, file) => {
      implicit val sourceGear = projectSourcegear
      projectStatusInstance.touch
      filesStateMonitor.markUpdated(file)
      if (file === projectFile.file) {
        projectFile.reload
      } else {
        if (shouldWatchFile(file)) projectActor ! FileUpdated(file, this)
      }
    }
    case (EventType.ENTRY_DELETE, file) => {
      implicit val sourceGear = projectSourcegear
      filesStateMonitor.markUpdated(file)
      projectStatusInstance.touch
      if (shouldWatchFile(file)) projectActor ! FileDeleted(file, this)
    }
  }

  def stopWatching = {
    actorSystem.stop(watcher)
    projectStatusInstance.monitoringStatus = NotWatching
  }


  def projectGraph: ProjectGraph = {
    implicit val timeout = Timeout(2 seconds)
    val future = projectActor ? CurrentGraph
    Await.result(future, timeout.duration).asInstanceOf[ProjectGraphWrapper].projectGraph

  }

  /* Staged File Monitor */
  val filesStateMonitor : FileStateMonitor = new FileStateMonitor(shouldWatchFile)
  def stageFileContents(file: File, contents: String): Future[ParseStatus] = {
    implicit val timeout: akka.util.Timeout = Timeout(10 seconds)
    filesStateMonitor.stageContents(file, contents)
    (projectActor ? FileUpdatedInMemory(file, contents, this)(projectSourcegear))
      .map(_.asInstanceOf[ParseStatus])
  }

  /* Control logic for watching files */

  def shouldWatchFile(file: File) : Boolean = {
    file.isRegularFile &&
    file.extension.isDefined &&
    projectSourcegear.validExtensions.contains(file.extension.get)
  }

  def filesToWatch : Set[File] = baseDirectory.listRecursively.toVector.filter(shouldWatchFile).toSet

  /* Output the search paths for MD in project */

  def projectSearchPaths : ProjectKnowledgeSearchPaths = {
    val searchPaths = projectFile.interface
      .map(_.knowledgePaths.value.toVector)
      .getOrElse(Vector())
      .collect {
        case s: YamlString => File(s.value)
      }
      .filter(f=> f.exists && f.isDirectory)

    ProjectKnowledgeSearchPaths(searchPaths:_*)
  }

}