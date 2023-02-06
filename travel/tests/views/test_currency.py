# -*- coding: utf-8 -*-
import mock
from django.utils import translation

from common.models.currency import Currency
from common.tester.factories import create_currency
from common.tester.testcase import TestCase
from common.views.currency import get_currency_info


class TestGetCurrencyInfo(TestCase):
    def setUp(self):
        create_currency(name=u'рубли', code='RUR', name_in=u'в рублях')
        create_currency(name=u'доллары', code='USD', name_in=u'в долларах')
        create_currency(name=u'турецкие лиры', code='TRY', name_in=u'в турецких лирах')

    def test_valid(self):
        rates = {'TRY': 20.7039, 'UAH': 2.53002, 'USD': 55.0663, 'RUR': 1, 'EUR': 61.7184}
        src = u'Центрального Банка Республики Турции'

        # get_currency_info проверяет наличие переводов для валют на нужном языке,
        # поэтому явно зададим язык с переводами.
        m_fetch_rates = mock.Mock(side_effect=lambda *args: (src, rates))
        with mock.patch.object(Currency, 'fetch_rates', m_fetch_rates), translation.override('ru'):
            tld = 'com.tr'

            info = get_currency_info(tld, 'RUR')
            assert info.selected == 'RUR'

            info = get_currency_info(tld)
            assert info.selected == 'TRY'
            assert set(info.json['available']) == {'RUR', 'USD', 'TRY'}
