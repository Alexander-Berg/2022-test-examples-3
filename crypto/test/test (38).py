import datetime

from crypta.lib.python import time_utils

from crypta.ltp.viewer.lib.structs.id import Id
from crypta.ltp.viewer.lib.structs.filter import Filter
from crypta.ltp.viewer.lib.structs.task import Task
from crypta.ltp.viewer.lib.structs.page import Page
from crypta.ltp.viewer.lib.structs.record import Record


WATCH_LOG = "WatchLog"
TX_LOG = "TxLog"


def test_client(clean_local_ydb, client):
    id1 = Id("yuid", "1")
    id2 = Id("puid", "2")
    ids = [
        id1,
        id2,
    ]
    history_id = "history-id"
    tasks = [
        Task("yuid", "1", WATCH_LOG, "2020-10-01"),
        Task("yuid", "1", WATCH_LOG, "2020-10-02"),
        Task("puid", "2", WATCH_LOG, "2020-10-01"),
        Task("puid", "2", TX_LOG, "2020-10-03"),
    ]
    records = [
        Record(1600000000, "description", "additional_description"),
        Record(1600000002, "description2", "additional_description2"),
        Record(1600000003, "description3", "additional_description3"),
    ]
    scheduled = client.add_graph(ids, history_id, tasks, schedule_limit=1)
    for _ in range(2):
        for row in scheduled:
            scheduled = client.insert_chunk(row.date, row.log, records, Id(row.id_type, row.id))

    page_all = Page(10, 0)

    history = {
        "offset": client.get_history(id1, Page(2, 1)),
        "min_timestamp": client.get_history(id1, page_all, Filter(min_timestamp=1600000003)),
        "max_timestamp": client.get_history(id1, page_all, Filter(max_timestamp=1600000000)),
        "log": client.get_history(id1, page_all, Filter(log_filter=WATCH_LOG)),
        "id": client.get_history(id1, page_all, Filter(id_filter=id1.id)),
        "id_type": client.get_history(id1, page_all, Filter(id_type_filter=id2.id_type)),
        "description": client.get_history(id1, page_all, Filter(description_filter="2")),
        "additional_description_filter": client.get_history(id1, page_all, Filter(additional_description_filter="3")),
    }

    return {
        "id-to-history-id": clean_local_ydb.dump_table(client.path("id-to-history-id")),
        "log": clean_local_ydb.dump_dir(client.path(history_id)),
        "history": history,
        "progress": client.get_progress(id1),
    }


def test_user_queries(client):
    id1 = Id("yuid", "1")
    id2 = Id("puid", "2")
    owner = "owner"

    time_utils.set_current_time(1600000000)
    client.save_query(owner, id1, "2021-10-01", "2021-10-02")
    time_utils.set_current_time(1600001000)
    client.save_query(owner, id2, "", "")

    all_queries = client.get_user_queries(owner)

    client.expire_queries(datetime.timedelta(seconds=500))
    not_expired_queries = client.get_user_queries(owner)
    return {
        "all_queries": all_queries,
        "not_expried_queries": not_expired_queries,
    }


def test_expire_history(clean_local_ydb, client):
    def add_graph(ts, id_):
        time_utils.set_current_time(ts)
        client.add_graph([id_], "{}-{}".format(id_.id_type, id_.id), [Task(id_.id_type, id_.id, WATCH_LOG, "2020-10-01")], schedule_limit=1)

    add_graph(1600000000, Id("yuid", "1"))
    add_graph(1600001000, Id("yuid", "2"))
    add_graph(1600002000, Id("yuid", "3"))
    add_graph(1600003000, Id("yuid", "4"))

    time_utils.set_current_time(1600003000)
    expired = client.get_expired_history_ids(datetime.timedelta(seconds=1500))
    for history_id in expired:
        client.drop_history(history_id)

    return {
        "expired": expired,
        "id-to-history-id": clean_local_ydb.dump_table(client.path("id-to-history-id")),
        "files": sorted(item.name for item in clean_local_ydb.client.list_directory(""))
    }
