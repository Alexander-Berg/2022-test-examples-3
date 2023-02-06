source /ymbot/ros2/foxy/bin/setup.bash
source /ymbot/ymbot/bin/local_setup.bashPOINTS_CONFIG=../config/forward_test.yaml
MOVEMENT_CONFIG=../config/base_movement.yaml

ros2 launch movement_test base_movement.launch.py base_move_path:=$POINTS_CONFIG movement_test_config:=$MOVEMENT_CONFIG
