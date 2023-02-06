from travel.hotels.test_helpers.app import BaseApp
from travel.hotels.test_helpers.message_bus import MessageBus
import travel.hotels.geocounter.proto.config_pb2 as config_pb2
import travel.hotels.proto2.bus_messages_pb2 as bus_messages_pb2
import travel.hotels.proto.geocounter_service.geocounter_service_pb2_grpc as geocounter_service_pb2_grpc

from google.protobuf.text_format import Parse

import grpc
import logging
import os
import yatest.common

LOG = logging.getLogger(__name__)


class GeoCounterApp(BaseApp):
    def __init__(self, yt_stuff, http_port, grpc_port, mon_port):
        super().__init__('geocounter', http_port)

        log_file = os.path.join(yatest.common.work_path(), 'geocounter.log')

        self.prepare_config(config_pb2.TConfig, http_port=http_port, grpc_port=grpc_port,
                            mon_port=mon_port, main_log_file=log_file, yt_server=yt_stuff.get_server())

        channel = grpc.insecure_channel('[::1]:%s' % grpc_port)
        self.grpc_stub = geocounter_service_pb2_grpc.GeoCounterServiceV1Stub(channel)

    def grpc_ping(self, req):
        return self.grpc_stub.Ping(req)

    def grpc_get_counts(self, req):
        return self.grpc_stub.GetCounts(req)

    @staticmethod
    def write_geocounter_table(yt_stuff):
        GeoCounterApp.write_json_to_yt(yt_stuff, 'travel/hotels/geocounter/tests/resources/geocounter_table.json', '//home/travel/testing/geocounter-table')

    @staticmethod
    def write_regions_table(yt_stuff):
        GeoCounterApp.write_json_to_yt(yt_stuff, 'travel/hotels/geocounter/tests/resources/regions_table.json', '//home/travel/testing/regions-table')

    @staticmethod
    def write_prices_table(yt_stuff):
        GeoCounterApp.write_json_to_yt(yt_stuff, 'travel/hotels/geocounter/tests/resources/prices.json', '//home/travel/testing/prices-table')

    @staticmethod
    def write_hotel_traits_table(yt_stuff):
        GeoCounterApp.write_json_to_yt(yt_stuff, 'travel/hotels/geocounter/tests/resources/hotel_traits.json', '//home/travel/testing/hotel-traits-table')

    @staticmethod
    def write_original_id_to_permalink_mapper_table(yt_stuff):
        GeoCounterApp.write_json_to_yt(yt_stuff, 'travel/hotels/geocounter/tests/resources/original_id_to_permalink_mapper.json', '//home/travel/testing/original_id_to_permalink_mapper')

    @staticmethod
    def write_offer_bus_messages(yt_stuff):
        message_bus = MessageBus(yt_stuff, '//home/travel/testing/offer_bus')
        LOG.info('MessageBus created')

        messages_dir = yatest.common.source_path('travel/hotels/geocounter/tests/resources/offer_bus_messages')
        messages = []
        for filename in os.listdir(messages_dir):
            LOG.info("Reading message file %s", filename)
            messages.append(GeoCounterApp.offer_bus_message_from_file(os.path.join(messages_dir, filename)))

        message_bus.write('ru.yandex.travel.hotels.TSearcherMessage', messages)

    @staticmethod
    def offer_bus_message_from_file(filename):
        with open(filename) as f:
            text = f.read()
        message = bus_messages_pb2.TSearcherMessage()
        return Parse(text, message)
