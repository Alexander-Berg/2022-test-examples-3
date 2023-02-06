
from collections import namedtuple
from mail.pypg.pypg.fake_cursor import FakeCursor
from mail.pypg.pypg import query_handler

import pytest

ABType = namedtuple('ABType', ('a', 'b'))

# pylint: disable=R0201


def test_FetchAs():
    cur = FakeCursor(column_names=['b', 'a'], rows=[[2, 1]])
    fetcher = query_handler.FetchAs(ABType)
    assert fetcher(cur) == [ABType(1, 2)]


class TestFetchHeadExpectOneRowAs(object):
    def test_good_case(self):
        cur = FakeCursor(column_names=['b', 'a'], rows=[[2, 1]])
        fetcher = query_handler.FetchHeadExpectOneRowAs(ABType)
        assert fetcher(cur) == ABType(1, 2)

    def test_raise_when_more_then_one_row(self):
        cur = FakeCursor(column_names=['b', 'a'], rows=[[2, 1], [3, 4]])
        with pytest.raises(query_handler.ExpectOneItemError):
            query_handler.FetchHeadExpectOneRowAs(ABType)(cur)

    def test_raise_when_get_no_rows(self):
        cur = FakeCursor(column_names=['b', 'a'], rows=[])
        with pytest.raises(query_handler.ExpectOneItemError):
            query_handler.FetchHeadExpectOneRowAs(ABType)(cur)


class Test_sync_fetch_as_list(object):
    def test_good_case(self):
        real = query_handler.sync_fetch_as_list(
            FakeCursor(['a'], [[1], [2], [3]])
        )
        assert real == [1, 2, 3]

    def test_raise_when_get_query_with_more_then_one_column(self):
        with pytest.raises(query_handler.QueryHandlerContractViolation):
            query_handler.sync_fetch_as_list(
                FakeCursor(['a', 'b'], [[1, 2]])
            )
