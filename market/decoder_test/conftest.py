import pytest
import yaml
import cv2

from lib.config.decoder import DecoderConfig
from lib.types.image import Image


@pytest.fixture
def config() -> DecoderConfig:
    config_filepath = "/home/ymbot-jetson/dev/py_inventory_mvp/data/decoder/config.yaml"
    with open(config_filepath) as f:
        data = yaml.safe_load(f)
        if not data:
            raise ValueError("Unable to load config")
        return DecoderConfig.Schema().load(data)


@pytest.fixture
def image() -> Image:
    image_filename = "/home/ymbot-jetson/dev/py_inventory_mvp/test/decoder_test/data/test_decode_image.png"
    return Image(cv2.imread(image_filename))
