from unittest import TestCase

from travel.avia.admin.avia_scripts.update_heater_conf import merge_configs


class TestMergeConfigs(TestCase):
    def test(self):
        old_configs = [
            {'code_from': 1, 'code_to': 2},
            {'code_from': 1, 'code_to': 3},
        ]

        new_configs = [
            {'code_from': 1, 'code_to': 2},
            {'code_from': 1, 'code_to': 4},
            {'code_from': 1, 'code_to': 5},
        ]

        expected = [
            {'code_from': 1, 'code_to': 2},
            {'code_from': 1, 'code_to': 3},
            {'code_from': 1, 'code_to': 4},
        ]

        assert expected == merge_configs(old_configs, new_configs, 3)
