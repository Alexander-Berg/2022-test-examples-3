import os

from ament_index_python import get_package_share_directory
from launch import LaunchDescription
from launch.actions import ExecuteProcess, DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node

CARTOGRAPHER_PACKAGE = "cartographer_ros"


def get_cartographer_node():
    cartographer_configuration_directory = os.path.join(
        get_package_share_directory("test_localization"),
        "cartographer_configuration"
    )
    return Node(
        package=CARTOGRAPHER_PACKAGE,
        executable='cartographer_node',
        name='cartographer_node',
        arguments=[
            "-configuration_directory",
            {cartographer_configuration_directory},
            "-configuration_basename",
            "laser_odometry_3d.lua"
        ],
    )


def get_occupancy_grid_node():
    cartographer_configuration_directory = os.path.join(
        get_package_share_directory("test_localization"),
        "cartographer_configuration"
    )
    print(cartographer_configuration_directory)
    return Node(
        package=CARTOGRAPHER_PACKAGE,
        executable='occupancy_grid_node',
        name='occupancy_grid_node',
    )


def get_rosbag_node():
    rosbag_file = LaunchConfiguration('rosbag')
    return ExecuteProcess(
        cmd=["ros2", "bag", "play", rosbag_file]
    )


def get_test_localization_node():
    return Node(
        package="test_localization",
        executable="test_localization_node",
        name="test_localization_node",
        parameters=[{
            "test_localization.log_name": "cartographer",
            "test_localization.log_transform_parent_frame": "map",
            "test_localization.log_transform_child_frame": "PandarQT",
            "test_localization.log_directory": LaunchConfiguration("log_directory"),
            "test_localization.dataset_file": LaunchConfiguration("rosbag")
        }]
    )


def get_rviz_node():
    rviz_path = LaunchConfiguration('rviz_config')
    return Node(
        package='rviz2',
        executable='rviz2',
        name='rviz2',
        output='screen',
        arguments=['-d', rviz_path]
    )


def generate_launch_description():
    rviz_arg = DeclareLaunchArgument(name='rviz_config', description='Absolute path to rviz config file')
    rosbag_arg = DeclareLaunchArgument(name='rosbag', description='path to rosbag file')
    log_directory_arg = DeclareLaunchArgument(name="log_directory", description="path to log directory")
    return LaunchDescription([
        get_cartographer_node(),
        get_occupancy_grid_node(),
        get_rosbag_node(),
        get_test_localization_node(),
        get_rviz_node(),
        rviz_arg,
        rosbag_arg,
        log_directory_arg
    ])
