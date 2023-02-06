#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.unified_hide_rules import UnifiedHideRule, UnifiedHideRulesFeature, UnifiedHideRuleAccessDynamic
from core.testcase import TestCase, main
from core.types import BlueOffer, MarketSku, Offer, Shop, DynamicMarketSku, Region
from core.dynamic import MarketDynamic
from core.logs import ErrorCodes

import os

CREATION_DATE = '1970-01-01T00:02:03'
CREATION_TIME = 123  # timestamp of CREATION_DATE

SKU_ID = 1
SKU_ID_BLOCKED_IN_MSK = 2
FEATURE_SKU_ID = 3

MODEL_ID = 100

BLUE_OFFER_SHOP_SKU = 'blue_offer_shop_sku'
WHITE_OFFER_ID = 'white_offer_id'

BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK = 'blue_offer_shop_sku_blocked_in_msk'
WHITE_OFFER_ID_BLOCKED_IN_MSK = 'white_offer_id_blocked_in_msk'

BLUE_OFFER_ALWAYS_VISIBLE_ID = 'blue_offer_always_visible'
BLUE_OFFER_ONLY_VISILBE_WITH_NONACTIVE_FEATURE_ID = 'blue_offer_only_visilbe_with_active_feature'
BLUE_OFFER_ONLY_VISILBE_WITH_ACTIVE_FEATURE_ID = 'blue_offer_only_visilbe_with_nonactive_feature'
WHITE_OFFER_ALWAYS_VISIBLE_ID = 'white_offer_always_visible'
WHITE_OFFER_ONLY_VISIBLE_WITH_NONACTIVE_FEATURE_ID = 'white_offer_only_visible_with_active_feature'
WHITE_OFFER_ONLY_VISIBLE_WITH_ACTIVE_FEATURE_ID = 'white_offer_only_visible_with_nonactive_feature'

BLUE_SHOP_ID = 1
WHITE_SHOP_ID = 2

BLUE_SHOP_ID_BLOCKED_IN_MSK = 3
WHITE_SHOP_ID_BLOCKED_IN_MSK = 4

HID = 1000

# Москва и потомки
MOSCOW_RID = 213
HAMOVNIKY_RID = 17
TROITSKIY_DISTRICT_RID = 18
TROITSK_RID = 19
NOVOMOSKOVSKIY_DISTRICT_RID = 20
NOVOMOSKOVSK_RID = 21

