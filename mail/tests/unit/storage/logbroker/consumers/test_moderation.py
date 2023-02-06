from typing import Any, Dict

import pytest
import ujson

from mail.payments.payments.core.entities.moderation import FastModerationRequest, ModerationResult
from mail.payments.payments.core.exceptions import CoreDataError
from mail.payments.payments.storage.logbroker.consumers.moderation import (
    FastModerationRequestConsumer, ModerationConsumer
)


class TestModerationConsumer:
    """Проверяем создание ModerationResults по входному потоку байтов результатов модерации."""
    @pytest.fixture
    def consumer(self, lb_factory_mock) -> ModerationConsumer:
        return ModerationConsumer(lb_factory_mock)

    @pytest.fixture(params=['No', 'Yes'])
    def verdict(self, request):
        """Фикстура для вердикта модерации: да/нет."""
        return request.param

    @pytest.fixture(params=[None, 'HighRisk'])
    def reason(self, request):
        return request.param

    @pytest.fixture(params=[None, [1, 2, 3]])
    def reasons(self, request):
        return request.param

    @pytest.fixture
    def data_dict(self, verdict, reason, reasons) -> Dict[str, Any]:
        """Фикстура-затычка: переопределяем далее."""
        return {}

    @pytest.fixture
    def data_bytes(self, data_dict: Dict[str, Any]) -> bytes:
        """Фикстура входящего потока байтов для ModerationConsumer."""
        return ujson.dumps(data_dict).encode('ascii')

    @pytest.fixture
    def get_moderation_expected(self):
        def _get_moderation_expected(data_dict):
            return ModerationResult(
                moderation_id=data_dict['meta']['id'],
                client_id=data_dict['meta'].get('client_id'),
                submerchant_id=data_dict['meta'].get('merchant_id'),
                approved=data_dict['result']['verdict'] == 'Yes',
                reason=data_dict['result'].get('reason'),
                reasons=data_dict['result'].get('reasons') or [],
                unixtime=data_dict['unixtime']
            )
        return _get_moderation_expected

    @pytest.fixture
    def moderation_expected(self, data_dict, get_moderation_expected) -> ModerationResult:
        """Ожидаемый результат."""
        return get_moderation_expected(data_dict)

    class TestOrderParsing:
        """Проверяем парсинг результатов по заказам."""
        @pytest.fixture
        def data_dict(self, verdict, reason, reasons) -> Dict[str, Any]:
            return {
                "service": "pay",
                "type": "order",
                "meta": {
                    "id": 12345,
                    "order_id": 111,
                    "client_id": 5678
                },
                "result": {
                    "verdict": verdict,
                    "reasons": reasons,
                    "reason": reason,
                },
                'unixtime': 1553096365000,
                'create_time': '2019-03-20 18:39:25',
            }

        def test_order_parsing__parse_data(self, consumer, moderation_expected, data_bytes):
            parsed_result = next(consumer.parse_data(data_bytes))
            assert parsed_result == moderation_expected

    class TestMerchantParsing:
        """Проверяем парсинг результатов по продавцам."""
        @pytest.fixture
        def data_dict(self, verdict, reason, reasons) -> Dict[str, Any]:
            return {
                'service': 'pay',
                'type': 'merchants',
                'meta': {
                    'id': 123,
                    'client_id': 456,
                    'merchant_id': 789,
                },
                'result': {
                    'verdict': verdict,
                    'reason': reason,
                    'reasons': reasons,
                },
                'unixtime': 1553096365000,
                'create_time': '2019-03-20 18:39:25',
            }

        def test_merchant_parsing__parse_data(self, consumer, moderation_expected, data_bytes):
            parsed_result = next(consumer.parse_data(data_bytes))
            assert parsed_result == moderation_expected

        def test_merchant_parsing__without_optional_fields(self, consumer, get_moderation_expected, data_dict):
            del data_dict['meta']['client_id']
            del data_dict['meta']['merchant_id']
            data_bytes = ujson.dumps(data_dict).encode('ascii')
            moderation_expected = get_moderation_expected(data_dict)

            parsed_result = next(consumer.parse_data(data_bytes))

            assert parsed_result == moderation_expected

    @pytest.mark.parametrize('data', [
        {'unixtime': None},
        {'unixtime': 'hi'},
    ])
    @pytest.mark.asyncio
    async def test_raises_error_on_bad_unixtime_in_result(self, consumer, data):
        with pytest.raises(CoreDataError):
            consumer._get_unixtime(data)

    @pytest.mark.asyncio
    async def test_unixtime_truncates_floats(self, consumer):
        assert consumer._get_unixtime({'unixtime': 1.9}) == 1


class TestFastModerationRequestConsumer:
    @pytest.fixture
    def consumer(self, lb_factory_mock):
        return FastModerationRequestConsumer(lb_factory_mock)

    @pytest.fixture
    def data_dict(self):
        return {
            'service': 'pay',
            'type': 'merchants',
            'meta': {
                'id': 123,
                'client_id': 456,
                'merchant_id': 789,
            },
            'unixtime': 1553096365000,
        }

    @pytest.fixture
    def data_bytes(self, data_dict):
        return ujson.dumps(data_dict).encode('ascii')

    @pytest.fixture
    def expected_request(self, data_dict):
        return FastModerationRequest(
            service=data_dict['service'],
            type_=data_dict['type'],
            meta=data_dict['meta'],
            unixtime=data_dict['unixtime']
        )

    def test_parse_data(self, consumer, expected_request, data_bytes):
        parsed_result = next(consumer.parse_data(data_bytes))
        assert parsed_result == expected_request
