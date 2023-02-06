from unittest.mock import MagicMock

import pytest
from fastapi.testclient import TestClient

import ymbot_robot_control
from ymbot_robot_control.path_planner.dijkstra_planner import DijkstraPlanner
from ymbot_robot_control.path_planner.path_planner_adapter import PathPlannerAdapter
from ymbot_robot_control.path_planner.path_planner_map import Node, Edge, PathPlannerMap
from ymbot_robot_control.robot_server import RobotServer, RobotServerConfig
from ymbot_robot_control.route_planner.greedy_route_planner import GreedyRoutePlanner
from ymbot_robot_control.route_planner.route_planner_map import RoutePlannerMap
from ymbot_robot_control.virtual_railway_map import VirtualRailwayMap


@pytest.fixture
def simple_path_planner_map():
    nodes = [
        Node(uid=0),
        Node(uid=1),
    ]
    edges = [
        Edge(uid=0, start_node=nodes[0], end_node=nodes[1], cost=1),
        Edge(uid=0, start_node=nodes[1], end_node=nodes[0], cost=1)
    ]
    return PathPlannerMap(nodes=nodes, edges=edges)


@pytest.fixture
def medium_path_planner_map():
    nodes = [
        Node(uid=0),
        Node(uid=1),
        Node(uid=2),
        Node(uid=3),
        Node(uid=4)
    ]
    edges = [
        Edge(uid=0, start_node=nodes[0], end_node=nodes[1], cost=7),
        Edge(uid=0, start_node=nodes[1], end_node=nodes[0], cost=7),
        Edge(uid=0, start_node=nodes[0], end_node=nodes[2], cost=3),
        Edge(uid=0, start_node=nodes[2], end_node=nodes[0], cost=3),
        Edge(uid=0, start_node=nodes[1], end_node=nodes[2], cost=1),
        Edge(uid=0, start_node=nodes[2], end_node=nodes[1], cost=1),
        Edge(uid=0, start_node=nodes[2], end_node=nodes[3], cost=2),
        Edge(uid=0, start_node=nodes[3], end_node=nodes[2], cost=2),
        Edge(uid=0, start_node=nodes[1], end_node=nodes[3], cost=2),
        Edge(uid=0, start_node=nodes[3], end_node=nodes[1], cost=2),
        Edge(uid=0, start_node=nodes[1], end_node=nodes[4], cost=6),
        Edge(uid=0, start_node=nodes[4], end_node=nodes[1], cost=6),
        Edge(uid=0, start_node=nodes[4], end_node=nodes[3], cost=4),
        Edge(uid=0, start_node=nodes[3], end_node=nodes[4], cost=4),
    ]
    return PathPlannerMap(nodes=nodes, edges=edges)


@pytest.fixture
def dijkstra_planner():
    planner = DijkstraPlanner()
    return planner


@pytest.fixture
def path_planner_adapter(dijkstra_planner):
    return PathPlannerAdapter(dijkstra_planner)


@pytest.fixture
def virtual_railway_map():
    nodes = [
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=0, x=0., y=1.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=1, x=1., y=1.5),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=2, x=3., y=1.),
    ]
    edges = [
        ymbot_robot_control.virtual_railway_map.EdgeEntity(uid=0, start_node_id=0, end_node_id=1),
        ymbot_robot_control.virtual_railway_map.EdgeEntity(uid=1, start_node_id=2, end_node_id=1),
    ]
    return VirtualRailwayMap(_nodes=nodes, _edges=edges, _pallet_columns=[], start_node_id=0, start_angle=0.)


@pytest.fixture
def virtual_railway_map_with_pallets():
    nodes = [
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=0, x=0., y=0.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=1, x=0., y=3.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=2, x=0., y=6.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=3, x=4., y=0.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=4, x=4., y=3.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=5, x=4., y=6.),
        ymbot_robot_control.virtual_railway_map.NodeEntity(uid=6, x=8., y=0.),
    ]
    edges = [
        ymbot_robot_control.virtual_railway_map.EdgeEntity(uid=0, start_node_id=0, end_node_id=3),
        ymbot_robot_control.virtual_railway_map.EdgeEntity(uid=1, start_node_id=1, end_node_id=4),
        ymbot_robot_control.virtual_railway_map.EdgeEntity(uid=2, start_node_id=2, end_node_id=5),
    ]
    pallet_places = [
        ymbot_robot_control.virtual_railway_map.PalletColumnEntity(uid=0, edge_id=0, x=0, y=0, name="A"),
        ymbot_robot_control.virtual_railway_map.PalletColumnEntity(uid=1, edge_id=1, x=0, y=0, name="B"),
        ymbot_robot_control.virtual_railway_map.PalletColumnEntity(uid=2, edge_id=2, x=0, y=0, name="C"),
    ]
    return VirtualRailwayMap(_nodes=nodes, _edges=edges, _pallet_columns=pallet_places, start_node_id=0, start_angle=0.)


@pytest.fixture
def route_planner_map(virtual_railway_map_with_pallets):
    return RoutePlannerMap(virtual_railway_map_with_pallets)


@pytest.fixture
def route_planner():
    return GreedyRoutePlanner()


@pytest.fixture
def virtual_railway_path_planner_map(virtual_railway_map, path_planner_adapter):
    return path_planner_adapter.make_path_planner_map_from_virtual_railway_map(virtual_railway_map)


@pytest.fixture
def robot_server():
    return RobotServer(RobotServerConfig("0.0.0.0", 8000))


@pytest.fixture
def set_robot_server(robot_server):
    robot_server.task_event = MagicMock()
    robot_server.task_event.is_set.return_value = True
    return robot_server


@pytest.fixture
def unset_robot_server(robot_server):
    robot_server.task_event = MagicMock()
    robot_server.task_event.is_set.return_value = False


@pytest.fixture
def fastapi_client(robot_server):
    return TestClient(robot_server.app)


@pytest.fixture
def inventory_task():
    return {
        "pallets": [
            {
                "wmsAssignId": "string",
                "location": "A1",
                "expectedResult": "string"
            }
        ],
        "rmsTaskId": "string",
        "route": [
            {
                "positionX": 0,
                "positionY": 0
            }
        ]
    }
