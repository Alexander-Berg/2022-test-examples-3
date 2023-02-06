import numpy as np
import pytest

from ymbot_utils.position_2d import Position2D
from ymbot_utils.position_array_2d import PositionArray2D


@pytest.fixture
def position_2d():
    return Position2D(1, 2, 3)


@pytest.fixture
def vector():
    return np.array([4, 5, 6])


@pytest.fixture
def vector_2d():
    return np.arange(12).reshape((4, 3))


@pytest.fixture
def ros_pose(position_2d):
    return position_2d.as_ros_pose()


@pytest.fixture
def position_2d_2():
    return Position2D(4, 5, 6)


@pytest.fixture
def point():
    return np.ones(2)


@pytest.fixture
def points():
    return np.arange(6).reshape((3, 2))


@pytest.fixture
def points4():
    return np.arange(8).reshape((4, 2))


@pytest.fixture
def position_array_2d(vector_2d):
    return PositionArray2D.from_vec(vector_2d)
