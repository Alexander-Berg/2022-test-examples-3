#!/usr/bin/env python
# encoding: utf-8

import os
import pytest
import json

from google.protobuf import json_format

from mapreduce.yt.python.yt_stuff import YtConfig
import yatest.common
from yatest.common import network

from travel.hotels.test_helpers.app import BaseApp
from travel.hotels.test_helpers.label_codec import LABEL_KEY, TOKEN_KEY, REDIR_ADD_INFO_KEY, LabelCodec
from travel.hotels.test_helpers.message_bus import yt_config_args, MessageBus
from travel.hotels.test_helpers.yt_key_value_storage import InitialMessage, YtKeyValueStorage
import travel.hotels.proto2.bus_messages_pb2 as bus_messages_pb2
import travel.hotels.proto.search_flow_offer_data.offer_data_pb2 as offer_data_pb2
import travel.hotels.redir.proto.config_pb2 as config_pb2


class RedirApp(BaseApp):
    def __init__(self, http_port, message_bus, reqans_path):
        super(RedirApp, self).__init__("redir", http_port)
        self.message_bus = message_bus
        self.reqans_path = reqans_path
        self.reqans_line = 0

    def redir_by_url(self, url, label, other_params=None):
        params = {
            'PUrl': url,
            'ProtoLabel': label,
        }
        if other_params:
            params.update(other_params)
        res = self.get('redir', params=params, allow_redirects=False)
        self.wait_flush()
        return res

    def redir_by_token(self, token, label, label_hash, other_params=None):
        params = {
            'Token': token,
            'ProtoLabel': label,
            'LabelHash': label_hash,
        }
        if other_params:
            params.update(other_params)
        res = self.get('redir', params=params, allow_redirects=False)
        self.wait_flush()
        return res

    def redir_by_offer_id(self, offer_id, label, other_params=None):
        params = {'OfferId': offer_id, 'ProtoLabel': label}
        if other_params:
            params.update(other_params)
        res = self.get('redir', params=params, allow_redirects=False)
        self.wait_flush()
        return res

    def wait_flush(self):
        self.get('wait-flush')

    def read_price_check_requests(self):
        return self.message_bus.read('ru.yandex.travel.hotels.TPriceCheckReq', bus_messages_pb2.TPriceCheckReq)

    def read_reqans_new_records(self):
        if not os.path.exists(self.reqans_path):
            return []
        res = []
        with open(self.reqans_path, 'r') as f:
            for line_nr, line in enumerate(f):
                if line_nr < self.reqans_line:
                    continue  # Skip already read lines
                else:
                    self.reqans_line += 1
                    res.append(json.loads(line))
        return res


@pytest.fixture(scope='session')
def yt_config(request):
    return YtConfig(**yt_config_args)


def write_initial_offers(offer_data_storage):
    offers = []
    with open(yatest.common.source_path('travel/hotels/redir/tests/resources/initial_offers.json')) as f:
        for raw_offer in json.load(f):
            offers.append(InitialMessage(raw_offer["OfferId"], json_format.Parse(json.dumps(raw_offer), offer_data_pb2.TOfferDataMessage())))
    offer_data_storage.write('NTravelProto.NSearchFlowOfferData.TOfferDataMessage', offers)


@pytest.fixture(scope='module')
def redir_app(yt_stuff):
    with network.PortManager() as pm:
        message_bus = MessageBus(yt_stuff, '//home/travel/test/pricecheckreq_bus')
        offer_data_storage = YtKeyValueStorage(yt_stuff, '//home/travel/test/search_flow_offer_data_storage')
        write_initial_offers(offer_data_storage)

        yatest.common.execute([
            yatest.common.binary_path('travel/hotels/devops/cfg_tool/cfg_tool'),
            '-e', 'testing', '--i-know-what-i-am-doing',
            '--yt-proxy', yt_stuff.get_server(),
            '-c', yatest.common.source_path('travel/hotels/devops/config/cfg_tool'),
            'push'
        ]).wait(check_exit_code=True)

        add_info_key_file = os.path.join(yatest.common.work_path(), 'redir_add_info_key.txt')
        with open(add_info_key_file, 'w') as f:
            f.write(REDIR_ADD_INFO_KEY)

        main_port = pm.get_port()
        mon_port = pm.get_port()
        reqans_path = os.path.join(yatest.common.work_path(), 'reqans.log')
        app = RedirApp(main_port, message_bus, reqans_path)

        app.prepare_config(config_pb2.TConfig, main_port=main_port, mon_port=mon_port, reqans_logfile=reqans_path,
                                    yt_table=message_bus.table_path, yt_server=message_bus.server_name,
                                    add_info_key_file=add_info_key_file,
                                    yt_offer_data_table=offer_data_storage.table_path)

        label_key_file = os.path.join(yatest.common.work_path(), 'redir_label_key.txt')
        with open(label_key_file, 'wb') as f:
            f.write(LABEL_KEY)

        token_key_file = os.path.join(yatest.common.work_path(), 'redir_token_key.txt')
        with open(token_key_file, 'wb') as f:
            f.write(TOKEN_KEY)

        app.start('-l', label_key_file, '--travel-token-path', token_key_file)
        try:
            app.wait_ready()
            yield app
        finally:
            app.stop()


@pytest.fixture(scope='session')
def label_codec():
    lc = LabelCodec()
    try:
        lc.start()
        yield lc
    finally:
        lc.stop()
