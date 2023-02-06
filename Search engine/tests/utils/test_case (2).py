import logging

from search.martylib.test_utils import TestCase

from search.zephyr.src.clients import Clients
from search.zephyr.tests.utils.database_utils import clear_db


class ZephyrTestCase(TestCase):
    logger = logging.getLogger('zephyr.test')


class ZephyrDBTestCase(ZephyrTestCase):
    @classmethod
    def setUpClass(cls):
        Clients()

    def setUp(self):
        clear_db()
