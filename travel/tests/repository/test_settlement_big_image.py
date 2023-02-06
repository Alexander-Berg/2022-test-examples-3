# -*- encoding: utf-8 -*-
from __future__ import absolute_import

from datetime import datetime

from mock import Mock

from travel.avia.library.python.common.utils.date import MSK_TZ
from travel.avia.backend.repository.settlement_big_image import SettlementBigImageRepository
from travel.avia.library.python.tester.factories import create_settlement_image
from travel.avia.library.python.tester.testcase import TestCase


class SettlementBigImageRepositoryTest(TestCase):
    def setUp(self):
        self._environment = Mock()
        self._environment.now_aware = Mock(
            return_value=MSK_TZ.localize(datetime(2017, 9, 1))
        )
        self._repo = SettlementBigImageRepository(Mock())

    def test_some(self):
        test_settlement_id = 213
        img = create_settlement_image(settlement_id=test_settlement_id, url2=u'url2_image')

        self._repo.pre_cache()
        m = self._repo.get(test_settlement_id)

        assert m.pk == img.pk
        assert m.url2 == img.url2
        assert m.settlement_id == img.settlement_id
