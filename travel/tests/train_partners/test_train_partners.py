# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.train_api.train_partners import get_partner_api, im, ufs
from travel.rasp.train_api.train_purchase.core.models import TrainPartner


def test_get_partner_api():
    assert get_partner_api(TrainPartner.IM) is im
    assert get_partner_api(TrainPartner.UFS) is ufs