# Питер и потомки
PITER_RID = 2
VASYA_OSTROV_RID = 15
PALACE_RID = 16


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.use_unified_hide_rules_from_access = True
        cls.settings.market_access_settings.use_market_dynamic_from_access_for_search = True

        cls.index.regiontree += [
            Region(
                rid=PITER_RID,
                region_type=Region.CITY,
                children=[
                    Region(rid=VASYA_OSTROV_RID, region_type=Region.CITY_DISTRICT),
                    Region(rid=PALACE_RID, region_type=Region.CITY_DISTRICT),
                ],
            ),
            Region(
                rid=MOSCOW_RID,
                region_type=Region.CITY,
                children=[
                    Region(
                        rid=TROITSKIY_DISTRICT_RID,
                        region_type=Region.CITY_DISTRICT,
                        children=[
                            Region(rid=TROITSK_RID, region_type=Region.CITY),
                        ],
                    ),
                    Region(
                        rid=NOVOMOSKOVSKIY_DISTRICT_RID,
                        region_type=Region.CITY_DISTRICT,
                        children=[
                            Region(rid=NOVOMOSKOVSK_RID, region_type=Region.CITY),
                        ],
                    ),
                    Region(rid=HAMOVNIKY_RID, region_type=Region.CITY_DISTRICT),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=SKU_ID,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_SHOP_SKU,
                        feedid=BLUE_SHOP_ID,
                    )
                ],
            ),
            MarketSku(
                sku=SKU_ID_BLOCKED_IN_MSK,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK,
                        feedid=BLUE_SHOP_ID_BLOCKED_IN_MSK,
                    )
                ],
            ),
        ]
        cls.index.offers += [
            Offer(offerid=WHITE_OFFER_ID, sku=SKU_ID, fesh=WHITE_SHOP_ID, hyperid=MODEL_ID, hid=HID),
            Offer(
                offerid=WHITE_OFFER_ID_BLOCKED_IN_MSK,
                sku=SKU_ID_BLOCKED_IN_MSK,
                fesh=WHITE_SHOP_ID_BLOCKED_IN_MSK,
                hyperid=MODEL_ID,
                hid=HID,
            ),
        ]
        cls.index.shops += [
            Shop(
                fesh=BLUE_SHOP_ID,
                datafeed_id=BLUE_SHOP_ID,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=BLUE_SHOP_ID_BLOCKED_IN_MSK,
                datafeed_id=BLUE_SHOP_ID_BLOCKED_IN_MSK,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

    @classmethod
    def prepare_report_market_dynamic_from_access(cls):
        cls.settings.market_access_settings.download_market_dynamic = True
        cls.settings.market_access_settings.use_market_dynamic_from_access_for_search = True

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        access_server.create_publisher(name='mbi')
        access_server.create_resource(name='market_dynamic', publisher_name='mbi')

        market_dynamic_v1_path = os.path.join(
            cls.meta_paths.access_resources, 'market_dynamic/1.0.0/market_dynamic.tar.gz'
        )
        mds_market_dynamic_v1_url = cls._get_mds_url(shade_host_port, market_dynamic_v1_path)
        dynamic = MarketDynamic(
            src_path=cls.meta_paths.access_resources_tmp,
            dst_path=cls.meta_paths.access_resources_tmp,
            paths=cls.meta_paths,
            creation_time=CREATION_TIME,
        )
        dynamic.disabled_market_sku = [DynamicMarketSku(market_sku=str(SKU_ID))]
        dynamic.save_archive(market_dynamic_v1_path)
        access_server.create_version('market_dynamic', http_url=mds_market_dynamic_v1_url)

        hide_rule_dynamic = UnifiedHideRuleAccessDynamic(
            access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths
        )

        hide_rule_dynamic.hide_rules += [
            UnifiedHideRule(
                rule_id=1,
                mskus_include=[
                    SKU_ID,
                ],
            ),
            UnifiedHideRule(
                rule_id=2,
                regions_include=[
                    MOSCOW_RID,
                ],
                regions_exclude=[
                    TROITSK_RID,
                ],
                mskus_include=[
                    SKU_ID_BLOCKED_IN_MSK,
                ],
            ),
            UnifiedHideRule(
                rule_id=3,
                mskus_include=[
                    FEATURE_SKU_ID,
                ],
                offer_id_include=[
                    WHITE_OFFER_ONLY_VISIBLE_WITH_NONACTIVE_FEATURE_ID,
                    BLUE_OFFER_ONLY_VISILBE_WITH_NONACTIVE_FEATURE_ID,
                ],
            ),
            UnifiedHideRule(
                rule_id=4,
                mskus_include=[
                    FEATURE_SKU_ID,
                ],
                offer_id_include=[
                    WHITE_OFFER_ONLY_VISIBLE_WITH_ACTIVE_FEATURE_ID,
                    BLUE_OFFER_ONLY_VISILBE_WITH_ACTIVE_FEATURE_ID,
                ],
            ),
        ]

        # NOTE: unfortunately lite tests don't allow rearr flags without a value, so we must name our features like
        # `feature_name=something` in order to feed it to lite tests
        # Whether report supports such rearr flags is another question. Internally it just splits the flag string by
        # semicolons and saves the result in a hashset (which is then used in UHR to extract flags).
        # If at some point lite tests will allow for "valueless" flags, all of `=1` will have to be removed
        hide_rule_dynamic.features += [
            UnifiedHideRulesFeature(name="enable_rule_3=1", enabled_rules=[3]),
            UnifiedHideRulesFeature(name="disable_rule_4=1", disabled_rules=[4]),
        ]

        hide_rule_dynamic.create_version()

    @staticmethod
    def _get_mds_url(shade_host_port, path):
        path = path if path.startswith('/') else '/' + path
        return '{host_port}/mds{path}'.format(
            host_port=shade_host_port,
            path=path,
        )

    def check_offers(self, msku, blue, white, region=0, hide_rules_strategy='use_unified_hide_rules', features=[]):
        blue_offer_id, show_blue = blue
        white_offer_id, show_white = white
        blue_offer_expected = {'shopSku': blue_offer_id}
        white_offer_expected = {'feed': {'offerId': white_offer_id}}

        request = 'place=offerinfo&market-sku={msku}&show-urls=&rids={region}&regset=1&rearr-factors=hide_rules_strategy={strategy}'.format(
            msku=msku, region=region, strategy=hide_rules_strategy
        )

        if len(features) > 0:
            request = request + ';' + ';'.join(features)

        response = self.report.request_json(request)
        if show_blue:
            self.assertFragmentIn(response, blue_offer_expected)
        else:
            self.assertFragmentNotIn(response, blue_offer_expected)
        if show_white:
            self.assertFragmentIn(response, white_offer_expected)
        else:
            self.assertFragmentNotIn(response, white_offer_expected)

    def test_unified_hide_rules_from_access(self):
        '''
        Проверяем, что белый и синий офферы скрыты через access
        '''
        self.check_offers(SKU_ID, (BLUE_OFFER_SHOP_SKU, False), (WHITE_OFFER_ID, False))

    def test_generate_unified_hide_rules_from_access_market_dynamic(self):
        '''
        После отключения выкачивания mmap файла с универсальными правилами через access,
        mmap файл с универсальными скрытиями будет сгенерирован под репортом на основе маркет динамика скачанного из access,
        оффера будут также скрыты
        '''
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(use_unified_hide_rules_from_access=False)
        self.emergency_flags.save()
        self.restart_report()

        self.check_offers(SKU_ID, (BLUE_OFFER_SHOP_SKU, False), (WHITE_OFFER_ID, False))

    def test_disable_access_sources(self):
        '''
        После отключения выкачивания mmap файла с универсальными правилами и маркет динамика через access будут показаны оба оффера
        '''
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(
            use_unified_hide_rules_from_access=False, use_access_market_dynamic_for_search=False
        )
        self.emergency_flags.save()
        self.restart_report()

        self.check_offers(SKU_ID, (BLUE_OFFER_SHOP_SKU, True), (WHITE_OFFER_ID, True))

    def test_region_hidings(self):
        '''
        Проверяем региональные скрытия
        '''
        # Включаем загрузку правил через Access
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(
            use_unified_hide_rules_from_access=True, use_access_market_dynamic_for_search=True
        )
        self.emergency_flags.save()
        self.restart_report()

        # Белый и синий оффера скрыты везде в Москве и потомках, за исключением Троицка
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, False),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, False),
            region=MOSCOW_RID,
        )
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, False),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, False),
            region=HAMOVNIKY_RID,
        )
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, False),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, False),
            region=TROITSKIY_DISTRICT_RID,
        )
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, False),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, False),
            region=NOVOMOSKOVSKIY_DISTRICT_RID,
        )
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, False),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, False),
            region=NOVOMOSKOVSK_RID,
        )

        # В Троицке и Питере оффера открыты
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, True),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, True),
            region=TROITSK_RID,
        )
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, True),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, True),
            region=PITER_RID,
        )

    @classmethod
    def prepare_features(cls):
        cls.index.mskus += [
            MarketSku(
                sku=FEATURE_SKU_ID,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_ALWAYS_VISIBLE_ID,
                        feedid=BLUE_SHOP_ID,
                    )
                ],
            ),
            MarketSku(
                sku=FEATURE_SKU_ID,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_ONLY_VISILBE_WITH_NONACTIVE_FEATURE_ID,
                        feedid=BLUE_SHOP_ID,
                    )
                ],
            ),
            MarketSku(
                sku=FEATURE_SKU_ID,
                hyperid=MODEL_ID,
                hid=HID,
                blue_offers=[
                    BlueOffer(
                        offerid=BLUE_OFFER_ONLY_VISILBE_WITH_ACTIVE_FEATURE_ID,
                        feedid=BLUE_SHOP_ID,
                    )
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                offerid=WHITE_OFFER_ALWAYS_VISIBLE_ID, sku=FEATURE_SKU_ID, fesh=WHITE_SHOP_ID, hyperid=MODEL_ID, hid=HID
            ),
            Offer(
                offerid=WHITE_OFFER_ONLY_VISIBLE_WITH_NONACTIVE_FEATURE_ID,
                sku=FEATURE_SKU_ID,
                fesh=WHITE_SHOP_ID,
                hyperid=MODEL_ID,
                hid=HID,
            ),
            Offer(
                offerid=WHITE_OFFER_ONLY_VISIBLE_WITH_ACTIVE_FEATURE_ID,
                sku=FEATURE_SKU_ID,
                fesh=WHITE_SHOP_ID,
                hyperid=MODEL_ID,
                hid=HID,
            ),
        ]

    def test_features(self):
        """
        Test the features mechanism in UHR
        Currently all the features are just strings parsed from rearr flags
        See MINIMARKET-101
        """
        # Включаем загрузку правил через Access
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(
            use_unified_hide_rules_from_access=True, use_access_market_dynamic_for_search=True
        )
        self.emergency_flags.save()
        self.restart_report()

        # No active features => offers must be visible
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ALWAYS_VISIBLE_ID, True),
            (WHITE_OFFER_ALWAYS_VISIBLE_ID, True),
            region=0,
            features=[],
        )
        # All disabling features are active - the offers still must be visible
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ALWAYS_VISIBLE_ID, True),
            (WHITE_OFFER_ALWAYS_VISIBLE_ID, True),
            region=0,
            features=['disable_rule_4=1'],
        )
        # All enabling features are active - the offers still must be visible
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ALWAYS_VISIBLE_ID, True),
            (WHITE_OFFER_ALWAYS_VISIBLE_ID, True),
            region=0,
            features=['enable_rule_3=1'],
        )

        # No active features - offers are visible
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ONLY_VISILBE_WITH_NONACTIVE_FEATURE_ID, True),
            (WHITE_OFFER_ONLY_VISIBLE_WITH_NONACTIVE_FEATURE_ID, True),
            region=0,
            features=[],
        )
        # Enabling features active - offers are hidden
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ONLY_VISILBE_WITH_NONACTIVE_FEATURE_ID, False),
            (WHITE_OFFER_ONLY_VISIBLE_WITH_NONACTIVE_FEATURE_ID, False),
            region=0,
            features=['enable_rule_3=1'],
        )

        # No active features - offers are hidden
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ONLY_VISILBE_WITH_ACTIVE_FEATURE_ID, False),
            (WHITE_OFFER_ONLY_VISIBLE_WITH_ACTIVE_FEATURE_ID, False),
            region=0,
            features=[],
        )
        # Disabling feature is active - offers are visible
        self.check_offers(
            FEATURE_SKU_ID,
            (BLUE_OFFER_ONLY_VISILBE_WITH_ACTIVE_FEATURE_ID, True),
            (WHITE_OFFER_ONLY_VISIBLE_WITH_ACTIVE_FEATURE_ID, True),
            region=0,
            features=['disable_rule_4=1'],
        )

    def test_errors_count(self):
        '''
        Проверяем, что ошибка о расхождении скрытий между динамиком и универсальными правилами пишется один раз на запрос
        '''

        # Включаем загрузку правил через Access
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(
            use_unified_hide_rules_from_access=True, use_access_market_dynamic_for_search=True
        )
        self.emergency_flags.save()
        self.restart_report()

        # Региональных скрытий нет в динамиках, поэтому будет генерироваться ошибка
        self.base_error_log.expect(code=ErrorCodes.UNIFIED_HIDE_RULE_SERVICE_GAP).times(1)
        self.check_offers(
            SKU_ID_BLOCKED_IN_MSK,
            (BLUE_OFFER_SHOP_SKU_BLOCKED_IN_MSK, False),
            (WHITE_OFFER_ID_BLOCKED_IN_MSK, False),
            region=MOSCOW_RID,
            hide_rules_strategy='use_all_sources',
        )


if __name__ == '__main__':
    main()
