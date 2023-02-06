import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, greater_than, has_entries, has_properties, match_equality

from mail.payments.payments.core.actions.order.download_image import DownloadImageAction
from mail.payments.payments.core.actions.order.send_to_history import SendToHistoryOrderAction
from mail.payments.payments.core.entities.enums import TaskState, TaskType
from mail.payments.payments.core.entities.image import Image
from mail.payments.payments.interactions.avatars import AvatarsClient
from mail.payments.payments.interactions.zora_images import ZoraImagesClient


@pytest.fixture
def image_bytes():
    return b'The quick brown fox jumps over the lazy dog'


@pytest.fixture
def md5():
    return '9e107d9d372bb6826bd81d3542a419d6'


@pytest.fixture
def sha256():
    return 'd7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592'


def test_get_digest(image_bytes, md5, sha256):
    md5 = '9e107d9d372bb6826bd81d3542a419d6'
    sha256 = 'd7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592'
    assert DownloadImageAction._get_digest(image_bytes) == (md5, sha256)


class TestUpdateDependants:
    @pytest.fixture
    async def image(self, create_image):
        return await create_image()

    @pytest.fixture
    async def image_orig(self, create_image):
        return await create_image()

    @pytest.fixture
    async def item(self, storage, order, create_item, image_orig):
        return await create_item(order=order, image_id=image_orig.image_id)

    @pytest.fixture
    async def returned(self, item, image, image_orig):
        action = DownloadImageAction(
            uid=image_orig.uid, image_id=image_orig.image_id
        )
        async with action.storage_setter(transact=action.transact):
            return await action._update_dependants(image=image, image_orig=image_orig)

    @pytest.mark.asyncio
    async def test_image_orig_deleted(self, returned, storage, image_orig):
        with pytest.raises(Image.DoesNotExist):
            await storage.image.get(uid=image_orig.uid, image_id=image_orig.image_id)

    @pytest.mark.asyncio
    async def test_item_image_id_updated(self, returned, storage, item, image):
        actual_item = await storage.item.get(uid=item.uid, order_id=item.order_id, product_id=item.product_id)
        assert_that(actual_item.image_id, equal_to(image.image_id))

    @pytest.mark.asyncio
    async def test_order_revision_increased(self, returned, storage, order):
        actual_order = await storage.order.get(uid=order.uid, order_id=order.order_id)
        assert_that(actual_order.revision, greater_than(order.revision))

    @pytest.mark.asyncio
    async def test_send_to_history_scheduled(self, returned, storage, order):
        tasks = await alist(storage.task.find(task_type=TaskType.RUN_ACTION))
        task = next(
            task
            for task in tasks
            if task.state == TaskState.PENDING and task.action_name == SendToHistoryOrderAction.action_name
        )
        assert_that(
            task.params,
            has_entries({
                'action_kwargs': {
                    'uid': order.uid, 'order_id': order.order_id
                },
            })
        )


class TestHandle:
    @pytest.fixture
    def image_url(self):
        return 'http://image.test/dog.png'

    @pytest.fixture
    def avatars_path(self):
        return 'mds/path'

    @pytest.fixture(autouse=True)
    def mock_download_image(self, mocker, coromock, image_bytes):
        return mocker.patch.object(ZoraImagesClient, 'get_image', coromock(image_bytes))

    @pytest.fixture(autouse=True)
    def mock_avatars(self, mocker, coromock, avatars_path):
        mock = mocker.Mock()
        mock.url = avatars_path
        return mocker.patch.object(AvatarsClient, 'upload', coromock(mock))

    @pytest.fixture(autouse=True)
    def mock_get_digest(self, mocker, md5, sha256):
        return mocker.patch.object(DownloadImageAction, '_get_digest', mocker.Mock(return_value=(md5, sha256)))

    @pytest.fixture(autouse=True)
    def mock_update_dependants(self, mocker, md5, sha256, coromock):
        return mocker.patch.object(DownloadImageAction, '_update_dependants', coromock())

    @pytest.fixture
    async def image(self, create_image, image_url):
        return await create_image(url=image_url, stored_path=None)

    @pytest.fixture
    def returned_func(self, image):
        async def returned(uid=image.uid, image_id=image.image_id, **kwargs):
            return await DownloadImageAction(
                uid=uid, image_id=image_id, **kwargs
            ).run()

        return returned

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_calls_download_image(self, returned, mock_download_image, image_url):
        mock_download_image.assert_called_once_with(image_url)

    def test_calls_get_digest(self, returned, mock_get_digest, image_bytes):
        mock_get_digest.assert_called_once_with(image_bytes)

    @pytest.mark.asyncio
    async def test_sets_digest(self, returned, storage, image, avatars_path, md5, sha256, image_url):
        image = await storage.image.get_by_digest(uid=image.uid, md5=md5, sha256=sha256)
        assert_that(
            image,
            has_properties({
                'uid': image.uid,
                'url': image_url,
                'md5': md5,
                'sha256': sha256,
                'stored_path': avatars_path,
            })
        )

    def test_calls_upload_image(self, returned, mock_avatars, image_bytes):
        mock_avatars.assert_called_once_with(image_bytes)

    @pytest.mark.asyncio
    async def test_calls_update_dependants(self, returned, mock_update_dependants, storage, image):
        image = await storage.image.get(uid=image.uid, image_id=image.image_id)
        mock_update_dependants.assert_called_once_with(
            image=match_equality(
                has_properties({
                    'image_id': image.image_id,
                }),
            ),
            image_orig=match_equality(
                has_properties({
                    'image_id': image.image_id,
                })
            ),
        )

    class TestImageAlreadyDownloaded:
        @pytest.fixture
        async def image(self, create_image, image_url):
            return await create_image(url=image_url, stored_path='avatars/path')

        def test_image_already_downloaded_not_calls_download_image(self, returned, mock_download_image):
            mock_download_image.assert_not_called()

    class TestWhenImageAlreadyExists:
        @pytest.fixture(autouse=True)
        async def existing_image(self, storage, image, md5, sha256, image_url, avatars_path):
            return await storage.image.create(
                Image(uid=image.uid, url=image_url, md5=md5, sha256=sha256, stored_path=avatars_path)
            )

        def test_image_exists__not_calls_upload_image(self, returned, mock_avatars):
            mock_avatars.assert_not_called()

        def test_image_exists__calls_update_dependnts(self, returned, mock_update_dependants, image, existing_image):
            mock_update_dependants.assert_called_once_with(
                image=match_equality(
                    has_properties({
                        'image_id': existing_image.image_id,
                    }),
                ),
                image_orig=match_equality(
                    has_properties({
                        'image_id': image.image_id,
                    })
                ),
            )
