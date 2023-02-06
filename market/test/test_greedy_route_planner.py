from ymbot_robot_control.virtual_railway_map import Point


def test_greedy_route_planner(route_planner, route_planner_map):
    route_planner.setup(route_planner_map)
    result = route_planner.plan([
        route_planner_map._virtual_railway_map.pallet_columns[0],
        route_planner_map._virtual_railway_map.pallet_columns[1]], Point(x=8, y=0))
    assert len(result) == 2
    assert result[0].edge.uid == 0
    assert result[0].end_node.uid == 0
    assert result[1].edge.uid == 1
    assert result[1].end_node.uid == 4
