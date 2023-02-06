# -*- encoding: utf-8 -*-

from crypta.lib.python.trie import (
    create_trie,
    load_trie,
)

import tempfile


TRIE_DATA = [
    (u'яндекс', (1.0,)),
    (u'рамблер', (2.0,)),
    (u'гугл', (3.0,))
]


def test_creating_trie():
    trie = create_trie(TRIE_DATA)
    assert trie
    assert trie[u'яндекс'] == [(1.0,)]
    assert trie[u'рамблер'] == [(2.0,)]
    assert trie[u'гугл'] == [(3.0,)]
    assert u'яху' not in trie


def test_loading_trie():
    trie = create_trie(TRIE_DATA)

    tmp = tempfile.NamedTemporaryFile()
    trie.save(tmp.name)
    other_trie = load_trie(tmp.name)
    assert other_trie[u'яндекс'] == [(1.0,)]
    assert other_trie[u'рамблер'] == [(2.0,)]
    assert other_trie[u'гугл'] == [(3.0,)]


def test_loading_trie_int():
    trie = create_trie([
        (u'яндекс', (20000123,)),
        (u'рамблер', (20000124,)),
        (u'гугл', (20000125,))
    ], fmt='q')
    tmp = tempfile.NamedTemporaryFile()
    trie.save(tmp.name)
    other_trie = load_trie(tmp.name, fmt='q')
    assert other_trie[u'яндекс'] == [(20000123,)]
    assert other_trie[u'рамблер'] == [(20000124,)]
    assert other_trie[u'гугл'] == [(20000125,)]
