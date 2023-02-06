from __future__ import annotations

import tools
import checkers
from offercache import OfferCacheApp

import travel.hotels.proto2.bus_messages_pb2 as bus_messages_pb2
import travel.hotels.proto2.hotels_pb2 as hotels_pb2
import travel.proto.commons_pb2 as commons_pb2
import google.protobuf.wrappers_pb2 as wrappers_pb2
import google.protobuf.timestamp_pb2 as timestamp_pb2

from travel.hotels.test_helpers.message_bus import MessageBus

import logging
import random
import json
import uuid

from collections import defaultdict
from enum import Enum
from typing import Dict, List, Tuple


LOG = logging.getLogger(__name__)


class TravellineRatePlanType(Enum):
    BLOCKED = 1
    ALLOWED = 2
    UNKNOWN = 3


class DolphinItemType(Enum):
    BLOCKED = 1
    ALLOWED = 2
    UNKNOWN = 3


class BNovoRatePlanType(Enum):
    BLOCKED = 1
    ALLOWED = 2
    UNKNOWN = 3


class RefundRule:
    def __init__(self, type: hotels_pb2.ERefundType, starts_at_sec: int, ends_at_sec: int, penalty: int):
        self.type = type
        self.starts_at_sec = starts_at_sec
        self.ends_at_sec = ends_at_sec
        self.penalty = penalty


class IdentifiersProvider:
    def __init__(self):
        self.last_permalink = 1000
        self.last_original_id = 1000
        self.last_permaroom_id = 1000
        self.last_permaroom_name = 0
        self.last_date = '2050-01-01'
        self.last_original_room_id = 0
        self.last_travelline_rate_plan_code = 10
        self.last_dolphin_tour_code = 10000
        self.last_dolphin_pansion_code = 20000
        self.last_dolphin_room_code = 30000
        self.last_dolphin_room_cat_code = 40000
        self.last_bnovo_rate_plan_code = 50000

    def get_permalink(self) -> int:
        self.last_permalink += 1
        return self.last_permalink

    def get_original_id(self) -> str:
        self.last_original_id += 1
        return str(self.last_original_id)

    def get_permaroom_id(self) -> str:
        self.last_permaroom_id += 1
        return 'PermaroomId_' + str(self.last_permaroom_id)

    def get_permaroom_name(self) -> str:
        self.last_permaroom_name += 1
        name = random.choice(['Стандартный', 'Комфорт', 'Люкс'])
        name += ' с видом на '
        name += random.choice(['лес', 'двор', 'море'])
        name += ' #' + str(self.last_permaroom_name)
        return name

    def get_permaroom_description(self) -> str:
        return 'Просторный и уютный, кровать king-size, балкон с видом на Эверест. Шампанское в подарок.'

    def get_photo(self) -> TestContextPermaroom.Photo:
        sizes = [TestContextPermaroom.PhotoSize(tools.random_uint16(), tools.random_uint16(), x) for x in
                 ['S', 'L', 'XL', 'orig']]
        url = f"https://avatars.mds.yandex.net/get-altay/{tools.random_int()}/2a000001{tools.random_hex_string(28)}/%s"
        return TestContextPermaroom.Photo(sizes, url)

    def get_binary_feature(self) -> TestContextPermaroom.BinaryFeature:
        return TestContextPermaroom.BinaryFeature('FeatureId-' + tools.random_hex_string(8),
                                                  'Name-' + tools.random_hex_string(8),
                                                  tools.random_bool())

    def get_enum_feature(self) -> TestContextPermaroom.EnumFeature:
        return TestContextPermaroom.EnumFeature('FeatureId-' + tools.random_hex_string(8),
                                                'Name-' + tools.random_hex_string(8),
                                                'ValueId-' + tools.random_hex_string(8),
                                                'ValueName-' + tools.random_hex_string(8))

    def get_integer_feature(self) -> TestContextPermaroom.IntegerFeature:
        return TestContextPermaroom.IntegerFeature('FeatureId-' + tools.random_hex_string(8),
                                                  'Name-' + tools.random_hex_string(8),
                                                  tools.random_int())

    def get_float_feature(self) -> TestContextPermaroom.FloatFeature:
        return TestContextPermaroom.FloatFeature('FeatureId-' + tools.random_hex_string(8),
                                                  'Name-' + tools.random_hex_string(8),
                                                  tools.random_float())

    def get_string_feature(self) -> TestContextPermaroom.StringFeature:
        return TestContextPermaroom.StringFeature('FeatureId-' + tools.random_hex_string(8),
                                                  'Name-' + tools.random_hex_string(8),
                                                  tools.random_string())

    def get_date(self) -> str:
        self.last_date = tools.date_days_after(self.last_date, 15)
        return self.last_date

    def get_nights(self) -> int:
        return 1

    def get_occupancy(self) -> str:
        return '2'

    def get_price(self) -> int:
        return random.randint(1000, 10000)

    def get_original_room_id(self) -> str:
        self.last_original_room_id += 1
        return f'Room-{self.last_original_room_id}-{tools.random_string()}'

    def get_travelline_rate_plan_code(self) -> str:
        self.last_travelline_rate_plan_code += 1
        return f'{self.last_travelline_rate_plan_code}-{tools.random_string()}'

    def get_dolphin_tour_code(self) -> int:
        self.last_dolphin_tour_code += 1
        return self.last_dolphin_tour_code

    def get_dolphin_pansion_code(self) -> int:
        self.last_dolphin_pansion_code += 1
        return self.last_dolphin_pansion_code

    def get_bnovo_rate_plan_code(self) -> int:
        self.last_bnovo_rate_plan_code += 1
        return self.last_bnovo_rate_plan_code

    def get_dolphin_room_code(self) -> int:
        self.last_dolphin_room_code += 1
        return self.last_dolphin_room_code

    def get_dolphin_room_cat_code(self) -> int:
        self.last_dolphin_room_cat_code += 1
        return self.last_dolphin_room_cat_code


