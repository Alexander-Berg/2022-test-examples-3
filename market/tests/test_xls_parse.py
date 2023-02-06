# -*- coding: utf-8 -*-

import datetime
import io
import logging
import market.pylibrary.yatestwrap.yatestwrap as yatestwrap

from dateutil import parser, tz
from urllib.parse import quote_plus

from market.idx.promos.blue_gift_with_purchase.lib.shops_data import ShopsData, ANY_SHOP, ANY_WAREHOUSE, DUMMY_SHOPSDATA
from market.idx.promos.blue_gift_with_purchase.lib.xls_reader import read_xls, OBSOLETE_SHOP_PROMO_ID_TAG
from market.idx.promos.blue_gift_with_purchase.lib.xls_types.direct_discount import DirectDiscountXlsType
from market.idx.promos.blue_gift_with_purchase.lib.utils import cast_value_to_bool, cast_value_to_unicode, BM_BASE_URL, UniquesChecker
from market.idx.promos.blue_gift_with_purchase.lib.version_data import TAG_ALLOW_DECOMMISIONED_VERSION
from market.idx.promos.blue_gift_with_purchase.lib.promo_details_helper import make_promo_key
from market.proto.feedparser.Promo_pb2 import MechanicsPaymentType
from market.pylibrary.const.payment_methods import PaymentMethod
from market.pylibrary.const.offer_promo import PromoType
from market.proto.common.promo_pb2 import EPromoType


TEST_XLS = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test.xlsx')
TEST_XLS_DUP = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_dup.xlsx')
TEST_XLS_GOOD = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_good.xlsx')
TEXT_XLS_URL_GEN2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_url_gen2.xlsx')
TEXT_XLS_URL_GENERATION = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_url_generation.xlsx')
TEST_XLS_GB_V6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_gb_v6.xlsx')
TEST_XLS_GB_V7 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_gb_v7.xlsx')
TEST_XLS_GB_V8 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_gb_v8.xlsx')
TEST_XLS_GB_V9 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_gb_v9.xlsx')
TEST_XLS_GB_V8_INVALID = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_gb_v8_invalid.xlsx')
TEST_XLS_CG_5 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_cg_5.xlsx')
TEST_XLS_CG_6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_cg_6.xlsx')
TEST_XLS_CG_7 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_cg_7.xlsx')
TEST_XLS_CG_8 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_cg_8.xlsx')
TEST_XLS_UNIQ_1 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_uniq_1.xlsx')
TEST_XLS_UNIQ_2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_uniq_2.xlsx')
TEST_XLS_WRONG = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_wrong.xlsx')
TEST_XLS_WRONG_V2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_wrong_v2.xlsx')
TEXT_XLS_SEVERAL_BUNDLES_IN_PROMO = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_several_bundles_in_promo.xlsx')
TEST_XLS_BLUE_FLASH_V5 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_flash_v5.xlsx')
TEST_XLS_BLUE_FLASH_V5_BAD = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_flash_v5_bad.xlsx')
TEST_XLS_BLUE_FLASH_V6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_flash_v6.xlsx')
TEST_XLS_BLUE_FLASH_V7 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_flash_v7.xlsx')
TEST_XLS_BLUE_FLASH_V7_NULL_WAREHOUSE = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_flash_v7_null_warehouse.xlsx')
TEST_XLS_BLUE_FLASH_V8 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_flash_v8.xlsx')
TEST_XLS_NOT_XLS = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/not_xls.xlsx')
TEST_XLS_BLUE_SET_BAD_V5 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_bad_v5.xlsx')
TEST_XLS_BLUE_SET_BAD_V8 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_bad_v8.xlsx')
TEST_XLS_BLUE_SET_GOOD_V4 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_good_v4.xlsx')
TEST_XLS_BLUE_SET_GOOD_V5 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_good_v5.xlsx')
TEST_XLS_BLUE_SET_GOOD_V6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_good_v6.xlsx')
TEST_XLS_BLUE_SET_GOOD_V7 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_good_v7.xlsx')
TEST_XLS_BLUE_SET_GOOD_V8 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_blue_set_good_v8.xlsx')
TEST_XLS_DIRECT_DISCOUNT_BAD_V6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_direct_discount_v6_bad.xlsx')
TEST_XLS_WH_UNIQ = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_wh_uniq.xlsx')
TEST_XLS_DIRECT_DISCOUNT_GOOD_V5 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_direct_discount_v5_good.xlsx')
TEST_XLS_DIRECT_DISCOUNT_GOOD_V6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_direct_discount_v6_good.xlsx')
TEST_XLS_DIRECT_DISCOUNT_GOOD_V7 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_direct_discount_v7_good.xlsx')
TEST_XLS_DIRECT_DISCOUNT_GOOD_V8 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_direct_discount_v8_good.xlsx')
TEST_XLS_SECRET_SALE_PLUS_V106 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_secret_sale_plus_v106.xlsx')
TEST_XLS_INCORRECT_PROMO_ID = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_incorrect_promo_id.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V1 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v1.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v2.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_EMPTY_BUDGET_EXCEPTION = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_empty_budget_exception.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_EXCLUDED_SUPPLIERS = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v1_excluded_suppliers.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_1 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_non_uniq_1.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_non_uniq_2.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_3 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_non_uniq_3.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_4 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_non_uniq_4.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_NO_BOUNDS = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_no_bounds.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_NO_MSKU = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_no_msku.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_BAD_INCLUDE_EXCLUDE = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_bad_include_exclude.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_DISABLED = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v1_disabled.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_BAD_DISCOUNT = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v1_bad_discount.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_BAD_DISCOUNT_2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v1_bad_discount_2.xlsx')
TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_BAD_DISCOUNT_3 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_count_v1_bad_discount_3.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.2.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_EMPTY = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_empty.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_EXCLUDED_IDS = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_excluded_ids.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_1 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_bad_discount_1.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_bad_discount_2.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_3 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_bad_discount_3.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_4 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_bad_discount_4.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_5 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_bad_discount_5.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_6 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_bad_discount_6.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_NON_UNIQUE = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_non_unique.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_NON_UNIQUE_EXCLUDED = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_non_unique_excluded.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_COLUMNS_CONFLICT = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_columns_conflict.xlsx')
TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_COLUMNS_CONFLICT_2 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_spread_discount_receipt_v7.1_columns_conflict_2.xlsx')
TEST_XLS_MARKETOUT_43095 = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_MARKETOUT-43095.xlsx')
TEST_XLS_MAX_ITEMS = yatestwrap.source_path('market/idx/promos/blue_gift_with_purchase/tests/data/test_max_items.xlsx')

logger = logging.getLogger()

TAG_ADV = [TAG_ALLOW_DECOMMISIONED_VERSION]


def test_bool_converter():
    assert cast_value_to_bool(None) is False
    assert cast_value_to_bool(None, True) is True
    assert cast_value_to_bool(None, False) is False

    assert cast_value_to_bool(True) is True
    assert cast_value_to_bool(True, True) is True
    assert cast_value_to_bool(True, False) is True
    assert cast_value_to_bool(False) is False
    assert cast_value_to_bool(False, True) is False
    assert cast_value_to_bool(False, False) is False

    assert cast_value_to_bool(1) is True
    assert cast_value_to_bool(0) is False

    assert cast_value_to_bool('1', False) is True
    assert cast_value_to_bool('yeS', False) is True
    assert cast_value_to_bool('True', False) is True
    assert cast_value_to_bool('да', False) is True
    assert cast_value_to_bool('Да', False) is True
    assert cast_value_to_bool('ВКЛ', False) is True
    assert cast_value_to_bool('0', True) is False
    assert cast_value_to_bool('NO', True) is False
    assert cast_value_to_bool('false', True) is False
    assert cast_value_to_bool('НЕТ', True) is False
    assert cast_value_to_bool('выкл', True) is False
    assert cast_value_to_bool('', True) is True
    assert cast_value_to_bool('', False) is False

    for value in [(), -1, 777, 'лопата', u'лопата']:
        try:
            cast_value_to_bool(value)
        except Exception as e:
            assert str(e) == 'некорректное булево значение: "{}"'.format(cast_value_to_unicode(value))


def __check_timestamp(ts, text_dt_msk):
    msk_ts = parser.parse(text_dt_msk)
    zero_ts = datetime.datetime(1970, 1, 1).replace(tzinfo=tz.gettz('UTC'))
    dt_delta = msk_ts - zero_ts
    assert abs(dt_delta.total_seconds() - ts) < 1


