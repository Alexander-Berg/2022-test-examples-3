import logging
import time
from time import sleep

import action_msgs.msg
import geometry_msgs.msg
import numpy as np
import pytest
import rclpy.executors
from geometry_msgs.msg import PoseStamped
from nav_msgs.msg import Path
from rclpy.action import ActionClient
from rclpy.node import Node
from rclpy.type_support import check_for_type_support

from ymbot_navigation.path_follower.path_follower import PathFollower, PathFollowerConfig
from ymbot_navigation.path_follower.path_follower_adapter import PathFollowerAdapter, PathFollowerAdapterConfig
from ymbot_navigation.path_follower.trajectory_factory import TrajectoryFactory, TrajectoryFactoryConfig
from ymbot_navigation_interface.action import PathFollowerAction
from ymbot_utils.position_2d import Position2D


class TrajectoryPublisherMock:
    def __init__(self):
        pass

    def publish(self, message):
        pass


class PathFollowerMock(PathFollower):
    def __init__(self, maximal_linear_velocity, maximal_angular_velocity, velocity, is_done):
        super().__init__(PathFollowerConfig(maximal_linear_velocity, maximal_angular_velocity))
        self._velocity = velocity
        self._is_done = is_done

    def control(self, robot_position):
        return self._velocity

    def is_done(self):
        return self._is_done


class RobotStateMock:
    def __init__(self, position):
        self._position = position

    async def robot_position_async(self):
        return self._position


class ClientNode(rclpy.node.Node):
    def __init__(self, trajectory):
        super().__init__("test_client_node")
        self.get_logger().set_level(logging.DEBUG)
        self.result = None
        self.result_future = None
        self.goal_handle = None
        self._trajectory = trajectory
        self.velocity = None
        self._subscriber = self.create_subscription(geometry_msgs.msg.Twist, "cmd_vel", self._velocity_callback,
                                                    qos_profile=100)
        self._action_client = ActionClient(self, PathFollowerAction, 'path_follower_action')

    def _velocity_callback(self, message: geometry_msgs.msg.Twist):
        self.velocity = (message.linear.x, message.angular.z)

    def wait_for_server(self):
        return self._action_client.wait_for_server(timeout_sec=1)

    def send_goal(self, executor):
        future = self.send_goal_async()
        self.wait(executor, future.done, 10)
        self.wait(executor, lambda: self.result_future is not None, 10)
        self.wait(executor, self.result_future.done, 100)
        self.wait(executor, lambda: self.result is not None, 10)
        return self.result

    @staticmethod
    def wait(executor, waitable, maximal_iterations, timeout_sec=0.1):
        for i in range(maximal_iterations):
            if waitable():
                return
            executor.spin_once(timeout_sec)
        assert waitable()

    def _make_goal_message(self):
        goal_message = PathFollowerAction.Goal(goal_path=Path())
        goal_message.goal_path.poses = [PoseStamped(
            pose=Position2D.from_vec(x).as_ros_pose()) for x in self._trajectory]
        return goal_message

    def send_goal_async(self):
        self.wait_for_server()
        goal_message = self._make_goal_message()
        future = self._action_client.send_goal_async(goal_message)
        self.result_future = None
        future.add_done_callback(self._response_callback)
        return future

    def _response_callback(self, future):
        goal_handle = future.result()
        self.goal_handle = goal_handle
        self.result_future = goal_handle.get_result_async()
        self.result = None
        self.result_future.add_done_callback(self._result_callback)

    def _result_callback(self, future):
        self.result = future.result()

    def send_goal_and_not_wait(self, executor):
        future = self.send_goal_async()
        self.wait(executor, future.done, 10)
        self.wait(executor, lambda: self.goal_handle is not None, 10)

    def cancel_goal(self, executor):
        future = self.goal_handle.cancel_goal_async()
        self.wait(executor, future.done, 100)
        self.wait(executor, lambda: self.result_future is not None, 10)
        self.wait(executor, self.result_future.done, 30)
        self.wait(executor, lambda: self.result is not None, 30)
        return self.result

    def destroy_node(self):
        self._action_client.destroy()


@pytest.fixture
def setup():
    rclpy.init()
    yield
    rclpy.shutdown()


@pytest.fixture
def executor(setup):
    executor_object = rclpy.executors.MultiThreadedExecutor(num_threads=4)
    yield executor_object
    executor_object.shutdown()


@pytest.fixture(params=[2.])
def velocity(request):
    return request.param, request.param


@pytest.fixture
def running_path_follower(velocity):
    return PathFollowerMock(1, 1, velocity, False)