class TravellineRatePlan:
    def __init__(self, hotel_code: str, rate_plan_code: str, rate_plan_type: TravellineRatePlanType):
        self.hotel_code = hotel_code
        self.rate_plan_code = rate_plan_code
        self.rate_plan_type = rate_plan_type


class DolphinItem:
    def __init__(self, item_code: int, dolphin_item_type: DolphinItemType):
        self.item_code = item_code
        self.dolphin_item_type = dolphin_item_type


class BNovoRatePlan:
    def __init__(self, account_id: int, rate_plan_id: int, rate_plan_type: BNovoRatePlanType):
        self.account_id = account_id
        self.rate_plan_id = rate_plan_id
        self.rate_plan_type = rate_plan_type


class TestItemsRegistry:
    def __init__(self):
        self.identifiers_provider = IdentifiersProvider()
        self.test_contexts: Dict[str, TestContext] = dict()
        self.hotels: List[TestContextHotel] = []
        self.partner_hotels: List[TestContextPartnerHotel] = []
        self.permarooms: List[TestContextPermaroom] = []
        self.offers: List[TestContextOffer] = []
        self.whitelist: Dict[int, List[Tuple[hotels_pb2.EPartnerId, str]]] = defaultdict(list)
        self.blacklist: Dict[int, List[Tuple[hotels_pb2.EPartnerId, str]]] = defaultdict(list)
        self.travelline_rate_plans: List[TravellineRatePlan] = []
        self.dolphin_tours: List[DolphinItem] = []
        self.dolphin_pansions: List[DolphinItem] = []
        self.dolphin_rooms: List[DolphinItem] = []
        self.dolphin_room_cats: List[DolphinItem] = []
        self.bnovo_rate_plans: List[BNovoRatePlan] = []
        self.partner2operators = {  # todo: read from config in yt
            hotels_pb2.PI_BOOKING: [hotels_pb2.OI_BOOKING],
            hotels_pb2.PI_HOTELS101: [hotels_pb2.OI_HOTELS101],
            hotels_pb2.PI_TRAVELLINE: [hotels_pb2.OI_TRAVELLINE],
            hotels_pb2.PI_HOTELSCOMBINED: [hotels_pb2.OI_AGODA, hotels_pb2.OI_HOTELSCOM, hotels_pb2.OI_HOTELINFO,
                                           hotels_pb2.OI_HTC_ONSITE, hotels_pb2.OI_ACCORHOTELSCOM, hotels_pb2.OI_AMOMA,
                                           hotels_pb2.OI_PRESTIGIACOM, hotels_pb2.OI_OTELCOM],
            hotels_pb2.PI_OSTROVOK: [hotels_pb2.OI_OSTROVOK],
            hotels_pb2.PI_EXPEDIA: [hotels_pb2.OI_EXPEDIA],
            hotels_pb2.PI_DOLPHIN: [hotels_pb2.OI_DOLPHIN],
            hotels_pb2.PI_BNOVO: [hotels_pb2.OI_BNOVO],
        }
        self.fakeOfferCache = TestContextOfferCache(True, None)
        self.realOfferCache = None
        self.oc_app: OfferCacheApp = None

    def create_hotel(self) -> TestContextHotel:
        self.hotels.append(TestContextHotel(self.identifiers_provider.get_permalink(), self))
        return self.hotels[-1]

    def create_partner_hotel(self, partner_id: hotels_pb2.EPartnerId, original_id: str) -> TestContextPartnerHotel:
        partner_id = partner_id or hotels_pb2.PI_BOOKING
        original_id = original_id or self.identifiers_provider.get_original_id()
        self.partner_hotels.append(TestContextPartnerHotel(partner_id, original_id, self))
        return self.partner_hotels[-1]

    def create_permaroom(self, permalink: int, name: str = None, description: str = None,
                         photos: List[TestContextPermaroom.Photo] = None,
                         binary_features: List[TestContextPermaroom.BinaryFeature] = None,
                         enum_features: List[TestContextPermaroom.EnumFeature] = None,
                         integer_features: List[TestContextPermaroom.IntegerFeature] = None,
                         float_features: List[TestContextPermaroom.FloatFeature] = None,
                         string_features: List[TestContextPermaroom.StringFeature] = None) -> TestContextPermaroom:
        self.permarooms.append(TestContextPermaroom(
            self.identifiers_provider.get_permaroom_id(),
            permalink,
            name or self.identifiers_provider.get_permaroom_name(),
            description or self.identifiers_provider.get_permaroom_description(),
            photos or tools.random_seq(self.identifiers_provider.get_photo),
            binary_features or tools.random_seq(self.identifiers_provider.get_binary_feature),
            enum_features or tools.random_seq(self.identifiers_provider.get_enum_feature),
            integer_features or tools.random_seq(self.identifiers_provider.get_integer_feature),
            float_features or tools.random_seq(self.identifiers_provider.get_float_feature),
            string_features or tools.random_seq(self.identifiers_provider.get_string_feature)
        ))
        return self.permarooms[-1]

    def create_offer(self,
                     operator_id: hotels_pb2.EOperatorId,
                     original_id: str,
                     date: str,
                     nights: int,
                     occupancy: str,
                     price: int,
                     pansion: hotels_pb2.EPansionType,
                     original_room_id: str,
                     travelline_rate_plan: TravellineRatePlan,
                     dolphin_tour: DolphinItem,
                     dolphin_pansion: DolphinItem,
                     dolphin_room: DolphinItem,
                     dolphin_room_cat: DolphinItem,
                     bnovo_rate_plan: BNovoRatePlan,
                     refund_rules: List[RefundRule]) -> TestContextOffer:
        assert operator_id is not None
        self.offers.append(TestContextOffer(
            operator_id=operator_id,
            original_id=original_id,
            date=date or self.identifiers_provider.get_date(),
            nights=nights or self.identifiers_provider.get_nights(),
            occupancy=occupancy or self.identifiers_provider.get_occupancy(),
            pansion=pansion,
            price=price or self.identifiers_provider.get_price(),
            original_room_id=original_room_id or self.identifiers_provider.get_original_room_id(),
            travelline_rate_plan=travelline_rate_plan,
            dolphin_tour=dolphin_tour,
            dolphin_pansion=dolphin_pansion,
            dolphin_room=dolphin_room,
            dolphin_room_cat=dolphin_room_cat,
            bnovo_rate_plan=bnovo_rate_plan,
            refund_rules=refund_rules
        ))
        return self.offers[-1]

    def create_travelline_rate_plan(self, original_id: str, rate_plan_type: TravellineRatePlanType) -> TravellineRatePlan:
        self.travelline_rate_plans.append(TravellineRatePlan(original_id, self.identifiers_provider.get_travelline_rate_plan_code(), rate_plan_type))
        return self.travelline_rate_plans[-1]

    def create_dolphin_tour(self, dolphin_item_type: DolphinItemType):
        tour_code = self.identifiers_provider.get_dolphin_tour_code()
        self.dolphin_tours.append(DolphinItem(tour_code, dolphin_item_type))
        return self.dolphin_tours[-1]

    def create_dolphin_pansion(self, dolphin_item_type: DolphinItemType):
        pansion_code = self.identifiers_provider.get_dolphin_pansion_code()
        self.dolphin_pansions.append(DolphinItem(pansion_code, dolphin_item_type))
        return self.dolphin_pansions[-1]

    def create_dolphin_room(self, dolphin_item_type: DolphinItemType):
        room_code = self.identifiers_provider.get_dolphin_room_code()
        self.dolphin_rooms.append(DolphinItem(room_code, dolphin_item_type))
        return self.dolphin_rooms[-1]

    def create_dolphin_room_cat(self, dolphin_item_type: DolphinItemType):
        room_cat_code = self.identifiers_provider.get_dolphin_room_cat_code()
        self.dolphin_room_cats.append(DolphinItem(room_cat_code, dolphin_item_type))
        return self.dolphin_room_cats[-1]

    def create_bnovo_rate_plan(self, original_id: str, rate_plan_type: BNovoRatePlanType) -> BNovoRatePlan:
        try:
            int_original_id = int(original_id)
        except ValueError:
            raise Exception('original_id should be int for bnovo')
        self.bnovo_rate_plans.append(BNovoRatePlan(int_original_id, self.identifiers_provider.get_bnovo_rate_plan_code(), rate_plan_type))
        return self.bnovo_rate_plans[-1]

    def create_search_params(self) -> Tuple[str, str, int, str]:
        return self.identifiers_provider.get_original_id(), self.identifiers_provider.get_date(), self.identifiers_provider.get_nights(), self.identifiers_provider.get_occupancy()

    def add_partner_hotel_to_whitelist(self, permalink, partner_hotel: TestContextPartnerHotel):
        self.whitelist[permalink].append((partner_hotel.partner_id, partner_hotel.original_id))

    def add_partner_hotel_to_blacklist(self, permalink, partner_hotel: TestContextPartnerHotel):
        self.blacklist[permalink].append((partner_hotel.partner_id, partner_hotel.original_id))

    def get_oc(self, register_phase: bool) -> TestContextOfferCache:
        if register_phase:
            return self.fakeOfferCache
        else:
            assert self.realOfferCache is not None
            return self.realOfferCache

    def get_test_context(self, name: str, register_phase: bool) -> TestContext:
        if name not in self.test_contexts:
            self.test_contexts[name] = TestContext(self)

        assert self.test_contexts[name]._register_phase == register_phase
        return self.test_contexts[name]

    def _set_oc(self, oc_app: OfferCacheApp) -> None:
        self.oc_app = oc_app
        self.realOfferCache = TestContextOfferCache(False, oc_app)

    def _generate_messages(self, hotel: TestContextHotel) -> List[bus_messages_pb2.TSearcherMessage]:
        grouped_offers = defaultdict(list)
        for partner_hotel in hotel.partner_hotels:
            if len(partner_hotel.empty_offers_params) > 0:
                for params in partner_hotel.empty_offers_params:
                    key = (partner_hotel.partner_id, partner_hotel.original_id, params.date, params.nights, params.occupancy)
                    grouped_offers[key] = []
            else:
                for offer in partner_hotel.offers:
                    key = (partner_hotel.partner_id, partner_hotel.original_id, offer.date, offer.nights, offer.occupancy)
                    grouped_offers[key].append(offer)

        messages = []
        for i, (key, group_offers) in enumerate(grouped_offers.items()):
            (partner_id, original_id, date, nights, occupancy) = key
            checkin = date
            checkout = tools.date_days_after(date, nights)
            req = hotels_pb2.TSearchOffersReq(
                HotelId=hotels_pb2.THotelId(PartnerId=partner_id, OriginalId=original_id),
                CheckInDate=checkin,
                CheckOutDate=checkout,
                Occupancy=occupancy,
                Currency=commons_pb2.C_RUB,
                RequestClass=hotels_pb2.RC_INTERACTIVE,
                Id=f'Req.TestContext.{hotel.permalink}.{i}'
            )
            resp = hotels_pb2.TSearchOffersRsp(Offers=hotels_pb2.TOfferList())
            for j, offer in enumerate(group_offers):
                price = hotels_pb2.TPriceWithDetails(Amount=offer.price, Currency=commons_pb2.C_RUB)
                partner_specific_data = None
                if partner_id == hotels_pb2.PI_TRAVELLINE:
                    partner_specific_data = hotels_pb2.TPartnerSpecificOfferData(
                        TravellineData=hotels_pb2.TPartnerSpecificOfferData.TTravellineData(
                            HotelCode=offer.travelline_rate_plan.hotel_code,
                            RatePlanCode=offer.travelline_rate_plan.rate_plan_code
                        )
                    )
                if partner_id == hotels_pb2.PI_DOLPHIN:
                    partner_specific_data = hotels_pb2.TPartnerSpecificOfferData(
                        DolphinData=hotels_pb2.TPartnerSpecificOfferData.TDolphinData(
                            Tour=offer.dolphin_tour.item_code,
                            Pansion=offer.dolphin_pansion.item_code,
                            Room=offer.dolphin_room.item_code,
                            RoomCat=offer.dolphin_room_cat.item_code
                        )
                    )
                if partner_id == hotels_pb2.PI_BNOVO:
                    partner_specific_data = hotels_pb2.TPartnerSpecificOfferData(
                        BNovoData=hotels_pb2.TPartnerSpecificOfferData.TBNovoData(
                            AccountId=offer.bnovo_rate_plan.account_id,
                            RatePlanId=offer.bnovo_rate_plan.rate_plan_id,
                        )
                    )
                pbOffer = resp.Offers.Offer.add(
                    Id=str(uuid.uuid4()),
                    DisplayedTitle=wrappers_pb2.StringValue(value='Offer-' + tools.random_string()),
                    OperatorId=offer.operator_id,
                    OriginalRoomId=offer.original_room_id,
                    Price=price,
                    Capacity='==%s' % occupancy,
                    Pansion=offer.pansion,
                    FreeCancellation=wrappers_pb2.BoolValue(value=True),
                    PartnerSpecificData=partner_specific_data
                )
                if offer.refund_rules is not None:
                    for rule in offer.refund_rules:
                        pbRefundRule = pbOffer.RefundRule.add(
                            Type=rule.type,
                        )
                        if rule.starts_at_sec is not None:
                            starts_at = timestamp_pb2.Timestamp()
                            starts_at.FromSeconds(rule.starts_at_sec)
                            pbRefundRule.StartsAt.CopyFrom(starts_at)
                        if rule.ends_at_sec is not None:
                            ends_at = timestamp_pb2.Timestamp()
                            ends_at.FromSeconds(rule.ends_at_sec)
                            pbRefundRule.EndsAt.CopyFrom(ends_at)
                        if rule.penalty is not None:
                            pbRefundRule.Penalty.CopyFrom(commons_pb2.TPrice(Amount=rule.penalty, Currency=commons_pb2.C_RUB))

            messages.append(bus_messages_pb2.TSearcherMessage(Request=req, Response=resp))

        return messages

    def write_initial_messages(self, message_bus: MessageBus):
        messages = sum([self._generate_messages(hotel) for hotel in self.hotels], [])
        LOG.info(f"Writing messages from TestContext: {messages}")
        message_bus.write('ru.yandex.travel.hotels.TSearcherMessage', messages)

    def build_whitelist(self):
        return self._build_blackwhitelist(self.whitelist)

    def build_blacklist(self):
        return self._build_blackwhitelist(self.blacklist)

    def build_permarooms(self):
        items = []
        def get_commons(feature):
            return {
                'Id': feature.id,
                'Name': feature.name,
            }

        for permaroom in self.permarooms:
            items.append({'Permaroom': json.dumps({
                'PermaroomId': permaroom.permaroom_id,
                'Permalink': permaroom.permalink,
                'Mappings': [{
                    'OperatorId': mapping.operator_id,
                    'OriginalId': mapping.original_id,
                    'MappingKey': mapping.mapping_key,
                } for mapping in permaroom.mappings],
                'Name': permaroom.name,
                'Description': permaroom.description,
                'Photos': [{
                    'Sizes': [{
                        'Height': size.height,
                        'Width': size.width,
                        'Size': size.size
                    } for size in photo.sizes],
                    'UrlTemplate': photo.url_template,
                } for photo in permaroom.photos],
                'BinaryFeatures': [{
                    'Commons': get_commons(feature),
                    'Value': feature.value,
                } for feature in permaroom.binary_features],
                'EnumFeatures': [{
                    'Commons': get_commons(feature),
                    'ValueId': feature.value_id,
                    'ValueName': feature.value_name,
                } for feature in permaroom.enum_features],
                'IntegerFeatures': [{
                    'Commons': get_commons(feature),
                    'Value': feature.value,
                } for feature in permaroom.integer_features],
                'FloatFeatures': [{
                    'Commons': get_commons(feature),
                    'Value': feature.value,
                } for feature in permaroom.float_features],
                'StringFeatures': [{
                    'Commons': get_commons(feature),
                    'Value': feature.value,
                } for feature in permaroom.string_features],
            })})
        return items

    def _build_blackwhitelist(self, data):
        self.ensure_oc_app_is_set()
        items = []
        for permalink, hotel_ids in data.items():
            item = {'permalink': permalink}
            for partner in self.oc_app.partners.values():
                item[partner["Code"]] = []
            for partner_id, original_id in hotel_ids:
                item[self.oc_app.partners[partner_id]["Code"]].append(original_id)
            items.append(item)
        return items

    def build_travelline_rate_plans(self):
        self.ensure_oc_app_is_set()
        items = []
        for rate_plan in self.travelline_rate_plans:
            if rate_plan.rate_plan_type == TravellineRatePlanType.UNKNOWN:
                continue
            items.append({
                'HotelCode': rate_plan.hotel_code,
                'RatePlanCode': rate_plan.rate_plan_code,
                'Enabled': rate_plan.rate_plan_type == TravellineRatePlanType.ALLOWED
            })
        return items

    def build_dolphin_tours(self):
        return self._build_dolphine_items(self.dolphin_tours)

    def build_dolphin_pansions(self):
        return self._build_dolphine_items(self.dolphin_pansions)

    def build_dolphin_rooms(self):
        return self._build_dolphine_items(self.dolphin_rooms)

    def build_dolphin_room_cats(self):
        return self._build_dolphine_items(self.dolphin_room_cats)

    def _build_dolphine_items(self, dolphin_items: List[DolphinItem]):
        self.ensure_oc_app_is_set()
        items = []
        for dolphin_item in dolphin_items:
            if dolphin_item.dolphin_item_type == DolphinItemType.UNKNOWN:
                continue
            items.append({
                'Key': dolphin_item.item_code,
                'Enabled': dolphin_item.dolphin_item_type == DolphinItemType.ALLOWED
            })
        return items

    def build_bnovo_rate_plans(self):
        self.ensure_oc_app_is_set()
        items = []
        for rate_plan in self.bnovo_rate_plans:
            if rate_plan.rate_plan_type == BNovoRatePlanType.UNKNOWN:
                continue
            items.append({
                'AccountId': rate_plan.account_id,
                'RatePlanId': rate_plan.rate_plan_id,
                'Enabled': rate_plan.rate_plan_type == BNovoRatePlanType.ALLOWED
            })
        return items

    def ensure_oc_app_is_set(self):
        assert self.oc_app is not None


