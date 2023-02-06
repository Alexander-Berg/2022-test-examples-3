# coding=utf-8

import unittest
import textwrap

from market.idx.pylibrary.mindexer_core.banner.banner_yql_lib import (
    BannerYQLHelpers,
    BannerYQLStatementsBuilder,
    MarketToGoogleCategsMappings
)

# ВАЖНО: отступы имеют значение внутри YQL выражения, т.е. там может быть втроенный питон.
# Он не будет выполняться, если скрипт с питоном начинается не с начала строки.
# Поэтому в тестах учитываются отступы


class YqlTestBase(unittest.TestCase):
    BAD_VENDORS = '242456,111,222'
    BAD_PARENT_CATEGORIES = '6091783,90802,13360737,8475840,90829,14334539,16155381'
    BAD_GOOGLE_CATEGORIES = '90533,818955,15754673'

    @staticmethod
    def _remove_whitespaces(text):
        # 1. remove whitespace-only lines
        # 2. rstrip lines
        return '\n'.join([line.rstrip() for line in text.splitlines() if line.strip()])

    def setUp(self):
        self.maxDiff = None

    def assertEqualYQL(self, fact, expected):
        self.assertMultiLineEqual(self._remove_whitespaces(fact), self._remove_whitespaces(expected))


class TestHelpers(YqlTestBase):
    def test_common_options(self):
        fact = BannerYQLHelpers.common_options(
            yt_cluster='arnold',
            yt_pool='some_pool',
        )
        expected = textwrap.dedent(u'''\
            -- ========== <Generated block: common_options> ==========
            USE arnold;
            PRAGMA yt.Pool = "some_pool";
            -- ========== </ Generated block: common_options> ==========
        ''')
        self.assertEqualYQL(fact, expected)

    def test_prepare_category_with_google(self):
        fact = BannerYQLHelpers.prepare_categories_with_google(
            yql_input_category_table='$input_categories',
            yql_output_category_table='$google_categories',
            bad_parent_categories=self.BAD_PARENT_CATEGORIES,
            root_category=90401,
        )
        expected = textwrap.dedent(u'''\
            -- ========== <Generated block: prepare_categories_with_google> ==========
            $banner_allow_src = @@
            def banner_allow(category_path):
                bad_categories = set([str(id) for id in [6091783,90802,13360737,8475840,90829,14334539,16155381]])
                parents = set(category_path.split(","))
                return not any(parents & bad_categories)
            @@;

            $bannerAllow = Python::banner_allow('(String?)->Bool', $banner_allow_src);

            $category_with_parents = (
                SELECT
                    hyper_id,
                    parents_with_order
                FROM (
                    SELECT
                        hyper_id,
                        -- ListEnumerate is used to save order before flattening list to table (see calculation of category_with_google below)
                        ListEnumerate(ListReverse(ListMap(String::SplitToList(parents, ","), "FromString", YQL::['Int64]))) as parents_with_order
                    FROM $input_categories
                    WHERE $bannerAllow(parents)
                )
                FLATTEN BY parents_with_order
                WHERE parents_with_order.1 != 90401
            );

            -- Make some categ-related fields: --
            --     * parent_name - full path in market category tree --
            --     * google_category - the most parent market categ mapped to google categ name --
            $google_categories = (
                SELECT
                    hyper_id,
                    String::JoinFromList(sorted_parent_names, " > " ) AS parent_name,
                    CASE sorted_parent_names{0}
            WHEN "Электроника" THEN "Электроника"
            WHEN "Компьютерная техника" THEN "Электроника"
            WHEN "Детские товары" THEN "Товары для новорожденных и маленьких детей"
            WHEN "Красота и здоровье" THEN "Красота и здоровье"
            WHEN "Дом и дача" THEN "Дом и сад"
            WHEN "Бытовая техника" THEN "Дом и сад"
            WHEN "Товары для животных" THEN "Животные и товары для питомцев"
            WHEN "Все для офиса" THEN "Канцелярские товары"
            WHEN "Досуг и развлечения" THEN "Искусство и развлечения"
            WHEN "Авто" THEN "Транспортные средства и запчасти"
            WHEN "Услуги по подписке" THEN "Программное обеспечение"
            WHEN "Продукты" THEN "Продукты, напитки и табачные изделия"
            WHEN "Спорт и отдых" THEN "Спортивные товары"
            WHEN "Оборудование" THEN "Оборудование и технические изделия"
            WHEN "Одежда, обувь и аксессуары" THEN "Предметы одежды и принадлежности"
                    ELSE ""
                    END as google_category
                FROM (
                    SELECT
                        cat.hyper_id as hyper_id,
                        -- Sorting by order added in previous query, since LIST does not guarantee any order, and then keeping only value
                        ListMap(ListSort(LIST((cat.parents_with_order.0, par.name))), ($tpl) -> {return $tpl.1;}) as sorted_parent_names
                    FROM $category_with_parents as cat
                    JOIN $input_categories as par
                    ON par.hyper_id = cat.parents_with_order.1
                    GROUP BY cat.hyper_id
                )
            );
            -- ========== </ Generated block: prepare_categories_with_google> ==========
        ''')
        self.assertEqualYQL(fact, expected)


