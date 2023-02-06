# -*- coding: utf-8 -*-

from travel.avia.ticket_daemon.ticket_daemon.partners import ctrip3
from travel.avia.ticket_daemon.tests.partners.helper import get_query


def test_ctrip_tr_national_version():
    test_query = get_query(lang='en', national_version='tr')
    request_data = ctrip3.request_json(test_query)
    expected = {
        "Currency": "TRY",
        "Language": "EN",
        "Site": "TRSite",
        "Country": "TR"
    }

    for key, value in expected.iteritems():
        assert request_data['Head'][key] == value


def test_ctip_com_national_version():
    test_query = get_query(lang='de', national_version='com')
    request_data = ctrip3.request_json(test_query)
    expected = {
        "Currency": "EUR",
        "Language": "DE",
        "Site": "EnglishSite",
        "Country": "DE"
    }

    for key, value in expected.iteritems():
        assert request_data['Head'][key] == value


def test_ctip_kz_national_version():
    test_query = get_query(lang='ru', national_version='kz')
    request_data = ctrip3.request_json(test_query)
    expected = {
        "Currency": "KZT",
        "Language": "RU",
        "Site": "RUSite",
        "Country": "KZ"
    }

    for key, value in expected.iteritems():
        assert request_data['Head'][key] == value