class TestContextOffer:
    def __init__(self,
                 operator_id: hotels_pb2.EOperatorId,
                 original_id: str,
                 date: str,
                 nights: int,
                 occupancy: str,
                 pansion: hotels_pb2.EPansionType,
                 price: int,
                 original_room_id: str,
                 travelline_rate_plan: TravellineRatePlan,
                 dolphin_tour: DolphinItem,
                 dolphin_pansion: DolphinItem,
                 dolphin_room: DolphinItem,
                 dolphin_room_cat: DolphinItem,
                 bnovo_rate_plan: BNovoRatePlan,
                 refund_rules: List[RefundRule]):
        self.operator_id = operator_id
        self.original_id = original_id
        self.date = date
        self.nights = nights
        self.occupancy = occupancy
        self.pansion = pansion
        self.price = price
        self.original_room_id = original_room_id
        self.travelline_rate_plan = travelline_rate_plan
        self.dolphin_tour = dolphin_tour
        self.dolphin_pansion = dolphin_pansion
        self.dolphin_room = dolphin_room
        self.dolphin_room_cat = dolphin_room_cat
        self.bnovo_rate_plan = bnovo_rate_plan
        self.refund_rules = refund_rules

    def _finish_registration(self) -> None:
        pass


class TestContextPartnerHotel:
    class EmptyOfferParams:
        def __init__(self, date, nights, occupancy):
            self.date = date
            self.nights = nights
            self.occupancy = occupancy

    def __init__(self, partner_id: hotels_pb2.EPartnerId, original_id: str, test_items_registry: TestItemsRegistry) -> None:
        self._register_phase = True
        self.test_items_registry = test_items_registry
        self.partner_id = partner_id
        self.original_id = original_id
        self.offers: List[TestContextOffer] = []
        self.offers_pos = -1
        self.empty_offers_params: List[TestContextPartnerHotel.EmptyOfferParams] = []

    def add_offer(self,
                  date: str = None,
                  nights: int = None,
                  occupancy: str = None,
                  pansion: hotels_pb2.EPansionType = hotels_pb2.PT_RO,
                  price: int = None,
                  operator_id: hotels_pb2.EOperatorId = None,
                  original_room_id: str = None,
                  travelline_rate_plan_type: TravellineRatePlanType = None,
                  dolphin_tour_type: DolphinItemType = None,
                  dolphin_pansion_type: DolphinItemType = None,
                  dolphin_room_type: DolphinItemType = None,
                  dolphin_room_cat_type: DolphinItemType = None,
                  bnovo_rate_plan_type: BNovoRatePlanType = None,
                  refund_rules: List[RefundRule] = None) -> TestContextOffer:

        if self._register_phase:
            if operator_id is not None:
                assert operator_id in self.test_items_registry.partner2operators[self.partner_id]
            operator_id = operator_id or self.test_items_registry.partner2operators[self.partner_id][0]

            travelline_rate_plan = None
            if operator_id == hotels_pb2.OI_TRAVELLINE:
                travelline_rate_plan_type = travelline_rate_plan_type or TravellineRatePlanType.UNKNOWN
            if travelline_rate_plan_type is not None:
                assert operator_id == hotels_pb2.OI_TRAVELLINE
                travelline_rate_plan = self.test_items_registry.create_travelline_rate_plan(self.original_id, travelline_rate_plan_type)

            if operator_id == hotels_pb2.OI_DOLPHIN:
                dolphin_tour_type = dolphin_tour_type or DolphinItemType.UNKNOWN
                dolphin_pansion_type = dolphin_pansion_type or DolphinItemType.UNKNOWN
                dolphin_room_type = dolphin_room_type or DolphinItemType.UNKNOWN
                dolphin_room_cat_type = dolphin_room_cat_type or DolphinItemType.UNKNOWN

            dolphin_tour, dolphin_pansion, dolphin_room, dolphin_room_cat = None, None, None, None
            if dolphin_tour_type is not None:
                assert operator_id == hotels_pb2.OI_DOLPHIN
                dolphin_tour = self.test_items_registry.create_dolphin_tour(dolphin_tour_type)
            if dolphin_pansion_type is not None:
                assert operator_id == hotels_pb2.OI_DOLPHIN
                dolphin_pansion = self.test_items_registry.create_dolphin_pansion(dolphin_pansion_type)
            if dolphin_room_type is not None:
                assert operator_id == hotels_pb2.OI_DOLPHIN
                dolphin_room = self.test_items_registry.create_dolphin_room(dolphin_room_type)
            if dolphin_room_cat_type is not None:
                assert operator_id == hotels_pb2.OI_DOLPHIN
                dolphin_room_cat = self.test_items_registry.create_dolphin_room_cat(dolphin_room_cat_type)

            bnovo_rate_plan = None
            if operator_id == hotels_pb2.OI_BNOVO:
                bnovo_rate_plan_type = bnovo_rate_plan_type or BNovoRatePlanType.UNKNOWN
            if bnovo_rate_plan_type is not None:
                assert operator_id == hotels_pb2.OI_BNOVO
                bnovo_rate_plan = self.test_items_registry.create_bnovo_rate_plan(self.original_id, bnovo_rate_plan_type)

            self.offers.append(self.test_items_registry.create_offer(operator_id, self.original_id, date, nights,
                                                                     occupancy, price, pansion, original_room_id,
                                                                     travelline_rate_plan, dolphin_tour,
                                                                     dolphin_pansion, dolphin_room, dolphin_room_cat,
                                                                     bnovo_rate_plan, refund_rules))
        self.offers_pos += 1
        return self.offers[self.offers_pos]

    def add_no_offers(self, date: str=None, nights: int=None, occupancy: str=None):
        if self._register_phase:
            self.empty_offers_params.append(TestContextPartnerHotel.EmptyOfferParams(
                date or self.test_items_registry.identifiers_provider.get_date(),
                nights or self.test_items_registry.identifiers_provider.get_nights(),
                occupancy or self.test_items_registry.identifiers_provider.get_occupancy()
            ))

    def _finish_registration(self) -> None:
        assert self._register_phase
        self._register_phase = False
        self.offers_pos = -1
        for offer in self.offers:
            offer._finish_registration()


