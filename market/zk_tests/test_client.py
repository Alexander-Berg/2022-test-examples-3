# -*- coding: utf-8 -*-

import pytest
import mock

from async_publishing import (
    CachedClient,
    Client,
)
from async_publishing.generation_meta import (
    GenerationMeta,
    DistGenerationMeta,
)


@pytest.fixture()
def reusable_client(reusable_zk):
    """async_publishing.Client который работает на свежих данных в каждом тесте
    """
    return Client(reusable_zk, '/test_publisher', '/test_publisher/generations')


@pytest.fixture()
def reusable_cached_client(reusable_zk):
    """async_publishing.CachedClient который работает на свежих данных в каждом тесте
    """
    return CachedClient(
        reusable_zk,
        '/test_publisher',
        '/test_publisher/generations',
        cache_size=100,
        cache_ttl=100500
    )


def test_publish_full_generation_conv_from_str(reusable_client):
    """Публикуем полное поклоление и проверяем, что оно опубликовалось.
    NB: На вход функции подается строка, которая должна быть приведена к GenerationMeta
    """
    GENERATION = "20190101_0101"
    assert reusable_client.full_generation is None
    reusable_client.publish_full_generation(GENERATION)
    assert reusable_client.full_generation == GenerationMeta(GENERATION, not_for_publishing=True)


def test_publish_full_generation(reusable_client):
    """Публикуем полное поклоление и проверяем, что оно опубликовалось.
    """
    GENERATION = GenerationMeta("20190101_0101")
    assert reusable_client.full_generation is None
    reusable_client.publish_full_generation(GENERATION)
    assert reusable_client.full_generation == GENERATION


def test_publish_dist_generation(reusable_client):
    """Публикуем дист и проверяем, что он опубликовался.
    """
    DIST_NAME = "search-part-1"
    DIST_GENERATION = DistGenerationMeta("20190101_0101", DIST_NAME, "marketserach3")
    assert reusable_client.dist_generation(DIST_NAME) is None
    reusable_client.publish_dist_generation(DIST_GENERATION)
    assert reusable_client.dist_generation(DIST_NAME) == DIST_GENERATION


@pytest.mark.skip(reason="very flaky")
def test_watch_dist_generation(reusable_client):
    """Проверяем что подписка на новые дисты работает и callback вызывается для:
    1. нового добавленного диста
    2. измененного диста
    """
    callback_mock = mock.Mock()
    reusable_client.watch_new_dist_generations(callback_mock)

    DIST_NAME = "search-part-1"
    DIST_GENERATION = DistGenerationMeta("20190101_0101", DIST_NAME, "marketserach3")
    reusable_client.publish_dist_generation(DIST_GENERATION)
    reusable_client._zk.sync("/test_publisher/generations/dists/search-part-1")
    callback_mock.assert_called_with(DIST_GENERATION)

    DIST_NEW_GENERATION = DistGenerationMeta("20190101_2020", DIST_NAME, "marketserach2")
    reusable_client.publish_dist_generation(DIST_NEW_GENERATION)
    reusable_client._zk.sync("/test_publisher/generations/dists/search-part-1")
    callback_mock.assert_called_with(DIST_NEW_GENERATION)


def test_read_dist_names(reusable_client):
    """Публикуем поколение с дистами и проверяем, что имена дистов считываются корректно.
    """
    GENERATION = GenerationMeta("20190101_0301")
    reusable_client.publish_full_generation(GENERATION)

    DIST_NAMES = {"search-part-1", "search-part-2"}
    for dist_name in DIST_NAMES:
        dist_meta = DistGenerationMeta(GENERATION.name, dist_name, "marketserach3")
        reusable_client.publish_dist_generation(dist_meta)

    actual_names = set(reusable_client.read_dist_names(GENERATION.name))
    assert actual_names == DIST_NAMES


def test_cached_client(reusable_client, reusable_cached_client):
    """Проверяем, что кэширующий клиент работает нормально: создается, кеширует
    данные и отдает по ним закешированную инфу.
    """
    NEW_GENERATION = "20210419_0101"
    PERMANENT_GENERATION = "12340101_0101"
    assert reusable_cached_client.full_generation is None
    assert reusable_client.full_generation is None

    # Создаем какое-то очень старое поколение, оно должно закешироваться.
    reusable_cached_client.publish_full_generation(PERMANENT_GENERATION)
    print(reusable_cached_client.full_generation)
    assert reusable_cached_client.full_generation.name == PERMANENT_GENERATION
    assert reusable_client.full_generation.name == PERMANENT_GENERATION

    # Пишем новое поколение через некеширующий клиент,
    # но в кеширующем закешировано старое и оно должно быть возвращено.
    reusable_client.publish_full_generation(NEW_GENERATION)
    assert reusable_cached_client.full_generation.name == PERMANENT_GENERATION
    assert reusable_client.full_generation.name == NEW_GENERATION


def test_cached_client_read_already_written(reusable_client, reusable_cached_client):
    """Проверяем, что кэширующий клиент корректно считывает данные, которые уже были
    записаны.
    """
    GENERATION = "20210419_0101"
    reusable_client.publish_full_generation(GENERATION)
    assert reusable_client.full_generation.name == GENERATION
    assert reusable_cached_client.full_generation.name == GENERATION
