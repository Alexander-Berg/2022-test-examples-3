# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from django.conf import settings

from common.tester.factories import create_thread, create_train_schedule_plan
from common.tester.skippers import skip_in_arcadia
from common.tester.utils.datetime import replace_now
from stationschedule.tester.factories import create_ztablo


pytestmark = pytest.mark.dbuser


@pytest.yield_fixture(autouse=True)
def currency(httpretty):
    response_xml = '''<?xml version="1.0" encoding="utf-8"?>
    <response>
    <status>ok</status>
    <data>
    <date>03.03.2017</date>
    <src>Московской биржи</src>
    <from>RUR</from>
    <rates>
    <to id="TRY" value="15.8"/>
    <to id="UAH" value="2.1"/>
    <to id="USD" value="58.6"/>
    <to id="ZAR" value="4.4"/>
    </rates>
    </data>
    </response>'''

    httpretty.register_uri(httpretty.GET, settings.CURRENCY_RATES_URL, body=response_xml.encode('utf8'))
    yield


@skip_in_arcadia
def test_blank_real_ztablo(rasp_client):
    thread = create_thread(t_type='bus', schedule_v1=[
        [None, 0, {'settlement': {}}],
        [10, None, {'settlement': {}}],
    ])
    create_ztablo(arrival=datetime(2000, 1, 1, 12), thread=thread)
    create_ztablo(departure=datetime(2000, 1, 1, 13), thread=thread)

    response = rasp_client.get('/thread/{}?tt=bus'.format(thread.uid))

    assert response.status_code == 200


@skip_in_arcadia
@replace_now('2000-06-01')
def test_schedule_plan_appendix(rasp_client):
    current_plan = create_train_schedule_plan(start_date='2000-01-01', end_date='2000-06-30', appendix_type='to')
    next_plan = create_train_schedule_plan(start_date='2000-07-01', end_date='2001-01-01')
    current_plan_thread = create_thread(t_type='suburban', template_text='ежедневно', schedule_plan=current_plan)
    response = rasp_client.get('/thread/{}?tt=suburban'.format(current_plan_thread.uid))

    assert response.status_code == 200
    assert 'по 30\N{no-break space}июня'.encode('utf-8') in response.content

    next_plan_thread = create_thread(t_type='suburban', template_text='ежедневно', schedule_plan=next_plan)
    response = rasp_client.get('/thread/{}?tt=suburban'.format(next_plan_thread.uid))

    assert response.status_code == 200
    assert 'с 1\N{no-break space}июля'.encode('utf-8') in response.content

    thread = create_thread(t_type='suburban', template_text='ежедневно')
    response = rasp_client.get('/thread/{}?tt=suburban'.format(thread.uid))

    assert response.status_code == 200
    assert '1\N{no-break space}июля'.encode('utf-8') not in response.content
