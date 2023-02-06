package ru.yandex.tours.testkit

import java.io.File

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OptionValues, TryValues, WordSpecLike}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 28.04.15
 */
trait BaseSpec extends WordSpecLike with Matchers with MockitoSugar with TryValues with OptionValues
  with ScalaFutures with IntegrationPatience {

  val root = new File("tours-data/data")

  implicit class RichFile(file: File) {
    def / (child: String): File = new File(file, child)
  }
}
