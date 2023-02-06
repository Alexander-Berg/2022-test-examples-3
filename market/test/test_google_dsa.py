# -*- coding: utf-8 -*-
import pytest

from hamcrest import assert_that, equal_to
from yt.wrapper import ypath_join

from market.idx.export.awaps.yatf.resources.google_dsa_table import GoogleDsaTable
from market.idx.export.awaps.yatf.test_envs.awaps_models import YtAwapsModelsTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from util import msku_url


DATA = [
    {
        'market_sku': 1723658605,
        'model_id': 12345,
        'published_on_market': True,
        'ware_md5': 'do_not_use_this_offer_id',
        'title': 'sku 1',
        'custom_label' : ""  # TODO надо сделать тест если здесь null
    },
    {
        'market_sku': 100321103912,
        'model_id': 12346,
        'published_on_market': False,
        'ware_md5': 'my_offer_id',
        'title': 'sku 2',
        'custom_label' : "CEHAC"
    },
]


@pytest.fixture(scope='module', params=['market.yandex.ru'])
def blue_domain(request):
    yield request.param


@pytest.fixture(scope='module')
def google_dsa_table(yt_server):
    tablepath = ypath_join(get_yt_prefix(), 'out', 'banner', 'google_dsa')
    return GoogleDsaTable(yt_server, tablepath, DATA)


@pytest.fixture(scope='module')
def workflow(yt_server, google_dsa_table, blue_domain):
    resources = {
        'google_dsa_table': google_dsa_table
    }
    bin_flags = [
        '--input', google_dsa_table.get_path(),
        '--feed', 'google-dsa',
        '--naming-scheme', 'SinglePart',
        '--blue_domain', blue_domain,
        '--blue_name', u'Yandex Market',
        '--blue-on-market',
    ]

    with YtAwapsModelsTestEnv(yt_stuff=yt_server, bin_flags=bin_flags, xml_output=False, **resources) as banner_upload:
        banner_upload.execute(ex=True)
        banner_upload.verify()
        yield banner_upload


@pytest.fixture(scope='module')
def output_offers(workflow):
    return workflow.outputs['offers']


@pytest.fixture(scope='module')
def expected_offers(blue_domain):
    data = [['Page URL', 'Custom Label']]
    for offer in DATA:
        data.append([msku_url(
            blue_domain=blue_domain,
            model_id=offer['model_id'],
            market_sku=offer['market_sku'],
            title=offer['title'].encode('utf-8'),
            utm_term=None,
            ware_md5=offer['ware_md5'],
            published_on_market=offer['published_on_market'],
        ),
        str("'{}'".format(offer['custom_label'] if offer['custom_label'] else "n/a"))  # приходится проставялть кавычки внутри (')
        ])
    return data


def test_feed_format(output_offers, expected_offers):
    """ Проверяем, что формат фида для Google DSA """
    assert_that(output_offers, equal_to(expected_offers))
