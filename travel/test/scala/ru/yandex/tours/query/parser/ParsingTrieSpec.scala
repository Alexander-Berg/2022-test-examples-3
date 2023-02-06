package ru.yandex.tours.query.parser

import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.01.16
 */
class ParsingTrieSpec extends BaseSpec {
  "ParsingTrie" should {
    "parse from file" in {
      val trie = ParsingTrie.fromFile(root / "wizard.reqans.trie")
      println(trie)
    }
  }
}
