from unittest import TestCase

from lib import common


class TestCommon(TestCase):
    def test_union_command_and_parameters(self):
        self.assertEqual('git log', common.union_command_and_parameters('git', 'log'))
        self.assertEqual('git log -n 4', common.union_command_and_parameters('git', 'log -n 4'))
        self.assertEqual('git log -n 4', common.union_command_and_parameters('git', ['log', '-n', '4']))

        self.assertListEqual(['git', 'log'], common.union_command_and_parameters(['git'], ['log']))
        self.assertListEqual(['git', 'log', '-n', '4'], common.union_command_and_parameters(['git'], 'log -n 4'))
        self.assertListEqual(['git', 'log', '-n', '4'],
                             common.union_command_and_parameters(['git'], ['log', '-n', '4']))
