import pytest
import datetime
import time
import random
import itertools
import logging
import binascii
import md5

import crypta.lib.python.bt.conf.conf as conf
import yt.wrapper as yt
from crypta.graph.export.lib.graphs import (
    ExportConf,
    Exporter,
    Paths,
)
from crypta.graph.export.lib.native import (
    graph_yql_proto_fields,
    is_yuid_private,
    convert_mac_to_md5,
)
from crypta.graph.export.lib.python.spanning_graph import GraphSpanningTreeTransformer
from crypta.graph.export.proto.graph_pb2 import TGraph
from crypta.lib.python.identifiers.identifiers import Yandexuid

from collections import namedtuple
logger = logging.getLogger(__name__)


Graph = namedtuple('Graph', ['crypta_id', 'edges', 'identifiers', 'vertices'])
VERSION = ExportConf.VERSION_2

CRYPTA_ID_ACTIVE_GRAPH_WITH_MAC = "1"
CRYPTA_ID_ACTIVE_GRAPH = "2"
CRYPTA_ID_ACTIVE_GRAPH_WITH_INACTIVE_LEAVES = "3"
ACTIVE_CRYPTAIDS = {CRYPTA_ID_ACTIVE_GRAPH, CRYPTA_ID_ACTIVE_GRAPH_WITH_MAC, CRYPTA_ID_ACTIVE_GRAPH_WITH_INACTIVE_LEAVES}


def destination_root():
    return Paths(VERSION).get_destination_root()


def proto_graph(record):
    graph = TGraph()
    graph.ParseFromString(record["graph"])
    return graph


def to_timestamp(str_date, format="%Y-%m-%dT%H:%M:%SZ"):
    return int(time.mktime(datetime.datetime.strptime(str_date, format).timetuple()))


