import pytest
from market.robotics.cv.library.py.toloka_reader.outputs import RectangleOutput, TolokaOuputType
from market.robotics.cv.library.py.toloka_reader.shapes import RectangleShape


@pytest.fixture
def data(value) -> dict:
    return {
        "shape": "rectangle",
        "value": value,
        "left": 0.0,
        "top": 0.1,
        "width": 0.5,
        "height": 0.4
    }


@pytest.fixture
def output(data: dict) -> RectangleOutput:
    return RectangleOutput(data)


@pytest.fixture
def shape(output: RectangleOutput, image_width, image_height) -> RectangleShape:
    return output.to_shape(image_width, image_height)


def test_output(output: RectangleOutput, image_width, image_height, left, top, width, height):
    assert output.shape_type == TolokaOuputType.RECTANGLE
    assert output.anchor.left == left
    assert output.anchor.top == top
    assert output.width == width
    assert output.height == height


def test_shape(shape: RectangleShape, scaled_x, scaled_y, scaled_width, scaled_height):
    assert isinstance(shape, RectangleShape)

    assert shape.x == scaled_x
    assert shape.y == scaled_y
    assert shape.width == scaled_width
    assert shape.height == scaled_height

    assert shape.get_bbox() == (scaled_x, scaled_y, scaled_width, scaled_height)
    assert shape.get_pil_bbox() == (scaled_x, scaled_y, scaled_x +
                                    scaled_width, scaled_y + scaled_height)


def test_output_to_str(output: RectangleShape):
    assert output.to_string() == "pltqr 0.00000 0.10000 0.50000 0.50000"


def test_rectangle():
    r1 = RectangleShape(x=200, y=745, width=349, height=349)
    r2 = RectangleShape(x=449, y=1508, width=377, height=379)
    intersection_rect = r1.intersect(r2)
    assert isinstance(intersection_rect, RectangleShape)
    assert intersection_rect.square() == 0


def test_intersection():
    rects = [RectangleShape(x=37, y=1193, width=263, height=261),
             None,
             RectangleShape(x=-20, y=-132, width=263, height=261),
             RectangleShape(x=484, y=-90, width=263, height=261)]

    rect_for_test = RectangleShape(x=340, y=176, width=263, height=261)
    assert not rect_for_test.list_intersection(rects)


def test_shape_to_output(output: RectangleOutput, shape: RectangleShape, image_width, image_height, value):
    output_tested = RectangleOutput.from_shape(shape, image_width, image_height, value)
    assert output_tested == output


def test_export(output: RectangleOutput, data: dict):
    assert output.export() == data


def test_near_border(shape: RectangleShape, image_width, image_height, border_size):
    assert shape.near_border(image_width, image_height, border_size)
