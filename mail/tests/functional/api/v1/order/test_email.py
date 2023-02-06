import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.exceptions import SpamError
from mail.payments.payments.tests.base import BaseTestMerchantRoles


@pytest.mark.usefixtures('base_merchant_action_data_mock')
@pytest.mark.usefixtures('moderation')
class TestEmail(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def spam_verdict(self):
        return False

    @pytest.fixture(params=(True, False))
    def spam_check(self, request):
        return request.param

    @pytest.fixture
    def data(self, spam_check):
        return {
            "to_email": "ozhegov@yandex-team.ru",
            "reply_email": "ozhegov@yandex-team.com.tr",
            "spam_check": spam_check
        }

    @pytest.fixture
    def campaign(self, randn, rands):
        return {
            'letters': [{'code': 'A', 'id': rands()}],
            'id': f'{randn()}',
            'submitted_by': 'noname',
            'title': 'test',
            'slug': rands()
        }

    @pytest.fixture(autouse=True)
    def mock_send_transactional_letter(self, sender_client_mocker, rands):
        with sender_client_mocker('send_transactional_letter', result=rands()) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def mock_render_transactional_letter(self, sender_client_mocker, rands):
        with sender_client_mocker('render_transactional_letter', result=rands(), multiple_calls=True) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def mock_campaign_detail(self, sender_client_mocker, campaign):
        with sender_client_mocker('campaign_detail', result=campaign) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def mock_so(self, so_client_mocker, spam_verdict):
        with so_client_mocker('form_is_spam', result=spam_verdict) as mock_so:
            yield mock_so

    @pytest.fixture
    def response_func(self, client, order, data, tvm):
        async def _inner(status=200):
            r = await client.post(f'/v1/order/{order.uid}/{order.order_id}/email', json=data)
            assert r.status == status
            return r

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    def test_response(self, response_data, mock_so, mock_send_transactional_letter, data, spam_check):
        mock_send_transactional_letter.assert_called_once()
        if spam_check:
            mock_so.assert_called_once()

        assert all((
            response_data['to_email'] == data['to_email'],
            response_data['reply_email'] == data['reply_email'],
        ))

    @pytest.mark.asyncio
    async def test_order_response(self, response, client, data, merchant, order):
        r = await client.get(f'/v1/order/{merchant.uid}/{order.order_id}')
        r_data = (await r.json())['data']
        assert all((
            r.status == 200,
            r_data.get('email') == {
                'to_email': data['to_email'],
                'reply_email': data['reply_email'],
            },
        ))

    @pytest.mark.asyncio
    async def test_order_list_response(self, response, client, data, merchant, order):
        r = await client.get(f'/v1/order/{merchant.uid}/')
        r_data = (await r.json())['data']
        assert all((
            r.status == 200,
            next(iter(r_data), {}).get('email') == {
                'to_email': data['to_email'],
                'reply_email': data['reply_email'],
            },
        ))

    @pytest.mark.parametrize('spam_check,spam_verdict', ((True, True),))
    @pytest.mark.asyncio
    async def test_spam(self, response_func):
        r = await response_func(400)
        r_data = (await r.json())['data']
        assert r_data['message'] == SpamError.MESSAGE
