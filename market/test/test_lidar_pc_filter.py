from sensor_msgs.msg import PointCloud2, PointField
from ymbot_obstacle_points.filtered_lidar_pc.lidar_pc_filter import LidarPCFilter


def test_apply_filters_empty_point_cloud(filtered_lidar_pc_node):
    assert isinstance(filtered_lidar_pc_node._filter_chain, LidarPCFilter)
    msg = PointCloud2()
    filtered_lidar_pc_node._filter_chain.apply_filters(msg)


def test_all_points_filter(all_pc_filter_node):
    assert isinstance(all_pc_filter_node._filter_chain, LidarPCFilter)
    msg = PointCloud2(height=1, width=2, fields=[PointField(name='x', offset=0, datatype=7, count=1),
                                                 PointField(name='y', offset=4, datatype=7, count=1),
                                                 PointField(name='z', offset=8, datatype=7, count=1),
                                                 PointField(name='intensity', offset=16, datatype=7,
                                                            count=1),
                                                 PointField(name='timestamp', offset=24, datatype=8,
                                                            count=1),
                                                 PointField(name='ring', offset=32, datatype=4, count=1)],
                      point_step=48, row_step=96,
                      data=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 178, 127, 0, 0, 0, 0, 127, 67, 0, 0, 0, 0, 165, 223,
                            101, 242, 9, 108, 214, 65, 35, 0, 0, 0, 67, 63, 22, 192, 0, 0, 0, 0, 136, 24, 41, 64, 0,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35, 205, 50, 64, 0, 0, 127, 67, 176, 237, 5, 192, 157,
                            223, 101, 242, 9, 108, 214, 65, 34, 0, 0, 0, 85, 251, 5, 192, 0, 0, 0, 0, 105, 202, 24,
                            64],
                      is_dense=True
                      )
    all_pc_filter_node._filter_chain.apply_filters(msg)
