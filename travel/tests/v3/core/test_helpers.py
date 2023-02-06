# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from common.models.currency import Currency

from common.tester.factories import create_currency
from common.tester.testcase import TestCase

from travel.rasp.api_public.api_public.v3.core.helpers import get_currency_info


class TestGetCurrencyInfo(TestCase):
    def setUp(self):
        create_currency(name=u'рубли', code='RUR')
        create_currency(name=u'доллары', code='USD')
        create_currency(name=u'турецкие лиры', code='TRY')

    def test_valid(self):
        rates = {'TRY': 20.7039, 'UAH': 2.53002, 'USD': 55.0663, 'RUR': 1, 'EUR': 61.7184}
        src = u'Центрального Банка Республики Турции'

        m_fetch_rates = mock.Mock(side_effect=lambda *args: (src, rates))
        with mock.patch.object(Currency, 'fetch_rates', m_fetch_rates):
            tld = 'com.tr'

            info = get_currency_info(tld, 'RUR')
            assert info.selected == 'RUR'

            info = get_currency_info(tld)
            assert info.selected == 'TRY'
            assert set(info.json['available']) == {'RUR', 'USD', 'TRY'}
