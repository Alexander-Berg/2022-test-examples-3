export ROS_DOMAIN_ID=50
source /opt/ros/foxy/setup.bash
source install/setup.bash
RVIZ_CONFIG=../../../../ymbot/ymbot_navigation/rviz/navigation.rviz
POINTS_CONFIG=../config/forward_test.yaml
MOVEMENT_CONFIG=../config/base_movement.yaml

ros2 launch movement_test path_follower_and_simulation.launch.py rviz_path:=$RVIZ_CONFIG base_move_path:=$POINTS_CONFIG movement_test_config:=$MOVEMENT_CONFIG
