# -*- coding: utf-8 -*-
from __future__ import absolute_import

import pytest

from travel.avia.library.python.tester.factories import create_partner, create_cpc_price, get_model_factory

from travel.avia.library.python.avia_data.models import NationalVersion
from travel.avia.library.python.common.models.partner import Partner, CPCPrice

from travel.avia.backend.main.api_types.partnersData import (
    PartnersDataHandler,
    _click_prices_by_partner_id,
    _CPCSource,
    _sources,
)


class TestPartnersData:
    @pytest.mark.dbuser
    def test_partners_data(self, faker):
        partner_id = 666
        client_id = 111
        order_id = 222
        click_price = 0.5
        title = 'title'
        site_url = 'http://www.test_is_good.com'
        NV_BY_CODE = {nv.code: nv for nv in NationalVersion.objects.all()}
        nv_click_prices = dict.fromkeys(NV_BY_CODE, click_price)
        nv_e_click_prices = get_e_cpc_values_for_partner(NV_BY_CODE, faker)
        click_prices = dict.fromkeys(map(lambda nv: 'click_price_' + nv, NV_BY_CODE), click_price)

        WIZARD_NV = ('ru', 'ua', 'kz', 'tr')
        enabled_in_wizard = dict.fromkeys(
            map(lambda nv: ('enabled_in_wizard_{}'.format(nv)), WIZARD_NV),
            True,
        )

        enabled_in_wizard_by_nv = dict.fromkeys(WIZARD_NV, True)

        other_arguments = {}
        for dct in (click_prices, enabled_in_wizard):
            other_arguments.update(dct)

        partner = create_partner(
            code='some1',
            title=title,
            site_url=site_url,
            id=partner_id,
            billing_client_id=client_id,
            billing_order_id=order_id,
            click_price=click_price,
            **other_arguments
        )
        other_partner = create_partner(
            code='some2',
            title=title,
            site_url=site_url,
            id=partner_id+1,
            billing_client_id=client_id+1,
            billing_order_id=order_id+1,
            click_price=click_price,
            can_sale_by_installments=True,
            **other_arguments
        )
        for p in [partner, other_partner]:
            for code in NV_BY_CODE:
                e_cpc_values = get_e_cpc_values_for_cpc(
                    national=code,
                    e_click_prices=nv_e_click_prices,
                )
                create_cpc_price(
                    partner=p,
                    national_version=NV_BY_CODE[code],
                    n_redirects=0,
                    cpa_sum=0,
                    **e_cpc_values
                )

        actual_response_content = PartnersDataHandler()(None, None)
        expected = [{
            u'id': partner.id,
            u'billingClientId': partner.billing_client_id,
            u'billingOrderId': partner.billing_order_id,
            u'code': u'some1',
            u'title': partner.title,
            u'siteUrl': partner.site_url,
            u'enabled': True,
            u'billingDatasourceId': None,
            u'clickPrice': click_price,
            u'marker': 'marker',
            u'canSaleByInstallments': False,
        }, {
            u'id': other_partner.id,
            u'billingClientId': other_partner.billing_client_id,
            u'billingOrderId': other_partner.billing_order_id,
            u'code': u'some2',
            u'title': other_partner.title,
            u'siteUrl': other_partner.site_url,
            u'enabled': True,
            u'billingDatasourceId': None,
            u'clickPrice': click_price,
            u'marker': 'marker',
            u'canSaleByInstallments': True,
        }]
        for partner_data in actual_response_content:
            # зачем то отправляет str(float()), поэтому могут быть аномалии,
            # вынесли проверку этого поля отдельно
            actual_click_price = partner_data.pop('clickPriceNational')
            actual_e_click_price = partner_data.pop('eClickPriceNational')
            assert {k: float(v) for k, v in actual_click_price.items()} == nv_click_prices
            assert {k: float(v) for k, v in actual_e_click_price.items()} == nv_e_click_prices
            assert partner_data.pop('enabledInWizard') == enabled_in_wizard_by_nv
            assert partner_data in expected


def get_e_cpc_values_for_cpc(national, e_click_prices):
    return {
        source.get_field_name():
            e_click_prices[source.get_api_field_name(national)]
        for source in _sources
    }


def get_e_cpc_values_for_partner(national_codes, faker):
    return {
        source.get_api_field_name(national): faker.pyint()
        for source in _sources
        for national in national_codes
    }


class Test_click_price_by_partner_id:

    @pytest.mark.dbuser
    def test__only_default_cpc__for_compatibility(self, faker):
        cpc = create_cpc(faker)
        result_cpc = _click_prices_by_partner_id()

        assert cpc.partner.id in result_cpc
        assert cpc.national_version.code in result_cpc[cpc.partner.id]
        assert cpc.eCPC == result_cpc[cpc.partner.id][cpc.national_version.code]

    @pytest.mark.dbuser
    @pytest.mark.parametrize('source', [
        'rasp_direct',
        'sovetnik_direct',
        'wizard_direct',
        'wizard_indirect',
    ])
    def test__check_cpc_field__in_returned(self, source, faker):
        source = _CPCSource(source)
        cpc = create_cpc(faker, **{source.get_field_name(): faker.pyint()})
        result_cpc = _click_prices_by_partner_id()

        assert cpc.partner.id in result_cpc

        source_field = source.get_api_field_name(cpc.national_version.code)
        assert source_field in result_cpc[cpc.partner.id]
        assert source.get_cpc_value(cpc) == result_cpc[cpc.partner.id][source_field]


def create_cpc(faker, **kwargs):
    _click_prices_by_partner_id.cache_clear()
    partner = get_model_factory(Partner)()
    national_version = get_model_factory(NationalVersion)(code=faker.pystr(max_chars=3))
    return get_model_factory(CPCPrice)(
        partner=partner,
        national_version=national_version,
        n_redirects=0,
        cpa_sum=0,
        eCPC=0,
        **kwargs
    )
