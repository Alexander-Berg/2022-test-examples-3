import pytest
from market.robotics.cv.library.py.toloka_reader.outputs import PointOutput, TolokaOuputType
from market.robotics.cv.library.py.toloka_reader.shapes import AnchorShape


@pytest.fixture
def data() -> dict:
    return {
        "shape": "point",
        "value": "pltqr",
        "left": 0.25,
        "top": 0.65
    }


@pytest.fixture
def output(data: dict) -> PointOutput:
    return PointOutput(data)


@pytest.fixture
def shape(output: PointOutput, image_width, image_height) -> AnchorShape:
    return output.to_shape(image_width, image_height)


def test_output(output: PointOutput, left, top):
    assert output.shape_type == TolokaOuputType.POINT
    assert output.anchor.left == left
    assert output.anchor.top == top


def test_get_scaled(output, image_width, image_height, scaled_x, scaled_y):
    x_, y_ = output.anchor.get_scaled(image_width, image_height)
    assert x_ == scaled_x
    assert y_ == scaled_y


def test_shape(shape: AnchorShape, scaled_x, scaled_y):
    assert isinstance(shape, AnchorShape)
    assert shape.x == scaled_x
    assert shape.y == scaled_y


def test_output_to_str(output: PointOutput):
    assert output.to_string() == "pltqr 0.25000 0.65000"


@pytest.fixture
def extra_data() -> dict:
    return {
        "shape": "Point",
        "value": "pltqr",
        "left": 0.25,
        "top": 0.65
    }


def test_wrong_output(extra_data, image_width, image_height):
    output = PointOutput(extra_data)
    assert output.shape_type == TolokaOuputType.UNKNOWN
    try:
        shape = output.to_shape(image_width, image_height)
        shape  # for usage check avoid
        assert False
    except ValueError:
        assert True


def test_shape_to_output(output: PointOutput, shape: AnchorShape, image_width, image_height, value):
    output_tested = PointOutput.from_shape(shape, image_width, image_height, value)
    assert output_tested == output


def test_export(output: PointOutput, data: dict):
    assert output.export() == data
