import yatest
import subprocess
import os

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

import market.proto.report.WhiteSuppliers_pb2 as WhiteSuppliers_pb2

from google.protobuf.json_format import MessageToDict


YT_SERVER = None


def setup_group_access(yt):
    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()
    setup_group_access(module.YT_SERVER.get_yt_client())


def create_table(yt, table_name, attrs={}):
    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=attrs,
    )


def run_bin(cmdlist, raise_log=False):
    try:
        subprocess.check_call(cmdlist)
    except:
        raise Exception(open('marketplaces.log').read())

    if raise_log:
        raise Exception(open('marketplaces.log').read())


def test_dump_suppliers_data():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    suppliers_table_path = '//home/suppliers_data'
    create_table(yt, suppliers_table_path)
    yt.write_table(suppliers_table_path, [{"ogrn": "123", "jur_name": "name1", "jur_address": "address1"},
                                          {"ogrn": "124", "jur_name": "name2", "jur_address": "address2"},
                                          {"jur_name": "name3", "jur_address": "address3"}])

    cmdlist = [
        yatest.common.binary_path('market/idx/marketplaces/banned-offers-reporter/bin/banned-offers-reporter'),
        "--yt-suppliers-data-table-path", suppliers_table_path,
        "--yt-server", YT_SERVER.get_server(),
        "--suppliers-data-dump-path", "."
    ]

    run_bin(cmdlist)
    assert os.path.exists('white_suppliers.pb')
    assert os.path.getsize('white_suppliers.pb') > 0

    result_file = open('white_suppliers.pb', 'rb')
    data = result_file.read()
    result_file.close()

    white_suppliers_proto = WhiteSuppliers_pb2.Suppliers()
    white_suppliers_proto.ParseFromString(data)

    result = MessageToDict(white_suppliers_proto, preserving_proto_field_name=True)
    message = result['suppliers'][0]
    assert message["ogrn"] == "123"
    assert message["jur_name"] == "name1"
    assert message["jur_address"] == "address1"

    message = result['suppliers'][1]
    assert message["ogrn"] == "124"
    assert message["jur_name"] == "name2"
    assert message["jur_address"] == "address2"


def test_banned_offers():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    bad_suppliers_table = '//home/bad_suppliers'
    yt_idx_path = '//home'
    offers_table = yt_idx_path + '/offers/recent'
    process_log_table = '//home/process_log'

    indexer_generation = "indexer_generation"

    create_table(yt, bad_suppliers_table)
    yt.write_table(bad_suppliers_table, [{"ogrn": "123"},
                                         {"ogrn": "124"},
                                         {"extra_column": "125"}])

    suppliers_table_path = '//home/suppliers_data'
    create_table(yt, suppliers_table_path)
    yt.write_table(suppliers_table_path, [{"ogrn": "100", "jur_name": "name1", "jur_address": "address1"},
                                          {"ogrn": "125", "jur_name": "name2", "jur_address": "address2"},
                                          {"jur_name": "name3", "jur_address": "address3"}])

    create_table(yt, offers_table,
                 attrs={"schema": [{"name": "supplier_id", "type": "uint64"},
                                   {"name": "offer_id", "type": "string"},
                                   {"name": "supplier_name", "type": "string"},
                                   {"name": "supplier_ogrn", "type": "uint64"},
                                   {"name": "feed_id", "type": "uint64"},
                                   {"name": "session_id", "type": "string"},
                                   {"name": "title", "type": "string"}]})
    yt.write_table(offers_table, [{"offer_id": "offer1", "supplier_ogrn": 123, "supplier_name": "s1", "feed_id": 1, "title": "title"},
                                  {"offer_id": "offer2", "supplier_ogrn": 123, "supplier_name": "s1", "feed_id": 1, "title": "title"},
                                  {"offer_id": "offer3", "supplier_ogrn": 100, "supplier_name": "s1", "feed_id": 2, "title": "title"},
                                  {"offer_id": "offer4", "supplier_ogrn": 124, "feed_id": 3, "title": "title"},
                                  {"offer_id": "offer5", "feed_id": 4, "title": "title"},
                                  {"offer_id": "offer6", "supplier_ogrn": 125, "feed_id": 1, "title": "title"},
                                  {"offer_id": "offer7", "supplier_ogrn": 200, "feed_id": 1, "title": "title"},
                                  ])

    feedsToSessions = {1: "20200316_1133", 2: "20200316_1134", 3: "20200316_1134", 4: "20200316_1135"}
    f = open('feedsToSessions', 'w')
    for feed_id, session_id in feedsToSessions.iteritems():
        f.write("%d,%s,,1,2\n" % (feed_id, session_id))
    f.close()

    cmdlist = [
        yatest.common.binary_path('market/idx/marketplaces/banned-offers-reporter/bin/banned-offers-reporter'),
        "--yt-idx-path", yt_idx_path,
        "--yt-server", YT_SERVER.get_server(),
        "--suppliers-data-dump-path", ".",
        "--yt-bad-offers-table-path", bad_suppliers_table,
        "--yt-suppliers-data-table-path", suppliers_table_path,
        "--yt-process-log-table-path", process_log_table,
        "--feeds-sessions-path", 'feedsToSessions',
        "--indexer-generation", indexer_generation,
    ]

    run_bin(cmdlist)

    assert "process_log" in yt.list('//home')

    process_log_rows = list(yt.select_rows("* from [{0}]".format(process_log_table)))
    blocked_offers = [x['offer_id'] for x in process_log_rows]
    assert "offer1" in blocked_offers
    assert "offer2" in blocked_offers
    assert "offer4" in blocked_offers
    assert "offer7" in blocked_offers
    assert "offer3" not in blocked_offers
    assert "offer5" not in blocked_offers
    assert "offer6" not in blocked_offers

    for row in process_log_rows:
        assert row["feed_id"] in feedsToSessions.keys()
        assert row["session_id"] in feedsToSessions.values()
        assert row["source"] == "indexer"
        assert row["indexer_generation"] == "indexer_generation"
        assert row["offer_title"] == "title"
        if row["offer_id"] == "offer7":
            assert row["code"] == "498"
        else:
            assert row["code"] == "497"

#   assert tmp table deleted
    assert len(filter(lambda x: '//tmp/market/idx/marketplaces/banned-offers-reporter' in x, yt.list('//home'))) == 0
