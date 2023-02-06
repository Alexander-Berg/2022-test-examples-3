# -*- coding: utf-8 -*-

import mock
import urllib

from mpfs.common.util.public_urls import any_url_to_private_hash


def test_any_url_to_private_hash():
    HASH = 'RZof0sWwbPYDnrz1x+rJYT2pJP03SByhS1QkOZU3tI='
    path = '/path/to the/res?o%2f2f&ur+c'
    cases = [
        (HASH, HASH, None),
        (HASH + ':' + path, HASH, path),

        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH), HASH, None),
        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH) + '&abc=def', HASH, None),
        ('https://yadi.sk/public/?hash=' + urllib.quote(HASH) + '&abc=def', HASH, None),

        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH + ':' + path), HASH, path),
        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH + ':/'), HASH, '/'),
        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH + ':' + path) + '&abc=def', HASH, path),

        ('https://yadi.sk/i/1s7-C6gc3Tqx3d', 'HASH1', None),
        ('https://yadi.sk/i/1s7-C6gc3Tqx3d?abc=def', 'HASH1', None),

        ('https://yadi.sk/i/1s7-C6gc3Tqx3d' + urllib.quote(path), 'HASH1', path),
        ('https://yadi.sk/i/1s7-C6gc3Tqx3d' + urllib.quote(path) + '?abc=def', 'HASH1', path),
        ('https://yadi.sk/i/1s7-C6gc3Tqx3d/', 'HASH1', '/'),
        ('https://yadi.sk/i/1s7-C6gc3Tqx3d/a', 'HASH1', '/a'),
    ]

    def clck_short_url_to_public_hash_mock(s):
        return {
            'https://yadi.sk/i/1s7-C6gc3Tqx3d': 'HASH1',
            'https://yadi.sk/i/1s7-C6gc3Tqx3d/': 'HASH1',
            'http://dummy.ya.net/d/5e89a7dd-7d05-4e87-bfa5-2269af0c33c1': 'HASH2'
        }[s]

    with mock.patch('mpfs.core.services.clck_service.Clck.short_url_to_public_hash',
                    side_effect=clck_short_url_to_public_hash_mock):
        for param_value, expected_hash, expected_path in cases:
            assert (expected_hash, expected_path) == any_url_to_private_hash(param_value, split_url=True)


def test_any_url_to_private_hash_without_split():
    HASH = 'RZof0sWwbPYDnrz1x+rJYT2pJP03SByhS1QkOZU3tI='
    path = '/path/to the/res?o%2f2f&ur+c'
    cases = [
        (HASH, HASH),
        (HASH + ':' + path, HASH + ':' + path),
        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH), HASH),
        ('https://disk.yandex.ru/public/?hash=' + urllib.quote(HASH + ':' + path), HASH + ':' + path)
    ]

    for param_value, expected in cases:
        assert expected == any_url_to_private_hash(param_value)
