import numpy as np

from ymbot_utils.position_array_2d import PositionArray2D


def test_from_vec(vector_2d):
    position_array = PositionArray2D.from_vec(vector_2d)
    assert len(position_array) == 4
    assert position_array[0].x == 0
    assert position_array[0].y == 1
    assert position_array[0].rotation == 2


def test_as_vector(position_array_2d):
    vector = position_array_2d.as_vec()
    assert len(vector.shape) == 2
    assert vector.shape[0] == 4
    assert vector.shape[1] == 3


def test_mul_position_to_position_array(position_array_2d, position_2d):
    result = position_array_2d * position_2d
    assert isinstance(result, PositionArray2D)
    assert len(result) == 4
    assert abs(result[0].x + 2.23474) < 1e-5
    assert abs(result[0].y - 1.07700) < 1e-5
    assert abs(result[0].rotation + 1.283185) < 1e-5


def test_mul_position_array_to_position(position_array_2d, position_2d):
    result = position_2d * position_array_2d
    assert isinstance(result, PositionArray2D)
    assert len(result) == 4
    assert abs(result[0].x - 0.858879) < 1e-5
    assert abs(result[0].y - 1.01000) < 1e-5
    assert abs(result[0].rotation + 1.283185) < 1e-5


def test_mul(position_array_2d):
    result = position_array_2d * position_array_2d
    assert isinstance(result, PositionArray2D)
    assert len(result) == 4


def test_inv(position_array_2d):
    result = position_array_2d.inv() * position_array_2d
    assert isinstance(result, PositionArray2D)
    assert len(result) == 4
    assert abs(result[0].x) == 0
    assert abs(result[0].y) == 0
    assert abs(result[0].rotation) == 0


def test_apply(position_array_2d, points4):
    result = position_array_2d.apply(points4)
    assert isinstance(result, np.ndarray)
    assert len(result.shape) == 2
    assert result.shape[0] == 4
    assert result.shape[1] == 2
    assert abs(result[0, 0] + 0.90929) < 1e-5
    assert abs(result[0, 1] - 0.58385) < 1e-5