class Sampler(object):
    def __init__(self, seed=None):
        if seed:
            random.seed(seed)
        self.now = datetime.datetime.now()

    def _value(self):
        return str(Yandexuid.next())

    def str_value(self, value=None):
        if value is None:
            value = self._value()
        return str(value)

    def id(self, value=None):
        return self.str_value(value)

    def mac(self):
        return ":".join(str(random.randint(10, 99)) for _ in range(6))

    def id_type(self, value=None, not_yuid=False):
        not_yuid_identifiers = ['device_id', 'puid', 'login', 'uuid', 'duid']
        if not_yuid:
            return random.choice(not_yuid_identifiers)
        return value or random.choice(['yandexuid'] + not_yuid_identifiers)

    def crypta_id(self, value=None):
        return value or self._value()

    def date(self, active=True, n_days_before=None):
        if n_days_before is None:
            n_days_before = random.randint(0, conf.Options.active_interval-1)
            if not active:
                n_days_before += conf.Options.active_interval+1
        date = self.now - datetime.timedelta(days=n_days_before)
        return str(date.strftime('%Y-%m-%d'))

    def dates(self, active=True, n_dates=None):
        if n_dates is None:
            n_dates = random.randint(0, 10)
        dates = [self.date(active=False) for _ in xrange(n_dates)]
        if active:
            if n_dates == 1:
                dates = []
            dates.append(self.date(active=True))
        random.shuffle(dates)
        return dates

    def vertex(self, not_yuid=False):
        return self.id(), self.id_type(None, not_yuid)

    def edge(self, crypta_id=None, id1=None, id1_type=None, id2=None, id2_type=None, active=True,
             log_source=None, source_type=None, weight=None):
        none_weight = random.choice([True, False])
        random_weight = random.uniform(0, 1) if none_weight else None
        return {
            "cryptaId": self.crypta_id(crypta_id),
            "id1": self.id(id1),
            "id1Type": self.id_type(id1_type),
            "id2": self.id(id2),
            "id2Type": self.id_type(id2_type),
            "dates": self.dates(active),
            "logSource": self.str_value(log_source),
            "sourceType": self.str_value(source_type),
            "indevice": bool(random.randint(0, 1)),
            "survivalWeight": weight if weight else random_weight
        }

    def identifier(self, id=None, id_type=None, active=True, n_dates=None):
        return {
            "id": self.id(id),
            "id_type": self.id_type(id_type),
            "dates": self.dates(active, n_dates),
        }

    def active_graph(self, n_vertices=None, crypta_id=None):
        crypta_id, edges, identifiers, vertices = self.inactive_graph(n_vertices, not_yuid=False, crypta_id=crypta_id)
        id, id_type = random.choice(vertices)
        identifiers = [self.identifier(id, id_type, active=True)]
        for id, id_type in vertices:
            if id_type == "yandexuid":
                identifiers.append(self.identifier(id, id_type, active=True, n_dates=2))
        return crypta_id, edges, identifiers, vertices

    def inactive_graph(self, n_vertices=None, not_yuid=True, crypta_id=None):
        if crypta_id is None:
            crypta_id = self.crypta_id()
        n_vertices = n_vertices or random.randint(2, 15)
        vertices = [self.vertex(not_yuid) for _ in xrange(n_vertices)]

        edges = []
        for i, (id, id_type) in enumerate(vertices[1:]):
            id_dst, id_type_dst = random.choice(vertices[:i+1])
            for _ in range(random.randint(1, 3)):
                edges.append(self.edge(crypta_id, id, id_type, id_dst, id_type_dst, active=False))

        identifiers = [self.identifier(id, id_type, active=False) for (id, id_type) in vertices[:2]]
        return crypta_id, edges, identifiers, vertices

    def active_graph_with_mac(self, n_vertices=None, crypta_id=None):
        crypta_id, edges, identifiers, vertices = self.active_graph(n_vertices, crypta_id)
        id, id_type = random.choice(vertices)
        mac_id, mac_type = (self.mac(), "mac")

        vertices.append((mac_id, mac_type))
        edges.append(self.edge(crypta_id, id, id_type, mac_id, mac_type))

        return crypta_id, edges, identifiers, vertices

    def household_graph(self, n_vertices=None):
        crypta_id = self.crypta_id()
        n_vertices = n_vertices or random.randint(2, 5)
        return self.household_edges(crypta_id, n_vertices)

    def household_edges(self, crypta_id=None, n_vertices=None, yuids=None):
        if crypta_id is None:
            crypta_id = self.crypta_id()
        if n_vertices is None:
            n_vertices = random.randint(2, 5)
        if yuids is None:
            yuids = [self.id() for _ in range(2*n_vertices)]
        # all vertices - always YANDEXUID
        vertices = [(self.id(), 'yandexuid') for _ in xrange(n_vertices)]
        edges = []
        for i, (id_dst, id_type_dst) in enumerate(vertices[1:]):
            id_src, id_type_src = random.choice(yuids), 'yandexuid'
            for _ in range(random.randint(1, 3)):
                hh_edge = self.edge(crypta_id, id_src, id_type_src, id_dst, id_type_dst, active=False)
                # household has only:
                #       cryptaId, id1, id1Type, id2, id2Type, sourceType, logSource
                # so, pop dates and indevice
                hh_edge.pop('dates')
                hh_edge.pop('indevice')
                edges.append(hh_edge)
        identifiers = [self.identifier(id, id_type, active=False) for (id, id_type) in vertices[:2]]
        return crypta_id, edges, identifiers, vertices

    def graph_with_inactive_yuid_leaves(self, n_vertices=None, crypta_id=None):
        crypta_id, edges, identifiers, vertices = self.active_graph(n_vertices, crypta_id)

        for i in xrange(4):
            id, id_type = random.choice(vertices)
            yuid = self.id()
            yuid_type = "yandexuid"
            edge = self.edge(crypta_id, id, id_type, yuid, yuid_type, active=False) if i \
                else self.edge(crypta_id, yuid, yuid_type, id, id_type, active=False)
            edges.append(edge)
            if i % 2:
                identifier = self.identifier(yuid, yuid_type)
                identifier["dates"] = self.dates(active=True, n_dates=1)
                identifiers.append(identifier)

        return crypta_id, edges, identifiers, vertices


