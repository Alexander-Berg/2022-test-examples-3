import mock

from travel.marketing.content.admin.admin.connection import MdbHostManager
from travel.library.python.avia_mdb_replica_info.avia_mdb_replica_info import ClusterInfo, Replica


def test_fallback_hosts():
    manager = MdbHostManager("token", "sas")

    with mock.patch.object(manager.mdb_api, 'get_cluster_info', side_effect=ValueError()):
        host_string = manager.get_host_string("c1", "sas-4431.yandex.net,iva-3451.yandex.net,man-8523.yandex.net")

    assert host_string == "sas-4431.yandex.net,iva-3451.yandex.net,man-8523.yandex.net"


def test_mdb_hosts():
    manager = MdbHostManager("token", "sas")

    cluster = ClusterInfo("c1", [
        Replica("sas-4431.yandex.net", "sas"),
        Replica("iva-3451.yandex.net", "iva"),
        Replica("man-8523.yandex.net", "man"),
    ])

    with mock.patch.object(manager.mdb_api, 'get_cluster_info', return_value=cluster):
        host_string = manager.get_host_string("c1", "sas-5413.yandex.net,iva-4313.yandex.net,man-6234.yandex.net")

    assert host_string == "sas-4431.yandex.net,iva-3451.yandex.net,man-8523.yandex.net"
