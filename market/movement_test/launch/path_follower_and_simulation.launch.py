import os

from ament_index_python import get_package_share_directory
from launch import LaunchDescription
from launch.actions import DeclareLaunchArgument
from launch.substitutions import LaunchConfiguration
from launch_ros.actions import Node

SIMULATION_PACKAGE = "ymbot_simulation"
COLLISION_CHECKER_PACKAGE = "ymbot_collision_checker"
NAVIGATION_PACKAGE = "ymbot_navigation"
OUTPUT = "log"


def get_velocity_limiter_description():
    config = os.path.join(
        get_package_share_directory(COLLISION_CHECKER_PACKAGE),
        'config',
        'velocity_limiter.yaml'
    )
    return Node(
        package='ymbot_collision_checker',
        executable='velocity_limiter_node',
        name='velocity_limiter_node',
        parameters=[config],
        output=OUTPUT
    )


def get_config(package, folder, filename):
    return os.path.join(
        get_package_share_directory(package),
        folder,
        filename
    )


def get_simulation_node_description(name: str, executable: str):
    config = get_config(package=SIMULATION_PACKAGE, folder='config', filename='simulation.yaml')
    return Node(
        package=SIMULATION_PACKAGE,
        executable=executable,
        name=name,
        parameters=[config],
        output=OUTPUT
    )


def get_experiment_runner_node(name: str, executable: str):
    config = get_config(package=SIMULATION_PACKAGE, folder='config', filename='simulation.yaml')
    directory = os.path.join(get_package_share_directory(SIMULATION_PACKAGE), "test_maps")
    return Node(
        package=SIMULATION_PACKAGE,
        executable=executable,
        name=name,
        parameters=[config, {"experiment_runner_adapter.map_description_directory": directory}, LaunchConfiguration('base_move_path')],
        output=OUTPUT
    )


def get_rviz_description():
    return Node(
        package='rviz2',
        executable='rviz2',
        name='rviz2',
        output=OUTPUT,
        arguments=['-d', LaunchConfiguration('rviz_path')]
    )


def get_path_follower_node():
    config = os.path.join(
        get_package_share_directory(NAVIGATION_PACKAGE),
        'config',
        'path_follower.yaml'
    )
    return Node(
        package=NAVIGATION_PACKAGE,
        executable='path_follower_node',
        name='path_follower_node',
        parameters=[config],
        output="screen",
    )


def generate_launch_description():
    default_rviz_path = get_config(package=COLLISION_CHECKER_PACKAGE, folder='rviz', filename='simulation.rviz')
    rviz_path_argument = DeclareLaunchArgument('rviz_path', default_value=default_rviz_path)
    base_move_path = DeclareLaunchArgument('base_move_path', default_value='default_movement_test.yaml')
    movement_test_config = DeclareLaunchArgument('movement_test_config', default_value='base_movement.yaml')

    return LaunchDescription([
        rviz_path_argument,
        base_move_path,
        movement_test_config,
        get_simulation_node_description(name='map_simulation_node', executable='map_simulation_node'),
        get_simulation_node_description(name='movement_simulation_node', executable='movement_simulation_node'),
        get_simulation_node_description(name='point_cloud_simulation_node', executable='point_cloud_simulation_node'),
        get_experiment_runner_node(name='experiment_runner_node', executable='experiment_runner_node'),
        get_velocity_limiter_description(),
        get_rviz_description(),
        get_path_follower_node(),
        Node(package='movement_test', executable="base_movement", name="base_movement",
             parameters=[LaunchConfiguration('base_move_path'), LaunchConfiguration('movement_test_config')])
    ])
