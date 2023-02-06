import collections
import dataclasses
import datetime
import json
from pathlib import Path
from typing import Dict, Optional

import geometry_msgs.msg
import rclpy.node
import rclpy.time
import sensor_msgs.msg
import tf2_msgs.msg
from rclpy.qos import qos_profile_sensor_data
from ymbot_utils.position_3d import Position3D
from ymbot_utils.ros_loader import RosLoader


@dataclasses.dataclass
class TestLocalizationNodeConfig:
    log_name: str
    log_transform_parent_frame: str
    log_transform_child_frame: str
    log_directory: str
    dataset_file: str


@dataclasses.dataclass
class StampedPosition:
    position: Position3D
    timestamp: int


class TestLocalizationNode:
    def __init__(self, node: rclpy.node.Node, parameters: TestLocalizationNodeConfig):
        self._node = node
        self._parameters = parameters
        self._publisher = self._node.create_publisher(sensor_msgs.msg.Imu, "imu", qos_profile=1)
        self._points_publisher = self._node.create_publisher(sensor_msgs.msg.PointCloud2, "points2", qos_profile=1)
        self._truth_lidar_times_dict: Dict[int, Optional[int]] = collections.defaultdict(lambda: None)
        self._trajectory = {}
        self._timer = self._node.create_timer(0.02, self._timer_callback)
        self._subscriber = self._node.create_subscription(sensor_msgs.msg.PointCloud2, "/hesai/pandar",
                                                          self._sensor_callback,
                                                          qos_profile=qos_profile_sensor_data)
        self._tf_subscriber = self._node.create_subscription(tf2_msgs.msg.TFMessage, "/tf", self._tf_callback, 1)

    def _timer_callback(self):
        message = sensor_msgs.msg.Imu()
        message.orientation.x = 0.
        message.orientation.y = 0.
        message.orientation.z = 1.
        message.orientation.w = 0.
        message.linear_acceleration.z = 10.
        message.header.frame_id = "PandarQT"
        message.header.stamp = self._node.get_clock().now().to_msg()
        self._publisher.publish(message)

    def _sensor_callback(self, message):
        truth_lidar_time = rclpy.time.Time.from_msg(message.header.stamp).nanoseconds
        message.header.stamp = self._node.get_clock().now().to_msg()
        message.header.frame_id = "PandarQT"
        published_lidar_time = rclpy.time.Time.from_msg(message.header.stamp).nanoseconds

        self._truth_lidar_times_dict[self._round_time(published_lidar_time)] = truth_lidar_time
        self._points_publisher.publish(message)

    @staticmethod
    def _round_time(timestamp):
        return int(round(timestamp * 1e-6))

    def _tf_callback(self, message: tf2_msgs.msg.TFMessage):
        transform: geometry_msgs.msg.TransformStamped = message.transforms[0]
        if transform.header.frame_id != self._parameters.log_transform_parent_frame:
            return
        if transform.child_frame_id != self._parameters.log_transform_child_frame:
            return
        position = Position3D.from_ros_transform(transform.transform)
        timestamp = rclpy.time.Time.from_msg(message.transforms[0].header.stamp).nanoseconds
        truth_time: int = self._truth_lidar_times_dict[self._round_time(timestamp)]
        if truth_time is None:
            return
        self._trajectory[truth_time] = StampedPosition(position, truth_time)

    def save_results(self):
        log_directory_name = f"{self._parameters.log_name}_{datetime.datetime.now().strftime('%Y-%m-%d-%H-%M-%S')}"
        self._node.get_logger().info(f"Save log to direction {log_directory_name}")
        path = Path(self._parameters.log_directory).joinpath(log_directory_name)
        path.mkdir(parents=True, exist_ok=True)
        with open(path.joinpath("result_trajectory.json").as_posix(), 'w', encoding='utf-8') as fd:
            json.dump(self._make_trajectory_data(), fd, ensure_ascii=False, indent=4)

    def _make_trajectory_data(self):
        trajectory = []
        for key in sorted(self._trajectory.keys()):
            position = self._trajectory[key]
            trajectory.append({
                "position": list(position.position.as_matrix().reshape(-1)),
                "time": position.timestamp
            })
        result = {
            "trajectory": trajectory,
            "dataset_file": self._parameters.dataset_file
        }
        return result


def main():
    rclpy.init(args=None)
    node = rclpy.node.Node("test_localization_node")
    node.test_localization = RosLoader().load(node, TestLocalizationNode, "test_localization")
    try:
        rclpy.spin(node)
    except KeyboardInterrupt:
        node.test_localization.save_results()
        pass
    finally:
        rclpy.shutdown()


if __name__ == '__main__':
    main()
