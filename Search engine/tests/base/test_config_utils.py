import logging

from search.martylib.test_utils import TestCase
from search.mon.workplace.src.libs.config_utils import make_intervals

TEST_CONFIG = {
    'intervals': {
        'test': {
            'test_coroutine': 3600,
        }
    }
}


class TestWorkplaceBaseLibs(TestCase):
    logger = logging.getLogger('base_libs.logger')

    def test_make_intervals_without_intervals(self):
        self.assertEqual(make_intervals('test', TEST_CONFIG.pop('intervals'), self.logger, False), tuple(),
                         'config has no intervals section')
