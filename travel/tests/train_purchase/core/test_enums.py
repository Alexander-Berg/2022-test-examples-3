# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner, clear_is_partner_active_cache
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory


class TestTrainPartner(object):
    @replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', '  tutu  im ')
    def test_enabled(self):
        assert not TrainPartner.UFS.enabled
        assert TrainPartner.IM.enabled


@pytest.mark.mongouser
@replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', 'im')
@replace_now('2019-12-25')
def test_active_partners_cache():
    """
    Создаем действующий контракат.
    Проверяем, что партнер активный.
    Стираем контракт и проверяем, что данные берутся из кэша.
    Чистим кэш и проверяем, что теперь партнер не активен.
    """
    im_contracts = ClientContractsFactory(updated_at=datetime(2019, 12, 25))

    assert TrainPartner.IM.is_active

    im_contracts.delete()
    assert TrainPartner.IM.is_active

    clear_is_partner_active_cache()
    assert not TrainPartner.IM.is_active


@pytest.mark.mongouser
@replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', 'im')
def test_active_partners__cache_change_date():
    """
    Создаем действующий контракат.
    Проверяем, что партнер активный.
    Стираем контракт и проверяем, что данные берутся из кэша.
    Меняем дату, проверяем, что данные взяты не из кэша и поэтому теперь партнер не активен.
    """
    with replace_now('2019-12-25'):
        im_contracts = ClientContractsFactory(updated_at=datetime(2019, 12, 25))

        assert TrainPartner.IM.is_active

        im_contracts.delete()
        assert TrainPartner.IM.is_active

    with replace_now('2019-12-26'):
        assert not TrainPartner.IM.is_active


@pytest.mark.mongouser
@replace_now('2019-12-25')
def test_active_partners_cache__change_dynamic_setting():
    ClientContractsFactory(updated_at=datetime(2019, 12, 25))

    with replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', 'im'):
        assert TrainPartner.IM.is_active

    with replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', ''):
        assert not TrainPartner.IM.is_active


@pytest.mark.mongouser
@replace_now('2019-12-25')
def test_active_partners_cache__do_not_cache_dynamic_setting():
    ClientContractsFactory(updated_at=datetime(2019, 12, 25))

    with replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', ''):
        assert not TrainPartner.IM.is_active

    with replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', 'im'):
        assert TrainPartner.IM.is_active
