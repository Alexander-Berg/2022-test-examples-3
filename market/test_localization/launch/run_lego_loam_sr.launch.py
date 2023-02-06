import os

from ament_index_python import get_package_share_directory
from launch import LaunchDescription
from launch.actions import ExecuteProcess, DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node

CARTOGRAPHER_PACKAGE = "cartographer_ros"


def get_lego_loam_node():
    config_file = os.path.join(
        get_package_share_directory("test_localization"),
        "config",
        "loam_config.yaml"
    )
    return Node(
        package='lego_loam_sr',
        executable='lego_loam_sr',
        output='screen',
        parameters=[config_file],
        remappings=[('/lidar_points', '/points2')],
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
            "test_localization.log_name": "lego_loam_sr",
            "test_localization.log_transform_parent_frame": "camera_init",
            "test_localization.log_transform_child_frame": "camera",
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
    transform_map = Node(
        package='tf2_ros',
        executable='static_transform_publisher',
        name='camera_init_to_map',
        arguments=['0', '0', '0', '1.570795', '0', '1.570795', 'map', 'camera_init'],
    )

    transform_camera = Node(
        package='tf2_ros',
        executable='static_transform_publisher',
        name='base_link_to_camera',
        arguments=['0', '0', '0', '-1.570795', '-1.570795', '0', 'camera', 'PandarQT'],
    )

    return LaunchDescription([
        get_lego_loam_node(),
        get_rosbag_node(),
        get_test_localization_node(),
        get_rviz_node(),
        transform_map,
        transform_camera,
        rviz_arg,
        rosbag_arg,
        log_directory_arg
    ])