def create_data():
    v2_edges_path = Paths(ExportConf.VERSION_2).get_source_edges()
    tv_edges_path = Paths(ExportConf.HOUSEHOLDS).get_source_edges()
    vertices_path = str(conf.Paths.Input.Vertices)
    identifier_paths = Paths.get_identifiers_paths(yt)
    for path in [v2_edges_path, tv_edges_path, vertices_path] + identifier_paths:
        yt.create('table', path, recursive=True, force=True)

    sampler = Sampler(42)
    active_graphs = [Graph(*sampler.active_graph(crypta_id=CRYPTA_ID_ACTIVE_GRAPH)), Graph(*sampler.active_graph_with_mac(crypta_id=CRYPTA_ID_ACTIVE_GRAPH_WITH_MAC))]
    inactive_graphs = [Graph(*sampler.inactive_graph()) for _ in xrange(3)]
    active_with_inactive_leaves_graphs = [Graph(*sampler.graph_with_inactive_yuid_leaves(4, crypta_id=CRYPTA_ID_ACTIVE_GRAPH_WITH_INACTIVE_LEAVES))]
    yuids = []
    for g in active_graphs:
        for id, id_type in g.vertices:
            if id_type == "yandexuid":
                yuids.append(id)

    households_graphs = list(itertools.chain(
        # (Graph(*sampler.household_graph()) for _ in xrange(2)),
        (Graph(*sampler.household_edges(g.crypta_id, 2, yuids)) for g in active_with_inactive_leaves_graphs)
    ))

    graphs = list(itertools.chain(*[active_graphs, inactive_graphs, active_with_inactive_leaves_graphs]))
    edges_records = itertools.chain(*[graph.edges for graph in graphs])
    households_records = itertools.chain(*[graph.edges for graph in households_graphs])
    identifiers_records = itertools.chain(*[graph.identifiers for graph in graphs])

    yt.run_sort(vertices_path, sort_by=["id_type", "id"])
    yt.write_table(v2_edges_path, edges_records)
    yt.write_table(tv_edges_path, households_records)
    yt.write_table(random.choice(identifier_paths), identifiers_records)

    return active_graphs, inactive_graphs, active_with_inactive_leaves_graphs, households_graphs


@pytest.fixture(scope="function")
def with_data(local_yt):
    return create_data()


def collect_graphs(diff_only=True):
    return sorted(
        [proto_graph(record) for record in
         yt.read_table(Exporter(yt).export(VERSION, diff_only=diff_only))],
        key=lambda graph: graph.CryptaId
    )


def clean_results():
    yt.remove(destination_root(), recursive=True, force=True)


def test_export(with_data):
    active_graphs, inactive_graphs, active_with_inactive_leaves_graphs, households_graphs = with_data
    original_graphs = itertools.chain(*[active_graphs, active_with_inactive_leaves_graphs, households_graphs])

    def join_graph_groups((group_key, records)):
        """ collapse graph by crypta_id (to insert in normal graph, extra hh edges) """
        main_graph = records.next()
        for graph in records:
            main_graph.edges.extend(graph.edges)
            main_graph.identifiers.extend(graph.identifiers)
            main_graph.vertices.extend(graph.vertices)
        return str(group_key), main_graph

    original_graphs = dict(map(join_graph_groups, itertools.groupby(
        sorted(original_graphs, key=lambda item: item.crypta_id), key=lambda item: item.crypta_id)))

    destination = Exporter(yt).export(VERSION, diff_only=False)
    export_graphs = {str(record.get("cryptaId")): proto_graph(record) for record in yt.read_table(destination)}

    assert len(export_graphs) == len(original_graphs), \
        "Wrong graphs count. {} != {}".format(len(export_graphs), len(original_graphs))

    assert export_graphs.keys() == original_graphs.keys(), "\n" + str(export_graphs) + "\n" + str(original_graphs)

    for active_crypta_id in ACTIVE_CRYPTAIDS:
        assert active_crypta_id in export_graphs, \
            str(active_crypta_id) + "\n" + str(original_graphs.get(active_crypta_id)) + "\n" + str(households_graphs)

    for cryptaId, each_graph in export_graphs.items():
        has_yandexuid = any(
            node.Type == 'yandexuid' for node in each_graph.Nodes)
        if not has_yandexuid:
            continue

        main_yandexuid_attributes = [attribute for attribute in each_graph.Attributes if attribute.Name == 'main_yandexuid']
        assert len(main_yandexuid_attributes) == 1, \
            "Main yandexuid should be available through attributes, only once"
        assert main_yandexuid_attributes[0].Value in (node.Id for node in each_graph.Nodes if node.Type == 'yandexuid'), \
            "Main yandexuid should be one of the yandexuids"

    for cryptaId, export_graph in export_graphs.items():
        original_vertices = set(original_graphs[cryptaId].vertices)
        export_vertices = set([(id.Id, id.Type) for id in export_graph.Nodes])

        intersect = original_vertices.intersection(export_vertices)
        assert len(intersect) > 0

    # mac_ext_md5 check
    graph_with_mac = export_graphs.get(CRYPTA_ID_ACTIVE_GRAPH_WITH_MAC, None)
    assert graph_with_mac is not None, graph_with_mac
    mac_vertices = set()
    mac_ext_md5_vertices = set()
    for node in graph_with_mac.Nodes:
        if node.Type == "mac":
            mac_vertices.add(node.Id)
        elif node.Type == "mac_ext_md5":
            mac_ext_md5_vertices.add(node.Id)
    real_mac_ext_md5_vertices = set(map(convert_mac_to_md5, mac_vertices))
    assert real_mac_ext_md5_vertices == mac_ext_md5_vertices

    assert yt.exists(Paths(VERSION).get_current_graphs())
    clean_results()


