# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.workflow import registry
from common.workflow.sleep import WaitTillStatuses
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, InsuranceFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainRefund
from travel.rasp.train_api.train_purchase.workflow.booking import CreateInsuranceRefund
from travel.rasp.train_api.train_purchase.workflow.ticket_refund import TICKET_REFUND_PROCESS

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


@mock.patch.object(registry.run_process, 'apply_async')
class TestCreateInsuranceRefund(object):

    def test_ok(self, m_run_process_apply_async):
        order = TrainOrderFactory(
            partner=TrainPartner.IM,
            passengers=[
                PassengerFactory(insurance=InsuranceFactory(operation_id='11111', trust_order_id='54321')),
                PassengerFactory(insurance=InsuranceFactory(operation_id='11112', trust_order_id='54322')),
                PassengerFactory(insurance=None),
                PassengerFactory(insurance=InsuranceFactory(operation_id=None, trust_order_id=None)),
            ],
        )
        event, order = process_state_action(
            CreateInsuranceRefund, [WaitTillStatuses.OK], order
        )

        assert event == WaitTillStatuses.OK
        assert order.insurance_auto_return_uuid
        refund = TrainRefund.objects.get(uuid=order.insurance_auto_return_uuid)
        m_run_process_apply_async.assert_called_once_with(
            [TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': order.uid}]
        )
        assert refund.email_is_sent
        assert not refund.blank_ids
        assert refund.insurance_ids == ['11111', '11112']
