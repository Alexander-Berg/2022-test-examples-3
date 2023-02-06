# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, contains, has_entries, has_properties

from common.apps.train_order.enums import CoachType
from common.models.currency import Price
from common.tester.factories import create_station
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.utils.date import UTC_TZ
from travel.proto import commons_pb2
from travel.rasp.train_api.tariffs.train.base.models import TrainTariff
from travel.rasp.train_api.tariffs.train.base.segments import fill_segment_fees, BanditClient
from travel.rasp.train_api.tariffs.train.factories.base import create_train_segment, create_train_tariffs_query
from travel.rasp.train_api.train_bandit_api.client import api_pb2, api_pb2_grpc
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _create_segments():
    segments = [
        create_train_segment(
            tariffs={'classes': {
                'compartment': TrainTariff(CoachType.COMPARTMENT, Price(1000), Price(200)),
                'platzkarte': TrainTariff(CoachType.PLATZKARTE, Price(500), Price(0)),
            }},
            station_from=create_station(title='Оттуда', pk=100),
            station_to=create_station(title='Туда', pk=200),
            departure=datetime(2020, 5, 7, 12, 0, tzinfo=UTC_TZ),
            arrival=datetime(2020, 5, 9, 12, 0, tzinfo=UTC_TZ),
        ),
        create_train_segment(
            tariffs={'classes': {
                'sitting': TrainTariff(CoachType.SITTING, Price(1000), Price(200)),
                'common': TrainTariff(CoachType.COMMON, Price(500), Price(0)),
            }},
            station_from=create_station(title='Оттуда 2', pk=300),
            station_to=create_station(title='Туда 2', pk=400),
            departure=datetime(2020, 5, 7, 12, 0, tzinfo=UTC_TZ),
            arrival=datetime(2020, 5, 9, 12, 0, tzinfo=UTC_TZ),
        ),
    ]
    return segments


def test_fill_segment_fees():
    query = create_train_tariffs_query()
    segments = _create_segments()
    ClientContractsFactory()

    fee = [ti.fee for rs in fill_segment_fees(segments, query) for ti in rs.tariffs['classes'].values()]
    assert all(f is not None for f in fee)


@pytest.mark.parametrize('charging, icookie, effect, call_count', [
    (False, 'some cookie', None, 4),
    (True, None, None, 0),
    (True, None, [Exception('Boom')], 4),
    (True, 'some cookie', [Exception('Boom')], 4),
])
@mock.patch.object(TrainTariff, 'calculate_fee', autospec=True)
def test_fill_segments_fees_by_bandit_fallback(m_calc, charging, icookie, effect, call_count):
    train_query = create_train_tariffs_query(icookie=icookie)
    segments = _create_segments()
    ClientContractsFactory()

    with replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_CHARGING', charging):
        with mock.patch.object(BanditClient, 'get_fee_for_segments', side_effect=effect):
            fill_segment_fees(segments, train_query)
            assert m_calc.call_count == call_count


def _create_bandit_response():
    return api_pb2.TGetChargeResponse(
        ChargesByContexts=[
            api_pb2.TCharge(
                InternalId=0,
                Permille=110,
                TicketFees={
                    0: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=11000, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=0, Precision=2),
                        IsBanditFeeApplied=True),
                    1: api_pb2.TTicketFee(
                        Fee=commons_pb2.TPrice(Amount=22000, Precision=2),
                        ServiceFee=commons_pb2.TPrice(Amount=0, Precision=2),
                        IsBanditFeeApplied=True)
                },
                BanditType='BanditType',
                BanditVersion=100500,
            ),
        ])


@replace_dynamic_setting('TRAIN_PURCHASE_BANDIT_CHARGING', True)
@mock.patch.object(api_pb2_grpc, 'BanditApiServiceV1Stub', autospec=True)
def test_fill_segments_fees_by_bandit(m_stub):
    train_query = create_train_tariffs_query(icookie='some cookie', bandit_type='bandit type')
    segment_data = dict(
        station_from=create_station(title='Оттуда', pk=100),
        station_to=create_station(title='Туда', pk=200),
        departure=datetime(2020, 5, 7, 12, 0, tzinfo=UTC_TZ),
        arrival=datetime(2020, 5, 9, 12, 0, tzinfo=UTC_TZ),
    )
    segments = [
        create_train_segment(
            tariffs={'classes': {
                'compartment': TrainTariff(CoachType.COMPARTMENT, Price(1000, 'RUB'), Price(200, 'RUB')),
            }},
            **segment_data
        ),
        create_train_segment(
            tariffs={'classes': {
                'compartment': TrainTariff(CoachType.COMPARTMENT, Price(2000, 'RUB'), Price(300, 'RUB')),
            }},
            **segment_data
        ),
    ]
    m_stub.return_value.GetCharge = mock.Mock(return_value=_create_bandit_response())

    fill_segment_fees(segments, train_query)

    assert m_stub.return_value.GetCharge.call_count == 1
    assert_that(
        m_stub.return_value.GetCharge.call_args.args[0],
        has_properties(
            BanditType='bandit type',
            ContextsWithPrices=contains(
                has_properties(
                    Context=has_properties(CarType='compartment'),
                    TicketPrices=has_entries({
                        0: has_properties(
                            Amount=has_properties(Amount=100000, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=20000, Precision=2, Currency=commons_pb2.C_RUB),
                        ),
                        1: has_properties(
                            Amount=has_properties(Amount=200000, Precision=2, Currency=commons_pb2.C_RUB),
                            ServiceAmount=has_properties(Amount=30000, Precision=2, Currency=commons_pb2.C_RUB),
                        )}
                    ),
                ),
            ),
        )
    )

    assert_that(segments, contains(
        has_properties(tariffs=has_entries({'classes': has_entries({
            'compartment': has_properties(
                fee_percent=Decimal('0.11'),
                fee=has_properties(value=Decimal('110.00')),
                is_bandit_fee_applied=True,
                bandit_type='BanditType',
                bandit_version=100500,
            ),
        })})),
        has_properties(tariffs=has_entries({'classes': has_entries({
            'compartment': has_properties(
                fee_percent=Decimal('0.11'),
                fee=has_properties(value=Decimal('220.00')),
                is_bandit_fee_applied=True,
                bandit_type='BanditType',
                bandit_version=100500,
            ),
        })})),
    ))
