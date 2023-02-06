import pytest
from google.protobuf.timestamp_pb2 import Timestamp
from hamcrest import assert_that, has_entries, equal_to, has_properties, only_contains
from mock import Mock

from travel.proto import commons_pb2
from travel.rasp.train_bandit_api.proto import api_pb2
from travel.rasp.pathfinder_proxy.services.train_fee_service import TrainFeeService
from travel.rasp.pathfinder_proxy.settings import Settings
from travel.rasp.pathfinder_proxy.tests.utils import transfer_variants_with_prices  # noqa


def raw_get_charge_result():
    ticket_fee = api_pb2.TTicketFee(
        IsBanditFeeApplied=True,
        Fee=commons_pb2.TPrice(Amount=12301, Precision=2, Currency=commons_pb2.C_RUB),
        ServiceFee=commons_pb2.TPrice(Amount=0, Precision=2, Currency=commons_pb2.C_RUB),
        TicketPrice=api_pb2.TTicketPrice(
            Amount=commons_pb2.TPrice(Amount=12301+100010, Precision=2, Currency=commons_pb2.C_RUB),
            ServiceAmount=commons_pb2.TPrice(Amount=10000, Precision=2, Currency=commons_pb2.C_RUB),
        )
    )
    return api_pb2.TGetChargeResponse(
        PartnerFee=commons_pb2.TPrice(Amount=4200, Precision=2, Currency=commons_pb2.C_RUB),
        PartnerRefundFee=commons_pb2.TPrice(Amount=4200, Precision=2, Currency=commons_pb2.C_RUB),
        ChargesByContexts=[
            api_pb2.TCharge(
                Context=context(),
                Permille=123,
                InternalId=0,
                BanditType='YYY',
                TicketFees={
                    0: ticket_fee,
                    1: ticket_fee,
                }
            )
        ]
    )


def context():
    return api_pb2.TContext(
        ICookie="test-icookie",
        PointFrom="s9609235",
        PointTo="s9605179",
        Arrival=Timestamp(seconds=1565248800),
        Departure=Timestamp(seconds=1565104200),
        CarType="compartment",
    )


@pytest.mark.asyncio
async def test_apply_fee(transfer_variants_with_prices):  # noqa: F811
    train_fee_service = TrainFeeService(Mock(), Settings())
    train_fee_service._get_charge = Mock(return_value=raw_get_charge_result())
    transfer_variants = transfer_variants_with_prices + transfer_variants_with_prices
    transfer_variants = await train_fee_service.apply_fee(transfer_variants, 'test-icookie', 'ZZZ',
                                                          None, 'desktop', 'req_id')
    assert_that(
        train_fee_service._get_charge.call_args[0][0],
        has_properties({
            'BanditType': equal_to('ZZZ'),
            'ContextsWithPrices': only_contains(has_properties({
                'Context': equal_to(context()),
                'AdditionalLogInfo': has_properties({
                    'EventType': 1,
                    'UserDevice': 'desktop',
                    'ReqID': 'req_id',
                }),
                'TicketPrices': has_entries({
                    0: has_properties({
                        'Amount': has_properties({'Amount': equal_to(100010)}),
                        'ServiceAmount': has_properties({'Amount': equal_to(10000)}),
                    }),
                    1: has_properties({
                        'Amount': has_properties({'Amount': equal_to(100010)}),
                        'ServiceAmount': has_properties({'Amount': equal_to(10000)}),
                    }),
                })
            }))
        })
    )
    expected_price = {
        "price": {
            "currency": "RUB",
            "value": 1123.11
        },
        "servicePrice": {
            "currency": "RUB",
            "value": 100.0
        }
    }
    assert_that(
        transfer_variants[0]['segments'][0]['tariffs']['classes']['compartment'],
        equal_to(expected_price)
    )
    assert_that(
        transfer_variants[2]['segments'][0]['tariffs']['classes']['compartment'],
        equal_to(expected_price)
    )
