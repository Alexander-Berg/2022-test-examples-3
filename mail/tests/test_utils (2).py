# encoding: utf-8
from __future__ import unicode_literals

from logbroker_client_common.utils import chunk_it


def test_chunk_it():
    result = chunk_it(range(10), 3)

    assert list(result) == [[0, 1, 2], [3, 4, 5], [6, 7, 8], [9]]
