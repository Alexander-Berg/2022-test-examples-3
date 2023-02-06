export ROS_DOMAIN_ID=50
source /opt/ros/foxy/setup.bash
colcon build --packages-select ymbot_navigation ymbot_simulation movement_test ymbot_navigation_interface ymbot_utils \
    ymbot_collision_checker ymbot_collision_checker_interface ymbot_simulation_interface --symlink-install \
    --event-handlers console_direct+ --base-paths ".." "../../../../ymbot"
