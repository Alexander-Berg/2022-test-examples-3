import re

import pytest

from hamcrest import assert_that, equal_to

from mail.payments.payments.interactions.zora_images.exceptions import ImageTooLargeError


class TestDownloadImage:
    @pytest.fixture
    def url(self):
        return 'http://image.test/dog.png'

    @pytest.fixture
    def image(self):
        return b'image'

    @pytest.fixture(autouse=True)
    def mock_download(self, mocker, aioresponses_mocker, zora_images_client, url, image):
        return aioresponses_mocker.get(
            re.compile(f'^{url}.*$'),
            body=image,
        )

    @pytest.mark.asyncio
    async def test_returns_image(self, zora_images_client, image, url):
        assert_that(
            await zora_images_client.get_image(url),
            equal_to(image),
        )

    class TestRaisesOnBigImage:
        @pytest.fixture(autouse=True)
        def set_limit(self, mocker):
            from mail.payments.payments.interactions import ZoraImagesClient
            mocker.patch.object(ZoraImagesClient, 'IMAGE_MAX_SIZE', 500)

        @pytest.fixture
        def image(self):
            return b'0' * 1000

        @pytest.mark.asyncio
        async def test_raises_on_big_image(self, zora_images_client, image, url):
            with pytest.raises(ImageTooLargeError):
                await zora_images_client.get_image(url)
