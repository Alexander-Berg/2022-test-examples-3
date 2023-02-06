# coding: utf-8


import datetime
import pytest

from hamcrest import (
    assert_that,
    equal_to,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
    OffersRawTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.pylibrary.proto_utils import proto_to_dict
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto,
    make_offer_proto_str,
    make_uc_proto,
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags, DisabledFlags
from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.proto.feedparser.Promo_pb2 import OfferPromo as OfferPromoPb


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 2
HALF_MODE = False
CATEGORY_ID = 624627
ABSENT_CATEGORY_ID = 5236262
PARAM_1_ID = 111
PARAM_2_ID = 222
ABSENT_PARAM_ID = 333
UNKNOWN_PARAM_ID = 444


def make_default_uc_proto():
    data = {
        'model_id': 125,
        'category_id': CATEGORY_ID,
        'params': [
            dict(param_id=PARAM_1_ID),
            dict(param_id=PARAM_2_ID),
            dict(param_id=UNKNOWN_PARAM_ID),
        ],
    }
    return make_uc_proto(**data)


def make_default_uc_proto_str():
    return make_default_uc_proto().SerializeToString()


@pytest.yield_fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.yield_fixture(scope="module")
def source_offer2param():
    return [
        {
            'param_id': 111,
            'category_id': 624627,
            'feed_id': 2000,
            'offer_id': 'noApiWhiteOffer',
        },
        {
            'param_id': 111,
            'category_id': 624627,
            'feed_id': 2000,
            'offer_id': 'offerWithHole',
        },
        {
            'param_id': 222,
            'category_id': 624627,
            'feed_id': 2000,
            'offer_id': 'offerWithHole',
        },
        {
            'param_id': 222,
            'category_id': 624627,
            'feed_id': 2000,
            'offer_id': 'noApiWhiteOffer',
        },
        {
            'param_id': 444,
            'category_id': 624627,
            'feed_id': 2000,
            'offer_id': 'noApiWhiteOffer',
        },
        {
            'param_id': 444,
            'category_id': 624627,
            'feed_id': 2000,
            'offer_id': 'offerWithHole',
        },
    ]


@pytest.yield_fixture(scope="module")
def source_promos_raw():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'offerWithHole',
            'offer': None,  # в проде есть такие записи, у которых отсутствует offer - это разные promos и проч.
            'session_id': 30,
            'uc': None,
            'promo': OfferPromoPb(offer_id='offerWithHole').SerializeToString(),
        }
    ]


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'noApiWhiteOffer',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20001, ware_md5='guBi0gAAAAAAAAAAAAAAAA'),
            'uc': make_default_uc_proto_str(),
        },

        {
            'feed_id': 2000,
            'offer_id': 'offerWithHole',
            'offer': make_offer_proto_str(price=20004, ware_md5='guBi0gAAAAAAAAAAAAAAAA'),
            'session_id': 30,
            'uc': make_default_uc_proto_str(),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        {
            'msku': 111,
            'feed_id': 3000,
            'offer_id': 'noApiBlueOffer',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30004,
                is_blue_offer=True,
                offer_flags64=OfferFlags.BLUE_OFFER.value,
                disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
                ware_md5='guBi0gAAAAAAAAAAAAAAAA',
            ),
            'uc': make_default_uc_proto_str(),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    return [
        {
            'msku': 111,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS111',
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        },
        {
            'msku': 222,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS222',
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        },
        {
            'msku': 333,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS333',
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        },
        {
            'msku': 444,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS444',
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        },
        {
            'msku': 129,  # no blue offers
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS555',
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        },
    ]


@pytest.fixture(scope="module",
                params=["true", "false"])
def or3_config_data(request, yt_server):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'use_promo_table': 'true',
            'set_api_data_prices': request.param,
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_offers_raw,
    source_promos_raw,
    source_offer2param,
    source_blue_offers_raw,
    source_msku_contex
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'promos_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'promos_raw'),
            data=source_promos_raw
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data={}
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data=source_offer2param
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        )
    }


@pytest.fixture(scope="module")
def expected_offers(or3_config):
    set_api_data_prices_str = or3_config.options['misc']['set_api_data_prices']

    return {
        # нет API цены для офера
        'noApiWhiteOffer': {
            'feed_id': 2000,
            'offer_id': 'noApiWhiteOffer',
            'session_id': 30,
            'offer': proto_to_dict(
                make_offer_proto(
                    price=20001,
                    set_api_data_prices=set_api_data_prices_str,
                    ware_md5='guBi0gAAAAAAAAAAAAAAAA',
                )
            ),
            'uc': proto_to_dict(make_default_uc_proto()),
            'ware_md5': 'guBi0gAAAAAAAAAAAAAAAA',
            'cpa': 0,
        },
        # офер, с двумя записями, одна из которых с пустым полем OFFER - это не должно ронять main-idx
        # а также не должно пропадать поле promo
        'offerWithHole': {
            'feed_id': 2000,
            'offer_id': 'offerWithHole',
            'session_id': 30,
            'offer': proto_to_dict(
                make_offer_proto(
                    price=20004,
                    set_api_data_prices=set_api_data_prices_str,
                    ware_md5='guBi0gAAAAAAAAAAAAAAAA',
                )
            ),
            'uc': proto_to_dict(make_default_uc_proto()),
            'promo': proto_to_dict(
                OfferPromoPb(offer_id='offerWithHole')
            ),
            'ware_md5': 'guBi0gAAAAAAAAAAAAAAAA',
            'cpa': 0,
        }
    }


@pytest.fixture(scope="module")
def expected_blue_offers(or3_config):
    set_api_data_prices_str = or3_config.options['misc']['set_api_data_prices']

    return {
        # нет API цены для офера
        'noApiBlueOffer': {
            'feed_id': 3000,
            'offer_id': 'noApiBlueOffer',
            'session_id': 30,
            'offer': proto_to_dict(
                make_offer_proto(
                    price=30004,
                    is_blue_offer=True,
                    offer_flags64=OfferFlags.BLUE_OFFER.value,
                    buybox_offer=True,
                    disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
                    set_api_data_prices=set_api_data_prices_str,
                    ware_md5='guBi0gAAAAAAAAAAAAAAAA',
                )
            ),
            'uc': proto_to_dict(make_default_uc_proto()),
            'ware_md5': 'guBi0gAAAAAAAAAAAAAAAA',
            'is_dsbs': False,
            'cpa': 0,
        },
   }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(
            yt_server.get_yt_client().exists(path),
            "Table {} doesn\'t exist".format(path)
        )

    yt_home_path = or3_config.options['yt']['home_dir']
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }
    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offers_count(main_idx):
    result_offers = main_idx.outputs['offers']

    assert_that(len(result_offers), equal_to(8))  # white + blue + msku


@pytest.mark.parametrize('test_case', ['noApiWhiteOffer',
                                       'offerWithHole'])
def test_white_api_enrich(main_idx, expected_offers, test_case):
    """
    Что проверяем: обогащение белых оферов API ценами по ключу feedId, offerId
    """
    result_offers = main_idx.outputs['offers_by_offer_id']
    actual_offer = result_offers[test_case]
    expected_offer = expected_offers[test_case]
    del actual_offer['msku']
    del actual_offer['is_fake_msku_offer']

    for k in expected_offer['offer']['genlog']:
        assert expected_offer['offer']['genlog'][k] == actual_offer['genlog'][k], k
