#!/usr/bin/env python
# encoding: utf-8

import datetime
import random
import string

import travel.proto.commons_pb2 as commons_pb2


DATE_FORMAT = '%Y-%m-%d'
DEFAULT_AGES = '88,88'
DEFAULT_DATE = '2018-01-02'
DEFAULT_NIGHTS = 1
FAKE_NOW = 1514764800  # 2018-01-01

FIRST_RANDOM_DATE = datetime.date(2018, 7, 1)
RANDOM_DATES = [FIRST_RANDOM_DATE + datetime.timedelta(days=d) for d in [0, 1]]
RANDOM_NIGHTS = [1, 3]
RANDOM_OCCUPANCIES_AND_AGES = {'1': '88', '2': '88,88', '3-1': '1,88,88,88'}


def iterate_random_params():
    for checkin in RANDOM_DATES:
        for nights in RANDOM_NIGHTS:
            for occup, ages in RANDOM_OCCUPANCIES_AND_AGES.items():
                yield (checkin, nights, occup, ages)


def random_string():
    return ''.join([random.choice(string.ascii_letters + string.digits) for n in range(32)])


def random_hex_string(length):
    return ''.join([random.choice('0123456789abcdef') for n in range(length)])


def random_int():
    return random.randint(1, 2 ** 31)


def random_uint16():
    return random.randint(1, 2 ** 15)


def random_float():
    return (random.random() - 0.5) * 2 ** 10


def random_bool():
    return random.randint(0, 1) == 0


def random_seq(producer, min_count=1, max_count=10):
    return [producer() for _ in range(random.randint(min_count, max_count))]


def format_date(dt):
    return dt.strftime(DATE_FORMAT)


def date_days_after(date_from, days):
    d = datetime.datetime.strptime(date_from, DATE_FORMAT).date()
    d += datetime.timedelta(days=days)
    return format_date(d)


def prepare_testids():
    return ','.join(['"%s"' % random.randint(-10, 1000) for x in range(random.randint(5, 30))])


def prepare_request(**params):
    result = {
        'SerpReqId': random_string(),
        'YandexUid': random_string(),
        'SearchQuery': random_string() + '\t' + random_string(),
        'utm_source': random_string(),
        'utm_medium': random_string(),
        'utm_campaign': random_string(),
        'utm_content': random_string(),
        'utm_term': random_string(),
        'TestIds': prepare_testids(),
        'TestBuckets': prepare_testids(),
        'PassportUid': random_string(),
        'Uuid': random_string(),
        'RequestRegion': random_int(),
        'UserRegion': random_int(),
        'ICookie': random_string(),
        'GeoClientId': random_string(),
        'GeoOrigin': random_string(),
        'UserDevice': random_string(),
        'gclid': random_string(),
        'YaTravelReqId': random_string(),
        'LabelHash': random_string(),
    }
    result.update(params)
    return result


def get_permalink(shotel_id):
    return shotel_id.split('~')[0]


def get_partner_hotel_ids(oc_app, shotel_id):  # -> {partner_id, orig_hotel_id}
    res = {}
    for part in shotel_id.split('~')[1:]:
        if not part.startswith('ytravel'):
            continue
        partner_code, orig_hotel_id = part.split('.', 1)
        res[oc_app.partnercode2id[partner_code]] = orig_hotel_id
    return res


def occupancy_to_ages(occup):
    parts = occup.split('-')
    adults = int(parts[0])
    children = []
    if len(parts) > 1:
        children = [int(age) for age in parts[1].split(',')]
    ages = sorted(children + [88] * adults)
    ages = [str(v) for v in ages]
    return ','.join(ages)


def parse_testids(test_ids_str):
    res = []
    for part in test_ids_str.split(','):
        assert part[0] == '"'
        assert part[-1] == '"'
        res.append(int(part[1:-1]))
    return res


def build_price(amount):
    if amount is None:
        return None
    return commons_pb2.TPrice(Currency=commons_pb2.C_RUB, Amount=amount, Precision=0)
