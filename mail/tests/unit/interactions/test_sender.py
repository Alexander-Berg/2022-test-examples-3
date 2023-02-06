from uuid import uuid4

import pytest

from mail.beagle.beagle.interactions.sender import SenderClient


@pytest.fixture
def sender_client(client_mocker):
    return client_mocker(SenderClient)


@pytest.fixture
def mailing_id():
    return str(uuid4())


@pytest.mark.asyncio
class TestSendTransactionalLetter:
    @pytest.fixture
    def message_id(self, rands):
        return rands()

    @pytest.fixture
    def to_email(self, rands):
        return 'olol@ya.ru'

    @pytest.fixture
    def render_context(self):
        return {'name': 'Harry', 'age': 20}

    @pytest.fixture(autouse=True)
    def response_json(self, message_id):
        """Фистура тела ответа из клиентского _make_request, похожая на ответ рассылятора."""
        return {'result': {'status': 'OK', 'message_id': message_id}}

    @pytest.fixture
    def returned_func(self, beagle_settings, mailing_id, to_email, render_context, response_json, clients, mock_sender):
        async def _inner():
            mock_sender(f'/api/0/{beagle_settings.SENDER_CLIENT_ACCOUNT_SLUG}/transactional/{mailing_id}/send',
                        response_json)
            return await clients.sender.send_transactional_letter(mailing_id=mailing_id, to_email=to_email,
                                                                  render_context=render_context)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_query_params(self, to_email, returned, last_sender_request, response_json):
        request = last_sender_request()
        assert request.query['to_email'] == to_email

    async def test_returned(self, to_email, returned, message_id, response_json):
        assert returned == message_id