class TestContextHotel:
    def __init__(self, permalink: int, test_items_registry: TestItemsRegistry) -> None:
        self._register_phase = True
        self.test_items_registry = test_items_registry
        self.permalink: int = permalink
        self.partner_hotels: List[TestContextPartnerHotel] = []
        self.partner_hotels_pos = -1
        self.permarooms: List[TestContextPermaroom] = []
        self.permarooms_pos = -1

    def add_partner_hotel(self, partner_id: hotels_pb2.EPartnerId = None, original_id: str = None, whitelisted: bool = False, blacklisted: bool = False) -> TestContextPartnerHotel:
        if self._register_phase:
            partner_id = partner_id or hotels_pb2.PI_BOOKING
            assert partner_id not in [x.partner_id for x in self.partner_hotels]
            self.partner_hotels.append(self.test_items_registry.create_partner_hotel(partner_id, original_id))
            if whitelisted:
                self.test_items_registry.add_partner_hotel_to_whitelist(self.permalink, self.partner_hotels[-1])
            if blacklisted:
                self.test_items_registry.add_partner_hotel_to_blacklist(self.permalink, self.partner_hotels[-1])
        self.partner_hotels_pos += 1
        return self.partner_hotels[self.partner_hotels_pos]

    def add_permaroom(self, name: str = None, description: str = None,
                      photos: List[TestContextPermaroom.Photo] = None,
                      binary_features: List[TestContextPermaroom.BinaryFeature] = None,
                      enum_features: List[TestContextPermaroom.EnumFeature] = None,
                      integer_features: List[TestContextPermaroom.IntegerFeature] = None,
                      float_features: List[TestContextPermaroom.FloatFeature] = None,
                      string_features: List[TestContextPermaroom.StringFeature] = None) -> TestContextPermaroom:
        if self._register_phase:
            self.permarooms.append(self.test_items_registry.create_permaroom(self.permalink, name, description, photos, binary_features,
                                                      enum_features, integer_features, float_features, string_features))
        self.permarooms_pos += 1
        return self.permarooms[self.permarooms_pos]

    def _get_default_date(self) -> str:
        return self._get_default_search_param('date')

    def _get_default_nights(self) -> int:
        return self._get_default_search_param('nights')

    def _get_default_occupancy(self) -> str:
        return self._get_default_search_param('occupancy')

    def _get_default_search_param(self, name: str):
        assert not self._register_phase
        all_offers = sum([x.offers for x in self.partner_hotels], [])
        assert len(all_offers) > 0
        assert all([getattr(offer, name) == getattr(all_offers[0], name) for offer in all_offers])
        return getattr(all_offers[0], name)

    def _get_s_hotel_id(self, oc_app: OfferCacheApp) -> str:
        assert not self._register_phase
        hotel_ids = list({(partner_hotel.partner_id, partner_hotel.original_id) for partner_hotel in self.partner_hotels})
        return '~'.join([str(self.permalink)] + [f'{oc_app.partners[x[0]]["Code"]}.{x[1]}' for x in hotel_ids])

    def _finish_registration(self) -> None:
        assert self._register_phase
        self._register_phase = False
        self.partner_hotels_pos = -1
        for partner_hotel in self.partner_hotels:
            partner_hotel._finish_registration()
        self.permarooms_pos = -1
        for permaroom in self.permarooms:
            permaroom._finish_registration()


