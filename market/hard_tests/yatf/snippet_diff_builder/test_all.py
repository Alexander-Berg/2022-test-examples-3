# coding: utf-8

import yt.wrapper as yt

from hamcrest import (
    assert_that,
    equal_to,
    empty,
    less_than_or_equal_to,
    greater_than_or_equal_to
)

from market.idx.generation.yatf.test_envs.snippet_diff_builder import (
    SnippetDiffBuilderTestEnv,
    whoami,
    OfferRow,
    ModelRow,
    OfferStateRow,
    ModelStateRow,
    now_minutes,
    now_seconds,
)

from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow
from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.offers.yatf.resources.offers_indexer.top_query import TopQuery

from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
    BOOLEAN,
    ENUM,
    NUMERIC,
    NUMERIC_ENUM
)

import base64
import hashlib


BOOK_CATEGORY = Category(
    hid=90881,
    process_as_book_category=True,
    models_enrich_type=Category.BOOK
)

CATEGORY = Category(
    hid=90401,
    parameter=[
        Parameter(id=1, xsl_name='bool_param1', value_type=BOOLEAN, published=True),
        Parameter(id=2, xsl_name='numeric_param1', value_type=NUMERIC, published=True),
        Parameter(id=3, xsl_name='enum_param1', value_type=ENUM, published=True),
        Parameter(id=4, xsl_name='numeric_enum_param1', value_type=NUMERIC_ENUM, published=True),
    ]
)


def gl_mbo():
    return [
        BOOK_CATEGORY,
        CATEGORY
    ]


def make_uid_from_binary(binary):
    return base64.b64encode(binary, altchars='-_')[:22]


def make_pic_id(url):
    h = hashlib.md5()
    h.update(url)
    return make_uid_from_binary(h.digest())


def book_stuff():
    return [
        {
            'model_id': 333,
            'hid': BOOK_CATEGORY.hid,
            'description': 'You are reading an arbitrary boring test book description',
            'url': 'https://my-shop.ru/shop/books/1.html',
            'shop_name': 'My-shop.ru',
            'pic': [
                {
                    'md5': make_pic_id('my-shop.ru/pics/1.jpg'),
                    'group_id': 1234,
                    'width': 200,
                    'height': 200,
                    'thumb_mask': 0
                }
            ],
            'barcodes': [
                '9785458169387',
                '978-5-458-16938-7',
            ]
        }
    ]


def minutes_from_days(days):
    return days * 24 * 60


def check_default_ttl(rows, default_ttl=SnippetDiffBuilderTestEnv.DEFAULT_TTL):
    for row in rows:
        assert_that(
            row['value']['deadline'] - now_minutes(),
            less_than_or_equal_to(minutes_from_days(default_ttl))
        )

        assert_that(
            row['value']['deadline'] - now_minutes(),
            greater_than_or_equal_to(minutes_from_days(default_ttl - 1))
        )


def test_output_tables_exists(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[
                OfferRow(feed_id=1, offer_id=1),
                OfferRow(feed_id=2, offer_id=2)
            ],
            models=[ModelRow(id=1, category_id=90592, current_type='GURU', published_on_market=True)],
            state=[]
    ) as env:
        env.execute()
        assert_that(env.exists(env.output_diff_table_path), 'Offer table does not exist')
        assert_that(env.exists(env.output_reverse_diff_table_path), 'Reverse Diff table does not exist')
        assert_that(env.exists(env.output_state_table_path), 'State table does not exist')
        assert_that(env.output_diff_table[0]['diff_priority'], equal_to(1))
        assert_that(env.output_diff_table[1]['diff_priority'], equal_to(1))
        assert_that(env.output_diff_table[2]['diff_priority'], equal_to(1))


def test_output_tables_exists_additional_offers(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            additional_offers=[
                OfferRow(feed_id=1, offer_id=1),
                OfferRow(feed_id=2, offer_id=2)
            ],
            models=[ModelRow(id=1, category_id=90592, current_type='GURU', published_on_market=True)],
            state=[]
    ) as env:
        env.execute()
        assert_that(env.exists(env.output_diff_table_path), 'Offer table does not exist')
        assert_that(env.exists(env.output_reverse_diff_table_path), 'Reverse Diff table does not exist')
        assert_that(env.exists(env.output_state_table_path), 'State table does not exist')
        assert_that(env.output_diff_table[0]['diff_priority'], equal_to(1))
        assert_that(env.output_diff_table[1]['diff_priority'], equal_to(1))
        assert_that(env.output_diff_table[2]['diff_priority'], equal_to(1))