def test_xls_parse():
    final_uniques = UniquesChecker({})
    default_tags = []

    with io.open(TEST_XLS, 'rb') as file:
        uniques = {}
        err_log = []

        default_shop_id = 123
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 1, 'is_dsbs': False},
        )

        (promo_list, _, _, _) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0

        found = False
        for pd in promo_list:
            if pd.shop_promo_id == 'disabled_gifts':
                assert pd.force_disabled is True
                assert pd.description == '1 - 7'
                # MARKETDISCOUNT-8323 - для отключенных акций надо загружать ассортимент
                assert len(pd.generic_bundle.bundles_content) == 1
                bc = pd.generic_bundle.bundles_content[0]
                assert bc.primary_item.offer_id == 'aa'
                assert bc.secondary_item.item.offer_id == 'bb'
                found = True
        assert found

    with io.open(TEST_XLS_MARKETOUT_43095, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 123
        shopsdat_table_rows = (
            {'shop_id': 1, 'warehouse_id': 1, 'datafeed_id': 1, 'is_dsbs': False},
            {'shop_id': 2, 'warehouse_id': 2, 'datafeed_id': 1, 'is_dsbs': False},
        )
        read_xls(logger, file, ShopsData(0, shopsdat_table_rows), uniques, err_log, tags=[])

        assert len(err_log) == 2
        assert err_log[0] == 'ERROR! [page-2 B4]: магазин 2 не представлен на складе 1'
        assert err_log[1] == 'ERROR! [page-2 B5]: магазин 1 не представлен на складе 2'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_EMPTY_BUDGET_EXCEPTION, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 123
        shopsdat_table_rows = (
            {'shop_id': 2249125, 'warehouse_id': 1, 'datafeed_id': 1, 'is_dsbs': False},
        )
        read_xls(logger, file, ShopsData(0, shopsdat_table_rows), uniques, err_log, tags=[])

        assert len(err_log) == 1
        assert err_log[0] == 'ERROR! [page-1 K5]: Необходимо заполнить поле лимит бюджета в акции #16371'

    with io.open(TEST_XLS_DUP, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 3232
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 1, 'is_dsbs': False},
            {'shop_id': 444, 'warehouse_id': 100, 'datafeed_id': 1, 'is_dsbs': False},
        )

        read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 1
        # первый файл содержит ошибки, но промо 'mega' запоминается, чтобы исключить дубли во всех обрабатываемых файлах
        assert err_log[0] == 'ERROR! не уникальный shop_promo_id mega (возможно в другом файле?)'

    with io.open(TEXT_XLS_URL_GENERATION, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 465852
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 111, 'is_dsbs': False},
        )

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0

        for pd in promo_list:
            if pd.shop_promo_id == 'test_url-#1':
                assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=test_url-%231'
                assert pd.landing_url == BM_BASE_URL + '/special/generic-bundle-landing?shopPromoId=test_url-%231'
                assert pd.no_landing_url is False

            elif pd.shop_promo_id == 'test_url_2':
                assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=test_url_2'
                assert pd.landing_url == 'https://landing.url'
                assert pd.no_landing_url is False

            elif pd.shop_promo_id == 'test_url_3':
                assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=test_url_3'
                assert pd.landing_url == ''
                assert pd.no_landing_url is True

            elif pd.shop_promo_id == 'test_url4':
                assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=test_url_4'
                assert pd.landing_url == ''
                assert pd.no_landing_url is True

    with io.open(TEXT_XLS_URL_GEN2, 'rb') as file:
        uniques = {}
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0

        for pd in promo_list:
            assert pd.url == BM_BASE_URL + '/special/cheapest-as-gift?shopPromoId={}'.format(quote_plus(pd.shop_promo_id))
            if pd.shop_promo_id == 'cag_url12':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-1-2-landing?shopPromoId=cag_url12'
            elif pd.shop_promo_id == 'cag_url23':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-2-3-landing?shopPromoId=cag_url23'
            elif pd.shop_promo_id == 'cag_url34':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=cag_url34'
            elif pd.shop_promo_id == 'cag_url45':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-4-5-landing?shopPromoId=cag_url45'
            elif pd.shop_promo_id == 'cag_url56':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-5-6-landing?shopPromoId=cag_url56'
            elif pd.shop_promo_id == 'cag_url67':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-6-7-landing?shopPromoId=cag_url67'
            elif pd.shop_promo_id == 'cag_url78':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-7-8-landing?shopPromoId=cag_url78'
            elif pd.shop_promo_id == 'cag_url89':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-8-9-landing?shopPromoId=cag_url89'
            elif pd.shop_promo_id == 'cag_url910':
                assert pd.landing_url == BM_BASE_URL + '/special/cheapest-as-gift-landing?shopPromoId=cag_url910'

    with io.open(TEST_XLS_GOOD, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 1
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 111, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 101, 'datafeed_id': 222, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 102, 'datafeed_id': 333, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        shopsdat = ShopsData(default_shop_id, shopsdat_table_rows)
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, shopsdat, uniques, err_log, tags=TAG_ADV, generation_ts=424242)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0

        # 1 акция, 3 записи за счёт отображения магазина на фиды
        assert len(promo_list) == 3
        assert n_promos == 1
        assert str(last_promo_date) == '2119-10-31'
        assert msg_ver == u'Тип %%подарок-за-покупку%% версия **7** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.generation_ts == 424242

        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 111
        assert pd.shop_promo_id == 'robot_tarabrina_smartfon'
        assert pd.start_date == 1569877200
        assert pd.end_date == 4728229199
        assert pd.generic_bundle
        assert pd.generic_bundle.restrict_refund is True
        assert len(pd.generic_bundle.bundles_content) == 1
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '000249.ZTE-AXON10.PRO.BL.ЭЮЯ'
        assert bc.primary_item.count == 1
        assert bc.secondary_item.item.offer_id == 'Ы.00249.ZTE-6902176031397'
        assert bc.secondary_item.item.count == 1
        assert bc.secondary_item.discount_price.value == 0
        assert bc.secondary_item.discount_price.currency == 'RUB'
        assert bc.spread_discount == 77.88
        assert make_promo_key(pd) == 'ZBACZKMKdt33eqPO8lbjRg'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert promo_list[1].feed_id == 222
        assert promo_list[2].feed_id == 333

    with io.open(TEST_XLS_WRONG, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log, tags=TAG_ADV)
        except:
            pass
        assert len(err_log) == 5
        assert err_log[0] == u'ERROR! [page-1 M6]: некорректное булево значение: "облом!"'
        assert err_log[1] == u'ERROR! [page-1 E7]: Некорректный URL http://'
        assert err_log[2] == u'ERROR! [page-1 E8]: Некорректный URL ftp://host/lalala/'
        assert err_log[3] == u'ERROR! [page-2 D4]: недопустимое значение процента распределения скидки: 107.0'
        assert err_log[4] == u'ERROR! [page-2]: пустая акция "q" магазина 0'

    with io.open(TEST_XLS_WRONG_V2, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            default_shop_id = 1
            shopsdat_table_rows = (
                {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 1, 'is_dsbs': False},
            )
            read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        except:
            pass
        assert len(err_log) == 2
        assert err_log[0] == u'ERROR! [page-1 A5]: shop_id **77** не найден в shops.dat'
        assert err_log[1] == u'ERROR! [page-1 A6]: shop_id **444** не найден в shops.dat'

    with io.open(TEST_XLS_GB_V6, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 7777, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 12345, 'warehouse_id': 818181, 'datafeed_id': 4535, 'is_dsbs': True},
        )

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log)
        # проверка на устаревшую версию
        assert len(err_log) == 1
        assert err_log[0] == 'ERROR! [page-1 D1]: Версия файла выведена из эксплуатации, обновите шаблон https://wiki.yandex-team.ru/market/pokupka/projects/promo-actions/'

        # но при задании тега - файл обработается
        err_log = []
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2029-08-01'
        assert msg_ver == u'Тип %%подарок-за-покупку%% версия **6** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 7777
        assert pd.shop_promo_id == 'gb_v6'
        assert pd.url == BM_BASE_URL + '/promo/1'
        assert pd.landing_url == BM_BASE_URL + '/special/promo/landing/1'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is True

        assert len(pd.generic_bundle.bundles_content) == 3
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '6_осн1'
        assert bc.primary_item.count == 1
        assert bc.secondary_item.item.offer_id == '6_доп2'
        assert bc.secondary_item.item.count == 1
        assert bc.secondary_item.discount_price.value == 0
        assert bc.secondary_item.discount_price.currency == 'RUB'
        assert bc.spread_discount == 5.5
        bc = pd.generic_bundle.bundles_content[1]
        assert bc.primary_item.offer_id == '6_осн1-A'
        assert bc.secondary_item.item.offer_id == '6_доп2-A'
        assert bc.secondary_item.discount_price.value == 100
        assert not bc.HasField('spread_discount')
        bc = pd.generic_bundle.bundles_content[2]
        assert bc.primary_item.offer_id == '6_осн3'
        assert bc.secondary_item.item.offer_id == '6_доп3'
        assert bc.spread_discount == 42

        assert pd.generic_bundle.restrict_refund is True
        assert pd.generic_bundle.spread_discount == 0.0  # поле не заполняется в версии 4
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert make_promo_key(pd) == '2WIYwzR1Ks4VTlLk2DjpIg'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 7777
        assert ids[0].offer_id == '6_осн1'
        assert ids[1].feed_id == 7777
        assert ids[1].offer_id == '6_осн1-A'
        assert ids[2].feed_id == 7777
        assert ids[2].offer_id == '6_осн3'

        pd = promo_list[1]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 4535
        assert pd.shop_promo_id == 'gb_v6_dsbs'
        assert pd.anaplan_promo_id == 'anaplan_dsbs'
        assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=gb_v6_dsbs'
        assert pd.landing_url == BM_BASE_URL + '/special/generic-bundle-landing?shopPromoId=gb_v6_dsbs'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is False

        assert len(pd.generic_bundle.bundles_content) == 1
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == 'dsbs1'
        assert bc.secondary_item.item.offer_id == 'dsbs2'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 1
        assert ids[0].feed_id == 4535
        assert ids[0].offer_id == 'dsbs1'

    with io.open(TEST_XLS_GB_V7, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 7777, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 12345, 'warehouse_id': 818181, 'datafeed_id': 4535, 'is_dsbs': True},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2029-08-01'
        assert msg_ver == u'Тип %%подарок-за-покупку%% версия **7** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 7777
        assert pd.shop_promo_id == 'gb_v7'
        assert pd.url == BM_BASE_URL + '/promo/1'
        assert pd.landing_url == BM_BASE_URL + '/special/promo/landing/1'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is True
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'yandex_cashback'

        assert len(pd.generic_bundle.bundles_content) == 3
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '7_осн1'
        assert bc.primary_item.count == 1
        assert bc.secondary_item.item.offer_id == '7_доп2'
        assert bc.secondary_item.item.count == 1
        assert bc.secondary_item.discount_price.value == 0
        assert bc.secondary_item.discount_price.currency == 'RUB'
        assert bc.spread_discount == 5.5
        bc = pd.generic_bundle.bundles_content[1]
        assert bc.primary_item.offer_id == '7_осн1-A'
        assert bc.secondary_item.item.offer_id == '7_доп2-A'
        assert bc.secondary_item.discount_price.value == 100
        assert not bc.HasField('spread_discount')
        bc = pd.generic_bundle.bundles_content[2]
        assert bc.primary_item.offer_id == '7_осн3'
        assert bc.secondary_item.item.offer_id == '7_доп3'
        assert bc.spread_discount == 42

        assert pd.generic_bundle.restrict_refund is True
        assert pd.generic_bundle.spread_discount == 0.0  # поле не заполняется в версии 4
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert make_promo_key(pd) == 'WmtmSYZJ-wmrSDVpH2hhlA'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 7777
        assert ids[0].offer_id == '7_осн1'
        assert ids[1].feed_id == 7777
        assert ids[1].offer_id == '7_осн1-A'
        assert ids[2].feed_id == 7777
        assert ids[2].offer_id == '7_осн3'

        pd = promo_list[1]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 4535
        assert pd.shop_promo_id == 'gb_v7_dsbs'
        assert pd.anaplan_promo_id == 'anaplan_dsbs'
        assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=gb_v7_dsbs'
        assert pd.landing_url == BM_BASE_URL + '/special/generic-bundle-landing?shopPromoId=gb_v7_dsbs'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is False

        assert len(pd.generic_bundle.bundles_content) == 1
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '7_dsbs1'
        assert bc.secondary_item.item.offer_id == '7_dsbs2'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 1
        assert ids[0].feed_id == 4535
        assert ids[0].offer_id == '7_dsbs1'

    with io.open(TEST_XLS_GB_V8, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 7777, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 12345, 'warehouse_id': 818181, 'datafeed_id': 4535, 'is_dsbs': True},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2029-08-01'
        assert msg_ver == u'Тип %%подарок-за-покупку%% версия **8** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 7777
        assert pd.shop_promo_id == 'gb_v8'
        assert pd.anaplan_promo_id == 'gb_v8'
        assert pd.url == BM_BASE_URL + '/promo/1'
        assert pd.landing_url == BM_BASE_URL + '/special/promo/landing/1'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is True
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'yandex_cashback'

        assert len(pd.generic_bundle.bundles_content) == 3
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '8_осн1'
        assert bc.primary_item.count == 1
        assert bc.secondary_item.item.offer_id == '8_доп2'
        assert bc.secondary_item.item.count == 1
        assert bc.secondary_item.discount_price.value == 0
        assert bc.secondary_item.discount_price.currency == 'RUB'
        assert bc.spread_discount == 5.5
        bc = pd.generic_bundle.bundles_content[1]
        assert bc.primary_item.offer_id == '8_осн1-A'
        assert bc.secondary_item.item.offer_id == '8_доп2-A'
        assert bc.secondary_item.discount_price.value == 100
        assert not bc.HasField('spread_discount')
        bc = pd.generic_bundle.bundles_content[2]
        assert bc.primary_item.offer_id == '8_осн3'
        assert bc.secondary_item.item.offer_id == '8_доп3'
        assert bc.spread_discount == 42

        assert pd.generic_bundle.restrict_refund is True
        assert pd.generic_bundle.spread_discount == 0.0  # поле не заполняется в версии 4
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert make_promo_key(pd) == 'YmAweMcphLUSwKrI8Mb1Sw'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 7777
        assert ids[0].offer_id == '8_осн1'
        assert ids[1].feed_id == 7777
        assert ids[1].offer_id == '8_осн1-A'
        assert ids[2].feed_id == 7777
        assert ids[2].offer_id == '8_осн3'

        pd = promo_list[1]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 4535
        assert pd.shop_promo_id == 'gb_v8_dsbs'
        assert pd.anaplan_promo_id == 'gb_v8_dsbs'
        assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=gb_v8_dsbs'
        assert pd.landing_url == BM_BASE_URL + '/special/generic-bundle-landing?shopPromoId=gb_v8_dsbs'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is False

        assert len(pd.generic_bundle.bundles_content) == 1
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '8_dsbs1'
        assert bc.secondary_item.item.offer_id == '8_dsbs2'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 1
        assert ids[0].feed_id == 4535
        assert ids[0].offer_id == '8_dsbs1'

    with io.open(TEST_XLS_GB_V9, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 7777, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 12345, 'warehouse_id': 818181, 'datafeed_id': 4535, 'is_dsbs': True},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=[])
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2029-08-01'
        assert msg_ver == u'Тип %%подарок-за-покупку%% версия **9**'

        pd = promo_list[0]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 7777
        assert pd.shop_promo_id == 'gb_v9'
        assert pd.anaplan_promo_id == 'gb_v9'
        assert pd.url == BM_BASE_URL + '/promo/1'
        assert pd.landing_url == BM_BASE_URL + '/special/promo/landing/1'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is True
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'yandex_cashback'
        assert pd.parent_promo_id == 'parent_promo'

        assert len(pd.generic_bundle.bundles_content) == 3
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '9_осн1'
        assert bc.primary_item.count == 1
        assert bc.secondary_item.item.offer_id == '9_доп2'
        assert bc.secondary_item.item.count == 1
        assert bc.secondary_item.discount_price.value == 0
        assert bc.secondary_item.discount_price.currency == 'RUB'
        assert bc.spread_discount == 5.5
        bc = pd.generic_bundle.bundles_content[1]
        assert bc.primary_item.offer_id == '9_осн1-A'
        assert bc.secondary_item.item.offer_id == '9_доп2-A'
        assert bc.secondary_item.discount_price.value == 100
        assert not bc.HasField('spread_discount')
        bc = pd.generic_bundle.bundles_content[2]
        assert bc.primary_item.offer_id == '9_осн3'
        assert bc.secondary_item.item.offer_id == '9_доп3'
        assert bc.spread_discount == 42

        assert pd.generic_bundle.restrict_refund is True
        assert pd.generic_bundle.spread_discount == 0.0  # поле не заполняется в версии 4
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert make_promo_key(pd) == 'oCEBznmbPwAq-zDJbT55kQ'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 7777
        assert ids[0].offer_id == '9_осн1'
        assert ids[1].feed_id == 7777
        assert ids[1].offer_id == '9_осн1-A'
        assert ids[2].feed_id == 7777
        assert ids[2].offer_id == '9_осн3'

        pd = promo_list[1]
        assert pd.type == PromoType.GENERIC_BUNDLE
        assert pd.feed_id == 4535
        assert pd.shop_promo_id == 'gb_v9_dsbs'
        assert pd.anaplan_promo_id == 'gb_v9_dsbs'
        assert pd.url == BM_BASE_URL + '/special/generic-bundle?shopPromoId=gb_v9_dsbs'
        assert pd.landing_url == BM_BASE_URL + '/special/generic-bundle-landing?shopPromoId=gb_v9_dsbs'
        assert pd.no_landing_url is False
        assert pd.start_date == 1561928400
        assert pd.end_date == 1880291879
        assert pd.disabled_by_default is False
        assert pd.parent_promo_id == ''

        assert len(pd.generic_bundle.bundles_content) == 1
        bc = pd.generic_bundle.bundles_content[0]
        assert bc.primary_item.offer_id == '9_dsbs1'
        assert bc.secondary_item.item.offer_id == '9_dsbs2'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 1
        assert ids[0].feed_id == 4535
        assert ids[0].offer_id == '9_dsbs1'

    with io.open(TEST_XLS_GB_V8_INVALID, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 7777, 'is_dsbs': False},
        )

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 2
        assert err_log[0] == u'ERROR! [page-2 D4]: недопустимое значение процента распределения скидки: 0.0'
        assert err_log[1] == u'ERROR! [page-2]: пустая акция "gb_v8_invalid" магазина 465852'

    with io.open(TEST_XLS_CG_5, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 101, 'datafeed_id': 10465854, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2120-07-05'
        assert msg_ver == u'Тип %%самый-дешевый-в-подарок%% версия **5** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        for pd in promo_list:
            if pd.shop_promo_id == '5_robot_fmcg_krasota':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.shop_promo_id == '5_robot_fmcg_krasota'
                assert pd.url == BM_BASE_URL + '/special/cheapest-as-gift?shopPromoId=5_robot_fmcg_krasota'
                assert pd.landing_url == BM_BASE_URL + '/special/krasota'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is True
                assert pd.start_date == 1591650000
                assert pd.end_date == 4749656399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 2
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465852
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '500160.801820'
                assert pd.cheapest_as_gift.feed_offer_ids[1].feed_id == 10465853
                assert pd.cheapest_as_gift.feed_offer_ids[1].offer_id == '5000331.СР2'
                assert pd.cheapest_as_gift.count == 3
                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/krasota'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 3 любых товара по цене 2'
                assert make_promo_key(pd) == 'yLVxRuRj0Mp0j3oFwNdoPQ'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 2
                assert ids[0].feed_id == 10465852
                assert ids[0].offer_id == '500160.801820'
                assert ids[1].feed_id == 10465853
                assert ids[1].offer_id == '5000331.СР2'

            elif pd.shop_promo_id == '5_robot_fmcg_krasota2':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.shop_promo_id == '5_robot_fmcg_krasota2'
                assert pd.url == BM_BASE_URL + '/special/dvaravnotri-tovarydlyakrasoty2'
                assert pd.landing_url ==BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=5_robot_fmcg_krasota2'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is False
                assert pd.start_date == 1578603600
                assert pd.end_date == 4747496399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 1
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465854
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '5331.22222222'
                assert pd.cheapest_as_gift.count == 4
                assert len(pd.restrictions.restricted_promo_types) == 1
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=5_robot_fmcg_krasota2'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 4 любых товара по цене 3'
                assert make_promo_key(pd) == '_IsKGhb04ynjecVic95fSg'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 10465854
                assert ids[0].offer_id == '5331.22222222'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_CG_6, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 101, 'datafeed_id': 10465854, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2120-07-05'
        assert msg_ver == u'Тип %%самый-дешевый-в-подарок%% версия **6** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        for pd in promo_list:
            if pd.shop_promo_id == '6_robot_fmcg_krasota':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.shop_promo_id == '6_robot_fmcg_krasota'
                assert pd.url == BM_BASE_URL + '/special/cheapest-as-gift?shopPromoId=6_robot_fmcg_krasota'
                assert pd.landing_url == BM_BASE_URL + '/special/krasota'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is True
                assert pd.start_date == 1591650000
                assert pd.end_date == 4749656399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 2
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465852
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '600160.801820'
                assert pd.cheapest_as_gift.feed_offer_ids[1].feed_id == 10465853
                assert pd.cheapest_as_gift.feed_offer_ids[1].offer_id == '6000331.СР2'
                assert pd.cheapest_as_gift.count == 3
                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/krasota'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 3 любых товара по цене 2'
                assert make_promo_key(pd) == 'vPskL_c94--o9YbCazvQ6g'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 2
                assert ids[0].feed_id == 10465852
                assert ids[0].offer_id == '600160.801820'
                assert ids[1].feed_id == 10465853
                assert ids[1].offer_id == '6000331.СР2'

            elif pd.shop_promo_id == '6_robot_fmcg_krasota2':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.shop_promo_id == '6_robot_fmcg_krasota2'
                assert pd.url == BM_BASE_URL + '/special/dvaravnotri-tovarydlyakrasoty2'
                assert pd.landing_url ==BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=6_robot_fmcg_krasota2'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is False
                assert pd.start_date == 1578603600
                assert pd.end_date == 4747496399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 1
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465854
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '6331.22222222'
                assert pd.cheapest_as_gift.count == 4
                assert len(pd.restrictions.restricted_promo_types) == 1
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=6_robot_fmcg_krasota2'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 4 любых товара по цене 3'
                assert make_promo_key(pd) == '9_r7eJ-VrD1QK6rEWPCtjg'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 1
                assert len(pd.restrictions.predicates[0].perks) == 1
                assert pd.restrictions.predicates[0].perks[0] == 'yandex_cashback'

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 10465854
                assert ids[0].offer_id == '6331.22222222'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_CG_7, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 101, 'datafeed_id': 10465854, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2120-07-05'
        assert msg_ver == u'Тип %%самый-дешевый-в-подарок%% версия **7** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        for pd in promo_list:
            if pd.shop_promo_id == '7_robot_fmcg_krasota':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.anaplan_promo_id == '7_robot_fmcg_krasota'
                assert pd.shop_promo_id == '7_robot_fmcg_krasota'
                assert pd.url == BM_BASE_URL + '/special/cheapest-as-gift?shopPromoId=7_robot_fmcg_krasota'
                assert pd.landing_url == BM_BASE_URL + '/special/krasota'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is True
                assert pd.start_date == 1591650000
                assert pd.end_date == 4749656399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 2
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465852
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '700160.801820'
                assert pd.cheapest_as_gift.feed_offer_ids[1].feed_id == 10465853
                assert pd.cheapest_as_gift.feed_offer_ids[1].offer_id == '7000331.СР2'
                assert pd.cheapest_as_gift.count == 3
                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/krasota'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 3 любых товара по цене 2'
                assert make_promo_key(pd) == 'FSeWMzMkDYR0kmBSIbypWw'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 2
                assert ids[0].feed_id == 10465852
                assert ids[0].offer_id == '700160.801820'
                assert ids[1].feed_id == 10465853
                assert ids[1].offer_id == '7000331.СР2'

            elif pd.shop_promo_id == '7_robot_fmcg_krasota2':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.anaplan_promo_id == '7_robot_fmcg_krasota2'
                assert pd.shop_promo_id == '7_robot_fmcg_krasota2'
                assert pd.url == BM_BASE_URL + '/special/dvaravnotri-tovarydlyakrasoty2'
                assert pd.landing_url ==BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=7_robot_fmcg_krasota2'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is False
                assert pd.start_date == 1578603600
                assert pd.end_date == 4747496399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 1
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465854
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '7331.22222222'
                assert pd.cheapest_as_gift.count == 4
                assert len(pd.restrictions.restricted_promo_types) == 1
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=7_robot_fmcg_krasota2'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 4 любых товара по цене 3'
                assert make_promo_key(pd) == 'InRrXZrbqCdG5F_Cs2QJow'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 1
                assert len(pd.restrictions.predicates[0].perks) == 1
                assert pd.restrictions.predicates[0].perks[0] == 'yandex_cashback'

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 10465854
                assert ids[0].offer_id == '7331.22222222'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_CG_8, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
            {'shop_id': 241241, 'warehouse_id': 101, 'datafeed_id': 10465854, 'is_dsbs': False},
            {'shop_id': 333777, 'warehouse_id': 100, 'datafeed_id': 3337771, 'is_dsbs': False},
            {'shop_id': 333777, 'warehouse_id': 101, 'datafeed_id': 3337772, 'is_dsbs': False},
            {'shop_id': 333777, 'warehouse_id': 102, 'datafeed_id': 3337773, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 3
        assert n_promos == 3
        assert str(last_promo_date) == '2120-07-05'
        assert msg_ver == u'Тип %%самый-дешевый-в-подарок%% версия **8**'

        for pd in promo_list:
            if pd.shop_promo_id == '8_robot_fmcg_krasota':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.anaplan_promo_id == '8_robot_fmcg_krasota'
                assert pd.shop_promo_id == '8_robot_fmcg_krasota'
                assert pd.url == BM_BASE_URL + '/special/cheapest-as-gift?shopPromoId=8_robot_fmcg_krasota'
                assert pd.landing_url == BM_BASE_URL + '/special/krasota'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is True
                assert pd.start_date == 1591650000
                assert pd.end_date == 4749656399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 2
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465852
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '800160.801820'
                assert pd.cheapest_as_gift.feed_offer_ids[1].feed_id == 10465853
                assert pd.cheapest_as_gift.feed_offer_ids[1].offer_id == '8000331.СР2'
                assert pd.cheapest_as_gift.count == 3
                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/krasota'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 3 любых товара по цене 2'
                assert make_promo_key(pd) == 'i8omD08EVpCbzGm0iSDKeA'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert pd.parent_promo_id == 'parent_promo_1'

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 2
                assert ids[0].feed_id == 10465852
                assert ids[0].offer_id == '800160.801820'
                assert ids[1].feed_id == 10465853
                assert ids[1].offer_id == '8000331.СР2'

            elif pd.shop_promo_id == '8_robot_fmcg_krasota2':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.anaplan_promo_id == '8_robot_fmcg_krasota2'
                assert pd.shop_promo_id == '8_robot_fmcg_krasota2'
                assert pd.url == BM_BASE_URL + '/special/dvaravnotri-tovarydlyakrasoty2'
                assert pd.landing_url ==BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=8_robot_fmcg_krasota2'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is False
                assert pd.start_date == 1578603600
                assert pd.end_date == 4747496399
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 1
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 10465854
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '8331.22222222'
                assert pd.cheapest_as_gift.count == 4
                assert len(pd.restrictions.restricted_promo_types) == 1
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                assert pd.cheapest_as_gift.promo_url == BM_BASE_URL + '/special/cheapest-as-gift-3-4-landing?shopPromoId=8_robot_fmcg_krasota2'
                assert cast_value_to_unicode(pd.cheapest_as_gift.link_text) == u'Заходите и выбирайте 4 любых товара по цене 3'
                assert make_promo_key(pd) == 'QZAWlHx4HkP4G4ZcGVEFIg'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 1
                assert len(pd.restrictions.predicates[0].perks) == 1
                assert pd.restrictions.predicates[0].perks[0] == 'yandex_cashback'
                assert pd.parent_promo_id == 'parent_promo_2'

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 10465854
                assert ids[0].offer_id == '8331.22222222'

            elif pd.shop_promo_id == '8_robot_cg_no_wh':
                assert pd.type == PromoType.CHEAPEST_AS_GIFT
                assert pd.feed_id == 0
                assert pd.anaplan_promo_id == '8_robot_cg_no_wh'
                assert pd.shop_promo_id == '8_robot_cg_no_wh'
                assert pd.url == BM_BASE_URL + '/special/cheapest-as-gift?shopPromoId=8_robot_cg_no_wh'
                assert pd.landing_url ==BM_BASE_URL + '/special/cheapest-as-gift-2-3-landing?shopPromoId=8_robot_cg_no_wh'
                assert pd.no_landing_url is False
                assert pd.disabled_by_default is False
                assert pd.start_date == 1577826000
                assert pd.end_date == 4747669199
                assert len(pd.cheapest_as_gift.feed_offer_ids) == 3
                assert pd.cheapest_as_gift.feed_offer_ids[0].feed_id == 3337771
                assert pd.cheapest_as_gift.feed_offer_ids[0].offer_id == '8333777.SKU'
                assert pd.cheapest_as_gift.feed_offer_ids[1].feed_id == 3337772
                assert pd.cheapest_as_gift.feed_offer_ids[1].offer_id == '8333777.SKU'
                assert pd.cheapest_as_gift.feed_offer_ids[2].feed_id == 3337773
                assert pd.cheapest_as_gift.feed_offer_ids[2].offer_id == '8333777.SKU'
                assert pd.cheapest_as_gift.count == 3
                assert len(pd.restrictions.restricted_promo_types) == 0
                assert make_promo_key(pd) == '-cBxS9MRdapswJxcsxXkoA'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 3
                assert ids[0].feed_id == 3337771
                assert ids[0].offer_id == '8333777.SKU'
                assert ids[1].feed_id == 3337772
                assert ids[1].offer_id == '8333777.SKU'
                assert ids[2].feed_id == 3337773
                assert ids[2].offer_id == '8333777.SKU'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_UNIQ_1, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 444, 'warehouse_id': 100, 'datafeed_id': 321, 'is_dsbs': False},
            {'shop_id': 444, 'warehouse_id': 101, 'datafeed_id': 654, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 12345, 'is_dsbs': False},
        )
        source_reference = 'http://source_reference.ru/duplicates'

        read_xls(logger, file,  ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, source_reference=source_reference, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 B4]: не уникальный offer_id "основной_сску" в акции "test_444_dup" магазина 444, '\
            'с 2000-01-01 00:00:00+03:00 по 2120-12-31 23:59:59+03:00 http://source_reference.ru/duplicates, '\
            'ранее в акции "mega_dup" магазина 444, с 2019-07-02 00:00:00+03:00 по 2029-08-31 23:59:59+03:00'

    with io.open(TEST_XLS_UNIQ_2, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 444, 'warehouse_id': 100, 'datafeed_id': 321, 'is_dsbs': False},
            {'shop_id': 444, 'warehouse_id': 101, 'datafeed_id': 654, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            {'shop_id': 465852, 'warehouse_id': 101, 'datafeed_id': 12345, 'is_dsbs': False},
        )

        read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 C3]: не уникальный offer_id "600160.801820" в акции "cag-2", '\
            'с 2019-11-01 00:00:00+03:00 по 2119-12-31 23:59:59+03:00, ранее в акции "6_robot_fmcg_krasota", '\
            'с 2020-06-09 00:00:00+03:00 по 2120-07-05 23:59:59+03:00'

    with io.open(TEXT_XLS_SEVERAL_BUNDLES_IN_PROMO, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 1
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 111, 'is_dsbs': False},
        )

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 1
        assert n_promos == 1
        assert str(last_promo_date) == '2121-10-31'

        expected_bundles = [
            {
                'primary': '000286.ZTE-AXON10.PRO.BL.ЭЮЯ',
                'secondary': 'Ы.00443.ZTE-6902176031397'
            },
            {
                'primary': '000234.ZTE-AXON10.PRO.BL.ЭЮЯ',
                'secondary': 'Ы.00459.ZTE-6902176031397'
            },
            {
                'primary': '000345.ZTE-AXON10.PRO.BL.ЭЮЯ',
                'secondary': 'Ы.00443.ZTE-6902176031397'
            }
        ]

        pd = promo_list[0]
        assert pd.feed_id == 111
        assert pd.shop_promo_id == 'smartphone_and_case'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.generic_bundle.bundles_content) == 3
        for (actual_bundle, expected_bundle) in zip(pd.generic_bundle.bundles_content, expected_bundles):
            assert actual_bundle.primary_item.offer_id == expected_bundle['primary']
            assert actual_bundle.primary_item.count == 1
            assert actual_bundle.secondary_item.item.offer_id == expected_bundle['secondary']
            assert actual_bundle.secondary_item.item.count == 1
            assert actual_bundle.secondary_item.discount_price.value == 0
            assert actual_bundle.secondary_item.discount_price.currency == 'RUB'

    with io.open(TEST_XLS_BLUE_FLASH_V5_BAD, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 10264169
        feed_id = 7788
        feed_id2 = 222
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': 555, 'warehouse_id': 100, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)

        assert len(err_log) == 5
        assert err_log[0] == u'ERROR! [page-2 F3]: Цена Бюджет задана в некорректном формате: "10 коп"'
        assert err_log[1] == u'ERROR! [page-2 D4]: Цена Стоимость задана в некорректном формате: "150 руб"'
        assert err_log[2] == u'ERROR! [page-2 D5]: empty value None for key price.value'
        assert err_log[3] == u'ERROR! [page-2 D6]: Цена Стоимость задана нулевой'
        assert err_log[4] == u'ERROR! [page-2]: пустая акция "blue-flash-v5-bad"'

    with io.open(TEST_XLS_BLUE_FLASH_V6, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 10264169
        feed_id = 7788
        feed_id2 = 222
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': 555, 'warehouse_id': 100, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 1
        assert n_promos == 1
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%флэш-скидка%% версия **6** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.type == PromoType.BLUE_FLASH
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'blue-flash-v6'
        assert pd.anaplan_promo_id == 'Анаплан ИД'
        assert pd.url == BM_BASE_URL + '/deals'
        assert pd.landing_url == BM_BASE_URL + '/special/flash'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572610855
        __check_timestamp(pd.start_date, '11/01/2019 15:20:55+03:00')
        assert pd.end_date == 4796632804
        __check_timestamp(pd.end_date, '12/31/2121 17:00:04+03:00')
        assert make_promo_key(pd) == 'RqrCd4N24M2a1eZbtBp3Xg'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0

        assert len(pd.restrictions.restricted_promo_types) == 0
        assert len(pd.blue_flash.items) == 3

        assert pd.blue_flash.items[0].price.value == 10000
        assert pd.blue_flash.items[0].price.currency == 'RUB'
        assert pd.blue_flash.items[0].offer.feed_id == feed_id
        assert pd.blue_flash.items[0].offer.offer_id == 'bf-6-1'

        assert pd.blue_flash.items[1].price.value == 15000
        assert pd.blue_flash.items[1].price.currency == 'RUB'
        assert pd.blue_flash.items[1].offer.feed_id == feed_id
        assert pd.blue_flash.items[1].offer.offer_id == 'bf-6-2'

        assert pd.blue_flash.items[2].price.value == 500000
        assert pd.blue_flash.items[2].price.currency == 'RUB'
        assert pd.blue_flash.items[2].offer.feed_id == feed_id2
        assert pd.blue_flash.items[2].offer.offer_id == 'bf-6-3'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == feed_id
        assert ids[0].offer_id == 'bf-6-1'
        assert ids[1].feed_id == feed_id
        assert ids[1].offer_id == 'bf-6-2'
        assert ids[2].feed_id == feed_id2
        assert ids[2].offer_id == 'bf-6-3'

    with io.open(TEST_XLS_BLUE_FLASH_V7, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 10264169
        feed_id = 7788
        feed_id2 = 222
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': 555, 'warehouse_id': 100, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 1
        assert n_promos == 1
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%флэш-скидка%% версия **7** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.type == PromoType.BLUE_FLASH
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'blue-flash-v7'
        assert pd.anaplan_promo_id == 'blue-flash-v7'
        assert pd.url == BM_BASE_URL + '/deals'
        assert pd.landing_url == BM_BASE_URL + '/special/flash'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572610855
        __check_timestamp(pd.start_date, '11/01/2019 15:20:55+03:00')
        assert pd.end_date == 4796632804
        __check_timestamp(pd.end_date, '12/31/2121 17:00:04+03:00')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0

        assert len(pd.restrictions.restricted_promo_types) == 0
        assert len(pd.blue_flash.items) == 3

        assert pd.blue_flash.items[0].price.value == 10000
        assert pd.blue_flash.items[0].price.currency == 'RUB'
        assert pd.blue_flash.items[0].offer.feed_id == feed_id
        assert pd.blue_flash.items[0].offer.offer_id == 'bf-7-1'

        assert pd.blue_flash.items[1].price.value == 15000
        assert pd.blue_flash.items[1].price.currency == 'RUB'
        assert pd.blue_flash.items[1].offer.feed_id == feed_id
        assert pd.blue_flash.items[1].offer.offer_id == 'bf-7-2'

        assert pd.blue_flash.items[2].price.value == 500000
        assert pd.blue_flash.items[2].price.currency == 'RUB'
        assert pd.blue_flash.items[2].offer.feed_id == feed_id2
        assert pd.blue_flash.items[2].offer.offer_id == 'bf-7-3'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == feed_id
        assert ids[0].offer_id == 'bf-7-1'
        assert ids[1].feed_id == feed_id
        assert ids[1].offer_id == 'bf-7-2'
        assert ids[2].feed_id == feed_id2
        assert ids[2].offer_id == 'bf-7-3'

    with io.open(TEST_XLS_BLUE_FLASH_V8, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 10264169
        feed_id = 7788
        feed_id2 = 222
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': 555, 'warehouse_id': 100, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=[])
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 1
        assert n_promos == 1
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%флэш-скидка%% версия **8**'

        pd = promo_list[0]
        assert pd.type == PromoType.BLUE_FLASH
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'blue-flash-v8'
        assert pd.anaplan_promo_id == 'blue-flash-v8'
        assert pd.url == BM_BASE_URL + '/deals'
        assert pd.landing_url == BM_BASE_URL + '/special/flash'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572610855
        __check_timestamp(pd.start_date, '11/01/2019 15:20:55+03:00')
        assert pd.end_date == 4796632804
        __check_timestamp(pd.end_date, '12/31/2121 17:00:04+03:00')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert pd.parent_promo_id == 'parent_promo'

        assert len(pd.restrictions.predicates) == 0

        assert len(pd.restrictions.restricted_promo_types) == 0
        assert len(pd.blue_flash.items) == 3

        assert pd.blue_flash.items[0].price.value == 10000
        assert pd.blue_flash.items[0].price.currency == 'RUB'
        assert pd.blue_flash.items[0].offer.feed_id == feed_id
        assert pd.blue_flash.items[0].offer.offer_id == 'bf-8-1'

        assert pd.blue_flash.items[1].price.value == 15000
        assert pd.blue_flash.items[1].price.currency == 'RUB'
        assert pd.blue_flash.items[1].offer.feed_id == feed_id
        assert pd.blue_flash.items[1].offer.offer_id == 'bf-8-2'

        assert pd.blue_flash.items[2].price.value == 500000
        assert pd.blue_flash.items[2].price.currency == 'RUB'
        assert pd.blue_flash.items[2].offer.feed_id == feed_id2
        assert pd.blue_flash.items[2].offer.offer_id == 'bf-8-3'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == feed_id
        assert ids[0].offer_id == 'bf-8-1'
        assert ids[1].feed_id == feed_id
        assert ids[1].offer_id == 'bf-8-2'
        assert ids[2].feed_id == feed_id2
        assert ids[2].offer_id == 'bf-8-3'

    # В файле на первой странице не указан склад. На второй странице указан один оффер.
    # Так как у магазина два склада, то в акцию добавляем исходный оффер для обоих складов.
    with io.open(TEST_XLS_BLUE_FLASH_V7_NULL_WAREHOUSE, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 10264179
        feed_id1 = 778899
        feed_id2 = 222333
        warehouse_id1 = 100
        warehouse_id2 = 101
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': warehouse_id1, 'datafeed_id': feed_id1, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': warehouse_id2, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 1
        assert n_promos == 1
        assert str(last_promo_date) == '2121-12-21'
        assert msg_ver == u'Тип %%флэш-скидка%% версия **7** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[0]
        assert pd.type == PromoType.BLUE_FLASH
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'blue-flash-v7-null-wh'
        assert pd.anaplan_promo_id == 'blue-flash-v7-null-wh'
        assert pd.url == BM_BASE_URL + '/deals'
        assert pd.landing_url == BM_BASE_URL + '/special/flash'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572956455
        __check_timestamp(pd.start_date, '11/05/2019 15:20:55+03:00')
        assert pd.end_date == 4795768804
        __check_timestamp(pd.end_date, '12/21/2121 17:00:04+03:00')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0

        assert len(pd.restrictions.restricted_promo_types) == 0
        assert len(pd.blue_flash.items) == 2

        assert pd.blue_flash.items[0].price.value == 11000
        assert pd.blue_flash.items[0].price.currency == 'RUB'
        assert pd.blue_flash.items[0].offer.feed_id == feed_id1
        assert pd.blue_flash.items[0].offer.offer_id == 'bf-7-1-null-wh'

        assert pd.blue_flash.items[1].price.value == 11000
        assert pd.blue_flash.items[1].price.currency == 'RUB'
        assert pd.blue_flash.items[1].offer.feed_id == feed_id2
        assert pd.blue_flash.items[1].offer.offer_id == 'bf-7-1-null-wh'

    with io.open(TEST_XLS_NOT_XLS, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log, tags=default_tags)
        except:
            pass
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! []: File is not a zip file'

    with io.open(TEST_XLS_BLUE_SET_BAD_V5, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            default_shop_id = ANY_SHOP
            shopsdat_table_rows = (
                {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 1, 'is_dsbs': False},
            )
            read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), dict(), err_log, tags=TAG_ADV, custom_params=[{'promo_type': 'default', 'max_items_per_promo': 1}])
        except:
            pass
        assert len(err_log) == 9
        assert err_log[0] == u'ERROR! [page-1 E7]: Некорректный URL not_a_url'
        assert err_log[1] == u'ERROR! [page-2 F3]: недопустимое значение процента скидки: -1.0'
        assert err_log[2] == u'ERROR! [page-2 G4]: задана скидка, но не указан товар номер 3'
        assert err_log[3] == u'ERROR! [page-2 C6]: не уникальный offer_id "000109.88611.1" в акции "kasumov_001" магазина 465852, '\
            'с 2019-07-01 00:00:00+03:00 по 2121-07-31 23:59:59+03:00, ранее в акции "kasumov_001" магазина 465852, '\
            'с 2019-07-01 00:00:00+03:00 по 2121-07-31 23:59:59+03:00'
        assert err_log[4] == u'ERROR! [page-2 H8]: Превышено максимальное количество (1) элементов в акции "test_uniq_new" магазина 465852'
        assert err_log[5] == u'ERROR! [page-2 B9]: не уникальный offer_id "A" в акции "test_uniq_new" магазина 465852, '\
            'с 2019-07-01 00:00:00+03:00 по 2121-07-31 23:59:59+03:00, ранее в акции "test_uniq_new" магазина 465852, '\
            'с 2019-07-01 00:00:00+03:00 по 2121-07-31 23:59:59+03:00'
        assert err_log[6] == u'ERROR! [page-2 H10]: Превышено максимальное количество (1) элементов в акции "test_uniq_new" магазина 465852'
        assert err_log[7] == u'ERROR! [page-2 B11]: не уникальный offer_id "Z" в акции "test_uniq_new" магазина 465852, '\
            'с 2019-07-01 00:00:00+03:00 по 2121-07-31 23:59:59+03:00, ранее в акции "test_uniq_new" магазина 465852, '\
            'с 2019-07-01 00:00:00+03:00 по 2121-07-31 23:59:59+03:00'
        assert err_log[8] == u'ERROR! [page-2 G12]: задана скидка, но не указан товар номер 3'

    with io.open(TEST_XLS_BLUE_SET_BAD_V8, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            default_shop_id = ANY_SHOP
            shopsdat_table_rows = (
                {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': 1, 'is_dsbs': False},
            )
            (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=[])
        except:
            pass
        assert len(promo_list) == 0
        assert n_promos == 0
        assert len(err_log) == 9
        assert err_log[0] == 'ERROR! [page-2 I11]: Комплект может быть либо связанным, либо вариативным (с выбором)'
        assert err_log[1] == 'ERROR! [page-2 I12]: Комплект может быть либо связанным, либо вариативным (с выбором)'
        assert err_log[2] == 'ERROR! [page-2 I13]: Комплект может быть либо связанным, либо вариативным (с выбором)'
        assert err_log[3] == 'ERROR! [page-2]: Присутствует дубликат оффера [\'11\', \'12\'] в вариациях c главным товаром \'1\' в акции  \'bad_1_v8\'.'
        assert err_log[4] == 'ERROR! [page-2]: Количество элементов оффера [\'31\', \'32\'] с главным товаром \'3\' в акции \'bad_1_v8\' отличается от 1.'
        assert err_log[5] == 'ERROR! [page-2]: Не достаточно офферов в комплектах вариаций c главным товаром \'2\' в акции \'bad_1_v8\'. '\
                            'Для корректности данных нужно составить все возможные вариации из списока использованных вторичных товаров: [\'21\', \'22\', \'23\', \'24\']. '\
                            'Нужное кол-во вариаций: 6. Текущее количество вариаций: 2. '\
                            'Текущие оффера: [\'21\', \'22\'] [\'23\', \'24\']'
        assert err_log[6] == 'ERROR! [page-2]: дублированный дополнительный товар \'61\' у оффера с главным товаром \'6\' в акции \'bad_3_v8\''
        assert err_log[7] == 'ERROR! [page-2]: Товар \'51\' является замороженным в оферах [[\'51\', \'52\'], [\'51\', \'53\'], [\'54\', \'51\']] '\
                            'c главным товаром \'5\' в акции  \'bad_3_v8\'. '\
                            'Замороженные товары должны стоять во всех офферах на первом месте.'
        assert err_log[8] == 'ERROR! [page-2]: пустая акция "bad_2_v8" магазина 465852'

    with io.open(TEST_XLS_BLUE_SET_GOOD_V4, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 465852
        feed_id = 12
        shop_id2 = 777
        feed_id2 = 888
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 101, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 100, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 101, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2122-12-31'
        assert msg_ver == u'Тип %%комплекты%% версия **4** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        for pd in promo_list:
            if pd.shop_promo_id == '4_demo_komplekt_2':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id2
                assert pd.shop_promo_id == '4_demo_komplekt_2'
                assert pd.url == BM_BASE_URL + '/promo/2'
                assert pd.landing_url == ''
                assert pd.no_landing_url is True
                assert pd.disabled_by_default is True
                assert pd.start_date == 1577826000
                __check_timestamp(pd.start_date, '01/01/2020 00:00:00+03:00')
                assert pd.end_date == 4828193999
                __check_timestamp(pd.end_date, '12/31/2122 23:59:59+03:00')
                assert make_promo_key(pd) == 'ucgj6u89u3WMUbYNKwyOdQ'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.blue_set.restrict_refund is False
                assert len(pd.blue_set.sets_content) == 1
                sc1 = pd.blue_set.sets_content[0]
                assert sc1.linked is False
                assert len(sc1.items) == 2
                assert sc1.items[0].offer_id == '4_000107.8861A'
                assert sc1.items[0].count == 1
                assert not sc1.items[0].HasField('discount')
                assert sc1.items[1].offer_id == '4_000107.8861B'
                assert sc1.items[1].count == 1
                assert sc1.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 888
                assert ids[0].offer_id == '4_000107.8861A'

            elif pd.shop_promo_id == '4_demo_komplekt':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id
                assert pd.shop_promo_id == '4_demo_komplekt'
                assert pd.url == BM_BASE_URL + '/promo/1'
                assert pd.landing_url == BM_BASE_URL + '/special/blue-set-landing?shopPromoId=4_demo_komplekt'
                assert pd.no_landing_url is False
                assert pd.start_date == 1561928400
                assert pd.end_date == 4783438799
                assert make_promo_key(pd) == 'J3xOW-AS8JohZgKU6Ijkfg'
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

                assert len(pd.restrictions.restricted_promo_types) == 0
                assert pd.blue_set.restrict_refund is True
                assert len(pd.blue_set.sets_content) == 4

                sc0 = pd.blue_set.sets_content[0]
                assert sc0.linked is True
                assert len(sc0.items) == 3
                assert sc0.items[0].offer_id == '4_000107.88612'
                assert sc0.items[0].discount == 5.0
                assert sc0.items[1].offer_id == '4_000107.88613'
                assert sc0.items[1].discount == 5.0
                assert sc0.items[2].offer_id == '4_00107.88614'
                assert sc0.items[2].discount == 5.0

                sc2 = pd.blue_set.sets_content[2]
                assert len(sc2.items) == 2
                assert sc2.items[0].offer_id == '4_000107.88613.1'
                assert sc2.items[0].discount == 20

                sc3 = pd.blue_set.sets_content[3]
                assert sc3.linked is False
                assert len(sc3.items) == 2
                assert sc3.items[0].offer_id == '4_000107.8861A'
                assert not sc3.items[0].HasField('discount')
                assert sc3.items[1].offer_id == '4_000107.8861B'
                assert sc3.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 7
                assert ids[0].feed_id == 12
                assert ids[0].offer_id == '4_000107.88612'
                assert ids[1].feed_id == 12
                assert ids[1].offer_id == '4_000107.88613'
                assert ids[2].feed_id == 12
                assert ids[2].offer_id == '4_00107.88614'
                assert ids[3].feed_id == 12
                assert ids[3].offer_id == '4_000107.886-10'
                assert ids[4].feed_id == 12
                assert ids[4].offer_id == '4_000107.88613.1'
                assert ids[5].feed_id == 12
                assert ids[5].offer_id == '4_000107.88613.2'
                assert ids[6].feed_id == 12
                assert ids[6].offer_id == '4_000107.8861A'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_BLUE_SET_GOOD_V5, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 465852
        feed_id = 12
        shop_id2 = 777
        feed_id2 = 888
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 101, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 100, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 101, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2122-12-31'
        assert msg_ver == u'Тип %%комплекты%% версия **5** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        for pd in promo_list:
            if pd.shop_promo_id == '5_demo_komplekt_2':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id2
                assert pd.shop_promo_id == '5_demo_komplekt_2'
                assert pd.url == BM_BASE_URL + '/promo/2'
                assert pd.landing_url == ''
                assert pd.no_landing_url is True
                assert pd.disabled_by_default is True
                assert pd.start_date == 1577826000
                __check_timestamp(pd.start_date, '01/01/2020 00:00:00+03:00')
                assert pd.end_date == 4828193999
                __check_timestamp(pd.end_date, '12/31/2122 23:59:59+03:00')
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 0

                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.blue_set.restrict_refund is False
                assert len(pd.blue_set.sets_content) == 1
                sc1 = pd.blue_set.sets_content[0]
                assert sc1.linked is False
                assert len(sc1.items) == 2
                assert sc1.items[0].offer_id == '5_000107.8861A'
                assert sc1.items[0].count == 1
                assert not sc1.items[0].HasField('discount')
                assert sc1.items[1].offer_id == '5_000107.8861B'
                assert sc1.items[1].count == 1
                assert sc1.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 888
                assert ids[0].offer_id == '5_000107.8861A'

            elif pd.shop_promo_id == '5_demo_komplekt':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id
                assert pd.shop_promo_id == '5_demo_komplekt'
                assert pd.url == BM_BASE_URL + '/promo/1'
                assert pd.landing_url == BM_BASE_URL + '/special/blue-set-landing?shopPromoId=5_demo_komplekt'
                assert pd.no_landing_url is False
                assert pd.start_date == 1561928400
                assert pd.end_date == 4783438799
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 0

                assert len(pd.restrictions.restricted_promo_types) == 0
                assert pd.blue_set.restrict_refund is True
                assert len(pd.blue_set.sets_content) == 4

                sc0 = pd.blue_set.sets_content[0]
                assert sc0.linked is True
                assert len(sc0.items) == 3
                assert sc0.items[0].offer_id == '5_000107.88612'
                assert sc0.items[0].discount == 5.0
                assert sc0.items[1].offer_id == '5_000107.88613'
                assert sc0.items[1].discount == 5.0
                assert sc0.items[2].offer_id == '5_00107.88614'
                assert sc0.items[2].discount == 5.0

                sc2 = pd.blue_set.sets_content[2]
                assert len(sc2.items) == 2
                assert sc2.items[0].offer_id == '5_000107.88613.1'
                assert sc2.items[0].discount == 20

                sc3 = pd.blue_set.sets_content[3]
                assert sc3.linked is False
                assert len(sc3.items) == 2
                assert sc3.items[0].offer_id == '5_000107.8861A'
                assert not sc3.items[0].HasField('discount')
                assert sc3.items[1].offer_id == '5_000107.8861B'
                assert sc3.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 7
                assert ids[0].feed_id == 12
                assert ids[0].offer_id == '5_000107.88612'
                assert ids[1].feed_id == 12
                assert ids[1].offer_id == '5_000107.88613'
                assert ids[2].feed_id == 12
                assert ids[2].offer_id == '5_00107.88614'
                assert ids[3].feed_id == 12
                assert ids[3].offer_id == '5_000107.886-10'
                assert ids[4].feed_id == 12
                assert ids[4].offer_id == '5_000107.88613.1'
                assert ids[5].feed_id == 12
                assert ids[5].offer_id == '5_000107.88613.2'
                assert ids[6].feed_id == 12
                assert ids[6].offer_id == '5_000107.8861A'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_BLUE_SET_GOOD_V6, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 465852
        feed_id = 12
        shop_id2 = 777
        feed_id2 = 888
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 101, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 100, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 101, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2122-12-31'
        assert msg_ver == u'Тип %%комплекты%% версия **6** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        for pd in promo_list:
            if pd.shop_promo_id == '6_demo_komplekt_2':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id2
                assert pd.shop_promo_id == '6_demo_komplekt_2'
                assert pd.anaplan_promo_id == '6_demo_komplekt_2'
                assert pd.url == BM_BASE_URL + '/promo/2'
                assert pd.landing_url == ''
                assert pd.no_landing_url is True
                assert pd.disabled_by_default is True
                assert pd.start_date == 1577826000
                __check_timestamp(pd.start_date, '01/01/2020 00:00:00+03:00')
                assert pd.end_date == 4828193999
                __check_timestamp(pd.end_date, '12/31/2122 23:59:59+03:00')
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 0

                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.blue_set.restrict_refund is False
                assert len(pd.blue_set.sets_content) == 1
                sc1 = pd.blue_set.sets_content[0]
                assert sc1.linked is False
                assert len(sc1.items) == 2
                assert sc1.items[0].offer_id == '6_000107.8861A'
                assert sc1.items[0].count == 1
                assert not sc1.items[0].HasField('discount')
                assert sc1.items[1].offer_id == '6_000107.8861B'
                assert sc1.items[1].count == 1
                assert sc1.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 888
                assert ids[0].offer_id == '6_000107.8861A'

            elif pd.shop_promo_id == '6_demo_komplekt':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id
                assert pd.shop_promo_id == '6_demo_komplekt'
                assert pd.anaplan_promo_id == '6_demo_komplekt'
                assert pd.url == BM_BASE_URL + '/promo/1'
                assert pd.landing_url == BM_BASE_URL + '/special/blue-set-landing?shopPromoId=6_demo_komplekt'
                assert pd.no_landing_url is False
                assert pd.start_date == 1561928400
                assert pd.end_date == 4783438799
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 0

                assert len(pd.restrictions.restricted_promo_types) == 0
                assert pd.blue_set.restrict_refund is True
                assert len(pd.blue_set.sets_content) == 4

                sc0 = pd.blue_set.sets_content[0]
                assert sc0.linked is True
                assert len(sc0.items) == 3
                assert sc0.items[0].offer_id == '6_000107.88612'
                assert sc0.items[0].discount == 5.0
                assert sc0.items[1].offer_id == '6_000107.88613'
                assert sc0.items[1].discount == 5.0
                assert sc0.items[2].offer_id == '6_00107.88614'
                assert sc0.items[2].discount == 5.0

                sc2 = pd.blue_set.sets_content[2]
                assert len(sc2.items) == 2
                assert sc2.items[0].offer_id == '6_000107.88613.1'
                assert sc2.items[0].discount == 20

                sc3 = pd.blue_set.sets_content[3]
                assert sc3.linked is False
                assert len(sc3.items) == 2
                assert sc3.items[0].offer_id == '6_000107.8861A'
                assert not sc3.items[0].HasField('discount')
                assert sc3.items[1].offer_id == '6_000107.8861B'
                assert sc3.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 7
                assert ids[0].feed_id == 12
                assert ids[0].offer_id == '6_000107.88612'
                assert ids[1].feed_id == 12
                assert ids[1].offer_id == '6_000107.88613'
                assert ids[2].feed_id == 12
                assert ids[2].offer_id == '6_00107.88614'
                assert ids[3].feed_id == 12
                assert ids[3].offer_id == '6_000107.886-10'
                assert ids[4].feed_id == 12
                assert ids[4].offer_id == '6_000107.88613.1'
                assert ids[5].feed_id == 12
                assert ids[5].offer_id == '6_000107.88613.2'
                assert ids[6].feed_id == 12
                assert ids[6].offer_id == '6_000107.8861A'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

    with io.open(TEST_XLS_BLUE_SET_GOOD_V7, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 465852
        feed_id = 12
        shop_id2 = 777
        feed_id2 = 888
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 101, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 100, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 101, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=[])
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2122-12-31'
        assert msg_ver == u'Тип %%комплекты%% версия **7**'

        n_promos = 0
        for pd in promo_list:
            if pd.shop_promo_id == '7_demo_komplekt_2':
                n_promos += 1
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id2
                assert pd.shop_promo_id == '7_demo_komplekt_2'
                assert pd.anaplan_promo_id == '7_demo_komplekt_2'
                assert pd.url == BM_BASE_URL + '/promo/2'
                assert pd.landing_url == ''
                assert pd.no_landing_url is True
                assert pd.disabled_by_default is True
                assert pd.start_date == 1577826000
                __check_timestamp(pd.start_date, '01/01/2020 00:00:00+03:00')
                assert pd.end_date == 4828193999
                __check_timestamp(pd.end_date, '12/31/2122 23:59:59+03:00')
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 0
                assert pd.parent_promo_id == ''

                assert len(pd.restrictions.restricted_promo_types) == 2
                pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
                pd.restrictions.restricted_promo_types[1] == EPromoType.PromoCode
                assert pd.blue_set.restrict_refund is False
                assert len(pd.blue_set.sets_content) == 1
                sc1 = pd.blue_set.sets_content[0]
                assert sc1.linked is False
                assert len(sc1.items) == 2
                assert sc1.items[0].offer_id == '7_000107.8861A'
                assert sc1.items[0].count == 1
                assert not sc1.items[0].HasField('discount')
                assert sc1.items[1].offer_id == '7_000107.8861B'
                assert sc1.items[1].count == 1
                assert sc1.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 1
                assert ids[0].feed_id == 888
                assert ids[0].offer_id == '7_000107.8861A'

            elif pd.shop_promo_id == '7_demo_komplekt':
                n_promos += 1
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id
                assert pd.shop_promo_id == '7_demo_komplekt'
                assert pd.anaplan_promo_id == '7_demo_komplekt'
                assert pd.url == BM_BASE_URL + '/promo/1'
                assert pd.landing_url == BM_BASE_URL + '/special/blue-set-landing?shopPromoId=7_demo_komplekt'
                assert pd.no_landing_url is False
                assert pd.start_date == 1561928400
                assert pd.end_date == 4783438799
                assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
                assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
                assert len(pd.restrictions.predicates) == 0
                assert pd.parent_promo_id == 'parent_promo'

                assert len(pd.restrictions.restricted_promo_types) == 0
                assert pd.blue_set.restrict_refund is True
                assert len(pd.blue_set.sets_content) == 4

                sc0 = pd.blue_set.sets_content[0]
                assert sc0.linked is True
                assert len(sc0.items) == 3
                assert sc0.items[0].offer_id == '7_000107.88612'
                assert sc0.items[0].discount == 5.0
                assert sc0.items[1].offer_id == '7_000107.88613'
                assert sc0.items[1].discount == 5.0
                assert sc0.items[2].offer_id == '7_00107.88614'
                assert sc0.items[2].discount == 5.0

                sc2 = pd.blue_set.sets_content[2]
                assert len(sc2.items) == 2
                assert sc2.items[0].offer_id == '7_000107.88613.1'
                assert sc2.items[0].discount == 20

                sc3 = pd.blue_set.sets_content[3]
                assert sc3.linked is False
                assert len(sc3.items) == 2
                assert sc3.items[0].offer_id == '7_000107.8861A'
                assert not sc3.items[0].HasField('discount')
                assert sc3.items[1].offer_id == '7_000107.8861B'
                assert sc3.items[1].discount == 10.0

                assert len(pd.offers_matching_rules) == 1
                ids = pd.offers_matching_rules[0].feed_offer_ids.ids
                assert len(ids) == 7
                assert ids[0].feed_id == 12
                assert ids[0].offer_id == '7_000107.88612'
                assert ids[1].feed_id == 12
                assert ids[1].offer_id == '7_000107.88613'
                assert ids[2].feed_id == 12
                assert ids[2].offer_id == '7_00107.88614'
                assert ids[3].feed_id == 12
                assert ids[3].offer_id == '7_000107.886-10'
                assert ids[4].feed_id == 12
                assert ids[4].offer_id == '7_000107.88613.1'
                assert ids[5].feed_id == 12
                assert ids[5].offer_id == '7_000107.88613.2'
                assert ids[6].feed_id == 12
                assert ids[6].offer_id == '7_000107.8861A'

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))
        assert n_promos == 2

    with io.open(TEST_XLS_BLUE_SET_GOOD_V8, 'rb') as file:
        uniques = {}
        err_log = []
        default_shop_id = 465852
        feed_id = 12
        shop_id2 = 777
        feed_id2 = 888
        shopsdat_table_rows = (
            {'shop_id': default_shop_id, 'warehouse_id': 100, 'datafeed_id': feed_id, 'is_dsbs': False},
            {'shop_id': default_shop_id, 'warehouse_id': 101, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 100, 'datafeed_id': 1000, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 101, 'datafeed_id': feed_id2, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(default_shop_id, shopsdat_table_rows), uniques, err_log, tags=[])
        assert len(err_log) == 0

        final_uniques.merge_uniqs(uniques, err_log)
        assert len(err_log) == 0
        assert len(promo_list) == 2
        assert n_promos == 2
        assert str(last_promo_date) == '2122-12-31'
        assert msg_ver == u'Тип %%комплекты%% версия **8**'
        n_checks = 0

        for pd in promo_list:
            if pd.shop_promo_id == '8_demo_komplekt_2':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id2
                assert len(pd.blue_set.sets_content) == 7
                n_checks += 1

                sc = pd.blue_set.sets_content[0]
                assert sc.linked is False
                assert len(sc.items) == 2
                assert sc.items[0].offer_id == '3'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '31'
                assert sc.items[1].discount == 5.0

                sc = pd.blue_set.sets_content[1]
                assert sc.linked is False
                assert len(sc.items) == 3
                assert sc.items[0].offer_id == '4'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '41'
                assert sc.items[1].discount == 5.0
                assert sc.items[2].offer_id == '42'
                assert sc.items[2].discount == 5.0

                sc = pd.blue_set.sets_content[2]
                assert sc.linked is False
                assert len(sc.items) == 3
                assert sc.items[0].offer_id == '4'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '41'
                assert sc.items[1].discount == 5.0
                assert sc.items[2].offer_id == '43'
                assert sc.items[2].discount == 5.0

                sc = pd.blue_set.sets_content[3]
                assert sc.linked is False
                assert len(sc.items) == 3
                assert sc.items[0].offer_id == '4'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '41'
                assert sc.items[1].discount == 5.0
                assert sc.items[2].offer_id == '44'
                assert sc.items[2].discount == 5.0

                sc = pd.blue_set.sets_content[4]
                assert sc.linked is False
                assert len(sc.items) == 3
                assert sc.items[0].offer_id == '5'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '51'
                assert sc.items[1].discount == 5.0
                assert sc.items[2].offer_id == '52'
                assert sc.items[2].discount == 5.0

                sc = pd.blue_set.sets_content[5]
                assert sc.linked is False
                assert len(sc.items) == 3
                assert sc.items[0].offer_id == '5'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '51'
                assert sc.items[1].discount == 5.0
                assert sc.items[2].offer_id == '53'
                assert sc.items[2].discount == 5.0

                sc = pd.blue_set.sets_content[6]
                assert sc.linked is False
                assert len(sc.items) == 3
                assert sc.items[0].offer_id == '5'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '52'
                assert sc.items[1].discount == 5.0
                assert sc.items[2].offer_id == '53'
                assert sc.items[2].discount == 5.0

            elif pd.shop_promo_id == '8_demo_komplekt':
                assert pd.type == PromoType.BLUE_SET
                assert pd.feed_id == feed_id
                assert len(pd.blue_set.sets_content) == 5
                n_checks += 1

                sc = pd.blue_set.sets_content[0]
                assert sc.linked is False
                assert len(sc.items) == 2
                assert sc.items[0].offer_id == '1'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '11'
                assert sc.items[1].discount == 5.0

                sc = pd.blue_set.sets_content[1]
                assert sc.linked is False
                assert len(sc.items) == 2
                assert sc.items[0].offer_id == '1'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '12'
                assert sc.items[1].discount == 5.0

                sc = pd.blue_set.sets_content[2]
                assert sc.linked is False
                assert len(sc.items) == 2
                assert sc.items[0].offer_id == '2'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '21'
                assert sc.items[1].discount == 5.0

                sc = pd.blue_set.sets_content[3]
                assert sc.linked is False
                assert len(sc.items) == 2
                assert sc.items[0].offer_id == '2'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '22'
                assert sc.items[1].discount == 5.0

                sc = pd.blue_set.sets_content[4]
                assert sc.linked is False
                assert len(sc.items) == 2
                assert sc.items[0].offer_id == '2'
                assert sc.items[0].discount == 5.0
                assert sc.items[1].offer_id == '23'
                assert sc.items[1].discount == 5.0

            else:
                raise Exception(u'Неизвестный shop_promo_id {}'.format(pd.shop_promo_id))

        assert n_checks == 2

    with io.open(TEST_XLS_DIRECT_DISCOUNT_GOOD_V5, 'rb') as file:
        uniques = {}
        err_log = []
        shop_id1 = 465852
        shop_id2 = 123
        shopsdat_table_rows = (
            {'shop_id': shop_id1, 'warehouse_id': 100, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 145, 'datafeed_id': 1002, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 147, 'datafeed_id': 1003, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 3
        assert n_promos == 3
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%прямая-скидка%% версия **5** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[2]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_3_v5'
        assert pd.landing_url == BM_BASE_URL + '/discount/1'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 0
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        # TODO тест на заполнение offers_matching_rules на категорию
        assert len(pd.offers_matching_rules) == 0

        pd = promo_list[1]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_2_v5'
        assert pd.landing_url == BM_BASE_URL + '/special/direct-discount-landing?shopPromoId=test_direct_2_v5'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 1
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1003
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v5_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[0].HasField('old_price')
        assert not pd.direct_discount.items[0].HasField('discount_price')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 1
        assert ids[0].feed_id == 1003
        assert ids[0].offer_id == 'direct_discount_v5_offer3'

        pd = promo_list[0]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_1_v5'
        assert pd.landing_url == ''
        assert pd.no_landing_url is True
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 3
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1001
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v5_offer1'
        assert pd.direct_discount.items[0].old_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].old_price.currency == u'RUB'
        assert pd.direct_discount.items[0].discount_price.value == 100 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[1].feed_id == 1002
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v5_offer2'
        assert pd.direct_discount.items[1].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].old_price.currency == u'RUB'
        assert pd.direct_discount.items[1].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[2].feed_id == 1003
        assert pd.direct_discount.items[2].offer_id == 'direct_discount_v5_offer2'
        assert pd.direct_discount.items[2].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].old_price.currency == u'RUB'
        assert pd.direct_discount.items[2].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].discount_price.currency == u'RUB'
        assert make_promo_key(pd) == 'QgCSiR7ar2tvTSacAahncg'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 1001
        assert ids[0].offer_id == 'direct_discount_v5_offer1'
        assert ids[1].feed_id == 1002
        assert ids[1].offer_id == 'direct_discount_v5_offer2'
        assert ids[2].feed_id == 1003
        assert ids[2].offer_id == 'direct_discount_v5_offer2'

    with io.open(TEST_XLS_DIRECT_DISCOUNT_GOOD_V6, 'rb') as file:
        uniques = {}
        err_log = []
        shop_id1 = 465852
        shop_id2 = 123
        shopsdat_table_rows = (
            {'shop_id': shop_id1, 'warehouse_id': 100, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 145, 'datafeed_id': 1002, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 147, 'datafeed_id': 1003, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 3
        assert n_promos == 3
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%прямая-скидка%% версия **6** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[2]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_3_v6'
        assert pd.landing_url == BM_BASE_URL + '/discount/1'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 0
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 2
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'
        assert pd.restrictions.predicates[0].perks[1] == 'EXTRA_CASHBACK'

        # TODO тест на заполнение offers_matching_rules на категорию
        assert len(pd.offers_matching_rules) == 0

        pd = promo_list[1]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_2_v6'
        assert pd.landing_url == BM_BASE_URL + '/special/direct-discount-landing?shopPromoId=test_direct_2_v6'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 2
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1002
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v6_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[0].HasField('old_price')
        assert not pd.direct_discount.items[0].HasField('discount_price')
        assert pd.direct_discount.items[1].feed_id == 1003
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v6_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[1].HasField('old_price')
        assert not pd.direct_discount.items[1].HasField('discount_price')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 2
        assert ids[0].feed_id == 1002
        assert ids[0].offer_id == 'direct_discount_v6_offer3'
        assert ids[1].feed_id == 1003
        assert ids[1].offer_id == 'direct_discount_v6_offer3'

        pd = promo_list[0]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_1_v6'
        assert pd.landing_url == ''
        assert pd.no_landing_url is True
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 3
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1001
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v6_offer1'
        assert pd.direct_discount.items[0].old_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].old_price.currency == u'RUB'
        assert pd.direct_discount.items[0].discount_price.value == 100 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[1].feed_id == 1002
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v6_offer2'
        assert pd.direct_discount.items[1].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].old_price.currency == u'RUB'
        assert pd.direct_discount.items[1].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[2].feed_id == 1003
        assert pd.direct_discount.items[2].offer_id == 'direct_discount_v6_offer2'
        assert pd.direct_discount.items[2].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].old_price.currency == u'RUB'
        assert pd.direct_discount.items[2].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].discount_price.currency == u'RUB'
        assert make_promo_key(pd) == 'AC_QX1NBFjhQHI6YLT8z7w'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 1001
        assert ids[0].offer_id == 'direct_discount_v6_offer1'
        assert ids[1].feed_id == 1002
        assert ids[1].offer_id == 'direct_discount_v6_offer2'
        assert ids[2].feed_id == 1003
        assert ids[2].offer_id == 'direct_discount_v6_offer2'

    with io.open(TEST_XLS_DIRECT_DISCOUNT_GOOD_V7, 'rb') as file:
        uniques = {}
        err_log = []
        shop_id1 = 465852
        shop_id2 = 123
        shopsdat_table_rows = (
            {'shop_id': shop_id1, 'warehouse_id': 100, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 145, 'datafeed_id': 1002, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 147, 'datafeed_id': 1003, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 3
        assert n_promos == 3
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%прямая-скидка%% версия **7** !!(УСТАРЕВШАЯ ВЕРСИЯ)!!'

        pd = promo_list[2]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_3_v7'
        assert pd.anaplan_promo_id == 'test_direct_3_v7'
        assert pd.landing_url == BM_BASE_URL + '/discount/1'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 0
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 2
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'
        assert pd.restrictions.predicates[0].perks[1] == 'EXTRA_CASHBACK'

        # TODO тест на заполнение offers_matching_rules на категорию
        assert len(pd.offers_matching_rules) == 0

        pd = promo_list[1]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_2_v7'
        assert pd.anaplan_promo_id == 'test_direct_2_v7'
        assert pd.landing_url == BM_BASE_URL + '/special/direct-discount-landing?shopPromoId=test_direct_2_v7'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 2
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1002
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v7_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[0].HasField('old_price')
        assert not pd.direct_discount.items[0].HasField('discount_price')
        assert pd.direct_discount.items[1].feed_id == 1003
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v7_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[1].HasField('old_price')
        assert not pd.direct_discount.items[1].HasField('discount_price')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 2
        assert ids[0].feed_id == 1002
        assert ids[0].offer_id == 'direct_discount_v7_offer3'
        assert ids[1].feed_id == 1003
        assert ids[1].offer_id == 'direct_discount_v7_offer3'

        pd = promo_list[0]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_1_v7'
        assert pd.landing_url == ''
        assert pd.no_landing_url is True
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 3
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1001
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v7_offer1'
        assert pd.direct_discount.items[0].old_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].old_price.currency == u'RUB'
        assert pd.direct_discount.items[0].discount_price.value == 100 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[1].feed_id == 1002
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v7_offer2'
        assert pd.direct_discount.items[1].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].old_price.currency == u'RUB'
        assert pd.direct_discount.items[1].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[2].feed_id == 1003
        assert pd.direct_discount.items[2].offer_id == 'direct_discount_v7_offer2'
        assert pd.direct_discount.items[2].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].old_price.currency == u'RUB'
        assert pd.direct_discount.items[2].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].discount_price.currency == u'RUB'
        assert make_promo_key(pd) == 'MFaI4R5Liq8S-7zg2Ghrxg'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 1001
        assert ids[0].offer_id == 'direct_discount_v7_offer1'
        assert ids[1].feed_id == 1002
        assert ids[1].offer_id == 'direct_discount_v7_offer2'
        assert ids[2].feed_id == 1003
        assert ids[2].offer_id == 'direct_discount_v7_offer2'

    with io.open(TEST_XLS_DIRECT_DISCOUNT_GOOD_V8, 'rb') as file:
        uniques = {}
        err_log = []
        shop_id1 = 465852
        shop_id2 = 123
        shopsdat_table_rows = (
            {'shop_id': shop_id1, 'warehouse_id': 100, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 145, 'datafeed_id': 1002, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 147, 'datafeed_id': 1003, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        # ставим ограничение в 1 элемент в акции, но оно сработать НЕ должно, т.к. тип DirectDiscount (для него не работает ограничение)
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(
            logger,
            file,
            ShopsData(ANY_SHOP, shopsdat_table_rows),
            uniques,
            err_log,
            tags=[],
            custom_params=[
                {'promo_type': 'direct-discount', 'max_items_per_promo': 500000},
                {'promo_type': 'default', 'max_items_per_promo': 1},
            ]
        )
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 3
        assert n_promos == 3
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%прямая-скидка%% версия **8**'

        pd = promo_list[2]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_3_v8'
        assert pd.anaplan_promo_id == 'test_direct_3_v8'
        assert pd.landing_url == BM_BASE_URL + '/discount/1'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 0
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 2
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'
        assert pd.restrictions.predicates[0].perks[1] == 'EXTRA_CASHBACK'
        assert pd.parent_promo_id == 'parent_promo_1'

        # TODO тест на заполнение offers_matching_rules на категорию
        assert len(pd.offers_matching_rules) == 0

        pd = promo_list[1]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_2_v8'
        assert pd.anaplan_promo_id == 'test_direct_2_v8'
        assert pd.landing_url == BM_BASE_URL + '/special/direct-discount-landing?shopPromoId=test_direct_2_v8'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 2
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1002
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v8_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[0].HasField('old_price')
        assert not pd.direct_discount.items[0].HasField('discount_price')
        assert pd.direct_discount.items[1].feed_id == 1003
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v8_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[1].HasField('old_price')
        assert not pd.direct_discount.items[1].HasField('discount_price')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'
        assert pd.parent_promo_id == 'parent_promo'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 2
        assert ids[0].feed_id == 1002
        assert ids[0].offer_id == 'direct_discount_v8_offer3'
        assert ids[1].feed_id == 1003
        assert ids[1].offer_id == 'direct_discount_v8_offer3'

        pd = promo_list[0]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_1_v8'
        assert pd.landing_url == ''
        assert pd.no_landing_url is True
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 3
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1001
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v8_offer1'
        assert pd.direct_discount.items[0].old_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].old_price.currency == u'RUB'
        assert pd.direct_discount.items[0].discount_price.value == 100 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[1].feed_id == 1002
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v8_offer2'
        assert pd.direct_discount.items[1].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].old_price.currency == u'RUB'
        assert pd.direct_discount.items[1].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[2].feed_id == 1003
        assert pd.direct_discount.items[2].offer_id == 'direct_discount_v8_offer2'
        assert pd.direct_discount.items[2].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].old_price.currency == u'RUB'
        assert pd.direct_discount.items[2].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].discount_price.currency == u'RUB'
        assert make_promo_key(pd) == '4UIj0qaNdvMdzd8Hwi7Xkg'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0
        assert pd.parent_promo_id == 'parent_promo'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 1001
        assert ids[0].offer_id == 'direct_discount_v8_offer1'
        assert ids[1].feed_id == 1002
        assert ids[1].offer_id == 'direct_discount_v8_offer2'
        assert ids[2].feed_id == 1003
        assert ids[2].offer_id == 'direct_discount_v8_offer2'

    with io.open(TEST_XLS_SECRET_SALE_PLUS_V106, 'rb') as file:
        uniques = {}
        err_log = []
        shop_id1 = 465852
        shop_id2 = 123
        shopsdat_table_rows = (
            {'shop_id': shop_id1, 'warehouse_id': 100, 'datafeed_id': 1001, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 145, 'datafeed_id': 1002, 'is_dsbs': False},
            {'shop_id': shop_id2, 'warehouse_id': 147, 'datafeed_id': 1003, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=[])
        final_uniques.merge_uniqs(uniques, err_log)

        assert len(err_log) == 0
        assert len(promo_list) == 3
        assert n_promos == 3
        assert str(last_promo_date) == '2121-12-31'
        assert msg_ver == u'Тип %%прямая-скидка%% версия **106**'

        pd = promo_list[2]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_3_v106'
        assert pd.landing_url == BM_BASE_URL + '/discount/1'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 0
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 2
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'
        assert pd.restrictions.predicates[0].perks[1] == 'EXTRA_CASHBACK'
        assert pd.conditions == 'Распродажа плюса'

        # TODO тест на заполнение offers_matching_rules на категорию
        assert len(pd.offers_matching_rules) == 0

        pd = promo_list[1]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_2_v106'
        assert pd.landing_url == BM_BASE_URL + '/special/direct-discount-landing?shopPromoId=test_direct_2_v106'
        assert pd.no_landing_url is False
        assert pd.disabled_by_default is False
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 2
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1002
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v106_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[0].HasField('old_price')
        assert not pd.direct_discount.items[0].HasField('discount_price')
        assert pd.direct_discount.items[1].feed_id == 1003
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v106_offer3'
        # Для этого оффера в эксель-файле не заданы ни old_price, ни discount_price (это валидный кейс)
        assert not pd.direct_discount.items[1].HasField('old_price')
        assert not pd.direct_discount.items[1].HasField('discount_price')
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 1
        assert len(pd.restrictions.predicates[0].perks) == 1
        assert pd.restrictions.predicates[0].perks[0] == 'YANDEX_CASHBACK'

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 2
        assert ids[0].feed_id == 1002
        assert ids[0].offer_id == 'direct_discount_v106_offer3'
        assert ids[1].feed_id == 1003
        assert ids[1].offer_id == 'direct_discount_v106_offer3'
        assert pd.conditions == 'Cекретная распродажа'

        pd = promo_list[0]
        assert pd.type == PromoType.DIRECT_DISCOUNT
        assert pd.feed_id == 0
        assert pd.shop_promo_id == 'test_direct_1_v106'
        assert pd.landing_url == ''
        assert pd.no_landing_url is True
        assert pd.disabled_by_default is True
        assert pd.start_date == 1572555600
        assert pd.end_date == 4796657999
        assert len(pd.direct_discount.items) == 3
        assert len(pd.restrictions.restricted_promo_types) == 0
        assert pd.direct_discount.items[0].feed_id == 1001
        assert pd.direct_discount.items[0].offer_id == 'direct_discount_v106_offer1'
        assert pd.direct_discount.items[0].old_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].old_price.currency == u'RUB'
        assert pd.direct_discount.items[0].discount_price.value == 100 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[0].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[1].feed_id == 1002
        assert pd.direct_discount.items[1].offer_id == 'direct_discount_v106_offer2'
        assert pd.direct_discount.items[1].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].old_price.currency == u'RUB'
        assert pd.direct_discount.items[1].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[1].discount_price.currency == u'RUB'
        assert pd.direct_discount.items[2].feed_id == 1003
        assert pd.direct_discount.items[2].offer_id == 'direct_discount_v106_offer2'
        assert pd.direct_discount.items[2].old_price.value == 200 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].old_price.currency == u'RUB'
        assert pd.direct_discount.items[2].discount_price.value == 150 * DirectDiscountXlsType.PRICE_PRECISION
        assert pd.direct_discount.items[2].discount_price.currency == u'RUB'
        assert make_promo_key(pd) == 'tQybw73JIxV8fctGs_2GWA'
        assert pd.allowed_payment_methods == PaymentMethod.PT_ALL
        assert pd.mechanics_payment_type == MechanicsPaymentType.CPA
        assert len(pd.restrictions.predicates) == 0
        assert pd.conditions == ''

        assert len(pd.offers_matching_rules) == 1
        ids = pd.offers_matching_rules[0].feed_offer_ids.ids
        assert len(ids) == 3
        assert ids[0].feed_id == 1001
        assert ids[0].offer_id == 'direct_discount_v106_offer1'
        assert ids[1].feed_id == 1002
        assert ids[1].offer_id == 'direct_discount_v106_offer2'
        assert ids[2].feed_id == 1003
        assert ids[2].offer_id == 'direct_discount_v106_offer2'

    with io.open(TEST_XLS_WH_UNIQ, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': ANY_SHOP, 'warehouse_id': ANY_WAREHOUSE, 'datafeed_id': 777, 'is_dsbs': False},
        )

        # исключения не ловим, ошибок быть не должно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)

        assert n_promos == 2

    with io.open(TEST_XLS_DIRECT_DISCOUNT_BAD_V6, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            shopsdat_table_rows = (
                {'shop_id': 465852, 'warehouse_id': 100, 'datafeed_id': 10465852, 'is_dsbs': False},
            )
            read_xls(logger, file, ShopsData(ANY_SHOP, shopsdat_table_rows), uniques, err_log, tags=TAG_ADV)
        except:
            pass
        assert len(err_log) == 4
        assert err_log[0] == u'ERROR! [page-2 E3]: Невалидная акция (у оффера "direct_discount_v3_offer1" заполнена цена до скидки, но не заполнена цена после скидки).'
        assert err_log[1] == u'ERROR! [page-2 E4]: у оффера "direct_discount_v3_offer2" цена до скидки -100 должна быть положительным числом'
        assert err_log[2] == u'ERROR! [page-2 E5]: у оффера "direct_discount_v3_offer3" цена со скидкой -200 должна быть положительным числом'
        assert err_log[3] == u'ERROR! [page-2 E6]: у оффера "direct_discount_v3_offer4" цена до скидки 300 должна быть больше цены со скидкой 300'

    with io.open(TEST_XLS_INCORRECT_PROMO_ID, 'rb') as file:
        uniques = {}
        err_log = []
        try:
            read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log, tags=default_tags)
        except:
            pass

        assert len(err_log) == 4
        assert err_log[0] == u'ERROR! [page-1 C5]: ID акции "_too_long_name_0124567890..." длиннее 25 символов'
        assert err_log[1] == u'ERROR! [page-1 C6]: ID акции "кирилл" содержит недопустимые символы. Список допустимых символов "#-0123456789_abcdefghijklmnopqrstuvwxyz"'
        assert err_log[2] == u'ERROR! [page-2 A4]: unknown shop_promo_id _too_long_name_0124567890_'
        assert err_log[3] == u'ERROR! [page-2 A5]: unknown shop_promo_id кирилл'

    with io.open(TEST_XLS_INCORRECT_PROMO_ID, 'rb') as file:
        uniques = {}
        err_log = []
        tags = default_tags + [OBSOLETE_SHOP_PROMO_ID_TAG]
        try:
            read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, 1, tags=tags)
        except:
            pass
        assert len(err_log) == 0

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V1, 'rb') as file:
        uniques = {}
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) =read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)
        assert len(err_log) == 0

        assert len(promo_list) == 1
        pd = promo_list[0]
        assert make_promo_key(pd) == 'Ma5o84NyKB2DYJmDpEHBjw'
        assert pd.type == PromoType.SPREAD_DISCOUNT_COUNT
        assert pd.shop_promo_id == 'spread_discount_count'
        assert pd.landing_url == BM_BASE_URL + '/special/spread-discount-count-landing?shopPromoId=spread_discount_count'
        assert pd.url == 'https://market.yandex.ru/special/spread_discount_count'
        assert pd.disabled_by_default is False
        assert pd.start_date == 1591650000
        assert pd.end_date == 4749656399
        assert len(pd.restrictions.restricted_promo_types) == 1
        assert pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus

        assert len(pd.offers_matching_rules) == 1
        assert len(pd.offers_matching_rules[0].suppliers.ids) == 2
        assert pd.offers_matching_rules[0].suppliers.ids[0] == 465852
        assert pd.offers_matching_rules[0].suppliers.ids[1] == 241241
        assert len(pd.offers_matching_rules[0].excluded_suppliers.ids) == 0
        assert len(pd.offers_matching_rules[0].warehouses.ids) == 2
        assert pd.offers_matching_rules[0].warehouses.ids[0] == 5432123
        assert pd.offers_matching_rules[0].warehouses.ids[1] == 6543212
        assert len(pd.offers_matching_rules[0].mskus.ids) == 3
        assert pd.offers_matching_rules[0].mskus.ids[0] == 1234534
        assert pd.offers_matching_rules[0].mskus.ids[1] == 9345234
        assert pd.offers_matching_rules[0].mskus.ids[2] == 3645234
        assert len(pd.spread_discount_count.discount_items) == 3
        assert pd.spread_discount_count.discount_items[0].msku == 1234534
        assert len(pd.spread_discount_count.discount_items[0].count_bounds) == 2
        assert pd.spread_discount_count.discount_items[0].count_bounds[0].count == 3
        assert pd.spread_discount_count.discount_items[0].count_bounds[0].percent_discount == 10
        assert pd.spread_discount_count.discount_items[0].count_bounds[1].count == 5
        assert pd.spread_discount_count.discount_items[0].count_bounds[1].percent_discount == 15
        assert pd.spread_discount_count.discount_items[1].msku == 9345234
        assert len(pd.spread_discount_count.discount_items[1].count_bounds) == 1
        assert pd.spread_discount_count.discount_items[1].count_bounds[0].count == 5
        assert pd.spread_discount_count.discount_items[1].count_bounds[0].percent_discount == 15
        assert pd.spread_discount_count.discount_items[2].msku == 3645234
        assert len(pd.spread_discount_count.discount_items[2].count_bounds) == 1
        assert pd.spread_discount_count.discount_items[2].count_bounds[0].count == 8
        assert pd.spread_discount_count.discount_items[2].count_bounds[0].percent_discount == 20

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V2, 'rb') as file:
        uniques = {}
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) =read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log)
        final_uniques.merge_uniqs(uniques, err_log)
        assert len(err_log) == 0

        assert len(promo_list) == 1
        pd = promo_list[0]
        assert make_promo_key(pd) == 'JrPY0evY76vCZjzr_63cKQ'
        assert pd.type == PromoType.SPREAD_DISCOUNT_COUNT
        assert pd.shop_promo_id == 'spread_discount_count_v2'
        assert pd.landing_url == BM_BASE_URL + '/special/spread-discount-count-landing?shopPromoId=spread_discount_count_v2'
        assert pd.url == 'https://market.yandex.ru/special/spread_discount_count'
        assert pd.force_disabled is True
        assert pd.disabled_by_default is False
        assert pd.start_date == 1591650000
        assert pd.end_date == 4749656399
        assert len(pd.restrictions.restricted_promo_types) == 1
        assert pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
        assert pd.parent_promo_id == 'parent_promo#303'

        assert len(pd.offers_matching_rules) == 1
        assert len(pd.offers_matching_rules[0].suppliers.ids) == 2
        assert pd.offers_matching_rules[0].suppliers.ids[0] == 465852
        assert pd.offers_matching_rules[0].suppliers.ids[1] == 241241
        assert len(pd.offers_matching_rules[0].excluded_suppliers.ids) == 0
        assert len(pd.offers_matching_rules[0].warehouses.ids) == 2
        assert pd.offers_matching_rules[0].warehouses.ids[0] == 5432123
        assert pd.offers_matching_rules[0].warehouses.ids[1] == 6543212
        assert len(pd.offers_matching_rules[0].mskus.ids) == 3
        assert pd.offers_matching_rules[0].mskus.ids[0] == 1234534
        assert pd.offers_matching_rules[0].mskus.ids[1] == 9345234
        assert pd.offers_matching_rules[0].mskus.ids[2] == 3645234
        assert len(pd.spread_discount_count.discount_items) == 3
        assert pd.spread_discount_count.discount_items[0].msku == 1234534
        assert len(pd.spread_discount_count.discount_items[0].count_bounds) == 2
        assert pd.spread_discount_count.discount_items[0].count_bounds[0].count == 3
        assert pd.spread_discount_count.discount_items[0].count_bounds[0].percent_discount == 10
        assert pd.spread_discount_count.discount_items[0].count_bounds[1].count == 5
        assert pd.spread_discount_count.discount_items[0].count_bounds[1].percent_discount == 15
        assert pd.spread_discount_count.discount_items[1].msku == 9345234
        assert len(pd.spread_discount_count.discount_items[1].count_bounds) == 1
        assert pd.spread_discount_count.discount_items[1].count_bounds[0].count == 5
        assert pd.spread_discount_count.discount_items[1].count_bounds[0].percent_discount == 15
        assert pd.spread_discount_count.discount_items[2].msku == 3645234
        assert len(pd.spread_discount_count.discount_items[2].count_bounds) == 1
        assert pd.spread_discount_count.discount_items[2].count_bounds[0].count == 8
        assert pd.spread_discount_count.discount_items[2].count_bounds[0].percent_discount == 20

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_1, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 D5]: ERROR! Не уникальный msku 1234534 в акции spread_discount_count'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_2, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 A5]: ERROR! Не уникальный supplier_id 241241 в акции spread_discount_count'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_3, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 C5]: ERROR! Не уникальный warehouse_id 5432123 в акции spread_discount_count'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_NON_UNIQ_4, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 B5]: ERROR! Не уникальный excluded_supplier_id 465852 в акции spread_discount_count'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_NO_BOUNDS, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2 E4]: Не указан обязательный уровень скидки.'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_NO_MSKU, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 3
        assert err_log[0] == u'ERROR! [page-2 D3]: Не указан msku'
        assert err_log[1] == u'ERROR! [page-2 D4]: Не указан msku'
        assert err_log[2] == u'ERROR! [page-2]: пустая акция "spread_discount_count" магазина None'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_BAD_INCLUDE_EXCLUDE, 'rb') as file:
        err_log = []

        read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: Могут быть указаны либо магазины включения, либо магазины исключения'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_EXCLUDED_SUPPLIERS, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 0

        assert len(promo_list) == 1
        pd = promo_list[0]

        assert len(pd.offers_matching_rules) == 1
        assert len(pd.offers_matching_rules[0].excluded_suppliers.ids) == 2
        assert pd.offers_matching_rules[0].excluded_suppliers.ids[0] == 465852
        assert pd.offers_matching_rules[0].excluded_suppliers.ids[1] == 241241
        assert len(pd.offers_matching_rules[0].suppliers.ids) == 0

        assert len(pd.restrictions.restricted_promo_types) == 0

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_DISABLED, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 0

        assert len(promo_list) == 1

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_BAD_DISCOUNT, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: Количество товаров для скидки дожно возрастать. msku товара 1234534'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_BAD_DISCOUNT_2, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: Скидка должна возрастать. msku товара 1234534'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_COUNT_V1_BAD_DISCOUNT_3, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: Скидка должна быть не меньше 5%. msku товара 1234534'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1, 'rb') as file:
        uniques = {}
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) =read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log, tags=TAG_ADV)
        final_uniques.merge_uniqs(uniques, err_log)
        assert len(err_log) == 0

        assert len(promo_list) == 1
        pd = promo_list[0]
        assert make_promo_key(pd) == 'DVKcrUOprdhva99JNcZkmQ'
        assert pd.type == PromoType.SPREAD_DISCOUNT_RECEIPT
        assert pd.shop_promo_id == 'spread_discount_receipt'
        assert pd.landing_url == BM_BASE_URL + '/special/spread-discount-receipt-landing?shopPromoId=spread_discount_receipt'
        assert pd.url == 'https://market.yandex.ru/special/skidka-za-chek'
        assert pd.disabled_by_default is True
        assert pd.start_date == 1591650000
        assert pd.end_date == 4749656399
        assert len(pd.restrictions.restricted_promo_types) == 1
        assert pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus

        assert len(pd.offers_matching_rules) == 1
        assert len(pd.offers_matching_rules[0].category_restriction.categories) == 3
        assert pd.offers_matching_rules[0].category_restriction.categories[0] == 94052
        assert pd.offers_matching_rules[0].category_restriction.categories[1] == 94168
        assert pd.offers_matching_rules[0].category_restriction.categories[2] == 54321
        assert len(pd.offers_matching_rules[0].category_restriction.excluded_categories) == 1
        assert pd.offers_matching_rules[0].category_restriction.excluded_categories[0] == 94078
        assert len(pd.offers_matching_rules[0].mskus.ids) == 3
        assert pd.offers_matching_rules[0].mskus.ids[0] == 12345678
        assert pd.offers_matching_rules[0].mskus.ids[1] == 23456789
        assert pd.offers_matching_rules[0].mskus.ids[2] == 34567890
        assert len(pd.offers_matching_rules[0].suppliers.ids) == 2
        assert pd.offers_matching_rules[0].suppliers.ids[0] == 12345
        assert pd.offers_matching_rules[0].suppliers.ids[1] == 23456
        assert len(pd.offers_matching_rules[0].vendors.ids) == 1
        assert pd.offers_matching_rules[0].vendors.ids[0] == 54321
        assert len(pd.offers_matching_rules[0].warehouses.ids) == 1
        assert pd.offers_matching_rules[0].warehouses.ids[0] == 12345

        assert pd.restrictions.budget_limit.value == 10000
        assert pd.restrictions.budget_limit.currency == "RUR"
        assert len(pd.restrictions.category_price_restrictions) == 2
        assert pd.restrictions.category_price_restrictions[0].category_id == 94052
        assert pd.restrictions.category_price_restrictions[0].min_price.value == 500
        assert pd.restrictions.category_price_restrictions[0].min_price.currency == "RUB"
        assert pd.restrictions.category_price_restrictions[0].max_price.value == 1000
        assert pd.restrictions.category_price_restrictions[0].max_price.currency == "RUB"
        assert pd.restrictions.category_price_restrictions[1].category_id == 94168
        assert pd.restrictions.category_price_restrictions[1].min_price.value == 400
        assert pd.restrictions.category_price_restrictions[1].min_price.currency == "RUB"
        assert pd.restrictions.category_price_restrictions[1].max_price.value == 1500
        assert pd.restrictions.category_price_restrictions[1].max_price.currency == "RUB"

        assert len(pd.spread_discount_receipt.receipt_bounds) == 2
        assert pd.spread_discount_receipt.receipt_bounds[0].discount_price.value == 1000
        assert pd.spread_discount_receipt.receipt_bounds[0].discount_price.currency == "RUR"
        assert pd.spread_discount_receipt.receipt_bounds[0].percent_discount == 10
        assert pd.spread_discount_receipt.receipt_bounds[1].discount_price.value == 2000
        assert pd.spread_discount_receipt.receipt_bounds[1].discount_price.currency == "RUR"
        assert pd.spread_discount_receipt.receipt_bounds[1].percent_discount == 15

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V2, 'rb') as file:
        uniques = {}
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) =read_xls(logger, file, DUMMY_SHOPSDATA, uniques, err_log)
        final_uniques.merge_uniqs(uniques, err_log)
        assert len(err_log) == 0

        assert len(promo_list) == 1
        pd = promo_list[0]
        assert make_promo_key(pd) == 'C8EQt1SDa49AVQmAB7IxjQ'
        assert pd.type == PromoType.SPREAD_DISCOUNT_RECEIPT
        assert pd.shop_promo_id == 'spd_receipt_v2'
        assert pd.landing_url == BM_BASE_URL + '/special/spread-discount-receipt-landing?shopPromoId=spd_receipt_v2'
        assert pd.url == 'https://market.yandex.ru/special/skidka-za-chek'
        assert pd.disabled_by_default is True
        assert pd.start_date == 1591650000
        assert pd.end_date == 4749656399
        assert len(pd.restrictions.restricted_promo_types) == 1
        assert pd.restrictions.restricted_promo_types[0] == EPromoType.MarketBonus
        assert pd.parent_promo_id == 'PP#202'

        assert len(pd.offers_matching_rules) == 1
        assert len(pd.offers_matching_rules[0].category_restriction.categories) == 3
        assert pd.offers_matching_rules[0].category_restriction.categories[0] == 94052
        assert pd.offers_matching_rules[0].category_restriction.categories[1] == 94168
        assert pd.offers_matching_rules[0].category_restriction.categories[2] == 54321
        assert len(pd.offers_matching_rules[0].category_restriction.excluded_categories) == 1
        assert pd.offers_matching_rules[0].category_restriction.excluded_categories[0] == 94078
        assert len(pd.offers_matching_rules[0].mskus.ids) == 3
        assert pd.offers_matching_rules[0].mskus.ids[0] == 12345678
        assert pd.offers_matching_rules[0].mskus.ids[1] == 23456789
        assert pd.offers_matching_rules[0].mskus.ids[2] == 34567890
        assert len(pd.offers_matching_rules[0].suppliers.ids) == 2
        assert pd.offers_matching_rules[0].suppliers.ids[0] == 12345
        assert pd.offers_matching_rules[0].suppliers.ids[1] == 23456
        assert len(pd.offers_matching_rules[0].vendors.ids) == 1
        assert pd.offers_matching_rules[0].vendors.ids[0] == 54321
        assert len(pd.offers_matching_rules[0].warehouses.ids) == 1
        assert pd.offers_matching_rules[0].warehouses.ids[0] == 12345

        assert pd.restrictions.budget_limit.value == 10000
        assert pd.restrictions.budget_limit.currency == "RUR"
        assert len(pd.restrictions.category_price_restrictions) == 2
        assert pd.restrictions.category_price_restrictions[0].category_id == 94052
        assert pd.restrictions.category_price_restrictions[0].min_price.value == 500
        assert pd.restrictions.category_price_restrictions[0].min_price.currency == "RUB"
        assert pd.restrictions.category_price_restrictions[0].max_price.value == 1000
        assert pd.restrictions.category_price_restrictions[0].max_price.currency == "RUB"
        assert pd.restrictions.category_price_restrictions[1].category_id == 94168
        assert pd.restrictions.category_price_restrictions[1].min_price.value == 400
        assert pd.restrictions.category_price_restrictions[1].min_price.currency == "RUB"
        assert pd.restrictions.category_price_restrictions[1].max_price.value == 1500
        assert pd.restrictions.category_price_restrictions[1].max_price.currency == "RUB"

        assert len(pd.spread_discount_receipt.receipt_bounds) == 2
        assert pd.spread_discount_receipt.receipt_bounds[0].discount_price.value == 1000
        assert pd.spread_discount_receipt.receipt_bounds[0].discount_price.currency == "RUR"
        assert pd.spread_discount_receipt.receipt_bounds[0].percent_discount == 10
        assert pd.spread_discount_receipt.receipt_bounds[1].discount_price.value == 2000
        assert pd.spread_discount_receipt.receipt_bounds[1].discount_price.currency == "RUR"
        assert pd.spread_discount_receipt.receipt_bounds[1].percent_discount == 15

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_EXCLUDED_IDS, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) =read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 0

        assert len(promo_list) == 1
        pd = promo_list[0]

        assert len(pd.restrictions.restricted_promo_types) == 0
        assert len(pd.offers_matching_rules) == 1
        assert len(pd.offers_matching_rules[0].excluded_mskus.ids) == 3
        assert pd.offers_matching_rules[0].excluded_mskus.ids[0] == 12345678
        assert pd.offers_matching_rules[0].excluded_mskus.ids[1] == 23456789
        assert pd.offers_matching_rules[0].excluded_mskus.ids[2] == 34567890
        assert len(pd.offers_matching_rules[0].excluded_suppliers.ids) == 2
        assert pd.offers_matching_rules[0].excluded_suppliers.ids[0] == 12345
        assert pd.offers_matching_rules[0].excluded_suppliers.ids[1] == 23456
        assert len(pd.offers_matching_rules[0].excluded_vendors.ids) == 2
        assert pd.offers_matching_rules[0].excluded_vendors.ids[0] == 54321
        assert pd.offers_matching_rules[0].excluded_vendors.ids[1] == 64532

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_EMPTY, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: пустая акция "spread_discount_receipt" магазина None'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_1, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 4
        assert err_log[0] == u'ERROR! [page-1 K5]: В акции spread_discount_receipt не указан первый порог скидки.'
        assert err_log[2] == u'ERROR! [page-1 N5]: В акции spread_discount_receipt не указан второй порог скидки.'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_2, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-1 P5]: Второй порог скидки в акции spread_discount_receipt должен быть больше первого'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_3, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-1 P5]: В акции spread_discount_receipt процент второй скидки должен быть больше первой.'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_4, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-1 P5]: В акции spread_discount_receipt вторая сумма скидки должна быть больше первой.'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_5, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-1 P5]: Нельзя смешивать типы скидок в акции spread_discount_receipt'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_BAD_DISCOUNT_6, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-1 P5]: Нельзя смешивать типы скидок в акции spread_discount_receipt'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_NON_UNIQUE, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 7
        assert err_log[0] == u'ERROR! [page-2 E5]: ERROR! Не уникальный vendor_id 54321 в акции spread_discount_receipt'
        assert err_log[1] == u'ERROR! [page-2 G6]: ERROR! Не уникальный warehouse_id 12345 в акции spread_discount_receipt'
        assert err_log[2] == u'ERROR! [page-2 A7]: ERROR! Не уникальный msku 23456789 в акции spread_discount_receipt'
        assert err_log[3] == u'ERROR! [page-2 C8]: ERROR! Не уникальный supplier_id 12345 в акции spread_discount_receipt'
        assert err_log[4] == u'ERROR! [page-2 H9]: ERROR! Не уникальный excluded_category_id 94078 в акции spread_discount_receipt'
        assert err_log[5] == u'ERROR! [page-2 I10]: ERROR! Не уникальный category_id 94052 в акции spread_discount_receipt'
        assert err_log[6] == u'ERROR! [page-2]: Некорректный диапазон цен для категории 756785'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_NON_UNIQUE_EXCLUDED, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 3
        assert err_log[0] == u'ERROR! [page-2 D5]: ERROR! Не уникальный excluded_supplier_id 12345 в акции spread_discount_receipt'
        assert err_log[1] == u'ERROR! [page-2 B6]: ERROR! Не уникальный excluded_msku 12345678 в акции spread_discount_receipt'
        assert err_log[2] == u'ERROR! [page-2]: Могут быть указаны либо товары (msku) включения, либо товары (msku) исключения'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_COLUMNS_CONFLICT, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: Могут быть указаны либо магазины включения, либо магазины исключения'

    with io.open(TEST_XLS_SPREAD_DISCOUNT_RECEIPT_V1_COLUMNS_CONFLICT_2, 'rb') as file:
        err_log = []

        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(logger, file, DUMMY_SHOPSDATA, dict(), err_log, tags=TAG_ADV)
        assert len(err_log) == 1
        assert err_log[0] == u'ERROR! [page-2]: Могут быть указаны либо бренды включения, либо бренды исключения'

    with io.open(TEST_XLS_MAX_ITEMS, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
        )

        # Проверяем, что работает порог в 2 оффера на акцию типа cheapest as gift
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(
            logger,
            file,
            ShopsData(ANY_SHOP, shopsdat_table_rows),
            uniques,
            err_log,
            custom_params=[
                {'promo_type': 'direct-discount', 'max_items_per_promo': 1},
                {'promo_type': 'cheapest-as-gift', 'max_items_per_promo': 2},
            ]
        )

        assert len(err_log) == 1
        assert err_log[0] == 'ERROR! [page-2 C5]: Превышено максимальное количество (2) элементов в акции "cg_max_items"'

    with io.open(TEST_XLS_MAX_ITEMS, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
        )

        # Проверяем, что работает дефолтный порог в 2 оффера
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(
            logger,
            file,
            ShopsData(ANY_SHOP, shopsdat_table_rows),
            uniques,
            err_log,
            custom_params=[
                {'promo_type': 'direct-discount', 'max_items_per_promo': 1},
                {'promo_type': 'default', 'max_items_per_promo': 2},
            ]
        )

        assert len(err_log) == 1
        assert err_log[0] == 'ERROR! [page-2 C5]: Превышено максимальное количество (2) элементов в акции "cg_max_items"'

    with io.open(TEST_XLS_MAX_ITEMS, 'rb') as file:
        uniques = {}
        err_log = []
        shopsdat_table_rows = (
            {'shop_id': 241241, 'warehouse_id': 100, 'datafeed_id': 10465853, 'is_dsbs': False},
        )

        # После увеличения порога до 3-х офферов акция загружается успешно
        (promo_list, n_promos, last_promo_date, msg_ver) = read_xls(
            logger,
            file,
            ShopsData(ANY_SHOP, shopsdat_table_rows),
            uniques,
            err_log,
            custom_params=[
                {'promo_type': 'direct-discount', 'max_items_per_promo': 1},
                {'promo_type': 'default', 'max_items_per_promo': 2},
                {'promo_type': 'cheapest-as-gift', 'max_items_per_promo': 3},
            ]
        )

        assert len(err_log) == 0
