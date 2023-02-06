import pytest

from sendr_utils import utcnow, without_none

from mail.payments.payments.core.entities.enums import NDS, PAY_METHOD_OFFLINE
from mail.payments.payments.interactions.floyd.entities import Message, MessageActor


class BaseArbiterTest:
    @pytest.fixture(autouse=True)
    def spy_post(self, arbiter_client, mocker):
        mocker.spy(arbiter_client, 'post')
        return arbiter_client


class TestCreateArbitrage(BaseArbiterTest):
    @pytest.fixture
    def messages(self, randn, rands):
        return [
            Message(
                message_id=randn(),
                text=rands(),
                sender=MessageActor.CLIENT if i % 2 == 0 else MessageActor.OPERATOR,
                recipient=MessageActor.CLIENT if i % 2 != 0 else MessageActor.OPERATOR,
                creation_time=utcnow(),
                attachments=[{rands(): rands()}]
            )
            for i in range(randn(min=10, max=100))
        ]

    @pytest.fixture(autouse=True)
    def response_json(self, randn):
        return {'id': randn()}

    @pytest.fixture
    def order_data(self, randn):
        return {
            'customer_uid': randn(),
            'closed': utcnow()
        }

    @pytest.fixture
    def returned_func(self, arbiter_client, merchant, order, items, arbitrage, messages):
        async def _inner():
            return await arbiter_client.create_arbitrage(merchant, order, arbitrage, messages)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, arbiter_client, payments_settings, merchant, messages, arbitrage, order):
        arbiter_client.post.assert_called_with(
            interaction_method='create_arbitrage',
            url=f'{arbiter_client.BASE_URL}/api/v1/service/conversation/add',
            json={
                "description": "Пользователь хочет получить деньги назад.",
                "merchant": without_none({
                    "idInService": merchant.merchant_id,
                    "inn": merchant.organization.inn,
                    "name": merchant.organization.full_name,
                    "ogrn": merchant.organization.ogrn,
                }),
                "messages": [
                    {
                        'id': message.message_id,
                        'creationTime': message.creation_time.isoformat(),
                        'attachments': message.attachments,
                        'sender': 'MERCHANT' if message.sender == MessageActor.OPERATOR else 'USER',
                        'recipient': 'MERCHANT' if message.recipient == MessageActor.OPERATOR else 'USER',
                        'text': message.text,
                    } for message in messages
                ],
                "serviceType": "SUPERAPP",
                "subject": {
                    "currency": order.currency,
                    "description": order.description,
                    "idInService": f"{order.uid}.{order.order_id}",
                    "orderInfo": {
                        "history": [
                            {
                                "status": "DELIVERED",
                                "time": order.closed.isoformat()
                            }
                        ],
                        "items": [
                            {
                                "currency": item.currency,
                                "idInService": f'{item.uid}.{item.order_id}.{item.product_id}',
                                "name": item.product.name,
                                "price": int(item.product.price * 100),
                                "quantity": float(item.amount),
                                "totalCost": int(item.price * 100),
                                "vat": NDS.to_arbitrage(item.nds)
                            } for item in order.items
                        ]
                    },
                    "paymentType": "CASH" if order.pay_method == PAY_METHOD_OFFLINE else "CREDIT_CARD",
                    "totalCost": int(order.price * 100),
                    "type": "ORDER"
                },
                "uid": order.customer_uid,
                "notificationChannels": [
                    {
                        'businesschatParams': {
                            'chatId': payments_settings.ARBITER_CHAT_ID,
                            'recipientId': arbitrage.chat_id,
                        },
                        'conversationSide': 'USER',
                        'type': 'BUSINESSCHAT'
                    },
                    {
                        'businesschatParams': {
                            'chatId': payments_settings.ARBITER_CHAT_ID,
                            'recipientId': arbitrage.arbiter_chat_id
                        },
                        'conversationSide': 'MERCHANT',
                        'type': 'BUSINESSCHAT'
                    }
                ]
            }
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert response_json['id'] == returned


class TestExecuteVerdict(BaseArbiterTest):
    @pytest.fixture
    def conversation_id(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def response_json(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def returned_func(self, arbiter_client, conversation_id):
        async def _inner():
            return await arbiter_client.execute_verdict(conversation_id=conversation_id)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, arbiter_client, conversation_id):
        arbiter_client.post.assert_called_with(
            interaction_method='execute_verdict',
            url=f'{arbiter_client.BASE_URL}/api/v1/service/verdict/execute',
            params={
                "conversationId": conversation_id,
            }
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert returned is None
