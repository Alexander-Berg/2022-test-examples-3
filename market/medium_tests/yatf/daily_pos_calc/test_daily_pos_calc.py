import pytest

from hamcrest import (
    assert_that,
    all_of,
    any_of,
    is_not,
    has_entry,
    has_entries,
    has_item,
    has_items,
    has_length,
)

from yt.wrapper.ypath import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.yql_env import YqlRunnerTestEnv
from market.idx.yatf.resources.yql_resource import (
    YtResource,
    YqlRequestResource
)

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.marketindexer.marketindexer import dropped_offers

TODAY = 239
THREASHOLD = 0.6


class ShowStatsTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        super(ShowStatsTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=False,
                external=False,
                schema=[
                    dict(name="feed_id", type="uint64"),
                    dict(name="offer_id", type="string"),
                    dict(name="cnt_shows", type="uint64"),
                ]
            )
        )


class MstatOffersTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        super(MstatOffersTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=False,
                external=False,
                schema=[
                    dict(name="feed_id", type="int64"),
                    dict(name="offer_id", type="string"),
                    dict(name="category_hid", type="int64"),
                    dict(name="shop_id", type="int64"),
                    dict(name="is_blue_offer", type="boolean"),
                    dict(name="flags", type="int64")
                ]
            )
        )


class SeasonStatTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        super(SeasonStatTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=False,
                external=False,
                schema=[
                    dict(name="start", type="int64"),
                    dict(name="stop", type="int64"),
                    dict(name="category_hid", type="int64"),
                    dict(name="threshold", type="double"),
                ]
            )
        )


class ShopsdatTable(YtTableResource):
    def __init__(self, yt_stuff, path, data):
        super(ShopsdatTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            data=data,
            attributes=dict(
                dynamic=False,
                external=False,
                schema=[
                    dict(name="shop_id", type="uint64"),
                    dict(name="is_mock", type="boolean"),
                    dict(name="cpa", type="string"),
                ]
            )
        )


@pytest.fixture(scope='module')
def input_data():
    def make_show_stat_offer(feed_id, offer_id, cnt_shows):
        return {
            'feed_id': feed_id,
            'offer_id': offer_id,
            'cnt_shows': cnt_shows,
        }

    def make_offer(feed_id, offer_id, category_hid=239, shop_id=1234, is_blue_offer=False, is_lavka_offer=False, is_eda_offer=False):
        if is_lavka_offer:
            flags = OfferFlags.IS_LAVKA
        elif is_eda_offer:
            flags = OfferFlags.IS_EDA
        else:
            flags = 0
        return {
            'feed_id': feed_id,
            'offer_id': offer_id,
            'category_hid': category_hid,
            'shop_id': shop_id,
            'is_blue_offer': is_blue_offer,
            'flags': flags,
        }

    def make_season_stat_row(start, stop, category_hid, threshold):
        return {
            'start': start,
            'stop': stop,
            'category_hid': category_hid,
            'threshold': threshold,
        }

    def make_shopsdat_row(shop_id, is_mock=False, cpa='NO'):
        return {
            'shop_id': shop_id,
            'is_mock': is_mock,
            'cpa': cpa,
        }

    shows_data = [
        make_show_stat_offer(1, 'OneShows1', 1),
        make_show_stat_offer(2, 'OneShows2', 1),
        make_show_stat_offer(1, 'TenShows1', 10),
        make_show_stat_offer(1, 'TenShows2', 10),
        make_show_stat_offer(2, 'TenShows3', 10),
        make_show_stat_offer(2, 'TenShows4', 10),
        make_show_stat_offer(100, 'TenShows5', 10),
        make_show_stat_offer(3, 'BlueShows', 1),
        make_show_stat_offer(4, 'BlueShows2', 1),
    ]

    offer_data = [
        make_offer(1, 'NotShows1'),
        make_offer(1, 'NotShows2', shop_id=1235),
        make_offer(2, 'NotShows3'),
        make_offer(2, 'NotShows3', 240),  # offer с таким же feed\offer_id но другой категорией
        make_offer(5, 'NotShows4', 200),
        make_offer(100, 'NotShows5', 300, shop_id=1236),
        make_offer(1, 'OneShows1'),
        make_offer(2, 'OneShows2', 300),
        make_offer(1, 'TenShows2', 200),
        make_offer(2, 'TenShows3', 200),
        make_offer(100, 'TenShows5', 200),
        make_offer(3, 'BlueNotShows', is_blue_offer=True),
        make_offer(3, 'BlueShows', is_blue_offer=True),
        make_offer(4, 'BlueNotShows2', is_blue_offer=True),
        make_offer(4, 'BlueShows2', is_blue_offer=True),
        make_offer(237, 'DsbsREALOffer', shop_id=1237),
        make_offer(238, 'DsbsSBXOffer', shop_id=1238),
        make_offer(300, 'LavkaOffer', shop_id=1239, is_lavka_offer=True),
        make_offer(400, 'EdaOffer', shop_id=1240, is_eda_offer=True),
    ]

    season_data = [
        make_season_stat_row(TODAY - 10, TODAY - 1, 239, THREASHOLD),
        make_season_stat_row(TODAY + 1, TODAY + 10, 239, THREASHOLD),
        make_season_stat_row(TODAY - 1, TODAY + 1, 200, THREASHOLD),
        make_season_stat_row(TODAY - 5, TODAY + 1, 300, THREASHOLD - 0.1),
    ]

    shopsdat_data = [
        make_shopsdat_row(1234, is_mock=False),
        make_shopsdat_row(1235, is_mock=False),
        make_shopsdat_row(1236, is_mock=True),
        make_shopsdat_row(1237, is_mock=False, cpa='REAL'),
        make_shopsdat_row(1238, is_mock=False, cpa='SBX'),
        make_shopsdat_row(1239, is_mock=False),
    ]

    return shows_data, offer_data, season_data, shopsdat_data


