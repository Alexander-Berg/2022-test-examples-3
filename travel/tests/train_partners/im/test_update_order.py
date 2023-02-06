# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_properties, contains

from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.update_order import IM_UPDATE_BLANKS_METHOD, update_order_info
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PartnerDataFactory

UPDATE_BLANKS_RESULT = '''{
  "Blanks": [
    {
      "OrderItemBlankId": 51946,
      "Number": "71234567890000",
      "BlankStatus": "ElectronicRegistrationPresent",
      "PendingElectronicRegistration": "NoValue"
    }
  ],
  "IsModified": false
}'''


@pytest.mark.dbuser
@pytest.mark.mongouser
def test_update_order_info(httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=42, operation_id='24'))
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body=UPDATE_BLANKS_RESULT)
    result = update_order_info(order)
    request = httpretty.last_request
    assert request.body == '{"OrderItemId": 24}'
    assert_that(result, has_properties(
        tickets=contains(has_properties(
            blank_id=51946,
            pending=False,
            rzhd_status=RzhdStatus.REMOTE_CHECK_IN
        ))
    ))
