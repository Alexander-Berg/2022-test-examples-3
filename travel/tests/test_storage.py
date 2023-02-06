# -*- encoding: utf-8 -*-
import pytest

from feature_flag_client import get_storage, Storage, ABFlagsContent

from tests.utils import dump_flag, flag_test_set


@pytest.mark.parametrize('flags,enable1,enable2', [
    ([], False, False),
    (['TEST1'], True, False),
    (['TEST1', 'test2'], True, False),
    (['TEST1', 'TEST2'], True, True),
])
def test_storage__get_flag(m, url, storage, flags, enable1, enable2):
    m.get(url, text=dump_flag(flags))

    assert storage.flag_enabled('TEST1') == enable1
    assert storage.flag_enabled('TEST2') == enable2


def test_storage__get_flag__with_infinit_cache(m, url, storage):
    m.get(url, text=dump_flag(['TEST1']))
    assert storage.flag_enabled('TEST1')

    m.get(url, text=dump_flag([]))
    assert storage.flag_enabled('TEST1')


def test_storage__get_flag__reset_cache(m, url, storage):
    m.get(url, text=dump_flag(['TEST1']))
    assert storage.flag_enabled('TEST1')

    storage.reset_context()
    m.get(url, text=dump_flag(['TEST2']))

    assert not storage.flag_enabled('TEST1')
    assert storage.flag_enabled('TEST2')


def test_storage__get_flag__update_context_after_ttl(m, url, storage, freezer):
    m.get(url, text=dump_flag(['TEST1']))
    assert storage.flag_enabled('TEST1')

    freezer.tick(storage.cache_ttl + 1)

    m.get(url, text=dump_flag([]))
    assert not storage.flag_enabled('TEST1')


@flag_test_set
def test_storage__get_ab_flag(m, url, storage, key, value, expected):
    m.get(url, text=dump_flag(['TEST1'], ['AB_TEST1']))

    storage.reset_context()
    ab_content = ABFlagsContent({key: value})
    assert expected == storage.flag_enabled(key, ab_content)


def test_get_storage():
    storage = get_storage(
        host='example.net',
        service_code='some-service',
    )
    assert isinstance(storage, Storage)