class TestContextPermaroom:
    class Mapping:
        def __init__(self, operator_id: hotels_pb2.EOperatorId, original_id: str, mapping_key: str):
            self.operator_id: hotels_pb2.EOperatorId = operator_id
            self.original_id: str = original_id
            self.mapping_key: str = mapping_key

    class PhotoSize:
        def __init__(self, width: int, height: int, size: str):
            self.width: int = width
            self.height: int = height
            self.size: str = size

    class Photo:
        def __init__(self, sizes: List[TestContextPermaroom.PhotoSize], url_template: str):
            self.sizes: List[TestContextPermaroom.PhotoSize] = sizes
            self.url_template: str = url_template

    class BinaryFeature:
        def __init__(self, id: str, name: str, value: bool):
            self.id: str = id
            self.name: str = name
            self.value: bool = value

    class EnumFeature:
        def __init__(self, id: str, name: str, value_id: str, value_name: str):
            self.id: str = id
            self.name: str = name
            self.value_id: str = value_id
            self.value_name: str = value_name

    class IntegerFeature:
        def __init__(self, id: str, name: str, value: str):
            self.id: str = id
            self.name: str = name
            self.value: str = value

    class FloatFeature:
        def __init__(self, id: str, name: str, value: float):
            self.id: str = id
            self.name: str = name
            self.value: float = value

    class StringFeature:
        def __init__(self, id: str, name: str, value: str):
            self.id: str = id
            self.name: str = name
            self.value: str = value

    def __init__(self, permaroom_id: str, permalink: int, name: str, description: str,
                 photos: List[TestContextPermaroom.Photo],
                 binary_features: List[TestContextPermaroom.BinaryFeature],
                 enum_features: List[TestContextPermaroom.EnumFeature],
                 integer_features: List[TestContextPermaroom.IntegerFeature],
                 float_features: List[TestContextPermaroom.FloatFeature],
                 string_features: List[TestContextPermaroom.StringFeature]) -> None:
        self._register_phase = True
        self.permaroom_id: str = permaroom_id
        self.permalink: int = permalink
        self.mappings: List[TestContextPermaroom.Mapping] = []
        self.name: str = name
        self.description: str = description
        self.photos: List[TestContextPermaroom.Photo] = photos
        self.binary_features: List[TestContextPermaroom.BinaryFeature] = binary_features
        self.enum_features: List[TestContextPermaroom.EnumFeature] = enum_features
        self.integer_features: List[TestContextPermaroom.IntegerFeature] = integer_features
        self.float_features: List[TestContextPermaroom.FloatFeature] = float_features
        self.string_features: List[TestContextPermaroom.StringFeature] = string_features

    def add_mapping(self, offer: TestContextOffer) -> None:
        if self._register_phase:
            self.mappings.append(TestContextPermaroom.Mapping(offer.operator_id, offer.original_id, offer.original_room_id))

    def add_raw_mapping(self, operator_id: hotels_pb2.EOperatorId, original_id: str, mapping_key: str) -> None:
        if self._register_phase:
            self.mappings.append(TestContextPermaroom.Mapping(operator_id, original_id, mapping_key))

    def _finish_registration(self) -> None:
        assert self._register_phase
        self._register_phase = False