def test_mac_ext_md5_convertation():
    def mac_to_md5(mac):
        mac_str = mac.replace(":", "").replace("\n", "").upper()
        mac_bin = binascii.unhexlify(mac_str)
        return md5.new(mac_bin).hexdigest().upper()

    mac = "11:22:33:44:55:66"
    assert mac_to_md5(mac) == convert_mac_to_md5(mac)

    mac = Sampler(123).mac()
    assert mac_to_md5(mac) == convert_mac_to_md5(mac), mac


def test_serialization(with_data):
    for graph_1, graph_2 in zip(collect_graphs(diff_only=False), collect_graphs(diff_only=False)):
        assert graph_1.CryptaId == graph_2.CryptaId
        assert graph_1.SerializeToString() == graph_2.SerializeToString()
    clean_results()


def test_diff(with_data):
    graphs = collect_graphs(diff_only=False)
    graphs_diff = collect_graphs(diff_only=True)
    assert len(graphs)
    assert len(graphs_diff) == 0
    tables = yt.search(destination_root(), node_type='table')
    for key, value in graph_yql_proto_fields().items():
        for table in tables:
            assert yt.get_attribute(table, key) == value
    clean_results()


def edge_has_attribute_value(node, attr_key, attr_value):
    return any(attr.Name == attr_key and attr.Value == attr_value for attr in node.Attributes)


def edge_has_attribute(node, attr_key):
    return any(attr.Name == attr_key for attr in node.Attributes)


def test_edge_attributes_present():
    graphs = collect_graphs(diff_only=False)
    for g in graphs:
        for e in g.Edges:
            assert edge_has_attribute(e, 'source_type')
            assert edge_has_attribute(e, 'log_source')
            assert not edge_has_attribute(e, 'weight'), "weight must be filtered"


def test_private_yuid():

    date = '2017-03-12'
    assert is_yuid_private(date, str(to_timestamp("2017-03-12T11:00:00Z")))
    assert is_yuid_private(
        date, "123"+str(to_timestamp("2017-03-12T23:59:59Z")))
    assert not is_yuid_private(date, str(to_timestamp("2017-03-11T23:59:59Z")))


