#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, MarketSku, Offer, Region, Shop, Stream, StreamName, Tax
from core.testcase import TestCase, main
from core.matcher import Wildcard, Round


class T(TestCase):
    class TestExpansions:
        TEXT = 'vga hdmi port'

    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True

    @classmethod
    def prepare_use_begemot(cls):
        qtree = 'cHicrZTPaxNBFMffm2zjOK0SWqpxIRrWg2tACOKhCKkiHoKI1OCh7sW2aWgqsZG0aMnFHERCD-IPEFoqQrVNUjBqPWuv_ugh_Qe86cmjvXjyze5ku4bNYsFAeLsz3_edz5u3M-KS6OMQgSjEmcmS0A86GJCA03C2PQ4mJOF8T7pnBEZhDPL4CGEJYQVhA-EDAv0-IbRwUq-huKrsOKVJu9CdqXEd465lyGMJaZCW-d8fWdtSyjtckzCEF9IcI6DLWQNMTOLIU-awlBJCjkZtxyHIsGrTwtWJMKnBHJjgFNmJWTOUh3lWZpxVEGgtX9JSdpxqx3gA6Y_HmktK8gBSmvUhpdGoHA0kRS-pcYDfi1QwyuIhsycJia8hkVbgmgLv1rE2dPd-_WK2GYsw8g82U-0PMLuPYkyR7VffUjhbnJnLzduWYWOqlMvN2MbhrpQqYwWV3G-HD8k1pKe9veqbLIPqbBXFZAAGN7KF8dnZ6ey_grgJe0YxHAoW16R_EofQGZeZzlPiO_7Hbj5EcbOj8t5bxclc4Ua2WCiWVBfu5qfncrZ7b1d3b5pshZ2z5_p365RP1jvkMCjz9RcornUcvnBro_W-9dZTu9_xc2tXcj-klH3-lECBvWwfwaNCTbinENoHMM_nwe2cJuFMzXrFOCfm8kX9CRNWJ_N2ZfsBMeNJA80A6uVn3167F5xK8iNvoIPuKDrRzwg1EUWFrlWbm18sXLewbmHNwjULG54bhe4RLGV8spY3P1ts_Q39mxarU6xTrFGsUVyjuEaxQbFBcZXiatPrSXcTuttEV4cpN20Bjwl70_oPc9QHeHhwa-tyKhYZvZ6KwylZaFvBXMXz26lzsczS4PBfCuZRLC4eGY7t_Dy-qxhQHr2cZ_Zx7A9dGcktYJ9aW-MHS-C8Shv7lTj_AKbnRy8,'  # noqa

        cls.reqwizard.on_request('vga rca').respond(
            qtree=qtree,
        )

        cls.reqwizard.on_request('vga rca', wizextras='market-lingboost').respond(
            qtree=qtree, expansions='2tACOKhCKkiHoKI1OCh7sW'
        )

        cls.reqwizard.on_pure_request('vga markup').respond({})

    def test_use_begemot(self):
        """
        Проверяем, что с флагом UseBegemotForService в конфиге запрос к реквизарду формируется
        так, чтобы использовалась специальная бегемотная инсталляция реквизарда.
        """

        self.error_log.expect(code=3670).once()

        request_prime = 'place=prime&debug=da&text=vga+rca'
        request_markup = 'place=brand_products&vendor_id=4&debug=da&text=vga+markup'

        for request, has_markup in ((request_prime, False), (request_markup, True)):
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {"logicTrace": [Wildcard('*/wizard*wizclient=market-main-report*')]})
            self.assertFragmentIn(response, {"logicTrace": [Wildcard('*/wizard*wizextra=market-lingboost*')]})

            markup = {"logicTrace": [Wildcard('*/wizard*action=markup*')]}

            if has_markup:
                self.assertFragmentIn(response, markup)
            else:
                self.assertFragmentNotIn(response, markup)
                self.assertFragmentIn(response, {"logicTrace": [Wildcard('*Got expansions for the request*')]})

    @classmethod
    def prepare_expansions(cls):
        cls.settings.use_factorann = True

        qtree = 'cHicrZZNaFxVFMfPue_N5M01LUOG4vggdQxqXyviVDdDIaHGIKELSapIHCl00plkBkxSXsYa3Di1fowGxI-F4ipYUvxiGtMKFWpaEWrtagYRughuhCLSjRtB3Xjvfd937jybwWxy373_c-ac3zn3vEeP0EFjV3p3FnJokTwMgQkjcAAehUODBqSB7YMFeTicmExMwQwcT1bxXYSPET5B2ET4FoH9XUdoY9n8DOk0dcwMZsbdaafmSybmfJdayCVMAndZvfjXXs8ll0te81DA8SMGpsHkpyNgMX0epz4gLBqoon2A8v2s8FmAo6TZKuL6bJLpwcrMGuw_2bdsaVVYIS8TgzQQ2K-Z7xH6jBSrXi0v1HiwmHOj1RXR_vlzxotWGKjCbaGIV5yzgJlJOOCHqDjIooXxEeMKse-TxFpzvVUETywko5IEm0Wy3ur2NzuQJiY6CzBxn_MLISbrhD4tMzm5ZNdZS8QiubVNfCRcr0LyvYuEnzMkzCSMhKXAD1gKsNMUWFntcdn8rSJZu1DEr6IeKmVmmGIetNrivPChCWfCx72yj0YIc6R3RnYZr6QbmCU5zUrk5UeebfGXAeO1RgP3QKM6Yf44QCcZVpImglzcLUtOJgXR3nfsNtJjUo0Sp2rlypKZyj34iHUwd3A_a2DL9Z1QVOtmynPuGKqq1XCq5QhYuTBcrDHq7PdbrVyXfU_U5iahJ-SWnC8tVMLZBsuYHv37Hr9Fub0q6atui_JzOWfWoHy735Qfl827GlTIy8IOHQdBo3r9GXURC-24BC1Zeqlkl5dN3N-NK6nA9dH7_pV2TWMutatQ9Ik4iEI77watdUPLyLfatYeAmr7WurAV9dDjVgso9l7qBpcFdzoG0IwVCF1q54LyS6znpac8FtBV6ZyqBcW1lHvBDy9NmKup__GCbxD6fNeLqTZfVd5vVaf_VgjeT8xOVbdr3vuJnctVe4yKbWWfucBtgR5Nslx3S9VldPoOjEZlox3dqVC_30J6UkJGy5W52mKtXltajB0VVAHwnc_93g-5UXGsCowhkQzzCRo6DPq430SvKxJdePGFem2hUq6Vdp7od9RPNHDT-_srJJITfYCGDoNEe8-os4TOSamkaov1ij1XOlFRjqmUIoPLTd3LILCO-SILRHIC4zQ463fKP6n0seNRf2dDKWZEsX02or4kxmk-omYOFSbMD0nXZ4PRvtLe6jQ6r3LasRPlZqvtY_atVJS_cCj7GhfyWQ_ym0j9M79JtObVG0XyaasbdXvDm_Hty_7qklgRk3Re91btr9mKMph6e6u96c0Z14Yrz3h7nVWBGBniAJee1i29uK05uN4YHpkwf9BoRcI16AXOfyOCTPX-vPbPr5qHLGKpwvYTEdgiOhndmd7ozkmv1D6xZf4bm30_jQSpfKmibdF0WNVZbX8TURJf-SylnrKzGkrr_I0innNj2-iZBWWrIHYSV1729DayTyhe0qG7DTQzRnLP79OXRofTM8-N5uBhXhJPQXzFH9b22PBqITEWUZCQ4vZ0YWx4drkcKDLChz50l2EcHTDIkPbUVMXZJf4uuruD7u_pxm4bnEcWrvPIov8XzUgLnQ,,'  # noqa

        expansions = 'ehRMWi40AQAAAACAKgUB8A4KQCozeh4SLRIPEgNkdmkYACACMgQQ5uxKIJMCKhIAkTICCAI4AUAAShYAEVIGAP8sIAAwhr6Gr4HuiJybAQphKlV6HhJPEiASFNC_0LXRgNC10YXQvtC00L3QuNC6GAAgATIEEOLVByCjAiojAAJDMgIIAWQAQvTNJFItAPETADCMoPfkkYuUsTUKUipGeh4SQBIYEgtkaXNwbGF5cG9ydK0AYAUQ1pTOAa4ACBsABbYAcwUQnvvPAVImAPEOADCAkZjWnty99wkKZipZeh4SUxIQEgRoZG1pGAH6AD_21RMSABEAcAABNwAFaQBhBBDW1hNSQQDfIAAwyenF3P6Rp67NAbwAqtFJKj16HhI3Eg0SAnRvbwFDAxDEAg8AADEBAA8AAAwBMwJ0bwoBIAFKFAAQUgUA8xUgADCq96SbtNSNljYKUipFeh4SPxIYEgzQutCw0LHQtdC70YweAjDQpAEeAgkbAAcWAkLc1AJSJQDPADD9-cXGxdmq-8kBvAIwE0rhALMUEgjQv9C-0YDRgpIAMIKYBpIABRcAB44AQsqqDVIhAPMXADDlkaqD3_TI79IBClYqSXoeEkMSGhIO0LTQuNGB0L_Qu9C10LlSADCI7AVSAAsdAAdYAELSzAtSJwCwADCBrLa6t5PglZNBAg86AT8PrABFD1ABOcFGKjp6HhI0EgwSAWSeA0IDEIYWDgAApgQADgAAewIjAWR6AiAAShMAEFIFAPAFIAAwkYfV8KG28Op4CjsqLnoeEii2AiHQuIwBMQMQ6G8CMwLQuH8BALYCMOgBUgUA8AYgADDOttySsv3l7NUBCjkqLHoeEiZ3AAIdBUEDEKwHdwAXaXcAMboJUhgA5wAwq_e3vdyfyPSfAQo6eAATsngAE9p4ABeyeAAQ2ngAENrbArPd_piIirSbgVkKVUwCkxESBtGH0LXQvEAAIshlEwBD0YLQvhMAIM4LVgIDJwAHVwAx4mlSHQC4ADDLjNC989LRiXijAmDRgNCw0LejBUHRhtCwTABQBBDglgpNAAsdAANVAADkBELg9BBSJwDzCwAwxbCYjOLFmZ-eAQo_KjJ6HhIsEg8SBNC9TgBAAxCOA00AARIAA0MAAGcBMI4DUgUAwCAAMMH27YDL-5K4yUEABYYGMXZnYWoEUQQQtPkehgY1dmdhbAFiBBD8kB9SHAC_ADD5yuSf2N-GpUELBDAPEAZV0z4qMXoeEisSDhICZHDqADDOxgTqADcCZHDpAEKshgVSGwDzFgAwlLeUz6vHqpTHARITCAEQERoIEgIIABoCCAAtmZgYPxInCAMVABABFQABHwAgAhoOAAApAOMDGgIIAi2HhgY_Eh0IAikAFQQpABAFKQBXLeno6D5dAAEfAFct5eTkPl0AARUAAV0AEAY0AAEKAAA-AGcCLd_e3j5dAAB8ABEAHwAQBykASC2vrq4fABUIfAAWCR8AMzEIBJsABsQAEAofAAFIAAApABECCgAAzgBYAy2dnJyaAABnAAJxAAB7AAj3ADiZmJgpABULewAVDFwAEQ0gAYOTkpI-ElkICIUABqQAFQ4pABAPKQABhQAAHgARAwoAIBAaGAEAZwEgERoOAQAKACASGiIBAAoAmBMaAggHLY2MjOAAANYAAq0AANYAAq0AFRRbAALgADjl5GSPARAVtwBILdnYWEcBEBYVAAGFABAXowACHwAPCwIBEBgfAAEpAABdAFgCLdXUVEgAAAoBApAAoA0aAggBLdXUVD4AAAA,'  # noqa
        # Only Qfuf (no OriginalRequest):
        # echo $expansions | ./quality/relev_tools/lboost_ops/unpackreqbundle/unpackreqbundle
        # ...
        # 0       Qfuf    [0:dvi ]::0.596078      (0 -> 0,0)
        # ...
        # 15      Qfuf    [0:дисплей  1:порт ]::0.207843  (12 -> 0,0) (13 -> 1,1)

        cls.reqwizard.on_request(cls.TestExpansions.TEXT).respond(
            qtree=qtree,
        )
        cls.reqwizard.on_request(cls.TestExpansions.TEXT, wizextras='market-lingboost').respond(
            qtree=qtree,
            expansions=expansions,
        )

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
            Shop(
                fesh=101010,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(fesh=101011, priority_region=213, name='blue_shop', cpa=Shop.CPA_REAL, warehouse_id=145),
        ]

        white_factor_streams = [
            Stream(name=name, region=225, annotation='дисплей порт', weight=1.0)
            for name in (
                StreamName.BQPR,
                StreamName.FIRST_CLICK_DT_XF,
                StreamName.AVG_DT_WEIGHTED_BY_RANK_MOBILE,
                StreamName.LONG_CLICK_SP,
                StreamName.ONE_CLICK,
                StreamName.SIMPLE_CLICK,
                StreamName.BQPR_SAMPLE,
                StreamName.MRKT_TITLE,
                StreamName.MRKT_CPA_QUERY,
            )
        ]

        cls.index.offers += [
            Offer(title='дисплей порт', waremd5='ZRK9Q9nKpuAsmQsKgmUtyg', fesh=1, factor_streams=white_factor_streams)
        ]

        blue_factor_streams = [
            Stream(name=name, region=225, annotation='дисплей порт', weight=1.0)
            for name in (
                StreamName.BQPR,
                StreamName.FIRST_CLICK_DT_XF,
                StreamName.AVG_DT_WEIGHTED_BY_RANK_MOBILE,
                StreamName.LONG_CLICK_SP,
                StreamName.ONE_CLICK,
                StreamName.SIMPLE_CLICK,
                StreamName.BQPR_SAMPLE,
                StreamName.BLUE_MRKT_MODEL_TITLE,
                StreamName.BLUE_MRKT_OFFER_DESCRIPTION,
                StreamName.BLUE_MRKT_OFFER_TITLE,
                StreamName.MRKT_CPA_QUERY,
            )
        ]

        cls.index.mskus += [
            MarketSku(
                fesh=101010,
                title="дисплей порт",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
                factor_streams=blue_factor_streams,
            )
        ]

    def test_expansions(self):
        """
        Проверяем, что  рекбандл, полученный из бегемота, мёржится с рекбандлом с OriginalRequest
        перед отправкой на базовый, а т.ж. считаются факторы по расширениям из него на белом и на синем.
        """
        request = 'place=prime&debug=da&rids=213&text={}'.format(self.TestExpansions.TEXT)

        response = self.report.request_json(request + '&reqid=1')
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Wildcard('*Expansions: OriginalRequest, Qfuf'),
                ]
            },
        )
        self.feature_log.expect(
            qfuf_all_max_wf_browser_page_rank_bm15_max_annotation_k0_01=Round(0.3, 2),
            qfuf_all_max_wf_first_click_dt_xf_bm15_max_annotation_k0_01=Round(0.3, 2),
            qfuf_all_max_wf_avg_dt_weighted_by_rank_mobile_bm15_max_annotation_k0_01=Round(0.3, 2),
            qfuf_all_max_wf_long_click_sp_bm15_max_annotation_k0_01=Round(0.3, 2),
            qfuf_all_max_f_one_click_bclm_mix_plain_k1e_05=Round(0.99, 2),
            qfuf_all_max_wf_simple_click_bm15_max_annotation_k0_01=Round(0.3, 2),
            qfuf_all_max_wf_market_title_bm15_max_annotation_k0_01=Round(0.3, 2),
            qfuf_top_min_wf_market_cpa_query_all_wcm_weighted_value=Round(0.30, 2),
            qfuf_all_max_f_market_cpa_query_bclm_mix_plain_k1e_05=Round(0.99, 2),
            req_id='1',
            ware_md5='ZRK9Q9nKpuAsmQsKgmUtyg',
        )

        response = self.report.request_json(request + '&rgb=blue&reqid=2')
        self.assertFragmentIn(response, {"logicTrace": [Wildcard('*Expansions: OriginalRequest, Qfuf')]})
        self.feature_log.expect(
            qfuf_all_max_wf_browser_page_rank_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_first_click_dt_xf_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_avg_dt_weighted_by_rank_mobile_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_long_click_sp_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_f_one_click_bclm_mix_plain_k1e_05=Round(1.0, 2),
            qfuf_all_max_wf_simple_click_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_bqpr_sample_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_market_blue_title_of_white_model_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_market_blue_title_of_white_offer_bclm_weighted_proximity1_bm15_size1_k0_01=Round(0.21, 2),
            qfuf_all_max_wf_market_blue_description_of_white_offer_bclm_weighted_proximity1_bm15_size1_k0_01=Round(
                0.21, 2
            ),
            qfuf_top_min_wf_market_cpa_query_all_wcm_weighted_value=Round(0.30, 2),
            qfuf_all_max_f_market_cpa_query_bclm_mix_plain_k1e_05=Round(0.99, 2),
            req_id='2',
            model_id=1,
        )

    def test_restrictions(self):
        """
        Проверяем, что рестрикты на расширения не включаются на синем и белом
        Проверяем также, что рестрикты включаются на &place=geo
        """
        request_base = 'debug=da&rids=213&text={}'.format(self.TestExpansions.TEXT)

        for rgb in ('&rgb=blue', '', '&rgb=green', '&rgb=green_with_blue'):
            response = self.report.request_json(request_base + rgb + '&place=prime')
            self.assertFragmentNotIn(response, {"logicTrace": [Wildcard('*Applying restrictions*')]})

            response = self.report.request_json(request_base + rgb + '&place=geo')
            self.assertFragmentIn(response, {"logicTrace": [Wildcard('*Applying restrictions*')]})

    @classmethod
    def prepare_bad_expansions(cls):
        cls.reqwizard.on_request('hi', wizextras='market-lingboost').respond(
            qtree='cHicdZHPK0RRFMfPOW8a120WkwnTi0wv5RE1WU2UNFlYStI0JXpGPaVoUFhNamqSjLKTlR9lNaEURVlYyOq9jYWNP8BC_gG5986dh8FbnXfu93zu_X4PH-MRBlGIswTYlIQmcudNsKAH-mEgwkicgDiBJAzDKIxBBmbA_TimXYR9hEMU8guEWwTxPSB4mOt5QT6qoSENhW9I-I10sYaDOphZQp7TsMYEKVjEWVidm84tzq6sL80pMFmOQkf-Rf-YEa-2nLqLkpDCdIu8RxJtTKL2ihtQQBAQqfiq0jmGQivsa_UeVeX5bi6acZSPSME4lSpZPHHCkmvHHCZnupZtw4U1yrdxw70_j0uA1ILTEEUT5-2Yy9Z6N4hR9pVYqVDA5mejY8R8Ip6p21bYL3pn3rWJ3RbaOmDjjxQO3oON6ZG__L-hMqUV2thRzVgRuT4J3Bmlu8csnVaydFIR3tAk79I2qi69C1XJ3o2qSFRXQe9apSEqf6fW88vB7E1QXdVm_aJKDfOJ6jP88rfgAqZIDvOdnFcV_qa__UNlfKlEvnqfVkhmass9b2EHV39NrQzNGOPNU-X0UPvk4EQqAX0yLqH_BOIcp1I,',  # noqa
            expansions='ololo',
        )

    def test_bad_expansions(self):
        self.report.request_json('place=prime&text=hi')
        self.error_log.expect(code=3670).once()


if __name__ == '__main__':
    main()