class TestContextOfferCacheResponse:
    def __init__(self, register_phase: bool, oc_app: OfferCacheApp, req: Dict, resp: Dict):
        self._register_phase = register_phase
        self.oc_app = oc_app
        self.req = req
        self.resp = resp

    def expect_result(self, result: List[Tuple[TestContextHotel, List[TestContextOffer]]], catroom_status=None, expected_refund_types=None, expected_refund_rules=None):
        if self._register_phase:
            pass
        else:
            expected = dict()
            for hotel, hotel_offers in result:
                hotel_offers = sorted(hotel_offers, key=lambda offer: offer.price)
                # todo more fields
                hotel_info = {
                    'WasFound': len(hotel_offers) > 0,
                    'PriceFields': {
                        'OperatorId': [offer.operator_id for offer in hotel_offers],
                        'OrigHotelId': [offer.original_id for offer in hotel_offers],
                        'Pansion': [hotels_pb2.EPansionType.Name(offer.pansion)[3:] for offer in hotel_offers],  # cropping PT_ prefix
                        'Price': [offer.price for offer in hotel_offers],
                    },
                    'OtherFields': {
                        'IsFinished': True,
                    }
                }
                if expected_refund_types is not None:
                    hotel_info['PriceFields']['RefundType'] = expected_refund_types
                if expected_refund_rules is not None:
                    hotel_info['PriceFields']['RefundRule'] = expected_refund_rules
                if catroom_status is not None:
                    hotel_info['OtherFields']['CatRoomStatus'] = catroom_status
                expected[hotel._get_s_hotel_id(self.oc_app)] = hotel_info

            checkers.check_hotels(self.oc_app, self.req, self.resp, expected)

    def expect_result_with_permarooms(self, result: List[Tuple[TestContextHotel, List[Tuple[TestContextPermaroom, List[TestContextOffer]]]]],
                                      strict_permaroom_order=False, was_found=True):
        if self._register_phase:
            pass
        else:
            expected = dict()
            for hotel, hotel_offers_by_permaroom in result:
                class OfferWithPermaroom:
                    def __init__(self, offer, permaroom):
                        self.offer = offer
                        self.permaroom = permaroom

                offers_with_permarooms = [OfferWithPermaroom(offer, permaroom)
                          for permaroom, offers in hotel_offers_by_permaroom
                          for offer in offers]
                offers_with_permarooms = sorted(offers_with_permarooms, key=lambda offer_with_permaroom: offer_with_permaroom.offer.price)
                if strict_permaroom_order:
                    permarooms = [permaroom for permaroom, _ in hotel_offers_by_permaroom]
                else:
                    permarooms = [permaroom for permaroom, _ in sorted(hotel_offers_by_permaroom, key=lambda pair: min([y.price for y in pair[1]]))]

                hotel_info = {
                    'WasFound': was_found,
                    'PriceFields': {
                        'OperatorId': [offer.offer.operator_id for offer in offers_with_permarooms],
                        'OrigHotelId': [offer.offer.original_id for offer in offers_with_permarooms],
                        'Pansion': [hotels_pb2.EPansionType.Name(offer.offer.pansion)[3:] for offer in offers_with_permarooms],  # cropping PT_ prefix
                        'Price': [offer.offer.price for offer in offers_with_permarooms],
                        'RawPermaroomId': [offer.permaroom.permaroom_id for offer in offers_with_permarooms],
                        'PermaroomId': [offer.permaroom.permaroom_id for offer in offers_with_permarooms],
                    },
                    'OtherFields': {
                        'IsFinished': True,
                        'ShowPermarooms': True,
                        'CatRoomStatus': 'CRS_OK',
                        'Permarooms': [{
                            'Id': permaroom.permaroom_id,
                            'Name': permaroom.name,
                            'Description': permaroom.description,
                            'Photos': [{
                                'Sizes': [{
                                    'Height': size.height,
                                    'Width': size.width,
                                    'Size': size.size
                                } for size in sorted(photo.sizes, key=lambda size: size.size)],
                                'UrlTemplate': photo.url_template,
                            } for photo in permaroom.photos],
                            'BinaryFeatures': [{
                                'Id': feature.id,
                                'Name': feature.name,
                                'Value': feature.value,
                            } for feature in permaroom.binary_features],
                            'EnumFeatures': [{
                                'Id': feature.id,
                                'Name': feature.name,
                                'ValueId': feature.value_id,
                                'ValueName': feature.value_name,
                            } for feature in permaroom.enum_features],
                            'IntegerFeatures': [{
                                'Id': feature.id,
                                'Name': feature.name,
                                'Value': feature.value,
                            } for feature in permaroom.integer_features],
                            'FloatFeatures': [{
                                'Id': feature.id,
                                'Name': feature.name,
                                'Value': feature.value,
                            } for feature in permaroom.float_features],
                            'StringFeatures': [{
                                'Id': feature.id,
                                'Name': feature.name,
                                'Value': feature.value,
                            } for feature in permaroom.string_features],
                        } for permaroom in permarooms]
                    },
                }
                expected[hotel._get_s_hotel_id(self.oc_app)] = hotel_info

            checkers.check_hotels(self.oc_app, self.req, self.resp, expected)