def test_quota_is_admired(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[
                OfferRow(feed_id=1, offer_id=1),
                OfferRow(feed_id=2, offer_id=2),
                OfferRow(feed_id=3, offer_id=3),
            ],
            models=[ModelRow(id=1)],
            state=[],
            quota=2
    ) as env:
        env.execute()
        assert_that(len(env.output_diff_table), equal_to(3))  # 1- Model, 2 - Offer
        assert_that(len(env.output_reverse_diff_table), equal_to(3))


def test_deleted_docs_goes_to_diff(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            models=[],
            state=[OfferStateRow(feed_id=1, offer_id=1),
                   ModelStateRow(model_id=3)],
            deleted_ttl=5
    ) as env:
        env.execute()

        assert_that(len(env.output_state_table), equal_to(0))  # no deleted docs in state (because no history data)
        for row in env.output_diff_table:
            assert_that(row['deleted'], equal_to(True))
            assert_that(
                row['value']['deadline'] - now_minutes(),
                less_than_or_equal_to(minutes_from_days(5))
            )

            assert_that(
                row['value']['deadline'] - now_minutes(),
                greater_than_or_equal_to(minutes_from_days(4))
            )

            assert_that(row['diff_priority'], equal_to(4))

        assert_that(len(env.output_reverse_diff_table), equal_to(2))
        assert_that(env.output_reverse_diff_table[0]['key'], equal_to('1-1'))
        assert_that(env.output_reverse_diff_table[1]['key'], equal_to('model-3'))


def test_no_deleted_docs_goes_to_diff_if_no_deletion(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            models=[],
            state=[OfferStateRow(feed_id=1, offer_id=1),
                   ModelStateRow(model_id=1)],
            deleted_ttl=5,
            do_not_delete_docs=True
    ) as env:
        env.execute()
        check_default_ttl(env.output_diff_table)
        assert_that(env.output_diff_table, empty())
        assert_that(env.output_reverse_diff_table, empty())

        for row in env.output_state_table:
            assert_that(row['deleted'], equal_to(False))


def test_deleted_docs_not_go_to_diff_beyound_quota(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            models=[],
            state=[
                OfferStateRow(feed_id=1, offer_id=1),
                OfferStateRow(feed_id=2, offer_id=2)
            ],
            quota=1
    ) as env:
        env.execute()
        assert_that(len(env.output_diff_table), 1)
        assert_that(len(env.output_reverse_diff_table), 1)
        assert_that(len(env.output_state_table), 1)


def test_old_diff_docs_ignored(yt_server):
    unchanged_ttl = 10
    default_ttl = unchanged_ttl + 7
    with SnippetDiffBuilderTestEnv(
        whoami(),
        yt_server,
        offers=[OfferRow(feed_id=1, offer_id=1, title='o1'), OfferRow(feed_id=2, offer_id=2, title='o1_')],
        models=[ModelRow(id=1, title='m1'), ModelRow(id=2, title='m1_')],
        state=[OfferStateRow(feed_id=1, offer_id=1, title='o2', last_update_seconds=now_seconds() + 11 * 24 * 60 * 60),
               ModelStateRow(model_id=1, title='m2', last_update_seconds=now_seconds() + 11 * 24 * 60 * 60)],
        unchanged_ttl=unchanged_ttl
    ) as env:
        env.execute()
        check_default_ttl(env.output_diff_table, default_ttl=default_ttl)
        assert_that(len(env.output_diff_table), equal_to(4))
        assert_that(len(env.output_reverse_diff_table), 4)

        assert_that(env.output_state_table[0]['value']['_Title'], 'o2')
        assert_that(env.output_state_table[2]['value']['_Title'], 'm2')


def test_genlog(yt_server):
    genlogs = [
        GenlogRow(
            id=0,
            feed_id=1,
            offer_id=1
        ),
        GenlogRow(
            id=1,
            feed_id=2,
            offer_id=2,
            credit_templates=[
                {'id': yt.yson.YsonUint64(1), 'is_installment': False},
                {'id': yt.yson.YsonUint64(2), 'is_installment': True},
            ],
        ),
    ]
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            genlogs=genlogs,
            models=[],
            state=[]
    ) as env:
        env.execute()
        assert_that(env.exists(env.output_diff_table_path), 'Offer table does not exist')
        assert_that(env.exists(env.output_reverse_diff_table_path), 'Reverse Diff table does not exist')
        assert_that(env.exists(env.output_state_table_path), 'State table does not exist')

        assert_that(len(env.output_diff_table), equal_to(len(genlogs)))
        assert_that(env.output_diff_table[0]['key'], equal_to('1-1'))
        assert_that(env.output_diff_table[1]['key'], equal_to('2-2'))

        for offerRow, genlogRow in zip(env.output_diff_table, genlogs):
            value = offerRow['value']
            genlog = genlogRow

            assert_that(offerRow['deleted'], equal_to(False))
            assert_that(offerRow['diff_priority'], 1)

            # assert_that(value['_Url'], equal_to(genlog.url))
            # assert_that(value['_Title'], equal_to(genlog.title))
            assert_that(value['LANG'], equal_to(genlog["lang"]))
            assert_that(value['history_price'], equal_to(genlog["snippet_history_price"]))
            assert_that(value['for_rotation'], equal_to('1' if genlog["for_rotation"] else '0'))
            assert_that(value['min_quantity'], equal_to(str(genlog["min_quantity"])))
            assert_that(value['offer_url_hash'], equal_to(genlog["offer_url_hash"]))
            assert_that(value['PicturesProtoBase64'], equal_to(genlog["pictures_proto_base64"]))
            assert_that(value['vat'], equal_to(str(genlog["vat"])))
            assert_that(value['step_quantity'], equal_to(str(genlog["step_quantity"])))


