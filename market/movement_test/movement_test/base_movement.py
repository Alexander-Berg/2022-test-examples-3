import logging
import time
from threading import Thread

import numpy as np
import rclpy
from rclpy.action import ActionClient
from tf2_msgs.msg._tf_message import TFMessage
from ymbot_navigation_interface.action import PathFollowerAction
from ymbot_utils.position_2d import Position2D
from ymbot_utils.position_array_2d import PositionArray2D
from ymbot_utils.termcolor import TextColors


class SendGoalClientNode(rclpy.node.Node):
    def __init__(self, ):
        super().__init__("test_client_node")
        self.get_logger().set_level(logging.DEBUG)
        self.result = None
        self.result_future = None
        self.goal_handle = None
        self.velocity = None
        self.declare_parameter(name='reference_points')
        self.declare_parameter(name='real_start_point')
        self.declare_parameter(name='move_forward')
        self.declare_parameter(name='rotation_angle')
        self.declare_parameter(name='current_position_info_period')
        self.use_real_start_point = self.get_parameter('real_start_point').value
        self.move_forward = self.get_parameter('move_forward').value
        self._rotation_angle = self.get_parameter('rotation_angle').value
        self._current_position_info_period = self.get_parameter('current_position_info_period').value
        self.x = None
        self.y = None
        self.angle = None
        self.create_subscription(TFMessage, '/tf', self.tf_listener, qos_profile=10)
        self._action_client = ActionClient(self, PathFollowerAction, 'path_follower_action')

    def tf_listener(self, tf_message):
        self.x = Position2D.from_ros_transform(tf_message.transforms[0].transform).x
        self.y = Position2D.from_ros_transform(tf_message.transforms[0].transform).y
        self.angle = Position2D.from_ros_transform(tf_message.transforms[0].transform).rotation
        self.get_logger().info(f'{TextColors.WARNING}Current point: x: {self.x:.4f}, y: {self.y:.4f}, '
                               f'angle: {self.angle:.4f}{TextColors.ENDC}',
                               throttle_duration_sec=self._current_position_info_period)

    def wait_for_server(self):
        return self._action_client.wait_for_server(timeout_sec=1)

    def send_goal(self):
        self.wait_for_server()
        self.get_logger().debug(
            f'{TextColors.WARNING}self.use_real_start_point: {self.use_real_start_point}{TextColors.ENDC}')
        self.get_logger().debug(
            f'{TextColors.WARNING}move_forward: {self.move_forward}{TextColors.ENDC}')
        self.get_logger().debug(
            f'{TextColors.WARNING}rotation_angle: {self._rotation_angle}{TextColors.ENDC}')

        if self.move_forward is not None:
            reference_points = self.calculate_forward_moving_points()
        elif self._rotation_angle is not None:
            reference_points = np.zeros((2, 3))
            reference_points[0] = self.get_current_position()
            reference_points[1] = reference_points[0]
            reference_points[1][2] = reference_points[1][2] + self._rotation_angle
        elif self.use_real_start_point:
            reference_points = np.array(self.get_parameter('reference_points').value).reshape((-1, 3))
            self.get_logger().info(
                f'\n{TextColors.WARNING}Reference_points: \n {str(reference_points)} {TextColors.ENDC}')
            reference_points[0] = self.get_current_position()
        else:
            reference_points = np.array(self.get_parameter('reference_points').value).reshape((-1, 3))

        self.get_logger().info(
            f'\n{TextColors.WARNING}Reference_points: \n {str(reference_points)} {TextColors.ENDC}')

        trajectory = PositionArray2D.from_vec(reference_points)
        goal_message = self._make_goal_message(trajectory)
        self.get_logger().info(
            f'{TextColors.WARNING}PathFollowerAction Response: {self._action_client.send_goal(goal_message)}'
            f'{TextColors.ENDC}')
        self.get_current_position()

    def calculate_forward_moving_points(self):
        reference_points = np.zeros((2, 3))
        reference_points[0] = self.get_current_position()
        reference_points[1] = np.array((
            (self.move_forward * np.cos(reference_points[0][2]) + reference_points[0][0]),  # x
            (self.move_forward * np.sin(reference_points[0][2]) + reference_points[0][1]),  # y
            (reference_points[0][2])))  # angle
        return reference_points

    def get_current_position(self):
        for i in range(100):
            if self.x is not None:
                break
            time.sleep(0.1)
            if i == 99:
                self.get_logger().info(f'\n{TextColors.WARNING} self.x too long is None{TextColors.ENDC}')

        self.get_logger().debug(
            f'{TextColors.WARNING}Current position '
            f'self.x: {self.x}, self.y: {self.y}, self.angle: {self.angle}{TextColors.ENDC}')
        current_point = np.array((self.x, self.y, self.angle))
        return current_point

    def _make_goal_message(self, trajectory):
        goal_message = PathFollowerAction.Goal(goal_path=trajectory.as_path())
        goal_message.goal_path.header.stamp = self.get_clock().now().to_msg()
        goal_message.goal_path.header.frame_id = "map"
        return goal_message

    def destroy_node(self):
        self._action_client.destroy()


def main():
    rclpy.init(args=None)
    node = SendGoalClientNode()
    try:
        spin_thread = Thread(target=rclpy.spin, args=(node,))
        spin_thread.start()
        node.send_goal()
        spin_thread.join()
    except KeyboardInterrupt:
        print("Got keyboard interrupt")
    finally:
        node.destroy_node()
        rclpy.shutdown()


if __name__ == '__main__':
    main()
