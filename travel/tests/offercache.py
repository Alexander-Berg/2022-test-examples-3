from tools import FAKE_NOW
from travel.hotels.test_helpers.app import BaseApp
from travel.hotels.test_helpers.label_codec import REDIR_ADD_INFO_KEY
import travel.hotels.offercache.proto.config_pb2 as config_pb2
import travel.hotels.proto.offercache_grpc.offercache_service_pb2_grpc as offercache_service_pb2_grpc
import travel.hotels.proto.promo_service.promo_service_pb2_grpc as promo_service_pb2_grpc
import travel.hotels.proto2.hotels_pb2 as hotels_pb2
import grpc

import logging
import os
import yatest.common
import json
from collections import defaultdict

LOG = logging.getLogger(__name__)


class OfferCacheApp(BaseApp):
    ENABLE_OPERATOR_BUMP = False
    __user_1__ = '1111111111'
    __user_2__ = '2222222222'
    __user_3__ = '3333333333'
    __user_4__ = '4444444444'

    __hotel_1__ = {'PartnerId': hotels_pb2.PI_TRAVELLINE, 'OriginalId': '1'}
    __hotel_2__ = {'PartnerId': hotels_pb2.PI_TRAVELLINE, 'OriginalId': '2'}
    __hotel_3__ = {'PartnerId': hotels_pb2.PI_TRAVELLINE, 'OriginalId': '3'}
    __hotel_4__ = {'PartnerId': hotels_pb2.PI_TRAVELLINE, 'OriginalId': '4'}
    __hotel_5__ = {'PartnerId': hotels_pb2.PI_TRAVELLINE, 'OriginalId': '5'}

    def __init__(self, http_port, grpc_port, mon_port, promo_service_grpc_port, searcher, message_bus, outdated_offer_bus, lc, yt_client):
        super(OfferCacheApp, self).__init__("offercache", http_port)
        redir_add_info_key_file = os.path.join(yatest.common.work_path(), 'offercache_redir_add_info_key.txt')
        with open(redir_add_info_key_file, 'w') as f:
            f.write(REDIR_ADD_INFO_KEY)
        LOG.info('Redir AddInfo key saved')

        self.log_file = os.path.join(yatest.common.work_path(), 'offercache.log')  # TODO remove it, let it writes to console again

        self.prepare_config(config_pb2.TConfig, http_port=http_port, grpc_port=grpc_port, mon_port=mon_port,
                            promo_service_grpc_port=promo_service_grpc_port,
                            searcher_port=searcher.port, redir_add_info_key_file=redir_add_info_key_file,
                            fake_now=FAKE_NOW, yt_table=message_bus.table_path, yt_server=message_bus.server_name,
                            main_log_file=self.log_file, yt_table_outdated_offer_bus=outdated_offer_bus.table_path)

        self.searcher = searcher
        self.lc = lc
        self.now = message_bus.now

        self.searcher.set_offercache(self)

        self.partners = dict()
        self.partners_by_id_string = dict()
        self.partnercode2id = dict()
        for row in yt_client.read_table('//home/travel/testing/config/partners'):
            self.partners[row['PartnerIdInt']] = row
            self.partners_by_id_string[row['PartnerId']] = row
            self.partnercode2id[row['Code']] = row['PartnerIdInt']

        self.operators = dict()
        self.partner2operators = defaultdict(list)
        for row in yt_client.read_table('//home/travel/testing/config/operators'):
            self.operators[row['OperatorIdInt']] = row
            if row['Enabled']:
                self.partner2operators[self.partners_by_id_string[row['PartnerId']]['PartnerIdInt']].append(row['OperatorIdInt'])

        self.bumped_op_ids = []
        if self.ENABLE_OPERATOR_BUMP:
            for b in self.base_config.Experiments.BumpedOperator:
                self.bumped_op_ids.append(b.Operator)

        oc_grpc_channel = grpc.insecure_channel('[::1]:%s' % grpc_port)
        self.grpc_stub = offercache_service_pb2_grpc.OfferCacheServiceV1Stub(oc_grpc_channel)

        promo_service_grpc_channel = grpc.insecure_channel('[::1]:%s' % promo_service_grpc_port)
        self.promo_service_grpc_stub = promo_service_pb2_grpc.PromoServiceV1Stub(promo_service_grpc_channel)

    def read_log(self):
        with open(self.log_file, 'rb') as f:
            return f.read()

    def read(self, params):
        print("\nREQ:\n%s\n----------------\n" % json.dumps(params, indent=2))
        resp = self.checked_get('read', params=params)
        print("\nRESP:\n%s----------------\n" % json.dumps(resp.json(), indent=2))
        return resp.json()

    def wait_flush(self):
        self.get('wait-flush')

    def grpc_ping(self, req):
        return self.grpc_stub.Ping(req)

    def grpc_search_offers(self, req):
        return self.grpc_stub.SearchOffers(req)

    def promo_service_ping(self, req):
        return self.promo_service_grpc_stub.Ping(req)

    def promo_service_determine_promos_for_offer(self, req):
        return self.promo_service_grpc_stub.DeterminePromosForOffer(req)

    def promo_service_get_white_label_points_props(self, req):
        return self.promo_service_grpc_stub.GetWhiteLabelPointsProps(req)

    @staticmethod
    def write_blacklist_table(yt_stuff, additional_rows):
        with open(yatest.common.source_path('travel/hotels/offercache/tests/resources/blacklisted_hotels.json')) as f:
            rows_from_file = json.load(f)
        assert len({x['permalink'] for x in rows_from_file} & {x['permalink'] for x in additional_rows}) == 0
        OfferCacheApp.write_data_to_yt(yt_stuff, rows_from_file + additional_rows, '//home/travel/testing/blacklist_hotels_transposed')

    @staticmethod
    def write_whitelist_table(yt_stuff, additional_rows):
        with open(yatest.common.source_path('travel/hotels/offercache/tests/resources/whitelisted_hotels.json')) as f:
            rows_from_file = json.load(f)
        assert len({x['permalink'] for x in rows_from_file} & {x['permalink'] for x in additional_rows}) == 0
        OfferCacheApp.write_data_to_yt(yt_stuff, rows_from_file + additional_rows, '//home/travel/testing/hotels_whitelist')

    @staticmethod
    def write_user_order_counters_table(yt_stuff):
        OfferCacheApp.write_data_to_yt(yt_stuff, [], '//home/travel/testing/promo/user_order_counters')

    @staticmethod
    def write_mir_white_list(yt_stuff):
        OfferCacheApp.write_data_to_yt(yt_stuff, [], '//home/travel/testing/general/mir/latest/hotels')

    @classmethod
    def write_plus_user_lists(cls, yt_stuff):
        whitelist = [
            {'PassportId': cls.__user_1__},
            {'PassportId': cls.__user_3__},
        ]
        OfferCacheApp.write_data_to_yt(yt_stuff, whitelist, '//home/travel/testing/general/plus_user_lists/latest/whitelist')
        blacklist = [
            {'PassportId': cls.__user_2__},
            {'PassportId': cls.__user_3__},
        ]
        OfferCacheApp.write_data_to_yt(yt_stuff, blacklist, '//home/travel/testing/general/plus_user_lists/latest/blacklist')

        user_order_counters_by_type = [
            {
                'PassportId': int(cls.__user_4__),
                'OrderType': 'OT_HOTEL',
                'OrderCount': 1,
            },
        ]
        OfferCacheApp.write_data_to_yt(yt_stuff, user_order_counters_by_type, '//home/travel/testing/general/plus_user_lists/latest/user_order_counters_by_type')

    @classmethod
    def write_plus_hotel_lists(cls, yt_stuff):
        whitelist = [
            cls.__hotel_5__,
        ]
        blacklist = list()
        OfferCacheApp.write_data_to_yt(yt_stuff, whitelist, '//home/travel/testing/general/promo_events/cultural_dreams/latest/whitelist')
        OfferCacheApp.write_data_to_yt(yt_stuff, blacklist, '//home/travel/testing/general/promo_events/cultural_dreams/latest/blacklist')

    @classmethod
    def write_yandex_eda_lists(cls, yt_stuff):
        whitelist = [
            cls.__hotel_1__,
            cls.__hotel_2__,
        ]
        blacklist = [
            cls.__hotel_2__,
        ]
        OfferCacheApp.write_data_to_yt(yt_stuff, whitelist, '//home/travel/testing/general/yandex_eda_2022_hotel_lists/latest/whitelist')
        OfferCacheApp.write_data_to_yt(yt_stuff, blacklist, '//home/travel/testing/general/yandex_eda_2022_hotel_lists/latest/blacklist')

    @classmethod
    def write_plus_additional_fee(cls, yt_stuff):
        rows = [
            {
                **cls.__hotel_3__,
                'StartsAtSeconds': FAKE_NOW - 10,
                'EndsAtSeconds': FAKE_NOW - 5,
                'AdditionalPointsPercent': 1.0,
                'AdditionalPointsMax': 1,
                'AdditionalFeePercent': 1.0,
                'AdditionalFeeMax': 1,
            },
            {
                **cls.__hotel_3__,
                'StartsAtSeconds': FAKE_NOW - 5,
                'EndsAtSeconds': FAKE_NOW + 5,
                'AdditionalPointsPercent': 1.5,
                'AdditionalPointsMax': 1,
                'AdditionalFeePercent': 1.5,
                'AdditionalFeeMax': 1,
            },
        ]
        OfferCacheApp.write_data_to_yt(yt_stuff, rows, '//home/travel/testing/general/plus_additional_fee/latest/hotels')

    @staticmethod
    def append_travelline_rate_plans_table(yt_stuff, rows):
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath('//home/travel/testing/config/travelline_rate_plan_info', append=True), rows, raw=False)

    @staticmethod
    def append_dolphin_tours_table(yt_stuff, rows):
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath('//home/travel/testing/config/dolphin_tours', append=True), rows, raw=False)

    @staticmethod
    def append_dolphin_pansions_table(yt_stuff, rows):
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath('//home/travel/testing/config/dolphin_pansions', append=True), rows, raw=False)

    @staticmethod
    def append_dolphin_rooms_table(yt_stuff, rows):
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath('//home/travel/testing/config/dolphin_rooms', append=True), rows, raw=False)

    @staticmethod
    def append_dolphin_room_cats_table(yt_stuff, rows):
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath('//home/travel/testing/config/dolphin_room_cats', append=True), rows, raw=False)

    @staticmethod
    def append_bnovo_rate_plans_table(yt_stuff, rows):
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath('//home/travel/testing/config/bnovo_rate_plan_info', append=True), rows, raw=False)

    @staticmethod
    def write_permarooms_table(yt_stuff, rows):
        dir_path = '//home/travel/testing/general/permarooms'
        yt_stuff.yt_client.create('map_node', dir_path, recursive=True)
        yt_stuff.yt_client.write_table(yt_stuff.yt_client.TablePath(f'{dir_path}/permarooms-json'), rows, raw=False)
