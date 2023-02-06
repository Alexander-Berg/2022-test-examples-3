# coding: utf-8

from __future__ import unicode_literals

import mock
from hamcrest import assert_that, has_property
from hamcrest.library.collection.issequence_containinginanyorder import contains_inanyorder

from common.utils.admindumps import get_latest_dump_url, get_latest_schema_url, get_dump_remote_files


INDEX_HTML = """<html>
    <head><title>Index of /admin/files/</title></head>
    <body bgcolor="white">
    <h1>Index of /admin/files/</h1><hr>
    <pre><a href="../">../</a>
    <a href="public/">public/</a> 11-Jan-2017 04:34
    <a href="bus_station_codes.json">bus_station_codes.json</a> 11-Jan-2017 05:47 3978423
    <a href="dump_manual_main1_rasp_service_db_20161212174305.sql.gz">dump_manual_main1_rasp_service_db_20161212174305.sql.gz</a> 12-Dec-2016 14:50 1799764957
    <a href="dump_20170109080343_switching_main1_rasp_service_db.sql.gz">dump_20170109080343_switching_main1_rasp_service_db.sql.gz</a> 09-Jan-2017 05:12 1986515981
    <a href="dump_switching_main1_rasp_service_db_20170108080343.sql.gz">dump_switching_main1_rasp_service_db_20170108080343.sql.gz</a> 09-Jan-2017 05:12 1986515981
    <a href="schema_20170109080343_switching_main1_rasp_service_db.sql.gz">schema_20170109080343_switching_main1_rasp_service_db.sql.gz</a> 09-Jan-2017 05:12 1986515981
    <a href="schema_20170108080343_switching_main1_rasp_service_db.sql.gz">schema_20170108080343_switching_main1_rasp_service_db.sql.gz</a> 09-Jan-2017 05:12 1986515981
    </pre><hr></body>
</html>"""  # noqa: E501


@mock.patch('requests.get')
def test_get_latest_dump_url(m_requests_get):
    m_requests_get.return_value = mock.Mock(text=INDEX_HTML)

    assert (get_latest_dump_url('https://admin.yandex.ru/') ==
            'https://admin.yandex.ru/dump_20170109080343_switching_main1_rasp_service_db.sql.gz')
    m_requests_get.assert_called_once_with('https://admin.yandex.ru/', timeout=mock.ANY, verify=mock.ANY)


@mock.patch('requests.get')
def test_get_dump_remote_files(m_requests_get):
    m_requests_get.return_value = mock.Mock(text=INDEX_HTML)

    remote_files = get_dump_remote_files('https://admin.yandex.ru/')
    m_requests_get.assert_called_once_with('https://admin.yandex.ru/', timeout=mock.ANY, verify=mock.ANY)

    assert len(remote_files) == 3
    assert_that(remote_files, contains_inanyorder(
        has_property('filename', 'dump_manual_main1_rasp_service_db_20161212174305.sql.gz'),
        has_property('filename', 'dump_20170109080343_switching_main1_rasp_service_db.sql.gz'),
        has_property('filename', 'dump_switching_main1_rasp_service_db_20170108080343.sql.gz')
    ))


@mock.patch('requests.get')
def test_get_latest_schema_url(m_requests_get):
    m_requests_get.return_value = mock.Mock(text=INDEX_HTML)

    assert (get_latest_schema_url('https://admin.yandex.ru/') ==
            'https://admin.yandex.ru/schema_20170109080343_switching_main1_rasp_service_db.sql.gz')
    m_requests_get.assert_called_once_with('https://admin.yandex.ru/', timeout=mock.ANY, verify=mock.ANY)
