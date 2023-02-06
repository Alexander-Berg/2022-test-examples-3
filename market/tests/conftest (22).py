import pytest


@pytest.fixture
def value():
    return "pltqr"


@pytest.fixture
def left(data):
    return data.get('left')


@pytest.fixture
def top(data):
    return data.get('top')


@pytest.fixture
def width(data):
    return data.get('width')


@pytest.fixture
def height(data):
    return data.get('height')


@pytest.fixture
def border_size():
    return 10


@pytest.fixture
def image_shape():
    return 1080, 1920


@pytest.fixture
def image_width(image_shape):
    return image_shape[0]


@pytest.fixture
def image_height(image_shape):
    return image_shape[1]


@pytest.fixture
def scaled_x(image_width, left):
    return int(image_width * left)


@pytest.fixture
def scaled_y(image_height, top):
    return int(image_height * top)


@pytest.fixture
def scaled_width(image_width, width):
    return int(image_width * width)


@pytest.fixture
def scaled_height(image_height, height):
    return int(image_height * height)
