source  /ymbot/ros2/foxy/bin/setup.bash
cd ../../../../ymbot
colcon build --packages-select movement_test ymbot_navigation_interface ymbot_utils --symlink-install \
    --event-handlers console_direct+ --base-paths . "../tools/ymbot_platform_scripts"
