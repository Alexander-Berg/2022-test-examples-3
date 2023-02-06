import os
import shutil
import unittest

from getter import core
from getter import util
from getter.service import picrobot

import market.idx.pictures.thumbnails as thumbnails

ROOTDIR = os.path.join(os.getcwd(), 'tmp')


def create_picrobot_service():
    service = picrobot.create_picrobot_service()
    root = core.Root(ROOTDIR, create_lazy=False)
    root.register('picrobot', lambda: service)
    return service


class Test(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        util.makedirs(ROOTDIR)

    def test_thumbs(self):
        service = create_picrobot_service()
        service.update_service(names=['picrobot_thumbs.meta'], lazy=False)

        result_path = os.path.join(ROOTDIR, 'picrobot', 'recent', 'picrobot_thumbs.meta')

        with open(result_path) as fn:
            all_thumbnails = fn.read()

        self.assertEqual(thumbnails.get_all_thumbnails(), all_thumbnails)
