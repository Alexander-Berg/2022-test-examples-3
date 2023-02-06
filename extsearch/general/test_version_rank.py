import extsearch.ymusic.scripts.reindex.gendocs as gd

import pytest


@pytest.fixture(scope='session')
def version_markers():
    yield gd.VersionMarkers.create()


@pytest.mark.parametrize(
    ('version', 'rank'),
    [
        ('remix', 0),
        ('remastered', 0.75),
        ('original', 1),
        ('album', 1),
        ('album mix', 0),
        ('some version', 0.5),
    ]
)
def test__get_version_rank__parametrized(version_markers, version, rank):
    assert rank == version_markers.get_version_rank(version, compilation=False)


def test__get_version_rank__compilation(version_markers):
    assert 0 == version_markers.get_version_rank('', compilation=True)
    assert 0 == version_markers.get_version_rank('remastered', compilation=True)


def test__get_version_rank__no_version(version_markers):
    assert 0.5 == version_markers.get_version_rank(None, compilation=False)
    assert 0.5 == version_markers.get_version_rank('', compilation=False)
