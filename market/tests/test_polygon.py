import pytest
from market.robotics.cv.library.py.toloka_reader.outputs import PolygonOutput, TolokaOuputType
from market.robotics.cv.library.py.toloka_reader.shapes import PolygonShape, RectangleShape


@pytest.fixture
def data() -> dict:
    return {
        "shape": "polygon",
        "value": "pltqr",
        "points": [
            {
                "left": 0.0,
                "top": 0.0
            },
            {
                "left": 0.5,
                "top": 0.0
            },
            {
                "left": 0.5,
                "top": 0.5
            },
            {
                "left": 0.0,
                "top": 0.5
            },
        ]
    }


@pytest.fixture
def points(data):
    return data.get('points')


@pytest.fixture
def output(data: dict) -> PolygonOutput:
    return PolygonOutput(data)


@pytest.fixture
def shape(output: PolygonOutput, image_width, image_height) -> PolygonShape:
    return output.to_shape(image_width, image_height)


def test_output(output: PolygonOutput, points, image_width, image_height):
    assert output.shape_type == TolokaOuputType.POLYGON
    assert output.value == "pltqr"
    assert len(output.points) == len(points)

    for i, anchor in enumerate(output.points):
        point: dict = points[i]
        left = point.get('left')
        top = point.get('top')
        assert left == anchor.left
        assert top == anchor.top
        # get_scaled
        x_, y_ = anchor.get_scaled(image_width, image_height)
        assert x_ == int(image_width * left)
        assert y_ == int(image_height * top)


def test_shape(shape: PolygonShape, output: PolygonOutput, points, image_width, image_height):
    assert isinstance(shape, PolygonShape)

    assert len(shape.anchors) == len(points)
    assert len(shape.anchors) == len(output.points)
    for i, anchor in enumerate(shape.anchors):
        point: dict = points[i]
        left = point.get('left')
        top = point.get('top')
        assert anchor.x == int(image_width * left)
        assert anchor.y == int(image_height * top)


def test_output_to_str(output: PolygonOutput):
    assert output.to_string() == "pltqr 0.00000 0.00000 0.50000 0.00000 0.50000 0.50000 0.00000 0.50000"


def test_converting_to_bbox(shape: PolygonShape):
    assert shape.get_bbox() == (0, 0, 540, 960)
    assert shape.get_pil_bbox() == (0, 0, 540, 960)
    assert shape.to_rectangle() == RectangleShape(0, 0, 540, 960)


def test_shape_to_output(output: PolygonOutput, shape: PolygonShape, image_width, image_height, value):
    output_tested = PolygonOutput.from_shape(shape, image_width, image_height, value)
    assert output_tested == output


def test_export(output: PolygonOutput, data: dict):
    assert output.export() == data


def test_near_border(shape: PolygonShape, image_width, image_height, border_size):
    assert shape.near_border(image_width, image_height, border_size)
