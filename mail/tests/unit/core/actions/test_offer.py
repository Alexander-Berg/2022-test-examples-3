from datetime import date

import pytest

from mail.payments.payments.core.actions.offer import GetOfferAction
from mail.payments.payments.core.entities.enums import AcquirerType
from mail.payments.payments.core.entities.service import OfferSettings
from mail.payments.payments.tests.base import parametrize_acquirer


class BaseTestGetOffer:
    @pytest.fixture
    async def override_template(self, merchant, storage, rands):
        merchant.options.offer_settings = OfferSettings(
            pdf_template=rands(),
            slug=rands(),
        )
        await storage.merchant.save(merchant)
        return merchant.options.offer_settings.pdf_template

    @pytest.fixture
    def params(self, merchant):
        return {'uid': merchant.uid}

    @pytest.fixture
    def action(self, params):
        return GetOfferAction(**params)


class TestGetHeaders(BaseTestGetOffer):
    @pytest.fixture
    def returned(self, action):
        return action.get_headers()

    def test_returned(self, returned):
        assert returned == {
            'Content-Type': 'application/pdf',
            'Content-Disposition': 'attachment; filename="offer.pdf"',
        }


class TestRenderPDF(BaseTestGetOffer):
    @pytest.fixture
    def rendered_html(self):
        return 'test-render-pds-rendered-html'

    @pytest.fixture
    def written_pdf(self):
        return 'test-render-pds-written-pdf'

    @pytest.fixture(autouse=True)
    def render_mock(self, mocker, rendered_html):
        return mocker.Mock(return_value=rendered_html)

    @pytest.fixture(autouse=True)
    def env_mock(self, mocker, render_mock):
        mock_from_string = mocker.Mock()
        mock_from_string.render = render_mock

        mock_get_template = mocker.Mock()
        mock_get_template.render = render_mock

        mock = mocker.Mock()
        mock.get_template = mocker.Mock(return_value=mock_get_template)
        mock.from_string = mocker.Mock(return_value=mock_from_string)
        mocker.patch('mail.payments.payments.core.actions.offer.env', mock)

        return mock

    @pytest.fixture(autouse=True)
    def create_pdf_mock(self, mocker, written_pdf, action):
        return mocker.patch.object(action, 'create_pdf', mocker.Mock(return_value=written_pdf))

    @pytest.fixture
    def returned(self, merchant, action):
        return action.render_pdf(merchant)

    def test_returned(self, written_pdf, returned):
        assert returned == written_pdf

    def test_render_call(self, merchant, render_mock, returned):
        render_mock.assert_called_once_with(today=date.today(), merchant=merchant)

    def test_create_pdf_mock_call(self, create_pdf_mock, rendered_html, returned):
        create_pdf_mock.assert_called_once_with(rendered_html)

    @parametrize_acquirer
    def test_get_template_called(self, acquirer, env_mock, returned):
        template_names = {
            AcquirerType.KASSA: 'offer-kassa.html',
            AcquirerType.TINKOFF: 'offer-tinkoff.html',
        }
        env_mock.get_template.assert_called_once_with(template_names[acquirer])

    def test_get_template_not_called(self, override_template, env_mock, returned):
        env_mock.get_template.assert_not_called()

    def test_from_string_called(self, override_template, env_mock, returned):
        env_mock.from_string.assert_called_once_with(override_template)

    def test_from_string_not_called(self, env_mock, returned):
        env_mock.from_string.assert_not_called()


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestGetOffer(BaseTestGetOffer):
    @pytest.fixture
    def headers(self):
        return {'some': 'header'}

    @pytest.fixture(autouse=True)
    def get_headers_mock(self, mocker, action, headers):
        return mocker.patch.object(
            action,
            'get_headers',
            mocker.Mock(return_value=headers),
        )

    @pytest.fixture
    def rendered_pdf(self):
        return b'test-get-offer-rendered-pdf'

    @pytest.fixture(autouse=True)
    def render_pdf_mock(self, mocker, action, rendered_pdf):
        return mocker.patch.object(
            action,
            'render_pdf',
            mocker.Mock(return_value=rendered_pdf),
        )

    @pytest.fixture
    async def returned(self, action):
        return await action.run()

    def test_returned(self, headers, rendered_pdf, returned):
        assert returned == (headers, rendered_pdf)

    def test_get_headers_call(self, get_headers_mock, returned):
        get_headers_mock.assert_called_once()

    def test_render_pdf_call(self, merchant, render_pdf_mock, returned):
        render_pdf_mock.assert_called_once_with(merchant)
