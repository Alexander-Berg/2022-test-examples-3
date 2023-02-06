import pytest

import extsearch.ymusic.scripts.reindex.classifiers as classifiers


@pytest.fixture()
def soundtrack_classifier():
    yield classifiers.SoundtrackClassifier()


@pytest.mark.parametrize('versions,genres,message', [
    ([], ['soundtrack'], 'Should match by "soundtrack" genre'),
    ([], ['videogame'], 'Should match by "videogame" genre'),
    (['Something from Motion Picture'], [], 'Should match by version 1'),
    (['Something is soundtrack'], [], 'Should match by version 2'),
    (['Something is video game'], [], 'Should match by version 3'),
])
def test__soundtrack_classifier__matched(soundtrack_classifier, versions, genres, message):
    context = classifiers.SoundtrackClassifierContext(
        versions=versions,
        genres=genres,
    )

    assert soundtrack_classifier.is_matched(context), message


@pytest.mark.parametrize('versions,genres,message', [
    ([], [], 'Should not match empty'),
    ([], ['rock', 'rap', 'soundtrack_fake'], 'Should not match some other genres'),
    (
        ['some version', 'some other version', 'not a desired version'],
        [],
        'Should not match non-soundtrack versions'
    )
])
def test__soundtrack_classifier__not_matched(soundtrack_classifier, versions, genres, message):
    context = classifiers.SoundtrackClassifierContext(
        versions=versions,
        genres=genres,
    )

    assert not soundtrack_classifier.is_matched(context), message
