package ru.yandex.tours.testkit

import java.lang.annotation.Annotation
import java.util.UUID

import org.scalatest.Reporter
import org.scalatest.events.{Event, _}
import ru.yandex.qatools.allure.Allure
import ru.yandex.qatools.allure.events._
import ru.yandex.qatools.allure.utils.AnnotationManager

class AllureReporter extends Reporter {

  private var lc = Allure.LIFECYCLE

  private val suiteIDToUUIDMap = scala.collection.mutable.HashMap[String, String]()

  def apply(event: Event) = event match {
    case TestStarting(_, _, suiteId, _, testName, _, _, location, _, _, _, _) => testCaseStarted(suiteId, testName, location)

    case TestSucceeded(_, _, _, _, _, _, _, _, _, _, _, _, _, _) => testCaseFinished()

    case TestFailed(_, message, _, _, _, _, _, _, throwable, _, _, _, _, _, _, _) => testCaseFailed(throwable match {
      case Some(t) => t
      case None => new RuntimeException(message)
    })

    case TestIgnored(_, _, _, _, _, _, _, _, _, _, _) => testCaseCanceled(None)

    case TestCanceled(_, _, _, _, _, _, _, _, throwable, _, _, _, _, _, _, _) => testCaseCanceled(throwable)

    case TestPending(_, _, _, _, _, _, _, _, _, _, _, _, _) => testCasePending()

    case SuiteStarting(_, _, suiteId, _, _, location, _, _, _, _) => testSuiteStarted(getSuiteUuid(suiteId), suiteId, location)

    case SuiteCompleted(_, _, suiteId, _, _, _, _, _, _, _, _) => testSuiteFinished(getSuiteUuid(suiteId))

    case _ => ()
  }

  def getSuiteUuid(suiteId: String): String = suiteIDToUUIDMap.get(suiteId) match {
    case Some(uuid) => uuid
    case None =>
      val uuid = UUID.randomUUID().toString
      suiteIDToUUIDMap += suiteId -> uuid
      uuid
  }

  def lifecycle = lc

  def setLifecycle(lifecycle: Allure) {
    lc = lifecycle
  }

  private def testSuiteStarted(uuid: String, suiteId: String, location: Option[Location]) {
    val event = new TestSuiteStartedEvent(uuid, suiteId)
    val annotationManager = new AnnotationManager(getAnnotations(location):_*)
    annotationManager.update(event)
    lifecycle.fire(event)
  }

  private def testSuiteFinished(uuid: String) {
    lifecycle.fire(new TestSuiteFinishedEvent(uuid))
  }

  private def testCaseStarted(suiteId: String, testName: String, location: Option[Location]) {
    val uuid = getSuiteUuid(suiteId)
    val event = new TestCaseStartedEvent(uuid, testName)
    val annotationManager = new AnnotationManager(getAnnotations(location):_*)
    annotationManager.update(event)
    lifecycle.fire(event)
  }

  private def testCaseFinished() {
    lifecycle.fire(new TestCaseFinishedEvent())
  }

  private def testCaseFailed(throwable: Throwable) {
    lifecycle.fire(new TestCaseFailureEvent().withThrowable(throwable))
  }

  private def testCaseCanceled(throwable: Option[Throwable]) {
    lifecycle.fire(throwable match {
      case Some(t) => new TestCaseCanceledEvent().withThrowable(t)
      case None => new TestCaseCanceledEvent()
    })
  }

  private def testCasePending() {
    lifecycle.fire(new TestCasePendingEvent())
  }

  def getAnnotations(location: Option[Location]): List[Annotation] = location match {
    case Some(ln) => ln match {
      case TopOfClass(className) => Class.forName(className).getAnnotations.toList
      case TopOfMethod(className, methodName) => Class.forName(className).getMethod(methodName).getDeclaredAnnotations.toList
      case _ => List()
    }
    case _ => List()
  }

}