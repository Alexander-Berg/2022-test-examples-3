import pytest
import cv2

from lib.types.image import Image



@pytest.fixture
def image() -> Image:
    image_filename = "/home/ymbot-jetson/dev/py_inventory_mvp/test/types_test/data/test_image.png"
    return Image(cv2.imread(image_filename))
