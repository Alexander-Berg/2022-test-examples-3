# -*- coding: utf-8 -*-
import django
django.setup()

import pytest

import travel.avia.avia_api.avia.v1.email_dispenser.api as api
from travel.avia.avia_api.avia.v1.model.subscriber import Subscriber

pytest_plugins = [
    'travel.avia.library.python.tester.initializer',
    'travel.avia.library.python.tester.plugins.transaction',
]


TEST_DATE = '2019-12-01'
DEPARTURE_DATE = '2019-12-21'
email1 = 'my.corrEct_email@domain1.domAIN2.domain3'
email2 = 'my.BAD_email@domain1.domAIN2.domain3'


@pytest.fixture()
def subscriber():
    def get_subscriber(**kwargs):
        # we assume that emails in subscriber model are always written in normalized form
        if 'email' in kwargs:
            email = kwargs.pop('email')
        else:
            email = api.normalize_email(email1)
        if 'api_version' in kwargs:
            api_version = kwargs.pop('api_version')
        else:
            api_version = '1.1'
        return Subscriber(email=email, api_version=api_version)

    return get_subscriber


qkeys = [
    qkey.format(DEPARTURE_DATE) for qkey in
    [
        'c213_c65_{}_2019-01-05_business_1_0_1_ru',
        'c65_c56_{}_2019-01-05_economy_1_1_1_ru',
        'c56_c2_{}_2019-01-05_economy_2_0_0_ru',
        'c2_c54_{}_2019-01-05_business_2_1_0_ru',
        'c54_c213_{}_2019-01-05_economy_2_0_1_ru',
        'c213_c65_{}_None_economy_1_0_1_ru',
        'c65_c56_{}_None_business_1_1_0_ru',
        'c56_c2_{}_None_economy_1_0_0_ru',
        'c2_c54_{}_None_economy_2_1_1_ru',
        'c54_c213_{}_None_business_2_0_1_ru',
    ]
]


def qkeys_all_pairs():
    for qkey1 in qkeys:
        for qkey2 in qkeys:
            if qkey1 == qkey2:
                continue
            yield qkey1, qkey2


@pytest.fixture(params=qkeys)
def qkey(request):
    return request.param


@pytest.fixture(params=qkeys_all_pairs())
def qkey_pair(request):
    return request.param
