def test_simple_map_dijkstra_planner(dijkstra_planner, simple_path_planner_map):
    dijkstra_planner.setup(simple_path_planner_map)
    result = dijkstra_planner.plan(simple_path_planner_map.nodes[0], simple_path_planner_map.nodes[1])
    assert len(result) == 1
    assert result[0] is simple_path_planner_map.edges[0]


def test_medium_map_dijkstra_planner(dijkstra_planner, medium_path_planner_map):
    dijkstra_planner.setup(medium_path_planner_map)
    result = dijkstra_planner.plan(medium_path_planner_map.nodes[0], medium_path_planner_map.nodes[4])
    assert len(result) == 3
    assert result[0] == medium_path_planner_map.edges[2]
    assert result[1] == medium_path_planner_map.edges[6]
    assert result[2] == medium_path_planner_map.edges[13]


def test_planner_map_factory(path_planner_adapter, virtual_railway_map):
    planner_map = path_planner_adapter.make_path_planner_map_from_virtual_railway_map(virtual_railway_map)
    assert len(planner_map.edges) == 4
    assert len(planner_map.nodes) == 3
    assert abs(planner_map.edges[0].cost - 1.118) < 1e-2
    assert abs(planner_map.edges[1].cost - 2.061) < 1e-2
    assert abs(planner_map.edges[2].cost - 1.118) < 1e-2
    assert abs(planner_map.edges[3].cost - 2.061) < 1e-2


def test_virtual_railway_path_planner(dijkstra_planner, virtual_railway_path_planner_map):
    dijkstra_planner.setup(virtual_railway_path_planner_map)
    result = dijkstra_planner.plan(virtual_railway_path_planner_map.nodes[0], virtual_railway_path_planner_map.nodes[2])
    assert len(result) == 2
    assert result[0] == virtual_railway_path_planner_map.edges[0]
    assert result[1] == virtual_railway_path_planner_map.edges[3]
