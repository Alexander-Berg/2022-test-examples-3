import pytest
import rclpy
import rclpy.node

from ymbot_obstacle_points.filtered_lidar_pc.lidar_point_cloud_filter_adapter import LidarPointCloudFilterAdapter
from ymbot_obstacle_points.filtered_lidar_pc.lidar_point_cloud_filter_adapter import FilteredLidarPCConfig


@pytest.fixture
def setup():
    rclpy.init()
    yield
    rclpy.shutdown()


@pytest.fixture
def executor(setup):
    executor_object = rclpy.executors.SingleThreadedExecutor()
    yield executor_object
    executor_object.shutdown()


@pytest.fixture
def ros_node(executor):
    node = rclpy.node.Node("filtered_lidar_pc")
    executor.add_node(node)
    yield node
    node.destroy_node()


@pytest.fixture
def filtered_lidar_pc_node(ros_node):
    filter_chain = '[{"intensity_filter": {"min_intensity": 240, "max_intensity": 1000}}]'
    node = LidarPointCloudFilterAdapter(ros_node,
                                        FilteredLidarPCConfig("lidar_topic", "filtered_lidar_pc_topic",
                                                              "filtered_lidar_pc_frame", "filtered_3d_pc_topic",
                                                              "filtered_3d_pc_frame", filter_chain,
                                                              0.02, 0.93, 0, 1.57))
    yield node
    # node.shutdown()


@pytest.fixture
def all_pc_filter_node(ros_node):
    filter_chain = '[{"point_in_box": {"delete": "in", "min_x": -1000, "min_y": -1000, "min_z": -1000, ' \
                   '"max_x": 1000, "max_y": 1000 , "max_z": 1000}}]'
    node = LidarPointCloudFilterAdapter(ros_node,
                                        FilteredLidarPCConfig("lidar_topic", "filtered_lidar_pc_topic",
                                                              "filtered_lidar_pc_frame", "filtered_3d_pc_topic",
                                                              "filtered_3d_pc_frame", filter_chain,
                                                              0.02, 0.93, 0, 1.57))
    yield node
