from extsearch.ymusic.indexer.libs.python.extensions import LeetExtensions

import pytest


@pytest.fixture(scope='session')
def leet_extender():
    yield LeetExtensions()


@pytest.mark.parametrize('titles,extended_titles', [
    (['title', 'second title'], ['title', 'second title']),
    (['a+b'], ['a+b', 'a plus b']),
    (['734 bag'], ['734 bag', 'tea bag']),
])
def test__leet_extensions__extend_titles(leet_extender, titles, extended_titles):
    assert extended_titles == leet_extender.extend_titles(titles)