@pytest.fixture(
    scope='module',
    params=[
        {'expected': 5},
        {'expected': 10, 'cnt_limit': 99},
        {'expected': 4, 'use_season': True},
        {'expected': 6, 'cnt_limit': 4, 'use_season': True},
        {'expected': 2, 'white_shop_id_list': [1234]},
        {'expected': 4, 'white_shop_id_list': [1235, 1299]},
        {'expected': 3, 'use_season': True, 'white_shop_id_list': [1235, 1299]},
        {'expected': 5, 'cnt_limit': 4, 'use_season': True, 'white_shop_id_list': [1235, 1299]},
        {'expected': 4, 'ignore_mock_shops': True},
        {'expected': 3, 'use_season': True, 'ignore_mock_shops': True},
        {'expected': 1, 'white_shop_id_list': [1234], 'ignore_mock_shops': True},

        {'expected': 5, 'use_auto_extending': True},
        {'expected': 5, 'use_auto_extending': True, 'cnt_limit': 99},
        {'expected': 7, 'use_auto_extending': True, 'cnt_limit': 2, 'minimum_pos_size': 99},
        {'expected': 5, 'use_auto_extending': True, 'cnt_limit': 2, 'minimum_pos_size': 5},
        {'expected': 4, 'use_auto_extending': True, 'use_season': True},
        {'expected': 4, 'use_auto_extending': True, 'cnt_limit': 4, 'use_season': True},
        {'expected': 2, 'use_auto_extending': True, 'white_shop_id_list': [1234]},
        {'expected': 4, 'use_auto_extending': True, 'white_shop_id_list': [1235, 1299]},
        {'expected': 3, 'use_auto_extending': True, 'use_season': True, 'white_shop_id_list': [1235, 1299]},
        {'expected': 3, 'use_auto_extending': True, 'use_season': True, 'white_shop_id_list': [1235, 1299]},
        {'expected': 4, 'use_auto_extending': True, 'ignore_mock_shops': True},
        {'expected': 3, 'use_auto_extending': True, 'use_season': True, 'ignore_mock_shops': True},
        {'expected': 1, 'use_auto_extending': True, 'white_shop_id_list': [1234], 'ignore_mock_shops': True},
    ],
    ids=[
        'original',
        'use_count_limit',
        'use_season',
        'use_season_and_show_limit',
        'white_shop_id_list_one_id',
        'white_shop_id_list_two_id',
        'use_season_and_white_shop_id_list',
        'use_season_and_show_limit_and_white_shop_id_list',
        'ignore_mock_shops',
        'ignore_mock_shops_and_use_season',
        'ignore_mock_shops_and_white_shop_id_list',

        'extending_original',
        'extending_use_count_limit',
        'extending_minimum_pos_size_and_no_show_limit',
        'extending_minimum_pos_size_and_show_limit',
        'extending_use_season',
        'extending_use_season_and_show_limit',
        'extending_white_shop_id_list_one_id',
        'extending_white_shop_id_list_two_id',
        'extending_use_season_and_white_shop_id_list',
        'extending_use_season_and_show_limit_and_white_shop_id_list',
        'extending_ignore_mock_shops',
        'extending_ignore_mock_shops_and_use_season',
        'extending_ignore_mock_shops_and_white_shop_id_list',
    ]
)
def pos_calc_params(request):
    default_param = {
        'cnt_limit': 0,
        'minimum_pos_size': 0,
        'use_season': False,
        'white_shop_id_list': [],
        'ignore_mock_shops': False,
        'use_auto_extending': False,
    }
    default_param.update(request.param)
    return default_param