def test_spanning_graph():
    graph = TGraph()

    graph.Nodes.add(
        Id="id1",
        Type="yandexuid"
    )
    graph.Nodes.add(
        Id="id2",
        Type="yandexuid"
    )
    graph.Nodes.add(
        Id="id3",
        Type="yandexuid"
    )

    graph.Edges.add(
        Node1=0,
        Node2=1
    )
    graph.Edges.add(
        Node1=1,
        Node2=2
    )
    graph.Edges.add(
        Node1=0,
        Node2=2
    )

    graph.Edges.add(
        # multi edges 0 -- 1
        Node1=1,
        Node2=0
    )

    #     _____
    #    /     \
    #   y1 --- y2
    #    \_y3__/

    transormer = GraphSpanningTreeTransformer()
    transormer.only_mark_edges = False

    no_active_spanning_graph = transormer.transform(graph, active_vertices=[])
    assert len(no_active_spanning_graph.Edges) == 0

    all_active_spanning_graph = transormer.transform(graph, active_vertices=[
        ("id1", "yandexuid"),
        ("id2", "yandexuid"),
        ("id3", "yandexuid"),
    ])

    #   y1 --- y2
    #    \_y3

    print (all_active_spanning_graph.Edges)
    assert len(all_active_spanning_graph.Edges) == 2

    some_active_spanning_graph = transormer.transform(graph, active_vertices=[
        ("id1", "yandexuid"),
        ("id2", "yandexuid")
    ])

    #   y1 --- y2

    print (all_active_spanning_graph.Edges)
    assert len(some_active_spanning_graph.Edges) == 1


def add_edge(proto_graph, id1, id2, source_type, log_source, weight):
    id1_index = [idx for idx, node in enumerate(proto_graph.Nodes) if node.Id == id1][0]
    id2_index = [idx for idx, node in enumerate(proto_graph.Nodes) if node.Id == id2][0]
    edge = proto_graph.Edges.add(
        Node1=id1_index,
        Node2=id2_index
    )
    edge.Attributes.add(Name='source_type', Value=source_type)
    edge.Attributes.add(Name='log_source', Value=log_source)
    edge.Attributes.add(Name='weight', Value=str(weight))


def test_weighted_spanning_graph():
    graph = TGraph()

    graph.Nodes.add(
        # should be dropped from graph
        Id="y2",
        Type="yandexuid"
    )
    graph.Nodes.add(
        Id="y1",
        Type="yandexuid"
    )
    graph.Nodes.add(
        Id="u1",
        Type="uuid"
    )
    graph.Nodes.add(
        Id="u2",
        Type="uuid"
    )
    graph.Nodes.add(
        Id="u3",
        Type="uuid"
    )
    graph.Nodes.add(
        Id="d1",
        Type="mm_device_id"
    )
    graph.Nodes.add(
        Id="d2",
        Type="mm_device_id"
    )

    #         ---
    #       / --- \
    # u1 - d1 --- u2 - d2 - u3
    #  \_________y1________/
    #   \________y2_______/
    # path from u1 to u3 should go through d1 and d2 via trusted edges

    add_edge(graph, 'd1', 'u2', 'app-metrica', 'mm', 1.0)
    add_edge(graph, 'd1', 'u2', 'app-metrica', 'mm', 0.3)
    add_edge(graph, 'u2', 'd1', 'app-metrica', 'mm', 0.3)
    add_edge(graph, 'd1', 'u1', 'app-metrica', 'mm', 1.0)
    add_edge(graph, 'd2', 'u2', 'app-metrica', 'mm', 1.0)
    add_edge(graph, 'd2', 'u3', 'app-metrica', 'mm', 1.0)
    add_edge(graph, 'y1', 'u1', 'app-url-redir', 'access', 0.3)
    add_edge(graph, 'y1', 'u3', 'app-url-redir', 'access', 1.0)
    add_edge(graph, 'y2', 'u1', 'app-url-redir', 'access', 0.1)
    add_edge(graph, 'y2', 'u3', 'app-url-redir', 'access', 0.1)

    transformer = GraphSpanningTreeTransformer()
    transformer.only_mark_edges = False
    spanning_graph = transformer.transform(graph, active_vertices=[
        ("u1", "uuid"),
        ("u3", "uuid"),
        ("y1", "yandexuid"),
    ])

    # u1 - d1 --- u2 - d2 - u3
    #            y1________/

    print (spanning_graph.Edges)
    print (spanning_graph.Nodes)

    assert len(spanning_graph.Edges) == 5
    assert len(spanning_graph.Nodes) == 6
