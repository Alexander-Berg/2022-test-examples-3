def test_trie_responses_correctly(trie):
    assert set(trie.fetch("")) == set()
    assert set(trie.fetch("/")) == {0}
    assert set(trie.fetch("/a")) == {0, 1}
    assert set(trie.fetch("/ab")) == {0, 1, 2}
    assert set(trie.fetch("/abc")) == {0, 1, 2, 3}
    assert set(trie.fetch("/ab/zzz?p=1")) == {0, 1, 2, 4}
    assert set(trie.fetch(u"/ab/zzz?p=1")) == {0, 1, 2, 4}
    assert set(trie.fetch("/c/d/e")) == {0, 5}
    assert set(trie.fetch("/c")) == {0}
    assert set(trie.fetch("a")) == set()
    assert set(trie.fetch("")) == set()
