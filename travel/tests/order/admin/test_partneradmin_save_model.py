# -*- coding: utf-8 -*-
import pytest
import mock
from django.contrib.admin.sites import site
from django.test.client import RequestFactory

from common.models.partner import Partner
from common.settings import WorkInstance
from order.admin import _get_billing_datasource_id_by_order_id, PartnerAdmin
from tester.factories import create_partner
from tester.utils.replace_setting import replace_setting


BILLING_DATASOURCE_ID = 34
BILLING_ORDER_ID = 1


@pytest.mark.dbuser
@replace_setting('ADMIN_TASK_RUN_IN_SEPARATE_PROCESS', False)
@replace_setting('INSTANCE_ROLE', WorkInstance)
def test_PartnerAdmin_save_model(superuser, tmpdir):
    partner = create_partner(code='one')

    def billing_datasource_id_by_order_id(billing_order_id, env_type, logger):
        return BILLING_DATASOURCE_ID

    assert partner.billing_order_id != BILLING_ORDER_ID
    assert partner.billing_datasource_id != BILLING_DATASOURCE_ID

    with mock.patch('order.admin._get_billing_datasource_id_by_order_id', side_effect=billing_datasource_id_by_order_id) \
            as mock_of_get_billing_datasource_id_by_order_id:
        PartnerAdmin.message_user = mock.Mock()
        partner_model_admin = PartnerAdmin(Partner, admin_site=site)
        request = RequestFactory().post('/admin/order/partner/{}/'.format(partner.pk))
        request.user = superuser
        ModelForm = partner_model_admin.get_form(request, Partner)
        form = ModelForm(
            {
                'code': 'one',
                'title': 'One',
                'billing_order_id': BILLING_ORDER_ID,
                'click_price': 0,
                'click_price_com': 0,
                'click_price_ru': 0,
                'click_price_tr': 0,
                'click_price_ua': 0,
                'current_balance': 0,
                'review_percent': 0,
                'site_url': 'whatever',
                't_type': 1,
            },
            request.FILES,
            instance=partner
        )
        assert form.is_valid()
        partner_model_admin.save_model(request, partner, form, change=True)
        mock_of_get_billing_datasource_id_by_order_id.assert_called_once_with(
            BILLING_ORDER_ID, mock.ANY, mock.ANY
        )

    p2 = Partner.objects.get(code='one')
    assert p2.billing_order_id == BILLING_ORDER_ID
    assert p2.billing_datasource_id == BILLING_DATASOURCE_ID


BILLING_XML = """<?xml version="1.0" encoding="UTF-8"?>
<data servant="cs-billing-api" version="1.0-81" host="mbi01e" actions="[getCampaign]"  executing-time="[12]" >
<campaign-info><agency-id>0</agency-id>
<campaign-id>48</campaign-id> <client-id>1143179</client-id> <datasource-id>47</datasource-id>
<service-id>114</service-id><tariff-id>3</tariff-id></campaign-info></data>"""


def test_get_billing_datasource_id_by_order_id():
    with mock.patch('requests.get', return_value=mock.Mock(content=BILLING_XML)):
        billing_datasource_id = _get_billing_datasource_id_by_order_id(
            billing_order_id=1, env_type='testing', logger=mock.Mock()
        )
    assert billing_datasource_id == 47
