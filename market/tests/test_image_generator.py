import pytest
import os.path
from PIL import Image
from market.robotics.cv.library.py.synthgen import generator
from typing import List

# only for local debug and visualisation
SAVE_RESULTS = False
SAVE_RESULTS_FILEPATH = "/Users/ilinvalery/Downloads/test_generation"


def save_test_folder() -> str:
    path = SAVE_RESULTS_FILEPATH
    if not os.path.exists(path):
        os.makedirs(path)
    return path


def save_image(image: Image.Image, filename, extension='png'):
    if SAVE_RESULTS:
        path = os.path.join(save_test_folder(), f"{filename}.{extension}")
        image.save(path)


@pytest.fixture
def crops(base_dir) -> List[Image.Image]:
    return [
        Image.open(os.path.join(base_dir, "1_0.jpg")),
        Image.open(os.path.join(base_dir, "1_0.jpg")),
        Image.open(os.path.join(base_dir, "1_0.jpg")),
        Image.open(os.path.join(base_dir, "1_0.jpg")),
        Image.open(os.path.join(base_dir, "1_0.jpg")),
    ]


@pytest.fixture
def background(base_dir) -> Image.Image:
    return Image.open(os.path.join(base_dir, "frame0_9468_1645021787.8854368.jpeg"))


def test_render(background, crops, config):
    import time
    image_gen = generator.ImageGenerator(config)
    for i in range(10):
        filename = f"generated_{time.time()}"
        generated, boxes = image_gen(background, crops)
        assert background.size == generated.size
        assert len(boxes) == len(crops)
        if SAVE_RESULTS:
            save_image(generated.convert('RGB'), filename, extension="jpeg")
            save_image(generator.visualize(generated, boxes).convert('RGB'), filename + "vis", extension="jpeg")
