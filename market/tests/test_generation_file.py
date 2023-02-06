# coding: utf-8

import pytest

from market.idx.pylibrary.s3_awaps.s3_awaps.awaps_uploader import GenerationsFile

from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import s3_client, BUCKET_NAME

assert s3_client


@pytest.fixture()
def generation_file(s3_client):
    yield GenerationsFile(s3client=s3_client, bucket=BUCKET_NAME, path='generations')


def test_append(generation_file):
    generation = '20170817_0101'
    generation_file.append_generation(generation)

    assert generation in generation_file.generations


def test_remove(generation_file):
    generation = '20170817_0102'
    generation_file.append_generation(generation)
    generation_file.remove_generation(generation)

    assert generation not in generation_file.generations


def test_twice_append(generation_file):
    generation = '20170817_0101'
    generation_file.append_generation(generation)
    generation_file.append_generation('20170817_0102')
    generation_file.append_generation(generation)

    assert len(generation_file.generations) == 2


def test_clean(generation_file):
    generation_file.append_generation('20170817_0101')
    generation_file.append_generation('20170817_0102')
    generation_file.append_generation('20170817_0103')
    generation_file.clean()

    assert len(generation_file.generations) == 0


def test_sync(generation_file):
    # arrange
    generation_file.append_generation('20170817_0101')
    generation_file.append_generation('20170817_0102')
    generation_file.append_generation('20170817_0103')

    # act
    generation_file.sync(['20170817_0102', '20170817_0103', '20170817_0104'])

    # assert
    assert generation_file.generations == ['20170817_0102', '20170817_0103']