class TestStatementsBuilder(YqlTestBase):
    def test_beru_google_merchant_xml(self):
        builder = BannerYQLStatementsBuilder(yt_cluster='arnold', yt_pool='some_pool')
        fact = builder.beru_google_merchant_xml(
            beru_shop_id=111,
            categories_table='//home/path/to/categories_table',
            offers_table='//home/path/to/offers_table',
            models_table='//home/path/to/models_table',
            vendors_table='//home/path/to/vendors_table',
            mskus_table='//home/path/to/mskus_table',
            bad_vendors=self.BAD_VENDORS,
            min_barcode_length=8,
            bad_parent_categories=self.BAD_PARENT_CATEGORIES,
            categories_list=self.BAD_GOOGLE_CATEGORIES,
            result_table='//home/path/to/result_table'
        )

        expected_common_options = BannerYQLHelpers.common_options(
            yt_cluster='arnold',
            yt_pool='some_pool',
        )

        expected_categories_with_google = BannerYQLHelpers.prepare_categories_with_google(
            yql_input_category_table='$category_table',
            yql_output_category_table='$category_with_google',
            bad_parent_categories=self.BAD_PARENT_CATEGORIES,
        )
        expected = textwrap.dedent(u'''\
            -- ============= Utils ================ --

            $mskuPictureExtractorSrc = @@
            def mskuPictureExtractor(picture_str):
                if not picture_str:
                    return ""
                protocol_end_position = picture_str.find('//')
                if protocol_end_position > 0:
                    return picture_str[protocol_end_position:]
                return picture_str
            @@;
            $mskuPictureExtractor = Python::mskuPictureExtractor('(String?)->String', $mskuPictureExtractorSrc);

            -- ============= Options ============== --
            {common_options}
            $shop_id = 111;

            -- ============= Input data ============== --
            $category_table = [//home/path/to/categories_table];
            $offers_table = [//home/path/to/offers_table];

            $models_table = [//home/path/to/models_table];
            $vendors_table = [//home/path/to/vendors_table];
            $mskus_table = [//home/path/to/mskus_table];

            -- ============= Content data prepare (categs, models, vendors) ============== --
            {categories_with_google}

            $models_assembled = (
                SELECT
                    model.id as model_id,
                    gc.google_category as google_category,
                    vendor.name as vendor,
                    gc.parent_name as parent_name
                FROM $models_table AS model
                JOIN $category_with_google as gc
                ON model.category_id == gc.hyper_id
                JOIN $vendors_table as vendor
                ON model.vendor_id == vendor.id
                WHERE vendor.id not in (242456,111,222)
                AND (model.vendor_min_publish_timestamp == 0 OR model.vendor_min_publish_timestamp <= DateTime::ToSeconds(CurrentUtcTimestamp()))
                AND (False OR (model.current_type != "PARTNER"))
            );

            -- ============= Offers data prepare, join with content data ============== --
            $original = (
                SELECT
                    market_sku,
                    hyper_id,
                    model_id,
                    MIN_BY(title, price) as title,
                    MIN_BY(description, price) as description,
                    category,
                    google_product_category,
                    MIN(price) as price,
                    MIN_BY(sale_price, price) as sale_price,
                    currency,
                    brand,
                    $mskuPictureExtractor(MIN_BY(picture, price)) as picture,
                    AGGREGATE_LIST(DISTINCT barcode) as barcodes
                FROM (
                    SELECT
                        offer.market_sku as market_sku,
                        offer.category_id as hyper_id,
                        offer.model_id as model_id,
                        offer.title as title,
                        offer.description as description,
                        m.parent_name as category,
                        m.google_category as google_product_category,
                        CASE
                            WHEN offer.oldprice IS NOT NULL THEN CAST(String::Substring(offer.oldprice, 4) as UINT64)
                            ELSE CAST(String::Substring(offer.price, 4) as UINT64)
                        END as price,
                        CASE
                            WHEN offer.oldprice IS NOT NULL THEN CAST(String::Substring(offer.price, 4) as UINT64)
                            ELSE null
                        END as sale_price,
                        CASE
                            WHEN length(offer.barcode) >= 8 and offer.barcode match '\\\\d+'
                            THEN offer.barcode
                            WHEN length(msku.offer.barcode) >= 8 and msku.offer.barcode match '\\\\d+'
                            THEN msku.offer.barcode
                            ELSE null
                        END as barcode,
                        String::Substring(offer.price, 0, 3) as currency,
                        m.vendor as brand,
                        offer.picture_url as picture,
                        offer.has_gone as has_gone
                    FROM $offers_table as offer
                    JOIN $models_assembled as m ON offer.model_id == m.model_id
                    JOIN $mskus_table as msku ON offer.market_sku = msku.msku
                    WHERE offer.shop_id=$shop_id
                    and not TestBit(offer.flags, 17)
                    and offer.category_id not in (90533,818955,15754673)
                    and (offer.has_gone is NULL or not offer.has_gone)
                    and offer.disabled_flags == 0
                    and not offer.disabled_by_dynamic
                    and String::Substring(offer.price, 0, 3) = "RUR"
                    and offer.warehouse_id != 147
                )
                GROUP BY market_sku,
                hyper_id,
                model_id,
                category,
                google_product_category,
                currency,
                brand
            );

            -- ============= Output result ============== --
            INSERT INTO [//home/path/to/result_table] WITH TRUNCATE
            SELECT
                orig.market_sku as market_sku,
                orig.hyper_id as hyper_id,
                orig.model_id as model_id,
                orig.title as title,
                orig.description as description,
                orig.category as category,
                orig.google_product_category as google_product_category,
                orig.price as price,
                orig.sale_price as sale_price,
                orig.currency as currency,
                orig.brand as brand,
                orig.picture as picture,
                orig.barcodes as barcodes,
                "in stock" as mstat_available,
                "in stock" as availability
                FROM $original as orig
        ''').format(
            common_options=expected_common_options,
            categories_with_google=expected_categories_with_google,
        )
        self.assertEqualYQL(fact, expected)
