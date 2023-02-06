from uuid import uuid4

import pytest
import ujson


@pytest.fixture
def mailing_id():
    return str(uuid4())


class TestSendTransactionalLetter:
    @pytest.fixture
    def message_id(self):
        return 'abcabcabcabcabc'

    @pytest.fixture
    def to_email(self):
        return 'guido@eggs.ni'

    @pytest.fixture
    def render_context(self):
        return {'name': 'Harry', 'age': 20}

    @pytest.fixture(autouse=True)
    def response_json(self, message_id):
        """Фистура тела ответа из клиентского _make_request, похожая на ответ рассылятора."""
        return {'result': {'status': 'OK', 'message_id': message_id}}

    @pytest.mark.asyncio
    async def test_send_transactional_letter_return_value(self,
                                                          sender_client,
                                                          mailing_id,
                                                          to_email,
                                                          render_context,
                                                          message_id):
        sent_message_id = await sender_client.send_transactional_letter(
            mailing_id=mailing_id, to_email=to_email, render_context=render_context)
        assert sent_message_id == message_id

    @pytest.mark.asyncio
    async def test_post_call_arguments(self, sender_client, mailing_id, to_email, render_context, mocker):
        mocker.spy(sender_client, 'post')
        await sender_client.send_transactional_letter(mailing_id=mailing_id, to_email=to_email,
                                                      render_context=render_context)
        sender_client.post.assert_called_once_with(
            interaction_method='send_transactional_letter',
            url=f'{sender_client.BASE_URL}/{sender_client.account_slug}/transactional/{mailing_id}/send',
            headers={},
            json={
                "async": True,
                "args": ujson.dumps(render_context or {}),
            },
            params={'to_email': to_email},
        )


class TestCampaignDetail:
    @pytest.fixture(autouse=True)
    def response_json(self, mailing_id):
        """Фистура тела ответа из клиентского _make_request, похожая на ответ рассылятора."""
        return {
            'letters': [{"code": "A", "id": "1"}],
            "id": "13878",
            "submitted_by": "noname",
            "title": "test",
            "slug": mailing_id
        }

    @pytest.mark.asyncio
    async def test_campaign_detail(self, sender_client, mailing_id):
        campaign = await sender_client.campaign_detail(mailing_id)
        assert campaign["slug"] == mailing_id

    @pytest.mark.asyncio
    async def test_get_call_arguments(self, sender_client, mailing_id, mocker):
        mocker.spy(sender_client, 'get')
        await sender_client.campaign_detail(mailing_id=mailing_id)
        sender_client.get.assert_called_once_with(
            interaction_method='campaign_detail',
            url=f'{sender_client.BASE_URL}/{sender_client.account_slug}/campaign/{mailing_id}/'
        )


class TestRenderTransactionalLetter:
    @pytest.fixture(params=('1', '2'))
    def value(self, request):
        return request.param

    @pytest.fixture
    def letter_id(self):
        return '1'

    @pytest.fixture
    def campaign_id(self):
        return '1'

    @pytest.fixture
    def render_context(self, value):
        return {'value': value}

    @pytest.fixture
    def html(self, render_context):
        return f'<html>{render_context["value"]}</html>'

    @pytest.fixture(autouse=True)
    def response_json(self, html):
        return {
            'result': html
        }

    @pytest.fixture(autouse=True)
    async def returned(self, mocker, sender_client, campaign_id, letter_id, render_context):
        mocker.spy(sender_client, 'post')
        return await sender_client.render_transactional_letter(campaign_id, letter_id, render_context)

    @pytest.mark.asyncio
    async def test_render_transactional_letter(self, returned, html):
        assert returned == html

    @pytest.mark.asyncio
    async def test_post_call_arguments(self, render_context, campaign_id, letter_id, sender_client):
        url = f'{sender_client.BASE_URL}/{sender_client.account_slug}/render/campaign/{campaign_id}/letter/{letter_id}'
        sender_client.post.assert_called_once_with(
            interaction_method='render_transactional_letter',
            url=url,
            json={"params": render_context}
        )
