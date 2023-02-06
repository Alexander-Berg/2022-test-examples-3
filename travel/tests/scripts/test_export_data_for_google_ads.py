# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import timedelta
from decimal import Decimal

import mock
import pytest

from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date.environment import now_utc
from travel.rasp.train_api.scripts import export_data_for_google_ads
from travel.rasp.train_api.scripts.export_data_for_google_ads import (
    mds_s3_public_client, net_profit, export_data_for_google_ads_task
)
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, TicketFactory, TicketPaymentFactory, InsuranceFactory, SourceFactory
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_net_profit():
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(payment=TicketPaymentFactory())],
            ),
        ]
    )
    value = net_profit(order)
    assert value == Decimal('58.33')


def test_net_profit_with_child():
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(payment=TicketPaymentFactory())],
            ),
            PassengerFactory(
                tickets=[TicketFactory(payment=TicketPaymentFactory(), places=[])],
            ),
        ]
    )
    value = net_profit(order)
    assert value == Decimal('58.33')


def test_net_profit_with_insurance():
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(payment=TicketPaymentFactory())],
                insurance=InsuranceFactory(trust_order_id='insurance_ordered'),
            ),
        ]
    )
    value = net_profit(order)
    assert value == Decimal('58.33')


def test_net_profit_with_refunded_insurance():
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(payment=TicketPaymentFactory())],
                insurance=InsuranceFactory(trust_order_id='insurance_ordered',
                                           refund_uuid='but_refunded_12345678901234567890'),
            ),
        ]
    )
    value = net_profit(order)
    assert value == Decimal('58.33')


def test_net_profit_less_zero():
    order = TrainOrderFactory(
        passengers=[
            PassengerFactory(
                tickets=[TicketFactory(payment=TicketPaymentFactory(fee=Decimal('0.01')))],
            ),
        ]
    )
    value = net_profit(order)
    assert value == Decimal('0.01')


RETURNED_EXPORT_DATA = """Parameters:TimeZone=Etc/GMT,,,,
Google Click ID,Conversion Name,Conversion Time,Conversion Value,Conversion Currency
gclid_1,zhd_import,2018-10-03T00:00:00,58.33,RUB
gclid_2,zhd_import,2018-10-04T00:00:00,58.33,RUB
gclid_3,zhd_import,2018-10-05T00:00:00,58.33,RUB
gclid_4,zhd_import,2018-10-06T00:00:00,58.33,RUB
gclid_5,zhd_import,2018-10-07T00:00:00,58.33,RUB
gclid_6,zhd_import,2018-10-08T00:00:00,58.33,RUB
gclid_7,zhd_import,2018-10-09T00:00:00,58.33,RUB
less_zero,zhd_import,2018-12-31T00:00:00,0.01,RUB
"""


@replace_now('2019-01-01 03:00:00')
@mock.patch.object(export_data_for_google_ads, 'CHUNK_SIZE_IN_ROWS', 5)
@mock.patch.object(mds_s3_public_client, 'save_data', autospec=True)
def test_export_data(m_mds_client):
    for i in range(0, 8):
        TrainOrderFactory(status=OrderStatus.DONE, source=SourceFactory(gclid='gclid_' + str(i)),
                          finished_at=now_utc() - timedelta(days=(91 - i)))
    TrainOrderFactory(status=OrderStatus.DONE, finished_at=now_utc() - timedelta(days=2))
    TrainOrderFactory(status=OrderStatus.PAID, source=SourceFactory(gclid='not_done'),
                      finished_at=now_utc() - timedelta(days=1))
    TrainOrderFactory(status=OrderStatus.DONE, source=SourceFactory(gclid='not_yet'), finished_at=now_utc())
    TrainOrderFactory(
        status=OrderStatus.DONE,
        source=SourceFactory(gclid='less_zero'),
        finished_at=now_utc() - timedelta(days=1),
        passengers=[PassengerFactory(tickets=[TicketFactory(payment=TicketPaymentFactory(fee=Decimal('0.01')))])],
    )

    export_data_for_google_ads_task()

    m_mds_client.assert_called_once_with(key='google_ads/import_data.csv', data=RETURNED_EXPORT_DATA)


def test_send_email_on_error():
    with mock.patch.object(export_data_for_google_ads, 'guaranteed_send_email',
                           autospec=True) as m_guaranteed_send_email, \
            mock.patch.object(export_data_for_google_ads, '_export_data_for_google_ads_task',
                              autospec=True) as m_export_data_for_google_ads_task:
        m_export_data_for_google_ads_task.side_effect = Exception('Boom!')

        with pytest.raises(Exception):
            export_data_for_google_ads_task()

        assert m_export_data_for_google_ads_task.call_count == 3
        assert m_guaranteed_send_email.call_count == 1
