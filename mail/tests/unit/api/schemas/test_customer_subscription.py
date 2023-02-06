import pytest
from marshmallow import ValidationError

from mail.payments.payments.api.schemas.customer_subscription import PostCustomerSubscriptionRequestSchema


class TestPostCustomerSubscriptionRequestSchema:
    def test_post_customer_subscription_request_success_user_ip(self, randn, rands):
        PostCustomerSubscriptionRequestSchema().load({
            'subscription_id': randn(),
            'user_ip': rands(),
        })

    def test_post_customer_subscription_request_success_region_id(self, randn):
        PostCustomerSubscriptionRequestSchema().load({
            'subscription_id': randn(),
            'region_id': randn(),
        })

    def test_post_customer_subscription_request_validation_error(self, randn):
        with pytest.raises(ValidationError):
            PostCustomerSubscriptionRequestSchema().load({
                'subscription_id': randn(),
            })
