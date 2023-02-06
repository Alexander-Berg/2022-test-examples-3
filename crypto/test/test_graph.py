from crypta.idserv.data.python.common import create_complete_graph, create_linear_graph, assert_all_nodes_are_adjacent
from crypta.idserv.data.python.graph import TGraph
import pytest


@pytest.mark.parametrize("size", [1, 3, 10])
def test_graph(size):
    graph = create_complete_graph(size)

    assert size == len(graph.GetNodes())
    assert_all_nodes_are_adjacent(graph)
    assert graph.GetNode(0) == graph.GetNode(0)


def test_cmp_graphs():
    g1 = create_complete_graph(2)
    g2 = create_complete_graph(3)
    g3 = create_complete_graph(3)
    g4 = create_complete_graph(3, attrs_factor=2)
    g5 = create_complete_graph(3)
    g5.SetId(111)

    assert g1 != g2
    assert g2 == g3
    assert g2 != g4
    assert g2 != g5


def test_cmp_parts():
    graph = TGraph()
    n1 = graph.CreateNode("yandexuid", "111")
    n2 = graph.CreateNode("idfa", "222")
    n3 = graph.CreateNode("puid", "333")
    e1 = graph.CreateEdge(n1, n2)
    e2 = graph.CreateEdge(n1, n3)

    nodes = graph.GetNodes()
    edges = graph.GetEdges()

    assert n1 != n2
    assert n1 != n3
    assert n2 != n3
    assert e1 != e2

    assert n1 == nodes[0]
    assert n2 == nodes[1]
    assert n3 == nodes[2]
    assert e1 == edges[0]
    assert e2 == edges[1]

    assert n1.Id != n2.Id
    assert n1.Attributes != n2.Attributes
    assert n1.Id == nodes[0].Id
    assert n1.Attributes == nodes[0].Attributes

    assert e1.Node1 == e2.Node1
    assert e1.Node2 != e2.Node2
    assert e1.Attributes != e2.Attributes
    assert e1.Node1 == edges[0].Node1
    assert e1.Node2 == edges[0].Node2
    assert e1.Attributes == edges[0].Attributes

    n4 = graph.CreateNode("android_id", "444")
    assert n4 == nodes[3]

    e3 = graph.CreateEdge(n2, n3)
    assert e3 == edges[2]


def test_iter():
    graph = create_complete_graph(3)

    assert len(list(graph.GetNodes())) == len(graph.GetNodes())
    assert len(list(graph.GetEdges())) == len(graph.GetEdges())

    for n in graph.GetNodes():
        assert n
        assert "yandexuid" == n.Id.Type

    for e in graph.GetEdges():
        assert e
        assert "yandexuid" == e.Node1.Id.Type
        assert "yandexuid" == e.Node2.Id.Type


def test_create_linear_graph():
    graph = create_linear_graph(10)

    assert len(graph.GetNodes()) - 1 == len(graph.GetEdges())
