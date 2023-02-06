from dataclasses import asdict
from datetime import datetime, timezone

import pytest

from hamcrest import has_entries, match_equality

from mail.payments.payments.interactions.floyd import Message, MessageActor


class BaseFloydTest:
    @pytest.fixture(autouse=True)
    def spy_post(self, floyd_client, mocker):
        mocker.spy(floyd_client, 'post')
        return floyd_client

    @pytest.fixture
    def org_id(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def response_json(self, rands):
        return {'result': {rands(): rands()}}


class TestCreateConsultation(BaseFloydTest):
    @pytest.fixture
    def order_data(self, customer_uid):
        return {'customer_uid': customer_uid}

    @pytest.fixture
    def description(self, rands):
        return rands()

    @pytest.fixture
    def customer_uid(self, randn):
        return randn()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def response_json(self, rands, consultation_id):
        return {'result': {'id': consultation_id, rands(): rands()}}

    @pytest.fixture
    def returned_func(self, floyd_client, org_id, description, order):
        async def _inner():
            return await floyd_client.create_consultation(org_id, description, order)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, description, order, payments_settings, org_id):
        floyd_client._make_request.assert_called_with(
            'create_consultation',
            'POST',
            f'{floyd_client.BASE_URL}/{org_id}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'createConsultation',
                    'params': {
                        'scenario_id': payments_settings.FLOYD_SCENARIO_ID,
                        'description': description,
                        'clients': [{'user_id': {'puid': order.customer_uid}, 'role_id': 'client'}],
                        'params': {
                            'org_id': org_id,
                            'description': description,
                            'arbitrage_iframe_link': payments_settings.FLOYD_ARBITRAGE_IFRAME_LINK.format(
                                **asdict(order)
                            )
                        }
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert response_json['result'] == returned


class TestUpdateChatbar(BaseFloydTest):
    @pytest.fixture
    def chatbar(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def role_id(self, rands):
        return rands()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, floyd_client, org_id, consultation_id, role_id, chatbar):
        async def _inner():
            return await floyd_client.update_chatbar(org_id, consultation_id, role_id, chatbar)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, consultation_id, role_id, chatbar, org_id):
        floyd_client._make_request.assert_called_with(
            'update_chatbar',
            'POST',
            f'{floyd_client.BASE_URL}/{org_id}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'updateChatBar',
                    'params': {
                        'consultation_id': consultation_id,
                        'role_id': role_id,
                        'chatbar': chatbar
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert response_json == returned


class TestUploadScenario(BaseFloydTest):
    @pytest.fixture
    def content(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, floyd_client, content):
        async def _inner():
            return await floyd_client.upload_scenario(content)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, payments_settings, org_id, content):
        floyd_client._make_request.assert_called_with(
            'upload_scenario',
            'POST',
            f'{floyd_client.BASE_URL}/{payments_settings.FLOYD_ORG_ID}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'uploadScenario',
                    'params': {
                        'scenario_id': payments_settings.FLOYD_SCENARIO_ID,
                        'content': content
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert response_json == returned


class TestCreateOrganizationsChat(BaseFloydTest):
    @pytest.fixture
    def operator_organization_id(self, rands):
        return rands()

    @pytest.fixture
    def client_organization_id(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def response_json(self, rands):
        return {
            'result': {
                'clients': {'chat_id': rands()},
                'operators': {'chat_id': rands()},
            }
        }

    @pytest.fixture
    def returned_func(self, floyd_client, operator_organization_id, client_organization_id):
        async def _inner():
            return await floyd_client.create_organizations_chat(operator_organization_id, client_organization_id)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, payments_settings, org_id, client_organization_id, operator_organization_id):
        floyd_client._make_request.assert_called_with(
            'create_organizations_chat',
            'POST',
            f'{floyd_client.BASE_URL}/{payments_settings.FLOYD_ORG_ID}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'createOrganizationsChat',
                    'params': {
                        'operator_organization_id': operator_organization_id,
                        'client_organization_id': client_organization_id
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        client_chat_id = response_json['result']['clients']['chat_id']
        operator_chat_id = response_json['result']['operators']['chat_id']
        assert client_chat_id, operator_chat_id == returned


class TestGetConsultationHistory(BaseFloydTest):
    @pytest.fixture
    def org_id(self, rands):
        return rands()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def response_json(self, randn, rands):
        msg = {
            'id': randn(),
            'body': {
                'text': rands(),
                'file': {'url': rands()},
                'image': {'url': rands()},
            }
        }
        return {
            'result': {
                'client_messages': [{**msg, 'sender': {'is_client': True}, 'timestamp': 1000}],
                'operator_messages': [{**msg, 'sender': {'is_client': False}, 'timestamp': 0}],
            }
        }

    @pytest.fixture
    def returned_func(self, floyd_client, org_id, consultation_id):
        async def _inner():
            return await floyd_client.get_consultation_history(org_id, consultation_id)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, payments_settings, org_id, consultation_id):
        floyd_client._make_request.assert_called_with(
            'get_consultation_history',
            'POST',
            f'{floyd_client.BASE_URL}/{org_id}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'getConsultationHistory',
                    'params': {
                        'consultation_id': consultation_id
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        items = response_json['result']['client_messages'] + response_json['result']['operator_messages']
        messages = []

        for item in items:
            attachments = []

            if item['body'].get('image'):
                attachments.append({"url": item['body']['image']['url']})
            if item['body'].get('file'):
                attachments.append({"url": item['body']['file']['url']})

            messages.append(
                Message(
                    message_id=int(item['id']),
                    text=item['body'].get('text') or '',
                    sender=MessageActor.CLIENT if item['sender']['is_client'] else MessageActor.OPERATOR,
                    recipient=MessageActor.OPERATOR if item['sender']['is_client'] else MessageActor.CLIENT,
                    creation_time=datetime.fromtimestamp(item['timestamp'] / 1000).astimezone(timezone.utc),
                    attachments=attachments
                )
            )

        assert sorted(messages, key=lambda x: x.creation_time) == returned


class TestSendDivCardMessage(BaseFloydTest):
    @pytest.fixture
    def org_id(self, rands):
        return rands()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture
    def card(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def message(self, rands):
        return rands()

    @pytest.fixture
    def to_role_ids(self, rands):
        return rands()

    @pytest.fixture
    def returned_func(self, floyd_client, org_id, consultation_id, card, message, to_role_ids):
        async def _inner():
            return await floyd_client.send_div_card_message(org_id, consultation_id, card, message, to_role_ids)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, payments_settings, org_id, card, message, to_role_ids, consultation_id):
        floyd_client._make_request.assert_called_with(
            'send_div_card_message',
            'POST',
            f'{floyd_client.BASE_URL}/{org_id}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'sendDivCardMessage',
                    'params': {
                        'consultation_id': consultation_id,
                        'card': card,
                        'message': message or '',
                        'to_role_ids': to_role_ids
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert response_json['result'] == returned


class TestCustomAction(BaseFloydTest):
    @pytest.fixture
    def org_id(self, rands):
        return rands()

    @pytest.fixture
    def consultation_id(self, rands):
        return rands()

    @pytest.fixture
    def action(self, rands):
        return rands()

    @pytest.fixture
    def params(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def returned_func(self, floyd_client, org_id, consultation_id, action, params):
        async def _inner():
            return await floyd_client.custom_action(org_id, consultation_id, action, params)

        return _inner

    @pytest.mark.usefixtures('returned')
    def test_call_post(self, floyd_client, payments_settings, org_id, action, params, consultation_id):
        floyd_client._make_request.assert_called_with(
            'custom_action',
            'POST',
            f'{floyd_client.BASE_URL}/{org_id}/jsonrpc/',
            json=match_equality(
                has_entries({
                    'jsonrpc': '2.0',
                    'method': 'customAction',
                    'params': {
                        'consultation_id': consultation_id,
                        'action': action,
                        'params': {
                            'params': params or {}
                        }
                    }
                })
            ),
        )

    @pytest.mark.asyncio
    async def test_returned(self, returned, response_json):
        assert response_json['result'] == returned
