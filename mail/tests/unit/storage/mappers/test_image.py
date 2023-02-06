import pytest

from hamcrest import assert_that, equal_to, has_properties

from mail.payments.payments.core.entities.image import Image
from mail.payments.payments.storage.exceptions import ImageHashAlreadyExistsStorageError


@pytest.fixture
def stored_path():
    return 'stored/path'


@pytest.fixture
def image_entity(merchant, stored_path):
    return Image(
        uid=merchant.uid,
        url='http://image-url.test',
        md5='md5',
        sha256='sha256',
        stored_path=stored_path,
    )


@pytest.fixture
async def image(storage, image_entity):
    return await storage.image.create(image_entity)


class TestCreate:
    @pytest.mark.asyncio
    async def test_success(self, storage, image_entity):
        created_image = await storage.image.create(image_entity)
        image_entity.image_id = created_image.image_id
        image_entity.created = created_image.created
        image_entity.updated = created_image.updated
        assert_that(
            created_image,
            equal_to(image_entity),
        )

    @pytest.mark.asyncio
    async def test_conflict(self, storage, image, stored_path):
        new_image = Image(
            uid=image.uid,
            url='http://image.test',
            md5=image.md5,
            sha256=image.sha256,
        )
        with pytest.raises(ImageHashAlreadyExistsStorageError):
            await storage.image.create(new_image)


class TestGet:
    @pytest.mark.asyncio
    async def test_found(self, storage, image):
        assert_that(
            await storage.image.get(uid=image.uid, image_id=image.image_id),
            equal_to(image),
        )

    @pytest.mark.asyncio
    async def test_not_found(self, storage, image):
        with pytest.raises(Image.DoesNotExist):
            await storage.image.get(uid=image.uid, image_id=image.image_id + 1),


class TestGetByDigest:
    @pytest.mark.asyncio
    async def test_found(self, storage, image):
        assert_that(
            await storage.image.get_by_digest(uid=image.uid, md5=image.md5, sha256=image.sha256),
            equal_to(image),
        )

    @pytest.mark.asyncio
    async def test_not_found(self, storage, image):
        with pytest.raises(Image.DoesNotExist):
            await storage.image.get_by_digest(uid=image.uid, md5=image.md5 + 'extra', sha256=image.sha256),


class TestSave:
    @pytest.mark.asyncio
    async def test_success(self, storage, image, stored_path):
        path = 'new_stored_path'
        image.stored_path = path
        saved_image = await storage.image.save(image)
        assert_that(
            saved_image,
            has_properties({
                'stored_path': path,
            }),
        )

    @pytest.mark.asyncio
    async def test_conflict(self, storage, image, stored_path):
        new_image = Image(
            uid=image.uid,
            url='http://image.test',
        )
        new_image = await storage.image.create(new_image)
        new_image.md5 = image.md5
        new_image.sha256 = image.sha256

        with pytest.raises(ImageHashAlreadyExistsStorageError):
            await storage.image.create(new_image)


@pytest.mark.asyncio
async def test_delete(storage, image, stored_path):
    await storage.image.delete(image)
    with pytest.raises(Image.DoesNotExist):
        await storage.image.get(uid=image.uid, image_id=image.image_id)