class TestContextOfferCache:
    def __init__(self, register_phase: bool, oc_app: OfferCacheApp):
        self._register_phase = register_phase
        self.oc_app = oc_app

    def req_by_hotel(self, hotel: TestContextHotel, date: str = None, nights: int = None, occupancy: str = None,
                     override_expected_date: str = None, expected_n_operators=None, **kwargs) -> TestContextOfferCacheResponse:
        if self._register_phase:
            return TestContextOfferCacheResponse(True, None, None, None)
        else:
            s_hotel_id = hotel._get_s_hotel_id(self.oc_app)
            if date is None:
                date = hotel._get_default_date()
            if nights is None:
                nights = hotel._get_default_nights()
            if occupancy is None:
                occupancy = hotel._get_default_occupancy()
            ages = tools.occupancy_to_ages(occupancy)
            req = tools.prepare_request(SHotelId=[s_hotel_id], Date=date, Nights=nights, Ages=ages,
                                        Full=1, UseSearcher=1, RequestId=0, **kwargs)
            resp = self.oc_app.read(req)
            self.oc_app.wait_flush()
            n_operators = expected_n_operators
            if n_operators is None:
                n_operators = len({offer.operator_id for partner_hotel in hotel.partner_hotels for offer in partner_hotel.offers})
            if override_expected_date is None:
                override_expected_date = date
            checkers.check_general(self.oc_app, req, resp, override_expected_date, nights, ages, is_finished=True, progress=(n_operators, n_operators))
            return TestContextOfferCacheResponse(False, self.oc_app, req, resp)


