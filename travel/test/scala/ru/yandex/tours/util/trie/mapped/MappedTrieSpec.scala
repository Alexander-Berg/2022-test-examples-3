package ru.yandex.tours.util.trie.mapped

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.trie._
import ru.yandex.tours.util.trie.TrieNode.ManyChildNode

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.01.16
 */
class MappedTrieSpec extends BaseSpec {
  private def node(id: Int, terminal: Boolean, bord: Int, map: Map[Char, Int]) = {
    val (chars, ints) = map.toSeq.sortBy(_._1).unzip
    val charMap = new FastCharMap(chars.toArray, ints.toArray)
    new ManyChildNode(id, terminal, bord, charMap)
  }

  "MappedTrie" should {
    "write empty Trie" in {
      val trie = new ArrayTrie(Array.empty)
      val baos = new ByteArrayOutputStream()
      MappedTrie.writeTo(baos, trie)
      baos.toByteArray.toVector shouldBe Vector(0, 0, 0, 0, 0)
    }

    "write simple Trie" in {
      val node1 = node(id = 0, terminal = false, bord = 1, map = Map.empty)
      val trie = new ArrayTrie(Array(node1))

      val baos = new ByteArrayOutputStream()
      MappedTrie.writeTo(baos, trie)
      val resTrie = new MappedTrie(ByteBuffer.wrap(baos.toByteArray))

      resTrie should have size 1
      resTrie.getRoot should have (
        'stateId (0),
        'terminal (false),
        'bord (1)
      )
    }
    "write simple Trie #2" in {
      val map1 = Map('a' -> 1)
      val map2 = Map('b' -> 5, 'c' -> 6)
      val node1 = node(id = 0, terminal = false, bord = 1, map = map1)
      val node2 = node(id = 1, terminal = true, bord = 5, map = map2)
      val trie = new ArrayTrie(Array(node1, node2))

      val baos = new ByteArrayOutputStream()
      MappedTrie.writeTo(baos, trie)
      val resTrie = new MappedTrie(ByteBuffer.wrap(baos.toByteArray))

      resTrie should have size 2
      resTrie.getRoot should have (
        'stateId (0),
        'terminal (false),
        'bord (1)
      )
      resTrie.getSon(resTrie.getRoot, 'b') shouldBe TrieNode.UNDEFINED
      resTrie.getSon(resTrie.getRoot, 'a') should have (
        'stateId (1),
        'terminal (true),
        'bord (5)
      )
    }
  }
}
