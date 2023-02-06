# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base import RzhdStatus


class TestRzhdStatus(object):
    @pytest.mark.parametrize('status, expected', [
        (RzhdStatus.REMOTE_CHECK_IN, True),
        (RzhdStatus.NO_REMOTE_CHECK_IN, True),
        (RzhdStatus.PAYMENT_NOT_CONFIRMED, True),
        (RzhdStatus.RESERVATION, True),
        (RzhdStatus.STRICT_BOARDING_PASS, False),
        (RzhdStatus.PLACES_REFUNDED, False),
        (RzhdStatus.CANCELLED, False),
        (RzhdStatus.REFUNDED, False)
    ])
    def test_is_refundable(self, status, expected):
        assert status.is_refundable() is expected
