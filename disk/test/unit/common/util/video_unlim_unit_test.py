# coding=utf-8
import unittest

import mock
from nose_parameterized import parameterized

from mpfs.common.util.video_unlim import user_unlim_experiment_data
from mpfs.core.metastorage.control import disk_info


EXPERIMENTS = [{
    'CONTEXT': {
        'DISK': {
            'flags': ['disk_forbidden_video_unlim'], 'testid': ['229827']
        }
    },
    'HANDLER': 'DISK'
}]


class VideoUnlimUnitTestCase(unittest.TestCase):

    @parameterized.expand([
        ('RU', []),
        ('RU', EXPERIMENTS),
        ('TR', []),
    ])
    def test_is_user_in_forbidden_video_unlim_experiment(self, country, exps):
        with mock.patch('mpfs.common.util.video_unlim.get_user_country', return_value=country), \
             mock.patch('mpfs.core.services.uaas_service.new_uaas.get_disk_experiments', return_value=exps):
            assert user_unlim_experiment_data("0")[0]
