# -*- coding: utf-8 -*-
from datetime import datetime, date

import mock
import pytest

from common.models.currency import Currency
from common.models.transport import TransportType
from common.views.currency import fetch_currency_info

from common.tester.factories import create_currency
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.api_public.old_versions.core.api_errors import ApiError
from travel.rasp.api_public.api_public.old_versions.core.helpers import (
    get_currency_info, get_transport_types, check_date_range, get_date_from_request
)
from travel.rasp.api_public.tests.old_versions.factories import create_request


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

            # Проверяем, что для наших кейсов get_currency_info работает так же, как fetch_currency_info,
            # т.к. мы заменяем одно другим в уже работающем коде
            # TODO: выпилить, когда пройдет проверку верменем
            request = create_request(tld=tld, NATIONAL_VERSION='tr')
            fetch_info = fetch_currency_info(request)

            assert info.json == fetch_info.json


class TestTransportTypes(TestCase):
    def test_get_transport_types(self):
        t_type_codes = ['bus', 'suburban']
        request = create_request(GET={'transport_types': ','.join(t_type_codes)})
        t_types = get_transport_types(request)
        assert set(t_types) == set(TransportType.objects.filter(code__in=t_type_codes))

        water_type_codes = ['sea', 'river', 'water']
        for water_type_code in water_type_codes:
            request = create_request(GET={'transport_types': water_type_code})
            t_types = get_transport_types(request)
            assert set(t_types) == set(TransportType.objects.filter(code__in=water_type_codes))

        TransportType.objects.get(code='sea').delete()
        request = create_request(GET={'transport_types': water_type_code})
        t_types = get_transport_types(request)
        assert set(t_types) == set(TransportType.objects.filter(code__in=water_type_codes[1:]))


class TestDateRange(TestCase):
    @replace_now('2000-02-02 00:00:00')
    def test_check_date_range(self):
        date = datetime(year=2000, month=1, day=2).date()
        with pytest.raises(ApiError):
            check_date_range(date)

        date = datetime(year=2001, month=1, day=3).date()
        with pytest.raises(ApiError):
            check_date_range(date)


class TestGetDateFromRequest(object):
    @replace_now('2000-01-10 00:00:00')
    def test_get_date_from_request(self):
        with pytest.raises(ApiError):
            request = create_request(GET={'date': '2000.01.01'})
            obtained_date = get_date_from_request(request)

        request = create_request(GET={'date': '2000-01-01'})
        obtained_date = get_date_from_request(request)
        assert obtained_date == date(2000, 01, 01)

        request = create_request()
        obtained_date = get_date_from_request(request)
        assert obtained_date is None

        default_date = date(2000, 01, 01)
        obtained_date = get_date_from_request(request, default=default_date)
        assert obtained_date == default_date
