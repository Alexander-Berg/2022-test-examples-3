package ru.yandex.tours.wizard.query

import ru.yandex.tours.query.parser.ParsingTrie
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 29.08.16
 */
class PragmaticsParserSpec extends BaseSpec {
  "PragmaticsParser" should {
    "work" in {
      val trie = ParsingTrie.fromFile(root / "wizard.reqans.trie")
      val parser = new PragmaticsParser(trie)
      val userRequest = "отель radisson royal"
      val parsed = parser.parse(userRequest)

      parsed.userRequest shouldBe "отель radisson royal "
      parsed.containHoles shouldBe false
      parsed.filter(p ⇒ p.startPosition == 0 && p.length == p.userRequest.length).containHoles shouldBe false
    }
  }
}
