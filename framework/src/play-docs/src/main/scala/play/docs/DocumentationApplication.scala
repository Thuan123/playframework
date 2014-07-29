/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.docs

import java.io.File
import java.util.concurrent.Callable
import play.api.mvc._
import play.api._
import play.core._
import scala.util.Success

/**
 * Provides a very simple application that renders Play documentation.
 */
case class DocumentationApplication(projectPath: File, buildDocHandler: BuildDocHandler,
    translationReport: Callable[File],
    forceTranslationReport: Callable[File]) extends ApplicationProvider {

  val application = new Application with WithDefaultConfiguration {
    def path = projectPath
    def classloader = this.getClass.getClassLoader
    def sources = None
    def mode = Mode.Dev
    def global = new GlobalSettings() {}
    def plugins = Nil
    override lazy val routes = None
  }

  Play.start(application)

  override def path = projectPath
  override def get = Success(application)
  override def handleWebCommand(request: RequestHeader) =
    buildDocHandler.maybeHandleDocRequest(request).asInstanceOf[Option[Result]].orElse(
      if (request.path == "/@report") {
        if (request.getQueryString("force").isDefined) {
          forceTranslationReport.call()
          Some(Results.Redirect("/@report"))
        } else {
          Some(Results.Ok.sendFile(translationReport.call(), inline = true, fileName = _ => "report.html"))
        }
      } else None
    ).orElse(
        Some(Results.Redirect("/@documentation"))
      )
}
