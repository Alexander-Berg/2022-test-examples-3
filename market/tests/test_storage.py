#!/usr/bin/env python
# -*- coding: utf-8 -*-

import logging
import pytest
import os
import yatest.common

from market.idx.pylibrary.dist_storages.local_storage.storage import DistLocalStorage

KEEP_GENERATION = '20200521_1610'
GENERATIONS = {
    KEEP_GENERATION: [
        'book-part-0',
        'book-snippet-0',
        'search-part-0',
        'search-wizard'
    ],
    '20200521_1207': [
        'book-part-1',
        'book-snippet-1',
        'search-part-1',
        'marketkgb'
    ]
}


@pytest.fixture(scope='session')
def dist_home_dir():
    return str(yatest.common.test_output_path())


@pytest.fixture(scope='module')
def make_dists(dist_home_dir):
    for generation_name, dists in GENERATIONS.iteritems():
        generation_path = os.path.join(dist_home_dir, generation_name)
        os.mkdir(generation_path)
        for dist in dists:
            os.mkdir(os.path.join(generation_path, dist))


@pytest.fixture(scope='module')
def storage(make_dists, dist_home_dir):
    dist_storage = DistLocalStorage(
        dists_dir=dist_home_dir,
        log=logging.getLogger('dists_storage_test')
    )
    dist_storage.get_generation_storage(KEEP_GENERATION).mark_last_complete()
    return dist_storage


def test_count_generations(storage):
    '''
    Проверяем, что кол-во поколений в хранилище дистов равно кол-ву поколений на входе
    Не добавляем ничего лишнего
    Иначе сломается функционал, который зависит от итерирования по поколениям
    '''
    assert len(storage.generation_names_list) == len(GENERATIONS)


def test_generation_names(storage):
    '''
    Проверяем, что список названий поколений такой же, как и на входе
    В нём не должно содержаться названий других папок, типа last_complete или др.
    Иначе сломается клинер дистов
    '''
    assert sorted(storage.generation_names_list) == sorted(GENERATIONS.keys())


def test_last_complete_name(storage):
    '''
    Проверяем, что последнее собранное поколение - это то поколение, которое мы указали
    '''
    assert storage.get_last_complete_storage().generation_name == KEEP_GENERATION


def test_crop_generation(storage):
    '''
    Проверяем клинер хранилища дистов. У нас удолжно удалиться всё, кроме тех поколений
    которые мы хотим оставить. Поколения, которые мы хотим оставить мы выбираем сами.
    При этом мы не должны удалить ничего лишнего.
    '''
    storage.crop_dists([KEEP_GENERATION])

    assert [KEEP_GENERATION] == storage.generation_names_list
