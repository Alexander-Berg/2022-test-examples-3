import time

import rclpy

from ymbot_simulation.checkers.collision_checker_adapter_factory import CollisionCheckerAdapterFactory
from ymbot_simulation.checkers.goal_position_checker import GoalPositionChecker
from ymbot_simulation.checkers.position_checker import PositionChecker
from ymbot_simulation.checkers.timeout_checker import TimeoutChecker
from ymbot_simulation.experiment_runner.command_adapter import CommandAdapter
from ymbot_simulation.experiment_runner.experiment_runner import ExperimentRunner
from ymbot_simulation.experiment_runner.map_adapter import MapAdapter
from ymbot_simulation.experiment_runner.navigation_metric_manager import NavigationMetricManager
from ymbot_simulation.experiment_runner.robot_state import RobotState
from ymbot_simulation.experiment_runner.snapshot_saver import SnapshotSaver
from ymbot_simulation.utils.transform_receiver_factory import TransformReceiverFactory


class NavigationTestFactory:
    def __init__(self, node: rclpy.node.Node):
        self._node = node
        self._linear_tolerance = 0.5
        self._angle_tolerance = 3.15
        self._linear_velocity_tolerance = 1e-2
        self._angular_velocity_tolerance = 1e-2
        self._time_delta = 30  # seconds
        self._distance_deviation = 0.05
        self._log_time = 60  # seconds
        self._log_time_rate = 40

        self._transform_receiver_factory = TransformReceiverFactory()
        self._command_adapter = CommandAdapter()
        self._map_adapter = MapAdapter("set_map_config", "get_map", "set_robot_position", "set_point_cloud_config",
                                       "set_map")

    def get_navigation_test(self, start_position, goal, timeout, map_description, initial_map=None):
        robot_state = RobotState(self._transform_receiver_factory)
        goal_checker = GoalPositionChecker(robot_state, goal, self._linear_tolerance, self._angle_tolerance,
                                           self._linear_velocity_tolerance, self._angular_velocity_tolerance)
        collision_checker = CollisionCheckerAdapterFactory().make_robot_collision_checker_adapter()
        timeout_checker = TimeoutChecker(timeout, robot_state)
        position_checker = PositionChecker(self._time_delta, self._distance_deviation, robot_state)
        metric_manager = NavigationMetricManager(robot_state)
        snapshot_saver = SnapshotSaver(robot_state, self._map_adapter, map_description, map_description, goal,
                                       self._log_time_rate, self._log_time)
        self._map_adapter.set_start_position(start_position)
        time.sleep(0.5)
        self._map_adapter.set_map_config(map_description, initial_map)
        self._map_adapter.set_point_cloud(map_description)
        return ExperimentRunner(goal, [goal_checker, collision_checker, timeout_checker, position_checker],
                                self._command_adapter, metric_manager, snapshot_saver)

    def get_navigation_test_without_checkers(self, start_position, goal, timeout, map_description, initial_map=None):
        self._node.get_logger().info("[NavigationTestFactory] Start creating navigation test without checkers")
        robot_state = RobotState(self._transform_receiver_factory)
        self._node.get_logger().info("[NavigationTestFactory] Robot state is created")
        metric_manager = NavigationMetricManager(robot_state)
        self._node.get_logger().info("[NavigationTestFactory] Metric manager is created")
        snapshot_saver = SnapshotSaver(robot_state, self._map_adapter, map_description, map_description, goal,
                                       self._log_time_rate, self._log_time)
        self._node.get_logger().info("[NavigationTestFactory] Snapshot saver is created")
        self._node.get_logger().info("[NavigationTestFactory] Set start position")
        self._map_adapter.set_start_position(start_position)
        self._node.get_logger().info("[NavigationTestFactory] Start position is set")
        time.sleep(0.5)
        self._node.get_logger().info("[NavigationTestFactory] Set map config")
        self._map_adapter.set_map_config(map_description, initial_map)
        self._node.get_logger().info("[NavigationTestFactory] Map config is set")
        self._node.get_logger().info("[NavigationTestFactory] Set point cloud")
        self._map_adapter.set_point_cloud(map_description)
        self._node.get_logger().info("[NavigationTestFactory] Point cloud is set")
        return ExperimentRunner(goal, [], self._command_adapter, metric_manager, snapshot_saver)
