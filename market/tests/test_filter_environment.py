# coding: utf8


import pytest
from market.sre.tools.balancer_regenerate.lib import filter_environment


ENVIRONMENTS = ('testing', 'production')
SECTIONS = ('context', 'servers', 'main')


def test_root_sections():
    """ В корне допускаются только ключи context, servers, main. Остальные игнорируются. """
    for env in ENVIRONMENTS:
        assert filter_environment(
            {
                'context': {'default': {'key': 'value'}},
                'servers': {'default': {'key': 'value'}},
                'main': {'default': {'key': 'value'}},
                'unknown': {'default': {'key': 'value'}},
            },
            env,
            dc='vla',
        ) == {
            'context': {'key': 'value'},
            'servers': {'key': 'value'},
            'main': {'key': 'value'},
        }


def test_merge_env():
    """ Настройки для окружения должны мерджиться с default """
    for section in SECTIONS:
        for env in ENVIRONMENTS:
            assert filter_environment(
                {
                    section: {
                        'default': {
                            'key1': 'default value',
                            'key2': 'default value',
                        },
                        env: {
                            'key1': 'env value',
                        },
                    },
                },
                env,
                dc='vla',
            ) == {
                section: {
                    'key1': 'env value',
                    'key2': 'default value',
                }
            }


def test_merge_with_dc():
    """ Если есть секция @dc, то в этом в дц выбирается она """
    # Выбирается ДЦ-специфичный дефолт
    assert \
        filter_environment(
            {
                'context': {
                    'default': {'key1': 'default value'},
                    'default@vla': {'key1': 'dc value'},
                },
            },
            env='testing',
            dc='vla',
        ) == {
            'context': {'key1': 'dc value'}
        }

    # Выбирается общий дефолт
    assert \
        filter_environment(
            {
                'context': {
                    'default': {'key1': 'default value'},
                    'default@vla': {'key1': 'dc value'},
                },
            },
            env='testing',
            dc='iva',
        ) == {
            'context': {'key1': 'default value'}
        }

    # Выбирается ДЦ-специфичное окружение
    assert \
        filter_environment(
            {
                'context': {
                    'default': {'key1': 'default value'},
                    'testing@vla': {'key1': 'dc value'},
                    'testing': {'key1': 'env value'},
                },
            },
            env='testing',
            dc='vla',
        ) == {
            'context': {'key1': 'dc value'}
        }

    # Выбирается общее окружение
    assert \
        filter_environment(
            {
                'context': {
                    'default': {'key1': 'default value'},
                    'testing@vla': {'key1': 'dc value'},
                    'testing': {'key1': 'env value'},
                },
            },
            env='testing',
            dc='iva',
        ) == {
            'context': {'key1': 'env value'}
        }


def test_mandatory_sections():
    """ Section default is mandatory """
    for env in ENVIRONMENTS:
        with pytest.raises(KeyError):
            filter_environment(
                {
                    'context': {},
                },
                env,
                dc='vla',
            )
