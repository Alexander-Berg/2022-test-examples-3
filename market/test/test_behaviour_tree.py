import time

import action_msgs.msg
import py_trees.logging
import py_trees_ros.trees
from py_trees.common import Status
from py_trees.logging import Level

py_trees.logging.level = Level.DEBUG


def test_init_robot_behaviour_tree(path_follower_mock, robot_behaviour_tree):
    assert isinstance(robot_behaviour_tree, py_trees_ros.trees.BehaviourTree)
    assert len(robot_behaviour_tree.root.children) == 2


def test_setup(path_follower_mock, robot_behaviour_tree):
    robot_behaviour_tree.setup()
    robot_behaviour_tree.shutdown()


def test_tick(setup_robot_behaviour_tree):
    setup_robot_behaviour_tree.tick()
    assert setup_robot_behaviour_tree.root.status == Status.RUNNING


def test_tick_with_goal(setup_robot_behaviour_tree, one_segment_route_plan):
    setup_robot_behaviour_tree.set_new_goal(one_segment_route_plan)
    setup_robot_behaviour_tree.tick()
    assert setup_robot_behaviour_tree.root.status == Status.RUNNING
    assert setup_robot_behaviour_tree.root.children[0].status == Status.RUNNING
    assert setup_robot_behaviour_tree.root.children[0].children[0].status == Status.RUNNING
    assert setup_robot_behaviour_tree.root.children[0].children[0].children[0].status == Status.FAILURE


def test_tick_with_wait_success(executor, setup_robot_behaviour_tree, one_segment_route_plan):
    setup_robot_behaviour_tree.set_new_goal(one_segment_route_plan)
    for i in range(30):
        setup_robot_behaviour_tree.tick()
        executor.spin_once(0.1)
        time.sleep(0.01)
        if setup_robot_behaviour_tree.get_status() == Status.SUCCESS:
            break
    assert setup_robot_behaviour_tree.get_status() == Status.SUCCESS
    assert setup_robot_behaviour_tree.root.children[0].children[0].children[1].children[0].status == Status.SUCCESS
    assert setup_robot_behaviour_tree.root.status == Status.RUNNING


def test_wait_for_behaviour_server(executor, action_client_mock, behaviour_tree_node):
    assert action_client_mock.wait_for_server()


def test_behaviour_action_send_goal(executor, path_follower_mock, behaviour_tree_node,
                                    action_client_mock):
    result = action_client_mock.send_goal(executor)
    assert isinstance(result.result.code, int)
    assert result.result.code == 0
    assert result.status == action_msgs.msg.GoalStatus.STATUS_SUCCEEDED
