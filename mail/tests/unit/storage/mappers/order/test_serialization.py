import pytest

from mail.payments.payments.storage.mappers.order.serialization import OrderDataDumper, OrderDataMapper


@pytest.fixture
def order_data_dict():
    return {
        'trust_form_name': '1',
        'trust_template': '1',
        'multi_max_amount': 10,
        'multi_issued': 0,
        'meta': None,
        'offline_prolongation_amount': None,
        'service_data': None,
        'turboapp_id': None,
        'tsid': None,
        'psuid': None,
        'receipt_type': 'complete',
        'fast_moderation': False,
        'recurrent': False,
        'without_3ds': False,
        'version': 2,
    }


@pytest.fixture
def order_dict(order, order_data_dict):
    result = {
        attr: getattr(order, attr)
        for attr in [
            'uid',
            'order_id',
            'parent_order_id',
            'parent_order_id',
            'original_order_id',
            'revision',
            'shop_id',
            'kind',
            'pay_status',
            'refund_status',
            'active',
            'autoclear',
            'verified',
            'created_by_source',
            'pay_by_source',
            'closed',
            'created',
            'updated',
            'held_at',
            'caption',
            'description',
            'user_email',
            'user_description',
            'trust_refund_id',
            'service_client_id',
            'service_merchant_id',
            'customer_uid',
            'return_url',
            'paymethod_id',
            'merchant_oauth_mode',
            'test',
            'email_message_id',
            'email_context',
            'customer_subscription_id',
            'customer_subscription_tx_purchase_token',
            'offline_abandon_deadline',
            'exclude_stats',
            'acquirer',
            'pay_status_updated_at',
            'commission',
        ]
    }
    result['data'] = order_data_dict
    return result


class TestOrderDataMapper:
    def test_map(self, loop, order, order_dict, order_data_dict):
        order.shop = None
        order.service_merchant = None

        row = {
            type(order).__name__ + '__' + key: value
            for key, value in order_dict.items()
        }
        assert OrderDataMapper()(row) == order


class TestOrderDataDumper:
    def test_unmap(self, loop, order, order_dict, order_data_dict):
        assert OrderDataDumper()(order) == order_dict

    def test_unmap_with_default_version(self, loop, order):
        del order.data.version
        assert OrderDataDumper()(order)['data']['version'] == 2
