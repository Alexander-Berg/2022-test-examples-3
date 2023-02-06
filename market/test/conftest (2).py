import numpy as np
import pytest
import rclpy

from ymbot_behaviour_interface.action import BehaviourTreeAction
from ymbot_behaviour_interface.msg import RoutePlan, RouteSegment
from ymbot_behaviour_tree.behaviour_tree.move_goal_behaviour import MoveGoalBehaviour, MoveGoalBehaviourConfig
from ymbot_behaviour_tree.behaviour_tree.multi_move_and_mast_behaviour import MultiMoveAndMastBehaviour, \
    MultiMoveAndMastBehaviourConfig
from ymbot_behaviour_tree.behaviour_tree.multi_move_goal_behaviour import MultiMoveGoalBehaviour, \
    MultiMoveGoalBehaviourConfig
from ymbot_behaviour_tree.behaviour_tree.robot_behaviour_tree import RobotBehaviourTree, RobotBehaviourTreeConfig
from ymbot_behaviour_tree.behaviour_tree.task_and_idle_behaviour import TaskAndIdleBehaviour, TaskAndIdleBehaviourConfig
from ymbot_behaviour_tree.behaviour_tree_node import BehaviourTreeNodeConfig, BehaviourTreeNode
from ymbot_behaviour_tree.utils.action_client_mock import ActionClientMock
from ymbot_behaviour_tree.utils.mast_simulation_node_mock import MastSimulationNodeMock
from ymbot_behaviour_tree.utils.path_follower_mock import PathFollowerNodeMock
from ymbot_navigation.path_follower.path_follower_adapter import PathFollowerAdapterConfig
from ymbot_utils.position_array_2d import PositionArray2D


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


@pytest.fixture
def ros_node(executor):
    node = rclpy.node.Node("behaviour_tree")
    executor.add_node(node)
    yield node
    node.destroy_node()


@pytest.fixture
def path_follower_adapter_config():
    return PathFollowerAdapterConfig("cmd_vel", 10, "path_follower_action_test", True)


@pytest.fixture
def path_follower_mock(executor, path_follower_adapter_config):
    node = PathFollowerNodeMock(path_follower_adapter_config)
    executor.add_node(node)
    yield node
    node.destroy()
    node.destroy_node()


@pytest.fixture
def plan_trajectory():
    return PositionArray2D.from_vec(np.array([[1, 1, 0], [2, 2, 0]]))


@pytest.fixture
def plan_trajectory2():
    return PositionArray2D.from_vec(np.array([[2, 2, 0], [3, 2, 0]]))


@pytest.fixture
def move_goal_behaviour():
    return MoveGoalBehaviour(MoveGoalBehaviourConfig("path_follower_action_test", "move_goal"))


@pytest.fixture
def task_and_idle_move_goal_behaviour(move_goal_behaviour):
    behaviour = TaskAndIdleBehaviour(move_goal_behaviour, TaskAndIdleBehaviourConfig("task_status"))
    return behaviour


@pytest.fixture
def robot_behaviour_tree(ros_node, task_and_idle_move_goal_behaviour):
    tree = RobotBehaviourTree(ros_node, task_and_idle_move_goal_behaviour, RobotBehaviourTreeConfig(True))
    yield tree


@pytest.fixture
def setup_robot_behaviour_tree(robot_behaviour_tree, path_follower_mock):
    robot_behaviour_tree.setup()
    yield robot_behaviour_tree
    robot_behaviour_tree.shutdown()


@pytest.fixture
def behaviour_tree_node(ros_node, robot_behaviour_tree, path_follower_mock):
    node = BehaviourTreeNode(ros_node, robot_behaviour_tree, BehaviourTreeNodeConfig("robot_behaviour_action", 20))
    yield node
    node.shutdown()


@pytest.fixture
def action_client_mock(executor, one_segment_route_plan):
    node = ActionClientMock(one_segment_route_plan, BehaviourTreeAction, "robot_behaviour_action")
    executor.add_node(node)
    yield node
    node.destroy_node()


@pytest.fixture
def multi_move_goal_behaviour():
    return MultiMoveGoalBehaviour(MultiMoveGoalBehaviourConfig(
        path_follower_action_name="path_follower_action_test",
        route_goal_name="route_plan",
        move_goal_name="move_goal",
        current_task_id_name="current_task_id"
    ))


@pytest.fixture
def task_and_idle_multi_move_goal_behaviour(multi_move_goal_behaviour):
    behaviour = TaskAndIdleBehaviour(multi_move_goal_behaviour, TaskAndIdleBehaviourConfig("task_status"))
    return behaviour


@pytest.fixture
def multi_move_goal_robot_behaviour_tree(ros_node, task_and_idle_multi_move_goal_behaviour):
    tree = RobotBehaviourTree(ros_node, task_and_idle_multi_move_goal_behaviour, RobotBehaviourTreeConfig(True))
    yield tree


@pytest.fixture
def setup_multi_move_goal_robot_behaviour_tree(multi_move_goal_robot_behaviour_tree, path_follower_mock):
    multi_move_goal_robot_behaviour_tree.setup()
    yield multi_move_goal_robot_behaviour_tree
    multi_move_goal_robot_behaviour_tree.shutdown()


@pytest.fixture
def one_segment_route_plan(plan_trajectory):
    return BehaviourTreeAction.Goal(route_plan=RoutePlan(segments=[RouteSegment(path=plan_trajectory.as_path())]))


@pytest.fixture
def two_segment_route_plan(plan_trajectory, plan_trajectory2):
    return BehaviourTreeAction.Goal(route_plan=RoutePlan(segments=[
        RouteSegment(path=plan_trajectory.as_path()), RouteSegment(path=plan_trajectory2.as_path())]))


@pytest.fixture
def mast_simulation_node_mock(executor):
    node = MastSimulationNodeMock("mast_action", 0)
    executor.add_node(node)
    yield node
    node.destroy()
    node.destroy_node()


@pytest.fixture
def multi_move_and_mast_behaviour():
    return MultiMoveAndMastBehaviour(MultiMoveAndMastBehaviourConfig(
        path_follower_action_name="path_follower_action_test",
        route_goal_name="route_plan",
        move_goal_name="move_goal",
        current_task_id_name="current_task_id",
        maximal_mast_extension=1.,
        minimal_mast_extension=0.5,
        mast_action_name="mast_action"
    ))


@pytest.fixture
def multi_move_and_mast_behaviour_tree(path_follower_mock, mast_simulation_node_mock, ros_node,
                                       multi_move_and_mast_behaviour):
    behaviour = TaskAndIdleBehaviour(multi_move_and_mast_behaviour, TaskAndIdleBehaviourConfig("task_status"))
    behaviour_tree = RobotBehaviourTree(ros_node, behaviour, RobotBehaviourTreeConfig(True))
    behaviour_tree.setup()
    yield behaviour_tree
    behaviour_tree.shutdown()


@pytest.fixture
def two_segments_with_inventory(plan_trajectory, plan_trajectory2):
    return BehaviourTreeAction.Goal(route_plan=RoutePlan(segments=[
        RouteSegment(path=plan_trajectory.as_path()), RouteSegment(path=plan_trajectory2.as_path(), type=1)]))