class TestContext:
    __test__ = False

    def __init__(self, test_items_registry: TestItemsRegistry):
        self._register_phase = True
        self._test_items_registry = test_items_registry
        self._hotels: List[TestContextHotel] = []
        self._hotels_pos = -1
        self._permarooms: List[TestContextPermaroom] = []
        self._permarooms_pos = -1
        self._search_params: List[Tuple[str, str, int, str]] = []
        self._search_params_pos = -1

    def log(self, msg):
        if self._register_phase:
            pass
        else:
            LOG.info(msg)

    def create_hotel(self) -> TestContextHotel:
        if self._register_phase:
            self._hotels.append(self._test_items_registry.create_hotel())
        self._hotels_pos += 1
        return self._hotels[self._hotels_pos]

    def create_permaroom(self) -> TestContextPermaroom:
        if self._register_phase:
            self._permarooms.append(self._test_items_registry.create_permaroom())
        self._permarooms_pos += 1
        return self._permarooms[self._permarooms_pos]

    def create_search_params(self) -> Tuple[str, str, int, str]:
        if self._register_phase:
            self._search_params.append(self._test_items_registry.create_search_params())
        self._search_params_pos += 1
        return self._search_params[self._search_params_pos]

    def get_oc(self) -> TestContextOfferCache:
        return self._test_items_registry.get_oc(self._register_phase)

    def _finish_registration(self):
        assert self._register_phase
        self._register_phase = False
        self._hotels_pos = -1
        self._search_params_pos = -1
        for hotel in self._hotels:
            hotel._finish_registration()

    def _finish_run(self):
        pass
