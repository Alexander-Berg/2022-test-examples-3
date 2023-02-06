import pytest

from extsearch.ymusic.scripts.reindex.gendocs import TributeClassifier


@pytest.fixture
def versions_file():
    with open('versions.txt', 'w') as f:
        f.write('[TRIBUTE]\ntest_tribute\n[REMIX]\ntest_remix\n')
    return 'versions.txt'


@pytest.fixture
def classifier(versions_file):
    classifier = TributeClassifier()
    classifier.init(versions_file)
    return classifier


def test_tribute(classifier):
    assert classifier.is_tribute(['test_tribute', 'not_tribute'])
    assert not classifier.is_tribute(['remix', 'test_remix', 'not_tribute'])


def test_remix(classifier):
    assert classifier.is_remix(['remix', 'test_remix'])
    assert not classifier.is_remix(['tribute', 'test_tribute', 'remix'])