def test_book_fields_from_model_pass_correctly(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            models=[
                ModelRow(id=333, title='Book title', micro_model_search="Description", category_id=BOOK_CATEGORY.hid)
            ],
            state=[],
            gl_mbo_pbuf_sn=GlMboPb(gl_mbo()),
            parameters=ParametersPb(BOOK_CATEGORY),
            book_stuff=book_stuff(),
    ) as env:
        env.execute()
        book = env.output_diff_table[0]

        # Description from book stuff
        assert_that(book['value']['description'], equal_to('You are reading an arbitrary boring test book description'))
        assert_that(book['value']['LANG'], equal_to('ru'))
        assert_that(book['value']['ProtoPicInfo'], equal_to('CiFodHRwOi8vbWFya2V0LnlhbmRleC5ydS9tb2RlbC5qcGcQZBhk'))
        assert_that(book['value']['_Title'], equal_to('Book title'))
        assert_that(book['value']['_Url'], equal_to('market.yandex.ru/product/333'))
        assert_that(book['value']['key'], equal_to('model-333'))

        assert_that(len(env.output_reverse_diff_table), 1)
        rev = env.output_reverse_diff_table[0]
        # 2 - delete
        assert_that(rev['message_type'], 2)
        assert_that(rev['value']['key'], equal_to('model-333'))


def test_remove_field_goes_to_diff(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[OfferRow(feed_id=1, offer_id=1)],
            models=[ModelRow(id=1)],
            state=[OfferStateRow(feed_id=1, offer_id=1, add_field='extra-field'),
                   ModelStateRow(model_id=1, add_field='extra-field')]
    ) as env:
        env.execute()

        check_default_ttl(env.output_diff_table)

        assert_that(len(env.output_diff_table), equal_to(2))

        assert_that('extra-field' not in env.output_diff_table[0]['value'])
        assert_that(env.output_diff_table[0]['diff_priority'], equal_to(2))
        assert_that('extra-field' not in env.output_state_table[0]['value'])

        assert_that('extra-field' not in env.output_diff_table[1]['value'])
        assert_that(env.output_diff_table[1]['diff_priority'], equal_to(2))
        assert_that('extra-field' not in env.output_state_table[1]['value'])

        assert_that(len(env.output_reverse_diff_table), equal_to(2))
        assert_that('extra-field' in env.output_reverse_diff_table[0]['value'])
        assert_that('extra-field' in env.output_reverse_diff_table[1]['value'])


def test_add_new_field_goes_to_diff(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[OfferRow(feed_id=1, offer_id=1)],
            models=[],
            state=[OfferStateRow(feed_id=1, offer_id=1, del_field='_Url')]
    ) as env:
        env.execute()
        check_default_ttl(env.output_diff_table)
        assert_that(len(env.output_diff_table), equal_to(1))
        assert_that('_Url' in env.output_diff_table[0]['value'])
        assert_that(env.output_diff_table[0]['diff_priority'], equal_to(2))
        assert_that('_Url' in env.output_state_table[0]['value'])

        assert_that(len(env.output_reverse_diff_table), equal_to(1))
        assert_that('_Url' not in env.output_reverse_diff_table[0]['value'])


def test_model_sale_begin_ts(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            genlogs=[
                GenlogRow(
                    id=0,
                    feed_id=1,
                    offer_id='OfferWithSaleBeginTs',
                    model_sale_begin_ts='1',
                ),
            ],
            models=[],
            state=[]
    ) as env:
        env.execute()

        offer1 = env.output_diff_table[0]
        assert offer1['value']['model_sale_begin_ts'] == '1'


def test_model_quantity(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            genlogs=[
                GenlogRow(
                    id=0,
                    feed_id=1,
                    offer_id='OfferWithQuantity',
                    model_quantity_value='123',
                    model_quantity_unit='456',
                ),
            ],
            models=[],
            state=[]
    ) as env:
        env.execute()

        offer1 = env.output_diff_table[0]
        assert offer1['value']['model_quantity_value'] == '123'
        assert offer1['value']['model_quantity_unit'] == '456'


