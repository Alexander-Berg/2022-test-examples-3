# coding: utf8

import __classic_import     # noqa
from market.library.shiny.lite.suite import ShinySuite
from market.click_n_collect.beam.service import ClickNCollectServer
from market.click_n_collect.proto.common.common_pb2 import TUserInfo
from market.click_n_collect.proto.event_log_pb2 import TOrder, TBan, TEventLogRows
from market.click_n_collect.proto.table_schema.orders_pb2 import TRow as TOrdersRow
from market.click_n_collect.proto.table_schema.counters_pb2 import TRow as TCountersRow
from market.click_n_collect.proto.table_schema.order_states_pb2 import TRow as TOrderStatesRow
from market.click_n_collect.mock.goods.proto.get_stock_pb2 import TResponse as TStockResponse
from market.click_n_collect.tools.yt_tables_deployer.library.protobuf_format import get_schema
from market.pylibrary.lite.structure_matching import contains_fragment
from market.library.shiny.lite.log import CommonLogFrontend
from market.pylibrary.lite.log import TskvBasedLogFrontend, LogBackend
from google.protobuf.json_format import MessageToJson
import json
import time


class TestSuite(ShinySuite):
    svc_cls = ClickNCollectServer

    trace_log = TskvBasedLogFrontend('trace log')
    mail_server_common_log = CommonLogFrontend()
    yasms_server_common_log = CommonLogFrontend()

    tvm_client_id = 43
    tvm_client_secret = "3:serv:CBAQ__________9_IgQIKxAq:JZZEG9kCMTHXlogE2WowCWbFUIZEbB017zoQPfh2jPmNzJng1J_x6-tCSxBic"\
        "fa1zy0KTbrrI2-SN7YA1YPXBLwC5T3v4q-CAktbCeF5SYa-S2yl0QVO3sG2gZAfpJeXDcAIDWVxNwbnfnPtZZX9O77OSDdDNVqbPN7gJ-R7k-o"

    @classmethod
    def _after_prepare(cls):
        cls.click_n_collect.before_server_start_callback = cls.before_server_start
        cls.click_n_collect.after_server_stop_callback = cls.after_server_stop
        super(TestSuite, cls)._after_prepare()

    def _teardown_check(self, server):
        error, reasons = self.trace_log.check_backend(LogBackend(server.config.Core.TraceLog.Target.FilePath))
        if error is not None:
            return error, reasons
        if server.with_mail:
            error, reasons = self.mail_server_common_log.check_backend(server.mail.common_log)
            if error is not None:
                return error, reasons
        if server.with_yasms:
            error, reasons = self.yasms_server_common_log.check_backend(server.yasms.common_log)
            if error is not None:
                return error, reasons
        return super(TestSuite, self)._teardown_check(server)

    @classmethod
    def get_headers(cls, headers={}):
        default = {'X-Ya-Service-Ticket': TestSuite.tvm_client_secret}
        default.update(headers)
        return default

    @classmethod
    def get_goods_outlet(cls, goods_id, id, latlon):
        outlet = TStockResponse.TOutlet()
        outlet.GoodsId = goods_id
        outlet.Price = 100 + id
        outlet.RemainQty = 1 + id
        outlet.Location.Identification.Id = str(id)
        outlet.Location.Identification.ExternalId = "7777777"
        outlet.Location.Identification.Name = "zzz"
        outlet.Location.Location.Geo.Lon = latlon[1]
        outlet.Location.Location.Geo.Lat = latlon[0]
        outlet.Location.Owner.Id = "12"
        outlet.Location.LegalInfo.Plain = "Legal Statement. This test is created and operated by Zaripov Kamil."
        outlet.Location.IsActive = True
        outlet.Location.Label.Caption = "Тестовая точка Goods.ru №3"
        outlet.Location.Label.Contacts = "+75992342323"
        outlet.Location.Label.Address = "г Москва,  к 2"
        outlet.Location.Label.Schedule = "ежедневно с 5 до 8"
        return outlet

    @classmethod
    def get_antifraud_ban_table_attrs(cls):
        table_schema = [
            {'name': 'buyer_yandexuids', 'type': 'any'},
            {'name': 'buyer_uids', 'type': 'any'},
            {'name': 'buyer_uuids', 'type': 'any'},
            {'name': 'frauds', 'type': 'any'},
        ]
        return {'strict': True, 'schema': table_schema}

    @classmethod
    def get_orders_table_attrs(cls):
        return {'strict': True, 'schema': get_schema(TOrdersRow), 'dynamic': True}

    @classmethod
    def get_counters_table_attrs(cls):
        return {'strict': True, 'schema': get_schema(TCountersRow), 'dynamic': True}

    @classmethod
    def get_orders_states_table_attrs(cls):
        return {'strict': True, 'schema': get_schema(TOrderStatesRow), 'dynamic': True}

    @classmethod
    def check_yt_table_contains(cls, yt, table_path, expected):
        actual = list(yt.read_table(table_path))
        actual_parsed = []
        for row in actual:
            for key, value in row.iteritems():
                if key == 'user_info':
                    msg = TUserInfo()
                    msg.ParseFromString(value)
                    row[key] = json.loads(MessageToJson(msg, preserving_proto_field_name=True))
                if key == 'order_info' and value:
                    msg = TOrder()
                    msg.ParseFromString(value)
                    row[key] = json.loads(MessageToJson(msg, preserving_proto_field_name=True))
                if key == 'ban_reason' and value:
                    msg = TBan()
                    msg.ParseFromString(value)
                    row[key] = json.loads(MessageToJson(msg, preserving_proto_field_name=True))
            actual_parsed.append(row)
        actual = actual_parsed

        result = contains_fragment(expected, actual, preserve_order=False, allow_different_len=False)
        assert result[0], '\n'+'\n'.join(result[1])

    @classmethod
    def check_yt_table_contains2(cls, yt, table, expected, preserve_order=False, allow_different_len=False):
        actual = list(yt.select_rows('* FROM [{table}]'.format(table=table)))
        result = contains_fragment(expected, actual, preserve_order=preserve_order, allow_different_len=allow_different_len)
        assert result[0], '\n'+'\n'.join(result[1])

    @classmethod
    def get_table_mtime(cls, yt, table):
        return yt.get(table + '/@modification_time')

    @classmethod
    def wait_table_changed(cls, yt, table, old_mtime, timeout_sec=60):
        deadline = time.time() + timeout_sec
        while time.time() < deadline:
            mtime = cls.get_table_mtime(yt, table)
            if mtime > old_mtime:
                return mtime
            time.sleep(.1)
        raise Exception('%s table not changed in %s sec' %(table, timeout_sec))

    @classmethod
    def check_fs_file_contains(cls, file_path, expected):
        msg = TEventLogRows()
        msg.ParseFromString(open(file_path, 'rb').read())
        actual = json.loads(MessageToJson(msg, preserving_proto_field_name=True))
        result = contains_fragment(expected, actual, preserve_order=False, allow_different_len=False)
        assert result[0], '\n'+'\n'.join(result[1])

    @classmethod
    def before_server_start(cls):
        # will be called before svn_cls will be started but after external services start
        pass

    @classmethod
    def after_server_stop(cls):
        # will be called after svn_cls will be stopped but before external services stop
        pass
