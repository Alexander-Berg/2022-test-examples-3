# -*- coding: utf-8 -*-
import os
import mock

from test.unit.base import NoDBTestCase
from mpfs.config.base import Config
from mpfs.config.errors import ConfigError, ValidationConfigError
from mpfs.config.builder import patch_from_straighten_environment
from mpfs.config.constants import MPFS_ENVIRONMENT_PREFIX


class ConfigTestCase(NoDBTestCase):
    base_conf = {
        'a': {
            'c': {
                'd': 1
            },
            'e': [1, 1],
            'f': True,
            'g': 1
        },
        'b': 1,
    }

    def test_patch(self):
        mpfs_config = Config(self.base_conf)
        patch = {
            'a': {
                'c': {
                    'd': 10
                },
                'e': [10],
                'f': False,
            },
            'b': 10,
        }
        mpfs_config.patch(patch)
        assert mpfs_config['a']['c']['d'] == 10

        assert len(mpfs_config['a']['e']) == 1
        assert mpfs_config['a']['e'][0] == 10

        assert mpfs_config['a']['f'] == False
        assert mpfs_config['a']['g'] == 1
        assert mpfs_config['b'] == 10

    def test_errors(self):
        mpfs_config = Config(self.base_conf)
        # нужен словарь в качестве патча
        self.assertRaises(ConfigError, mpfs_config.patch, ['1'])
        # нельзя добавлять новые поля
        self.assertRaises(ValidationConfigError, mpfs_config.patch, {'z': 1})
        # типы значений должны совпадать
        self.assertRaises(ValidationConfigError, mpfs_config.patch, {'b': True})

    def test_add_new_fields_success(self):
        """Протестировать добавление новых значений.

        Когда в исходном конфиге нет указанного пути.
        """
        config = Config({'a1': 'b1'})
        config.patch({'a2': {'b2': 'c2'}}, allow_new=True)
        assert config == {'a1': 'b1', 'a2': {'b2': 'c2'}}

        config.patch({'a2': {'b3': 'c3'}}, allow_new=True)
        assert config == {'a1': 'b1', 'a2': {'b2': 'c2', 'b3': 'c3'}}

    def test_override_different_types_error(self):
        """Протестировать попытку перезаписать значения разных типов.
        """
        config = Config({'a1': 'b1'})
        with self.assertRaises(ValidationConfigError):
            config.patch({'a1': {'b1': 'd1'}})

    def test_straighten_common(self):
        config = Config({
            'other': '1',
            'bad.delimer': '4',
            'one': {
                'two': 2.0,
                'three': '3',
                'four': {
                    'five': 5,
                }
            }
        })
        result = {
            'my_prefix_bad.delimer': 'bad.delimer str',
            'my_prefix_one_four_five': 'one four five int',
            'my_prefix_one_three': 'one three str',
            'my_prefix_one_two': 'one two float',
            'my_prefix_other': 'other str'
        }
        conf_env = config.get_straighten_paths(prefix='my_prefix_', delimeter='_')
        assert conf_env == result

    def test_straighten_patch_from_environment(self):
        config = Config({
            'other': 1,
            'bad.delimeter': '4',
            'one': {
                'two': 2.0,
                'three': '3',
                'four': {
                    'five': 5.0,
                }
            }
        })
        result = {
            'other': 1,
            'bad.delimeter': '1',
            'one': {
                'two': 2.71,
                'three': 'a',
                'four': {
                    'five': 9999.0,
                }
            }
        }
        new_env = {
            '%sBAD.delimeter' % MPFS_ENVIRONMENT_PREFIX: '1',
            '%sone_two' % MPFS_ENVIRONMENT_PREFIX: '2.71',
            '%sONE_three' % MPFS_ENVIRONMENT_PREFIX: 'a',
            '%sone_four_FIVE' % MPFS_ENVIRONMENT_PREFIX: '9999.0',
        }

        with mock.patch.dict(os.environ, new_env):
            patch_from_straighten_environment(config, True)
            assert config == result

