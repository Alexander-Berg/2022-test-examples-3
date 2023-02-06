import time

import numpy as np
import py_trees_ros
from py_trees.common import Status


def test_init_multi_move_goal_robot_behaviour_tree(path_follower_mock, multi_move_goal_robot_behaviour_tree):
    assert isinstance(multi_move_goal_robot_behaviour_tree, py_trees_ros.trees.BehaviourTree)
    assert len(multi_move_goal_robot_behaviour_tree.root.children) == 2


def test_multi_move_goal_robot_behaviour_tree_setup(path_follower_mock, multi_move_goal_robot_behaviour_tree):
    multi_move_goal_robot_behaviour_tree.setup()
    multi_move_goal_robot_behaviour_tree.shutdown()


def test_multi_move_goal_robot_behaviour_tree_tick(setup_multi_move_goal_robot_behaviour_tree):
    setup_multi_move_goal_robot_behaviour_tree.tick()
    assert setup_multi_move_goal_robot_behaviour_tree.root.status == Status.RUNNING


def test_multi_move_goal_robot_behaviour_tree_tick_with_goal(setup_multi_move_goal_robot_behaviour_tree,
                                                             two_segment_route_plan):
    setup_multi_move_goal_robot_behaviour_tree.set_new_goal(two_segment_route_plan)
    setup_multi_move_goal_robot_behaviour_tree.tick()
    assert setup_multi_move_goal_robot_behaviour_tree.root.status == Status.RUNNING
    assert setup_multi_move_goal_robot_behaviour_tree.root.children[0].status == Status.RUNNING
    assert setup_multi_move_goal_robot_behaviour_tree.root.children[0].children[0].status == Status.RUNNING
    assert setup_multi_move_goal_robot_behaviour_tree.root.children[0].children[0].children[0].status == Status.FAILURE


def test_multi_move_goal_robot_behaviour_tree_tick_with_wait_success(executor,
                                                                     setup_multi_move_goal_robot_behaviour_tree,
                                                                     two_segment_route_plan, path_follower_mock):
    setup_multi_move_goal_robot_behaviour_tree.set_new_goal(two_segment_route_plan)
    for i in range(30):
        setup_multi_move_goal_robot_behaviour_tree.tick()
        executor.spin_once(0.1)
        time.sleep(0.01)
        if setup_multi_move_goal_robot_behaviour_tree.get_status() == Status.SUCCESS:
            break
    assert setup_multi_move_goal_robot_behaviour_tree.get_status() == Status.SUCCESS
    assert setup_multi_move_goal_robot_behaviour_tree.root.children[0].children[0].children[1].children[
               0].status == Status.SUCCESS
    assert setup_multi_move_goal_robot_behaviour_tree.root.status == Status.RUNNING
    assert np.all(path_follower_mock.position.translation == np.array([3, 2]))
