import time

import numpy as np
import py_trees_ros
from py_trees.common import Status


def test_init_multi_move_and_mast_robot_behaviour_tree(path_follower_mock, mast_simulation_node_mock,
                                                       multi_move_and_mast_behaviour_tree):
    assert isinstance(multi_move_and_mast_behaviour_tree, py_trees_ros.trees.BehaviourTree)
    assert len(multi_move_and_mast_behaviour_tree.root.children) == 2


def test_multi_move_and_mast_robot_behaviour_tree_tick(multi_move_and_mast_behaviour_tree):
    multi_move_and_mast_behaviour_tree.tick()
    assert multi_move_and_mast_behaviour_tree.root.status == Status.RUNNING


def test_multi_move_and_mast_robot_behaviour_tree_tick_with_goal(multi_move_and_mast_behaviour_tree,
                                                                 two_segment_route_plan):
    multi_move_and_mast_behaviour_tree.set_new_goal(two_segment_route_plan)
    multi_move_and_mast_behaviour_tree.tick()
    assert multi_move_and_mast_behaviour_tree.root.status == Status.RUNNING
    assert multi_move_and_mast_behaviour_tree.root.children[0].status == Status.RUNNING
    assert multi_move_and_mast_behaviour_tree.root.children[0].children[0].status == Status.RUNNING
    assert multi_move_and_mast_behaviour_tree.root.children[0].children[0].children[0].status == Status.FAILURE


def test_multi_move_and_mast_robot_behaviour_tree_tick_with_wait_success(executor,
                                                                         multi_move_and_mast_behaviour_tree,
                                                                         two_segment_route_plan, path_follower_mock):
    multi_move_and_mast_behaviour_tree.set_new_goal(two_segment_route_plan)
    for i in range(30):
        multi_move_and_mast_behaviour_tree.tick()
        executor.spin_once(0.1)
        time.sleep(0.01)
        if multi_move_and_mast_behaviour_tree.get_status() == Status.SUCCESS:
            break
    assert multi_move_and_mast_behaviour_tree.get_status() == Status.SUCCESS
    assert multi_move_and_mast_behaviour_tree.root.children[0].children[0].children[1].children[
               0].status == Status.SUCCESS
    assert multi_move_and_mast_behaviour_tree.root.status == Status.RUNNING
    assert np.all(path_follower_mock.position.translation == np.array([3, 2]))


def test_multi_move_and_mast_robot_behaviour_tree_tick_with_wait_success_with_mast(executor,
                                                                                   multi_move_and_mast_behaviour_tree,
                                                                                   two_segments_with_inventory,
                                                                                   path_follower_mock,
                                                                                   mast_simulation_node_mock):
    multi_move_and_mast_behaviour_tree.set_new_goal(two_segments_with_inventory)
    for i in range(50):
        multi_move_and_mast_behaviour_tree.tick()
        executor.spin_once(0.1)
        time.sleep(0.01)
        if multi_move_and_mast_behaviour_tree.get_status() == Status.SUCCESS:
            break
    assert multi_move_and_mast_behaviour_tree.get_status() == Status.SUCCESS
    assert multi_move_and_mast_behaviour_tree.root.children[0].children[0].children[1].children[
               0].status == Status.SUCCESS
    assert multi_move_and_mast_behaviour_tree.root.status == Status.RUNNING
    assert np.all(path_follower_mock.position.translation == np.array([3, 2]))
    assert len(mast_simulation_node_mock.mast_extension_history) == 3
    assert mast_simulation_node_mock.mast_extension_history[0] == 0.
    assert mast_simulation_node_mock.mast_extension_history[1] == 1.
    assert mast_simulation_node_mock.mast_extension_history[2] == 0.5