def test_top_queries(yt_server):
    TOP_QUERY_OFFER_DATA = 'TOP_QUERY_OFFER_DATA'
    TOP_QUERIES_ALL_DATA = 'TOP_QUERIES_ALL_DATA'

    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            genlogs=[
                GenlogRow(
                    id=0,
                    feed_id=1,
                    offer_id='1',
                    top_queries_offer=TopQuery(TOP_QUERY_OFFER_DATA).base64,
                    top_queries_all=TopQuery(TOP_QUERIES_ALL_DATA).base64,
                )
            ],
            models=[],
            state=[]
    ) as env:
        env.execute()

        check_default_ttl(env.output_diff_table)
        assert_that(len(env.output_diff_table), equal_to(1))
        assert_that(len(env.output_reverse_diff_table), equal_to(1))

        offer = env.output_diff_table[0]
        # check meta
        assert_that(offer['key'], equal_to('1-1'))
        assert_that(offer['deleted'], equal_to(False))
        assert_that(offer['diff_priority'], 1)
        # check fields

        top_queries_offer = TopQuery.parse(offer['value']['top_queries_offer'])
        assert_that(
            top_queries_offer.record[0].query,
            equal_to(TOP_QUERY_OFFER_DATA)
        )

        top_queries_all = TopQuery.parse(offer['value']['top_queries_all'])
        assert_that(
            top_queries_all.record[0].query,
            equal_to(TOP_QUERIES_ALL_DATA)
        )


def test_genlog_contex(yt_server):
    genlogs = [
        GenlogRow(
            id=0,
            feed_id=1,
            offer_id=1,
        ),
        GenlogRow(  # orignal offer
            id=1,
            feed_id=2,
            offer_id=2,
            contex_info={
                'experiment_id': 'some_exp',
                'experimental_msku_id': yt.yson.YsonUint64(5)
            }
        ),
        GenlogRow(  # clone offer
            id=2,
            feed_id=2,
            offer_id=2,
            contex_info={
                'experiment_id': 'some_exp',
                'original_msku_id': yt.yson.YsonUint64(5),
            }
        ),
    ]
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            genlogs=genlogs,
            models=[],
            state=[]
    ) as env:
        env.execute()
        assert_that(env.exists(env.output_diff_table_path), 'Offer table does not exist')
        assert_that(env.exists(env.output_reverse_diff_table_path), 'Reverse Diff table does not exist')
        assert_that(env.exists(env.output_state_table_path), 'State table does not exist')

        assert_that(len(env.output_diff_table), equal_to(len(genlogs)))
        assert_that(env.output_diff_table[0]['key'], equal_to('1-1'))
        assert_that(env.output_diff_table[1]['key'], equal_to('2-2'))
        assert_that(env.output_diff_table[2]['key'], equal_to('2-2_contex_some_exp'))


def test_offers_contex(yt_server):
    offers = [
        OfferRow(
            feed_id=1,
            offer_id=1
        ),
        OfferRow(
            feed_id=2,
            offer_id=2,
            contex_info={
                'experiment_id': 'some_exp',
                'experimental_msku_id': 5
            }
        ),
        OfferRow(
            feed_id=2,
            offer_id=2,
            contex_info={
                'experiment_id': 'some_exp',
                'original_msku_id': 5
            }
        ),
    ]
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=offers,
            models=[],
            state=[]
    ) as env:
        env.execute()
        assert_that(env.exists(env.output_diff_table_path), 'Offer table does not exist')
        assert_that(env.exists(env.output_reverse_diff_table_path), 'Reverse Diff table does not exist')
        assert_that(env.exists(env.output_state_table_path), 'State table does not exist')

        assert_that(len(env.output_diff_table), equal_to(len(offers)))
        assert_that(env.output_diff_table[0]['key'], equal_to('1-1'))
        assert_that(env.output_diff_table[1]['key'], equal_to('2-2'))
        assert_that(env.output_diff_table[2]['key'], equal_to('2-2_contex_some_exp'))


def test_model_vidal(yt_server):
    with SnippetDiffBuilderTestEnv(
            whoami(),
            yt_server,
            offers=[],
            genlogs=[
                GenlogRow(
                    id=0,
                    feed_id=1,
                    offer_id='OfferWithVidal',
                    vidal_atc_code='j05ax13',
                ),
            ],
            models=[],
            state=[]
    ) as env:
        env.execute()

        offer1 = env.output_diff_table[0]
        assert offer1['value']['vidal_atc_code'] == 'j05ax13'
