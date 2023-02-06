include "map_builder.lua"
include "trajectory_builder.lua"

options = {
  map_builder = MAP_BUILDER,
  trajectory_builder = TRAJECTORY_BUILDER,
  map_frame = "map",
  tracking_frame = "PandarQT",
  published_frame = "PandarQT",
  odom_frame = "odom",
  provide_odom_frame = false,
  publish_frame_projected_to_2d = true,
  use_odometry = false,
  use_nav_sat = false,
  use_landmarks = false,
  num_laser_scans = 0,
  num_multi_echo_laser_scans = 0,
  num_subdivisions_per_laser_scan = 1,
  num_point_clouds = 1,
  lookup_transform_timeout_sec = 0.2,
  submap_publish_period_sec = 0.3,
  pose_publish_period_sec = 5e-3,
  trajectory_publish_period_sec = 30e-3,
  rangefinder_sampling_ratio = 1.,
  odometry_sampling_ratio = 1.,
  fixed_frame_pose_sampling_ratio = 1.,
  imu_sampling_ratio = 1.,
  landmarks_sampling_ratio = 1.,
}

TRAJECTORY_BUILDER_3D.num_accumulated_range_data = 1
TRAJECTORY_BUILDER_3D.use_online_correlative_scan_matching = true
TRAJECTORY_BUILDER_3D.voxel_filter_size = 0.03
TRAJECTORY_BUILDER_3D.ceres_scan_matcher.rotation_weight = 0.1
TRAJECTORY_BUILDER_3D.ceres_scan_matcher.translation_weight = 0.1
TRAJECTORY_BUILDER_3D.min_range = 3.
TRAJECTORY_BUILDER_3D.max_range = 20.
TRAJECTORY_BUILDER_3D.ceres_scan_matcher.ceres_solver_options.num_threads = 4
-- TRAJECTORY_BUILDER_3D.ceres_scan_matcher.ceres_solver_options.max_num_iterations = 100
-- TRAJECTORY_BUILDER_3D.ceres_scan_matcher.ceres_solver_options.use_nonmonotonic_steps = true
-- TRAJECTORY_BUILDER_3D.high_resolution_adaptive_voxel_filter.min_num_points=10000
TRAJECTORY_BUILDER_3D.submaps.high_resolution = 0.05
TRAJECTORY_BUILDER_3D.submaps.high_resolution_max_range = 10.
-- TRAJECTORY_BUILDER_3D.submaps.low_resolution = 0.3
TRAJECTORY_BUILDER_3D.real_time_correlative_scan_matcher.linear_search_window = 0.075
TRAJECTORY_BUILDER_3D.real_time_correlative_scan_matcher.angular_search_window = math.rad(0.25)
-- TRAJECTORY_BUILDER_3D.motion_filter.max_time_seconds = 0.00
-- TRAJECTORY_BUILDER_3D.motion_filter.max_distance_meters = 0.00
-- TRAJECTORY_BUILDER_3D.motion_filter.max_angle_radians = 0.00
TRAJECTORY_BUILDER_3D.low_resolution_adaptive_voxel_filter.max_length = 0.2
TRAJECTORY_BUILDER_3D.low_resolution_adaptive_voxel_filter.min_num_points = 500
TRAJECTORY_BUILDER_3D.submaps.num_range_data = 50

MAP_BUILDER.use_trajectory_builder_3d = true
MAP_BUILDER.num_background_threads = 7
POSE_GRAPH.optimize_every_n_nodes = 20
POSE_GRAPH.constraint_builder.fast_correlative_scan_matcher_3d.linear_xy_search_window=1.5
POSE_GRAPH.constraint_builder.fast_correlative_scan_matcher_3d.linear_z_search_window=0.2
POSE_GRAPH.constraint_builder.fast_correlative_scan_matcher_3d.angular_search_window=math.rad(5.)
POSE_GRAPH.constraint_builder.min_score = 0.2
-- POSE_GRAPH.global_constraint_search_after_n_seconds = 1.
POSE_GRAPH.optimization_problem.acceleration_weight = 0.1
POSE_GRAPH.optimization_problem.rotation_weight = 0.1
POSE_GRAPH.constraint_builder.global_localization_min_score = 0.05
POSE_GRAPH.constraint_builder.sampling_ratio = 1.
-- POSE_GRAPH.constraint_builder.fast_correlative_scan_matcher_3d.min_rotational_score = 0.5
-- POSE_GRAPH.constraint_builder.fast_correlative_scan_matcher_3d.min_low_resolution_score = 0.4


return options
