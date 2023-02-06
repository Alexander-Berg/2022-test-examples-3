import uuid

import pytest

from mail.payments.payments.core.actions.interactions.yandex_pay_admin import MerchantModerationResultNotifyAction
from mail.payments.payments.core.entities.document import Document
from mail.payments.payments.core.entities.enums import DocumentType
from mail.payments.payments.interactions.yandex_pay_admin import YandexPayAdminClient
from mail.payments.payments.interactions.yandex_pay_admin.entities import Document as YandexPayAdminDocument
from mail.payments.payments.interactions.yandex_pay_admin.entities import YandexPayAdminDocumentType


@pytest.mark.asyncio
async def test_notify_yandex_pay_admin_moderation(mocker):
    mock = mocker.patch.object(YandexPayAdminClient, 'update_moderation', mocker.AsyncMock())
    partner_id = uuid.uuid4()
    verified = mocker.Mock()

    await MerchantModerationResultNotifyAction(
        partner_id=partner_id,
        verified=verified,
        documents=[
            Document(
                document_type=DocumentType.PROXY,
                path='/1',
                size=0,
            ),
            Document(
                document_type=DocumentType.PCI_DSS_CERT,
                path='/2',
                name='n-ame',
                size=0,
            ),
        ],
    ).run()

    mock.assert_called_once_with(
        partner_id=partner_id,
        verified=verified,
        documents=[
            YandexPayAdminDocument(
                type=YandexPayAdminDocumentType.POWER_OF_ATTORNEY,
                path='/1',
                name=None,
            ),
            YandexPayAdminDocument(
                type=YandexPayAdminDocumentType.PCI_DSS_CERT,
                path='/2',
                name='n-ame',
            ),
        ],
    )
