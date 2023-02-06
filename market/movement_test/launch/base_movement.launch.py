from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node


def generate_launch_description():
    base_move_path = DeclareLaunchArgument('base_move_path', default_value='default_movement_test.yaml')
    movement_test_config = DeclareLaunchArgument('movement_test_config', default_value='base_movement.yaml')

    return LaunchDescription([
        base_move_path,
        movement_test_config,
        Node(package='movement_test', executable="base_movement", name="base_movement",
             parameters=[LaunchConfiguration('base_move_path'), LaunchConfiguration('movement_test_config')])
    ])