@pytest.fixture(scope='module')
def shows_stats_yt_table_path(yt_server, input_data):
    data, _, _, _ = input_data

    table = ShowStatsTable(yt_server, ypath_join(get_yt_prefix(), 'input', 'shows_stats'), data)
    table.create()

    return table.get_path()


@pytest.fixture(scope='module')
def mstat_offes_yt_table_path(yt_server, input_data):
    _, data, _, _ = input_data

    table = MstatOffersTable(yt_server, ypath_join(get_yt_prefix(), 'input', 'market-offers'), data)
    table.create()

    return table.get_path()


@pytest.fixture(scope='module')
def season_stat_yt_table_path(yt_server, input_data):
    _, _, data, _ = input_data

    table = SeasonStatTable(yt_server, ypath_join(get_yt_prefix(), 'input', 'season_stat'), data)
    table.create()

    return table.get_path()


@pytest.fixture(scope='module')
def shopsdat_yt_table_path(yt_server, input_data):
    _, _, _, data = input_data

    table = ShopsdatTable(yt_server, ypath_join(get_yt_prefix(), 'input', 'shopsdat'), data)
    table.create()

    return table.get_path()


@pytest.fixture(scope='module')
def daily_pos_yt_table_path(yt_server):
    return ypath_join(get_yt_prefix(), 'in', 'PoS', 'today')


@pytest.fixture(scope='module')
def yql_runner(yt_server):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
    }

    with YqlRunnerTestEnv(syntax_version=1, **resources) as test_env:
        yield test_env


@pytest.fixture(scope='module')
def daily_pos_calc_workflow(
    yt_server,
    shows_stats_yt_table_path,
    mstat_offes_yt_table_path,
    season_stat_yt_table_path,
    shopsdat_yt_table_path,
    daily_pos_yt_table_path,
    yql_runner,
    pos_calc_params
):
    request = dropped_offers._old_not_shown_calculate_yql(
        yt_server.get_server(),
        "test",
        shows_stats_yt_table_path,
        mstat_offes_yt_table_path,
        daily_pos_yt_table_path,
        season_stat_table=season_stat_yt_table_path,
        shopsdat_table=shopsdat_yt_table_path,
        show_threshold=pos_calc_params['cnt_limit'],
        minimum_result_table_size=pos_calc_params['minimum_pos_size'],
        white_shop_id_list=pos_calc_params['white_shop_id_list'],
        use_season=pos_calc_params['use_season'],
        ignore_mock_shops=pos_calc_params['ignore_mock_shops'],
        use_auto_extending=pos_calc_params['use_auto_extending'],
        today=TODAY,
    )

    #  remove USE
    request = '\n'.join(request.split('\n')[1:])

    result = yql_runner.execute(YqlRequestResource(request))
    if result.status != 'COMPLETED':
        raise RuntimeError('YQL ERROR: {}'.format(result))


@pytest.fixture(scope='module')
def pos_offers(yt_server, daily_pos_calc_workflow, daily_pos_yt_table_path):
    yt = yt_server.get_yt_client()
    return list(yt.read_table(daily_pos_yt_table_path))


@pytest.fixture(scope='module')
def pos_schema(yt_server, daily_pos_calc_workflow, daily_pos_yt_table_path):
    yt = yt_server.get_yt_client()
    return yt.get(ypath_join(daily_pos_yt_table_path, '@schema'))


def test_result_pos_table(pos_offers, pos_schema, pos_calc_params):
    assert_that(
        pos_offers,
        all_of(
            # check that Blue, DSBS, Lavka and Eda offers never fall into PoS
            is_not(
                has_item(
                    any_of(
                        has_entry('offer_id', 'BlueNotShows'),
                        has_entry('offer_id', 'BlueShows'),
                        has_entry('offer_id', 'BlueNotShows2'),
                        has_entry('offer_id', 'BlueShows2'),
                        has_entry('offer_id', 'DsbsSBXOffer'),
                        has_entry('offer_id', 'DsbsREALOffer'),
                        has_entry('offer_id', 'LavkaOffer'),
                        has_entry('offer_id', 'EdaOffer'),
                    )
                )
            ),
            # check number of offers in PoS
            has_length(pos_calc_params['expected']),
        )
    )

    assert_that(
        pos_schema,
        all_of(
            # check required columns names and types
            has_items(
                has_entries(dict(name='feed_id',  type='uint64')),
                has_entries(dict(name='offer_id', type='string')),
            ),
            # check number of columns
            has_length(2),
        )
    )
