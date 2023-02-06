import aiohttp
import pytest

from hamcrest import assert_that, has_entries, is_


class TestDocumentGet:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.document import GetDocumentsAction
        return mock_action(GetDocumentsAction)

    @pytest.fixture
    async def response(self, payments_client, merchant):
        return await payments_client.get(f'/v1/document/{merchant.uid}')

    def test_params(self, merchant, response, action):
        action.assert_called_once_with(uid=merchant.uid)


class TestDocumentPost:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.document import UploadDocumentAction
        return mock_action(UploadDocumentAction)

    @pytest.fixture
    async def response(self, payments_client, merchant):
        with aiohttp.MultipartWriter('passport') as mpwriter:
            mpwriter.append(b'some data')
            return await payments_client.post(
                f'/v1/document/{merchant.uid}',
                data=mpwriter,
            )

    def test_params(self, merchant, response, action):
        assert_that(
            action.call_args[1],
            has_entries({
                'uid': merchant.uid,
                'reader': is_(aiohttp.MultipartReader),
            })
        )


class TestDocumentByPathDelete:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.document import DeleteDocumentAction
        return mock_action(DeleteDocumentAction)

    @pytest.fixture
    async def response(self, payments_client, merchant, path):
        return await payments_client.delete(f'/v1/document/{merchant.uid}/{path}')

    def test_params(self, merchant, path, response, action):
        action.assert_called_once_with(uid=merchant.uid, path=path)


class TestDocumentDownloadGet:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.document import DownloadDocumentAction
        return mock_action(DownloadDocumentAction)

    @pytest.fixture
    async def response(self, payments_client, merchant, path):
        return await payments_client.get(f'/v1/document/{merchant.uid}/{path}/download')

    def test_params(self, merchant, path, response, action):
        action.assert_called_once_with(uid=merchant.uid, path=path)
