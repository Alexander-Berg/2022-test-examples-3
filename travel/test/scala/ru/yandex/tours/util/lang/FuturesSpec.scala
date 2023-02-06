package ru.yandex.tours.util.lang

import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.testkit.BaseSpec

import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 17.06.15
 */
class FuturesSpec extends BaseSpec with ScalaFutures {
  val unit = Unit.box(())

  class TestException extends Exception

  "Futures.all" should {
    "return completed future on empty list" in {
      val f = Futures.all(Seq.empty)
      f shouldBe 'completed
      f.futureValue shouldBe unit
    }
    "complete after all futures" in {
      val p1 = Promise.apply[Int]()
      val p2 = Promise.apply[Int]()
      val p3 = Promise.apply[Int]()

      val all = Futures.all(Seq(p1.future, p2.future, p3.future))
      all.isReadyWithin(10.millis) shouldBe false

      p1.success(1)
      all.isReadyWithin(10.millis) shouldBe false

      p2.success(2)
      all.isReadyWithin(10.millis) shouldBe false

      p3.success(3)
      all.isReadyWithin(10.millis) shouldBe true

      all shouldBe 'completed
      all.futureValue shouldBe unit
    }
    "fail if one of the futures failed" in {
      val p1 = Promise.apply[Int]()
      val p2 = Promise.apply[Int]()
      val all = Futures.all(Seq(p1.future, p2.future))
      all.isReadyWithin(10.millis) shouldBe false

      val ex = new RuntimeException("tst")
      p2.failure(ex)
      all.failed.isReadyWithin(10.millis) shouldBe true

      all shouldBe 'completed
      all.failed.futureValue should have message "tst"
    }
  }

  "Futures.first" should {
    "return completed future with None on empty list" in {
      val f = Futures.first[Int](Seq.empty)(_ > 0)
      f shouldBe 'completed
      f.futureValue shouldBe None
    }
    "complete with Some if result matches predicate" in {
      val p = Promise.apply[Int]()
      val f = Futures.first(Seq(p.future))(_ > 0)
      f.isReadyWithin(10.millis) shouldBe false

      p.success(1)
      f.isReadyWithin(10.millis) shouldBe true

      f shouldBe 'completed
      f.futureValue shouldBe Some(1)
    }
    "complete with None if result not matches predicate" in {
      val p = Promise.apply[Int]()
      val f = Futures.first(Seq(p.future))(_ > 0)
      f.isReadyWithin(10.millis) shouldBe false

      p.success(-10)
      f.isReadyWithin(10.millis) shouldBe true

      f shouldBe 'completed
      f.futureValue shouldBe None
    }
    "skip failed futures" in {
      val p1 = Promise.apply[Int]()
      val p2 = Promise.apply[Int]()
      val f = Futures.first(Seq(p1.future, p2.future))(_ > 0)
      f.isReadyWithin(10.millis) shouldBe false

      p1.failure(new RuntimeException("ex"))
      f.isReadyWithin(10.millis) shouldBe false

      p2.success(2)
      f.isReadyWithin(10.millis) shouldBe true

      f shouldBe 'completed
      f.futureValue shouldBe Some(2)
    }
    "ignore order of completeness" in {
      val p1 = Promise.apply[Int]()
      val p2 = Promise.apply[Int]()
      val f = Futures.first(Seq(p1.future, p2.future))(_ > 0)
      f.isReadyWithin(10.millis) shouldBe false

      p2.success(2)
      f.isReadyWithin(10.millis) shouldBe false

      p1.success(1)
      f.isReadyWithin(10.millis) shouldBe true

      f shouldBe 'completed
      f.futureValue shouldBe Some(1)
    }
  }
  "Futures.join" should {
    "completes when all futures in sequence will be completed" in {
      val p1 = Promise[Int]()
      val p2 = Promise[Int]()
      val p3 = Promise[Int]()

      val joined = Futures.join(Seq(p1.future, p2.future, p3.future))

      p1.success(1)
      p2.success(2)

      joined.isReadyWithin(100.millis) shouldBe false

      p3.success(3)
      joined.isReadyWithin(1.second)
      joined.futureValue shouldBe unit
    }

    "contains exception if any future in sequence completed with exception" in {
      val p1 = Promise[Int]()
      val p2 = Promise[Int]()

      val joined = Futures.join(Seq(p1.future, p2.future))

      joined.isReadyWithin(100.millis) shouldBe false

      p2.failure(new TestException)
      joined.failed.futureValue shouldBe a[TestException]
    }
  }
}
