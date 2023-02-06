package ru.yandex.tours.indexer.wizard.parser

import java.io.{File, FileOutputStream}

import org.scalatest.Inspectors
import ru.yandex.tours.extdata.LocalExtDataService
import ru.yandex.tours.query.parser.{LocalParserResources, ParsingTrie}
import ru.yandex.tours.testkit.{BaseSpec, TemporaryDirectory}
import ru.yandex.tours.util.io.ByteBuffers

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.01.16
 */
class ParsingTrieBuilderIntSpec extends BaseSpec with TemporaryDirectory with Inspectors {

  val edl = new LocalExtDataService(root / "wizard")
  val resources = new LocalParserResources(edl)

  "ParsingTrieBuilder" should {
    "build, write and read trie" in {
      val trie = ParsingTrieBuilder.build(resources)
      println(trie)

      val file = tempDir.newFile()
      trie.writeTo(new FileOutputStream(file))

      val buffer = ByteBuffers.mmap(file)
      println("Size = " + buffer.capacity())

      val mappedTrie = ParsingTrie.fromBuffer(buffer)
      println(mappedTrie)

      // checks correct serialization
      /*
      val states = trie.parsingStates.stateIds.toSeq
      println("states = " + states.size)
      var i = 0

      for (stateId <- states) {
        i += 1
        val expected = trie.getPragmatics(stateId, useCache = false).toSeq
        val got = mappedTrie.getPragmatics(stateId, useCache = false).toSeq
        if (i % 1000 == 0) println(s"Processed $i states. Current = $stateId, got = $got, expected = $expected")
        got shouldBe expected
      }
      */
    }
  }
}
