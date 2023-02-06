import numpy as np
from geometry_msgs.msg import Pose

from ymbot_utils.position_2d import Position2D


def test_translation(position_2d):
    translation = position_2d.translation
    assert isinstance(translation, np.ndarray)
    assert np.all(position_2d.translation == np.array([1, 2]))


def test_rotation(position_2d):
    rotation = position_2d.rotation
    assert rotation == 3.


def test_x(position_2d):
    assert position_2d.x == 1


def test_y(position_2d):
    assert position_2d.y == 2.


def test_from_vec(vector):
    position = Position2D.from_vec(vector)
    assert position.x == 4
    assert position.y == 5
    assert position.rotation == 6


def test_as_vec(position_2d):
    vector = position_2d.as_vec()
    assert isinstance(vector, np.ndarray)
    assert vector[0] == 1
    assert vector[1] == 2
    assert vector[2] == 3


def test_from_ros_pose(position_2d):
    pose = position_2d.as_ros_pose()
    assert isinstance(pose, Pose)
    assert pose.position.x == 1
    assert pose.position.y == 2
    assert pose.position.z == 0
    assert pose.orientation.x == 0
    assert pose.orientation.y == 0


def test_as_ros_pose(ros_pose):
    position = Position2D.from_ros_pose(ros_pose)
    assert position.x == 1
    assert position.y == 2
    assert position.rotation == 3


def test_mul(position_2d, position_2d_2):
    result = position_2d * position_2d_2
    assert abs(result.x + 3.6655700) < 1e-5
    assert abs(result.y + 2.3854824) < 1e-5
    assert abs(result.rotation - 2.716814) < 1e-5


def test_inv(position_2d):
    result = position_2d.inv()
    assert abs(result.x - 0.707752) < 1e-5
    assert abs(result.y - 2.121105) < 1e-5
    assert abs(result.rotation + 3) < 1e-5


def test_mul_inv(position_2d):
    result = position_2d.inv() * position_2d
    assert abs(result.x) < 1e-5
    assert abs(result.y) < 1e-5
    assert abs(result.rotation) < 1e-5


def test_apply_point(position_2d, point):
    result = position_2d.apply(point)
    assert len(result.shape) == 1
    assert result.shape[0] == 2
    assert abs(result[0] + 0.1311125) < 1e-5
    assert abs(result[1] - 1.1511275) < 1e-5


def test_apply_points(position_2d, points):
    result = position_2d.apply(points)
    assert len(result.shape) == 2
    assert result.shape[0] == 3
    assert result.shape[1] == 2
