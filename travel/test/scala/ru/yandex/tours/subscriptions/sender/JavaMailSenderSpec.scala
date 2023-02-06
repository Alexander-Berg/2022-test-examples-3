package ru.yandex.tours.subscriptions.sender

import ru.yandex.tours.model.subscriptions.Address
import ru.yandex.tours.testkit.BaseSpec

import scala.language.implicitConversions

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 17.03.16
 */
class JavaMailSenderSpec extends BaseSpec {
  private implicit def address(email: String): Address = Address(email)

  "JavaMailSender" should {
    "escape domain in email" in {
      JavaMailSender.escapeDomain("abc@ede.com").value shouldBe "abc@ede.com"
      JavaMailSender.escapeDomain("алфавит@ede.com").value shouldBe "алфавит@ede.com"
      JavaMailSender.escapeDomain("abc@рус.рф").value shouldBe "abc@xn--p1acf.xn--p1ai"
      JavaMailSender.escapeDomain("алфавит@xn--p1acf.xn--p1ai").value shouldBe "алфавит@xn--p1acf.xn--p1ai"
    }
  }
}