@pytest.fixture
def finished_path_follower():
    return PathFollowerMock(1, 1, (1, 1), True)


@pytest.fixture
def robot_state():
    return RobotStateMock(Position2D(1, 1, 1))


@pytest.fixture
def ros_node(executor):
    node = Node("path_follower")
    executor.add_node(node)
    yield node
    node.destroy_node()


@pytest.fixture
def trajectory_factory():
    return TrajectoryFactory(TrajectoryFactoryConfig(False, False, 1.))


@pytest.fixture
def trajectory_publisher():
    return TrajectoryPublisherMock()


@pytest.fixture
def finished_path_follower_adapter_node(ros_node, finished_path_follower, robot_state, trajectory_factory,
                                        trajectory_publisher):
    adapter = PathFollowerAdapter(ros_node, finished_path_follower, robot_state, trajectory_factory,
                                  PathFollowerAdapterConfig("cmd_vel", 10, "path_follower_action", True),
                                  trajectory_publisher)
    yield adapter
    adapter.destroy()


@pytest.fixture
def running_path_follower_adapter_node(ros_node, running_path_follower, robot_state, trajectory_factory,
                                       trajectory_publisher):
    adapter = PathFollowerAdapter(ros_node, running_path_follower, robot_state, trajectory_factory,
                                  PathFollowerAdapterConfig("cmd_vel", 10, "path_follower_action", True),
                                  trajectory_publisher)
    yield adapter
    adapter.destroy()


@pytest.fixture
def short_path_client_node(executor):
    node = ClientNode(np.array([[0, 0, 0]]))
    executor.add_node(node)
    yield node
    node.destroy_node()


@pytest.fixture
def long_path_client_node(executor):
    client_node = ClientNode(np.array([[0, 0, 0], [1, 1, 1]]))
    executor.add_node(client_node)
    yield client_node
    client_node.destroy_node()


def test_check_for_type_support():
    check_for_type_support(PathFollowerAction)


def test_node(ros_node, executor):
    executor.spin_once(0.1)


def test_wait_for_server(finished_path_follower_adapter_node, long_path_client_node):
    assert long_path_client_node.wait_for_server()


def test_finished_path_follower_long_path_client_node_action_server(ros_node, executor,
                                                                    finished_path_follower_adapter_node,
                                                                    long_path_client_node):
    result = long_path_client_node.send_goal(executor)
    assert isinstance(result.result.status, bool)
    assert result.result.status
    assert result.status == action_msgs.msg.GoalStatus.STATUS_SUCCEEDED


def test_finished_path_follower_short_path_client_node_action_server(ros_node, executor,
                                                                     finished_path_follower_adapter_node,
                                                                     short_path_client_node):
    result = short_path_client_node.send_goal(executor)
    assert isinstance(result.result.status, bool)
    assert not result.result.status
    assert result.status == action_msgs.msg.GoalStatus.STATUS_ABORTED


def test_zero_velocity_after_finish_path_follower_finish(ros_node, executor, finished_path_follower_adapter_node,
                                                         long_path_client_node):
    _ = long_path_client_node.send_goal(executor)
    long_path_client_node.wait(executor, lambda: long_path_client_node.velocity is not None, 10)
    assert long_path_client_node.velocity is not None
    assert long_path_client_node.velocity[0] == 0 and long_path_client_node.velocity[1] == 0


def test_cancel_goal(ros_node, executor, running_path_follower_adapter_node, long_path_client_node):
    long_path_client_node.send_goal_and_not_wait(executor)
    sleep(0.3)
    result = long_path_client_node.cancel_goal(executor)
    assert isinstance(result.result.status, bool)
    assert not result.result.status
    assert result.status == action_msgs.msg.GoalStatus.STATUS_CANCELED


def test_velocity_running_path_follower(ros_node, executor, running_path_follower_adapter_node, long_path_client_node,
                                        velocity):
    long_path_client_node.send_goal_and_not_wait(executor)
    long_path_client_node.wait(executor, lambda: long_path_client_node.velocity is not None, 10)
    assert long_path_client_node.velocity[0] == velocity[0]
    assert long_path_client_node.velocity[1] == velocity[1]
    long_path_client_node.cancel_goal(executor)
    long_path_client_node.wait(executor, lambda: (long_path_client_node.velocity[0] == 0) and
                                                 (long_path_client_node.velocity[1] == 0), 10)
    assert long_path_client_node.velocity[0] == 0
    assert long_path_client_node.velocity[1] == 0
