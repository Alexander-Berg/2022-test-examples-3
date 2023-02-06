import pytest
import yaml
import cv2
from lib.config.detector import DetectorConfig
from lib.types.image import Image


@pytest.fixture
def tf_config() -> DetectorConfig:
    config_filepath = "/home/ymbot-jetson/dev/py_inventory_mvp/data/detector/tf_config.yaml"
    with open(config_filepath) as f:
        data = yaml.safe_load(f)
        if not data:
            raise ValueError("Unable to load config")
        return DetectorConfig.Schema().load(data)


@pytest.fixture
def trt_config() -> DetectorConfig:
    config_filepath = "/home/ymbot-jetson/dev/py_inventory_mvp/data/detector/trt_config.yaml"
    with open(config_filepath) as f:
        data = yaml.safe_load(f)
        if not data:
            raise ValueError("Unable to load config")
        return DetectorConfig.Schema().load(data)


@pytest.fixture
def image() -> Image:
    image_filename = "/home/ymbot-jetson/dev/py_inventory_mvp/data/detector/test_detect_image.jpeg"
    return Image(cv2.imread(image_filename))


@pytest.fixture
def images_base_folder() -> str:
    return "/home/ymbot-jetson/dev/py_inventory_mvp/test/detector_test/data/gt/"
