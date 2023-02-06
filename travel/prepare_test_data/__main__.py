# -*- coding: utf-8 -*-

import logging
import random
from argparse import ArgumentParser, Namespace
from datetime import datetime, timezone
from enum import Enum
from uuid import uuid4

from travel.hotels.lib.python3.yt.ytlib import recreate_table, schema_from_dict, ypath_join
from yt.wrapper import YtClient


class AffiliatePartner(Enum):
    UNKNOWN = 'unknown'
    ADMITAD = 'admitad'
    TRAVELPAYOUTS = 'travelpayouts'
    WHITE_LABEL = 'white_label'


class WhiteLabelPartnerId(Enum):
    S7 = 's7'


class WhiteLabelPartner:
    def __init__(self, partner_id, points_type):
        self.partner_id = partner_id
        self.points_type = points_type


class App:
    __order_count__ = 100_000
    __max_order_value__ = 100_000
    __max_bonus_points__ = 10_000

    __schema_common_dict__ = {
        'category': 'string',
        'partner_name': 'string',
        'partner_order_id': 'string',
        'status': 'string',
        'currency_code': 'string',
        'order_amount': 'double',
        'created_at': 'uint64',
        'updated_at': 'uint64',
        'label_admitad_uid': 'string',
        'label_travelpayouts_uid': 'string',
    }

    __schema_hotels__ = schema_from_dict({**__schema_common_dict__, **{
        'label_clid': 'string',
        'label_affiliate_clid': 'string',
        'check_in': 'string',
        'check_out': 'string',
        'hotel_name': 'string',
        'hotel_country': 'string',
        'hotel_city': 'string',
        'label_referral_partner_id': 'string',
        'label_referral_partner_request_id': 'string',
        'white_label_points_type': 'string',
        'white_label_points_amount': 'int64',
        'white_label_customer_number': 'string'
    }})

    __schema_train__ = schema_from_dict({**__schema_common_dict__, **{
        'label_clid': 'string',
        'label_affiliate_clid': 'string',
        'departure': 'uint64',
        'arrival': 'uint64',
        'station_from_id': 'uint64',
        'station_to_id': 'uint64',
        'two_storey': 'boolean',
    }})

    __schema_avia__ = schema_from_dict({**__schema_common_dict__, **{
        'source': 'string',
        'label_affiliate_clid': 'string',
        'date_forward': 'string',
        'date_backward': 'string',
        'label_from_id': 'string',
        'label_to_id': 'string',
        'trip_type': 'string',
    }})

    __partners_by_category__ = {
        'hotels': [
            'bnovo',
            'booking',
            'dolphin',
            'expedia',
            'hotels101',
            'hotelscombined',
            'ostrovok',
            'travelline',
            'tvil',
        ],
        'train': [
            'im_boy',
        ],
        'avia': [
            'aeroflot',
            'agent',
            'aviakassa',
            'aviaoperator',
            'azimuth',
            'biletdv',
            'biletikaeroag',
            'biletinet',
            'biletix',
            'booktripruag',
            'citytravel',
            'citytravel1',
            'clickavia',
            'expressavia',
            'flyone',
            'gogate',
            'kiwi',
            'kupibilet',
            'megotravel',
            'nabortu',
            'nebotravel',
            'uralairlines',
            'onetwotripru',
            'ozon',
            'smartavia',
            'tripcom',
            'tripcom',
            'utair',
            'anywayanyday',
            'pobeda',
            'redwings',
            'rusline',
            's_seven',
            'superkassa',
            'supersaver',
            'svyaznoy',
            'tinkoff',
            'ticketsru',
            'trip_ru',
            'uzairways',
        ],
    }

    __statuses__ = [
        'pending',
        'confirmed',
        'payed',
        'cancelled',
        'refunded',
    ]

    __currencies__ = [
        'RUB',
        'USD',
        'EUR',
    ]

    __available_clids__ = [
        '2518885',
        '2591784',
        '2475341',
        '2588261',
        '2532438',
        '2586722',
        '2522274',
        '2621933',
        '2513591',
        '2593354',
        '2628259',
        '2619030',
        '2592239',
        '2592705',
        '2601477',
        '2530769',
        '2580147',
        '2604598',
        '2617708',
        '2637798',
        '2604907',
        '2587224',
        '2600441',
        '2508114',
    ]

    __affiliate_partners__ = list(AffiliatePartner)
    __white_label_partner_ids__ = list(WhiteLabelPartnerId)

    __white_label_partners__ = {
        WhiteLabelPartnerId.S7: WhiteLabelPartner('s7', 'WLP_S7')
    }

    __avia_trip_type__ = [
        None,
        'oneway',
        'unknown',
        'roundtrip',
        'openjaw',
    ]

    def __init__(self, args: Namespace):
        self.args = args
        self.yt_client = YtClient(proxy=args.yt_proxy)

    def run(self):
        random.seed()
        now_ts = int(datetime.now(timezone.utc).timestamp())

        stations = self._get_stations(self.args.stations_table)
        settlements = self._get_settlements(self.args.settlements_table)

        table_path = ypath_join(self.args.dst_path, 'hotels')
        logging.info(f'Writing to {table_path}')
        recreate_table(table_path, self.yt_client, self.__schema_hotels__)
        self.yt_client.write_table(table_path, self._get_hotels_orders(now_ts))

        table_path = ypath_join(self.args.dst_path, 'train')
        logging.info(f'Writing to {table_path}')
        recreate_table(table_path, self.yt_client, self.__schema_train__)
        self.yt_client.write_table(table_path, self._get_train_orders(now_ts, stations))

        table_path = ypath_join(self.args.dst_path, 'avia')
        logging.info(f'Writing to {table_path}')
        recreate_table(table_path, self.yt_client, self.__schema_avia__)
        self.yt_client.write_table(table_path, self._get_avia_orders(now_ts, stations, settlements))

        logging.info('All done')

    def _get_stations(self, path):
        stations = list()
        for row in self.yt_client.read_table(path):
            stations.append(row['Id'])
        return stations

    def _get_settlements(self, path):
        settlements = list()
        for row in self.yt_client.read_table(path):
            settlements.append(row['Id'])
        return settlements

    def _get_hotels_orders(self, now_ts):
        category = 'hotels'
        orders = list()
        for _ in range(self.__order_count__):
            created_at = random.randint(now_ts - 50_000, now_ts)
            updated_at = random.randint(created_at, now_ts)
            check_in_ts = random.randint(created_at, created_at + 30_000)
            check_out_ts = random.randint(check_in_ts, check_in_ts + 50_000)
            check_in = str(datetime.fromtimestamp(check_in_ts).date())
            check_out = str(datetime.fromtimestamp(check_out_ts).date())

            orders.append(dict(
                category=category,
                partner_name=random.choice(self.__partners_by_category__[category]),
                partner_order_id=str(uuid4()),
                status=random.choice(self.__statuses__),
                currency_code=random.choice(self.__currencies__),
                order_amount=random.random() * self.__max_order_value__,
                created_at=created_at,
                updated_at=updated_at,
                check_in=check_in,
                check_out=check_out,
                hotel_name='МетаМосква',
                hotel_country='Russia',
                hotel_city='Moscow',
                **self._get_affiliate_params(fill_clid=True, fill_white_label=True),
            ))

        return orders

    def _get_train_orders(self, now_ts, stations):
        category = 'train'
        orders = list()
        for _ in range(self.__order_count__):
            created_at = random.randint(now_ts - 50_000, now_ts)
            updated_at = random.randint(created_at, now_ts)
            departure = random.randint(created_at, created_at + 30_000)
            arrival = random.randint(departure, departure + 500_000)
            station_from_id = random.choice(stations)
            station_to_id = random.choice(stations)

            orders.append(dict(
                category=category,
                partner_name=random.choice(self.__partners_by_category__[category]),
                partner_order_id=str(uuid4()),
                status=random.choice(self.__statuses__),
                currency_code=random.choice(self.__currencies__),
                order_amount=random.random() * self.__max_order_value__,
                created_at=created_at,
                updated_at=updated_at,
                departure=departure,
                arrival=arrival,
                station_from_id=station_from_id,
                station_to_id=station_to_id,
                two_storey=self._rand_bool(.3),
                **self._get_affiliate_params(fill_clid=True, fill_white_label=False),
            ))

        return orders

    def _get_avia_orders(self, now_ts, stations, settlements):
        category = 'avia'
        orders = list()
        for _ in range(self.__order_count__):
            partner_name = random.choice(self.__partners_by_category__[category])
            source = random.choice(['boy', 'aeroflot_clickout']) if partner_name == 'aeroflot' else partner_name
            created_at = random.randint(now_ts - 50_000, now_ts)
            updated_at = random.randint(created_at, now_ts)
            departure = random.randint(created_at, created_at + 30_000)
            arrival = random.randint(departure, departure + 500_000)

            orders.append(dict(
                category=category,
                partner_name=partner_name,
                source=source,
                partner_order_id=str(uuid4()),
                status=random.choice(self.__statuses__),
                currency_code=random.choice(self.__currencies__),
                order_amount=random.random() * self.__max_order_value__,
                created_at=created_at,
                updated_at=updated_at,
                date_forward=self._ts_to_date(departure),
                date_backward=self._ts_to_date(arrival),
                label_from_id=self._get_random_point(stations, settlements),
                label_to_id=self._get_random_point(stations, settlements),
                trip_type=random.choice(self.__avia_trip_type__),
                **self._get_affiliate_params(fill_clid=False, fill_white_label=False),
            ))

        return orders

    def _get_random_point(self, stations, settlements):
        if self._rand_bool(.5):
            return f's{random.choice(stations)}'
        else:
            return f'c{random.choice(settlements)}'

    @staticmethod
    def _rand_bool(probability: float):
        return random.random() < probability

    @staticmethod
    def _ts_to_date(ts):
        return str(datetime.fromtimestamp(ts).date())

    def _get_affiliate_params(self, fill_clid, fill_white_label):
        affiliate_partner = random.choice(self.__affiliate_partners__)
        admitad_uid = ''
        travelpayouts_uid = ''

        if affiliate_partner == AffiliatePartner.ADMITAD:
            admitad_uid = str(random.randint(1, 10_000))
        elif affiliate_partner == AffiliatePartner.TRAVELPAYOUTS:
            travelpayouts_uid = str(random.randint(1, 10_000))

        affiliate_params = dict(
            label_admitad_uid=admitad_uid,
            label_travelpayouts_uid=travelpayouts_uid
        )

        if fill_white_label and affiliate_partner == AffiliatePartner.WHITE_LABEL:
            affiliate_params.update(self._get_white_label_params())

        clid = random.choice(self.__available_clids__)
        if fill_clid:
            affiliate_params['label_clid'] = clid
        affiliate_params['label_affiliate_clid'] = clid
        return affiliate_params

    def _get_white_label_params(self):
        white_label_partner = self.__white_label_partners__[random.choice(self.__white_label_partner_ids__)]
        referral_partner_id, white_label_points_type = white_label_partner.partner_id, white_label_partner.points_type
        referral_partner_request_id = str(random.randint(1, 10_000))
        white_label_points_amount = random.randint(1, self.__max_bonus_points__)
        white_label_customer_number = str(random.randint(1, 10_000))

        return dict(
            label_referral_partner_id=referral_partner_id,
            label_referral_partner_request_id=referral_partner_request_id,
            white_label_points_type=white_label_points_type,
            white_label_points_amount=white_label_points_amount,
            white_label_customer_number=white_label_customer_number
        )


def main():
    logging.basicConfig(level=logging.INFO)

    parser = ArgumentParser()
    parser.add_argument('--yt-proxy', default='hahn.yt.yandex.net')
    parser.add_argument('--yt-token', required=True)
    parser.add_argument('--stations-table', default='//home/travel/prod/rasp_dicts/station')
    parser.add_argument('--settlements-table', default='//home/travel/prod/rasp_dicts/latest/settlement')
    parser.add_argument('--dst-path', required=True)
    args = parser.parse_args()

    App(args).run()


if __name__ == '__main__':
    main()
