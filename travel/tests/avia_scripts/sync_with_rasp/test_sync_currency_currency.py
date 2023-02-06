# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from django.utils.encoding import force_text
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.library.python.dicts.currency_repository import CurrencyRepository
from travel.library.python.dicts.factories.currency import TCurrencyFactory
from travel.avia.admin.avia_scripts.sync_with_rasp.sync_currency_currency import sync_currency, MIN_NOT_RASP_CURRENCY_ID
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.tester.factories import create_currency

pytestmark = [pytest.mark.dbuser]


def test_sync_currency():
    assert Currency.objects.all().count() == 0
    not_in_rasp_currency = create_currency(id=10001, code='AAA')
    create_currency(id=100, code='RUR')

    currency_repository = CurrencyRepository()
    rasp_rur = TCurrencyFactory(Id=100, Code='RUR')
    rasp_xxx = TCurrencyFactory(Id=200, Code='XXX')
    currency_repository.add_objects([rasp_rur, rasp_xxx])

    sync_currency(currency_repository)

    db_currencies = list(Currency.objects.all())
    assert len(db_currencies) == 3
    assert_that(db_currencies, contains_inanyorder(
        has_properties(id=not_in_rasp_currency.id),
        _has_properties_by_rasp_currency(rasp_rur),
        _has_properties_by_rasp_currency(rasp_xxx),
    ))


def _has_properties_by_rasp_currency(rasp_currency):
    return has_properties(
        id=rasp_currency.Id,
        code=rasp_currency.Code,
        iso_code=rasp_currency.IsoCode,

        name=force_text(rasp_currency.Title.Ru),
        name_in=force_text(rasp_currency.TitleIn.Ru),
        name_uk=force_text(rasp_currency.Title.Uk),
        name_uk_in=force_text(rasp_currency.TitleIn.Uk),
        name_tr=force_text(rasp_currency.Title.Tr),

        template=force_text(rasp_currency.Template.Ru),
        template_whole=force_text(rasp_currency.TemplateWhole.Ru),
        template_cents=force_text(rasp_currency.TemplateCents.Ru),
        template_tr=force_text(rasp_currency.Template.Tr),
        template_whole_tr=force_text(rasp_currency.TemplateWhole.Tr),
        template_cents_tr=force_text(rasp_currency.TemplateCents.Tr),

        order=rasp_currency.Order,
        order_tr=rasp_currency.OrderTr,
        order_ua=rasp_currency.OrderUa,
    )


def test_sync_by_key():
    create_currency(id=100500, code='XXX')
    currency_repository = CurrencyRepository()
    currency_repository.add_object(TCurrencyFactory(Id=200300, Code='XXX'))

    sync_currency(currency_repository)

    assert not Currency.objects.filter(id=100500).exists()
    assert Currency.objects.filter(id=200300, code='XXX').exists()


@pytest.mark.parametrize('id, expected_same_id', (
    (MIN_NOT_RASP_CURRENCY_ID - 200, False),
    (MIN_NOT_RASP_CURRENCY_ID - 1, False),
    (MIN_NOT_RASP_CURRENCY_ID, True),
    (MIN_NOT_RASP_CURRENCY_ID + 1, True),
    (MIN_NOT_RASP_CURRENCY_ID + 200, True),
))
def test_not_in_rasp(id,  expected_same_id):
    create_currency(id=id, name='Not Rasp Currency')

    sync_currency(CurrencyRepository())

    currency = Currency.objects.get(name='Not Rasp Currency')
    if expected_same_id:
        assert currency.id == id
    else:
        assert currency.id != id


def test_shift_more_than_one_currency():
    create_currency(id=MIN_NOT_RASP_CURRENCY_ID - 1, name='Not Rasp Currency 1')
    create_currency(id=MIN_NOT_RASP_CURRENCY_ID - 2, name='Not Rasp Currency 2')

    sync_currency(CurrencyRepository())

    assert Currency.objects.filter(name__startswith='Not Rasp Currency').count() == 2
