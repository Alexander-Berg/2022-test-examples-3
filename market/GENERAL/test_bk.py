#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.types import HyperCategory, Offer, Region, ReqwExtMarkupTokenChar, Shop
from core.testcase import TestCase, main
from core.matcher import NotEmpty, NoKey, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(rid=54, name='Екатеринбург'),
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
            Region(rid=75, name='Владивосток'),
        ]

        cls.index.shops += [Shop(fesh=1, priority_region=213, regions=[225])]

        cls.index.offers += [Offer(title="кресло качалка MONDEO", fesh=1)]

        cls.index.hypertree += [HyperCategory(hid=1, name='gamaki', uniq_name='гамаки и прочая подвесная мебель')]

        # все верно - нужный таргет (smart_center), есть linkHead и count
        cls.bk.on_request(text='кресло-качалка', region=213).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head"}]},
            {
                'smart_center': [
                    {'url': 'http://some_ads_url.ru/direct/premium/1', 'count': '=direct_premium_1'},
                    {'url': 'http://some_ads_url.ru/direct/premium/2', 'count': '=direct_premium_2'},
                ]
            },
            {'bottom': [{'url': 'http://some_ads_url.ru/direct/all', 'count': '=direct_all_0'}]},
        )

        # неверный таргет (bottom)
        cls.bk.on_request(text='кресло-качалка', region=54).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head"}]},
            {'bottom': [{'url': 'http://some_ads_url.ru/ekb/direct/all', 'count': '=ekb_direct_all'}]},
        )

        # верный таргет но нет link_head
        cls.bk.on_request(text='кресло-качалка', region=2).respond(
            {"stat_exp": [{}]},
            {'smart_center': [{'url': 'http://some_ads_url.ru/ekb/direct/all', 'count': '=ekb_direct_all'}]},
        )

        # верный таргет но нет link_tail (count)
        cls.bk.on_request(text='кресло-качалка', region=75).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head"}]},
            {'smart_center': [{'url': 'http://some_ads_url.ru/ekb/direct/all'}]},
        )

        # отвечаем разной рекламой на разные заголовки
        cls.bk.on_request(
            text='кресло с подлокотниками', region=213, headers={'Cookie': 'mycookie=123', 'User-Agent': 'Mozilla'}
        ).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head_mozilla"}]},
            {'smart_center': [{'url': 'http://ads_with_cookie.ru/for_mozilla/mycookie123', 'count': '=link_tail123'}]},
        )

        cls.bk.on_request(
            text='кресло с подлокотниками', region=213, headers={'Cookie': 'mycookie=789', 'User-Agent': 'YaBro'}
        ).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head_yabro"}]},
            {'smart_center': [{'url': 'http://ads_with_cookie.ru/for_yabro/mycookie789', 'count': '=link_tail789'}]},
        )

        cls.bk.on_request(text='кресло с подлокотниками', region=213).respond('')
        # не суть важно что именно мы отдаем
        cls.adv.on_request(text='кресло с подлокотниками', region=213).respond(
            {
                "BannerID": 1138766911,
                "SimDistance": 26,
                "ContextType": 1,
                "BmScore": 158971,
                "PhraseID": 802899641,
                "BidMultiplier": 1,
            }
        )

        # отвечаем рекламой при запросе без &text= но с &suggest_text=
        cls.bk.on_request(text='gamak dlya sada', region=213).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head"}]},
            {'smart_center': [{'url': 'http://some_ads_url.ru/direct/gamak', 'count': '=direct_gamak'}]},
        )

        cls.bk.on_request(text='gamaki', region=213).respond(
            {"stat_exp": [{"link_head": "https://yabs.yandex.ru/count/link_head"}]},
            {'smart_center': [{'url': 'http://best-gamak.ru/gamak-dlya-tebya', 'count': '=gamak-dlya-tebya'}]},
        )

        cls.bk.on_default_request().respond()
        cls.adv.on_default_request().respond()

        # в бк должно передаваться не qtree4market а common_qtree
        # но пока непонятно как в лайтовых протобуфах это организовать
        cls.reqwizard.on_request("кресло с подлокотниками").respond(
            qtree='cHiclZXLaxRBEIerevIY2iQM2USXQXFZRMegsAQPwYOKRHwcRMSD7MmEoNGI0dVDDIibBCXoYmIUEREPvlDBZA2urHls4k09SA-IIooYEI8e_Ae055me3s7DXFI7U1XT9f2qquk-WqPXGnVxSKBFUlAPJiShCZpha40OBvDnYEEKdlTuqTwAh-EIdOIwwh2E-wjjCNMI_O8tAsMO8x6h7dQL03mYk66GTdhZVrD72CQrmZgIc1cJuWEPOLk733cFqSNh0mdS0II7n6OOBpgRvyRYmMIDD4h3zMwlpJH3cfdrLXBQG3zzPk2ejKbJo9F2ahCzghXYpKX59iTLc1vn6QmbCq2iaxGT2JcCixVcC7lVsmKRCMdvwNI6oQd7iY5ZBF6aOaLRAZQIrWYzrMReOgdkE6xk97NpVuRWnk2xIhcDEz6xOgWxkYH9AbJF86gQ_vUQLhonI72OdKXaX4A7y-E-HWvnCpsQ4lMjjS2CtOgKglyQEhsPn5bhzWyhi5YQd0pwDgb-iWKdeg_Mi5Ks1S8aWYyThGZVppwaMP1K07ERZs-3mg81mpEEa-B5x_k3SkJv5YWpoarOHg6nRhmu0ucBcfVR-su6NHNZZLe83W_nhOqpofnN7RDAzCaqTK3k5Sqvci_F0Xf3h2osTR47gzW2wGDF_nuwNOVgoThYyQpHIasi_YMEwn0gtEsSzhDOXbQvs4Igmq4Q7e2nOSNQrSxWpdhHb6LKfGW1LtAyF2k3PRsN91MVx7DUGKkALriPMk3y5-0hQXQPe8GVvVKB-E6IeJDQtrJ1P8UVf-GobucEvKpt_0uf3_ZClArs7WDbC34y1DM08loAOiMue3R78nUIqhh0mp0LkNnXgo61hxbCHF3sIZx8uDjuafSoBKeO8y7yNVVg03bOHhLwVCvwZP9UBHykQBWhp96ykDxlRp-RCh78v9x3wvhG1_dUOTAfIvrAPGtoyf5015Cd80d8gdyRjEusDLen11Op9CXXfiDZ17Cf3yn62e7jW3TAaaiIYKp-ntgb9rMYpVLri9_Pop-s1VWkevBeulwdlcoUyoe6FISLMzL97TX8aZXD3r9QybL5rqORwy6b7m_U9cafpWOt5jekHRLdFae7O052d3WfO3W8y8SNSbR8vCsUeO_e-v6cBITFSBXgEy5f0cvHeyPA20rFt2E9OOgvXbcoD8eGsxxHNceBngEmbnBvIMILJmHBxCAW8F9XcC11y69fpaMZ0xsaH-1u274GILs9AZudg3oe3N_3qG-c6725bc2uvkOBB8_4D8z-b_s,',  # noqa
            common_qtree='cHicnZRPaBNREMZn3m7bx2talsRoWQyGXLoKQhAPRaGVUrGIaCgeJKdaA0YrjUQPpReTlmLpxX8VRFBQKSiUtpQWQtNGBQ-liLzc9aB4E0QQPDu72d3ubpYg5jR5OzNvvt83u-KsiPAurbsHkmiwNERBhxQcgWNwIsJBAzoHA9Jwqm24LQOXYLQ9j_cRniK8RFhD2Eag3w6CxJz-gokrolHGqcxsF5Gb9ZLcqJdlVdZ0TLq92z29YRjM3vk_v5nT21cXuCcNfTi4ghw10H15KTCoRRozrxhNCnkszqLwZfRYF_bBiDL3fjfLXi9n2eLymNCYrsoNWTUUO67KVYo5XcDklhtVrIjprD7rRHLDipCimhHzVZh5M4aSh0mcYhxLCCROf4LiQoARq5dNMpi00WAImiXVIUPZzTzUwZMWDnpIEJbUBgMcZcQgIejUEo59bATvZmGM4OtgxAKjLShiBgOzHZRvZU2um-zkpqzVp-W2rFC0KrdkhVbFHbo7ZOjn0yln6pZ9wvwtM0tQyzqSSld4_X6AYn94hcf5d-T8mxUbguNtuN-xFn5XrG1B2paaXHNPm7wvHhctRdBgaA22ZwufhD1bUl38jlbCHpZUjLZ08K_JKvsTOY9_r10d0j-jyAUc7LxZyN0ojBduT1wb1_FwCg3bsc4Qxz78WH_mvt_eyjCLrlsOebNSphbMPHLsGBLep65SnLNeO1tuA3bvLQLXQeCwEYCOvQQQJxmhYDYKlauaakB2l-jEh5XckF5hYiIgWAty9qnmIap36o8fuqqbysOkVxtfn6ZcG4C7j_dQNOUENtH-Bvlg_MfX5x-30bNXDZgq_ZvHQ0I1GUYPcNRjfF988czlgQRAaSAJR03pTobqZnz6eK4_8evLt35fhupmRONfpxb6E6fLF_d6xOwenZyPdHAWVc5nivMYpVNGp4JO2zlGWSYzjxE7VeXdRXPav6-xyoo,',  # noqa
            relev='Cpo=81;ad_cat=O;ad_cat_pb=OAE,;ad_filtr=10',
            token_chars=[
                ReqwExtMarkupTokenChar(begin_char=0, end_char=6, text="кресло"),
                ReqwExtMarkupTokenChar(begin_char=7, end_char=8, text="с"),
                ReqwExtMarkupTokenChar(begin_char=9, end_char=23, text="подлокотниками"),
            ],
            found_main_categories=[1],
        )
        cls.reqwizard.on_default_request().respond()

    def test_bk(self):
        '''При наличии clid и флага bk-ads=1 показываются реклама в размещении smart_center'''

        bkAds = {
            'search': NotEmpty(),
            'bkAds': {
                'linkHead': 'https://yabs.yandex.ru/count/link_head',
                'items': [
                    {'url': 'http://some_ads_url.ru/direct/premium/1', 'count': '=direct_premium_1'},
                    {'url': 'http://some_ads_url.ru/direct/premium/2', 'count': '=direct_premium_2'},
                ],
            },
        }

        noAds = {'search': NotEmpty(), 'bkAds': NoKey('bkAds')}

        response = self.report.request_json('place=prime&text=кресло-качалка&rids=213&bk-ads=1')
        self.assertFragmentIn(response, bkAds, allow_different_len=False)

        # при отсутствии bk-ads=1 рекламы нет
        response = self.report.request_json('place=prime&text=кресло-качалка&rids=213')
        self.assertFragmentIn(response, noAds, allow_different_len=False)

        # если отдаваемая баннерокрутилкой реклама  не соответствует нашему таргету
        response = self.report.request_json('place=prime&text=кресло-качалка&rids=54&bk-ads=1')
        self.assertFragmentIn(response, noAds, allow_different_len=False)

        # если нет head_link
        response = self.report.request_json('place=prime&text=кресло-качалка&rids=2&bk-ads=1')
        self.assertFragmentIn(response, noAds, allow_different_len=False)
        self.error_log.expect(code=3766, message='Key not found in hashtable: link_head').once()

        # если нет count (link_tail) у баннера
        response = self.report.request_json('place=prime&text=кресло-качалка&rids=75&bk-ads=1')
        self.assertFragmentIn(response, noAds, allow_different_len=False)
        self.error_log.expect(code=3766, message='Key not found in hashtable: count').once()

        # если задано ограничение на количество элементов в рекламном блоке
        response = self.report.request_json(
            'place=prime&text=кресло-качалка&rids=213&bk-ads=1' '&rearr-factors=market_bk_ads_count=5&debug=da'
        )
        self.assertFragmentIn(response, noAds, allow_different_len=False)
        self.assertFragmentIn(response, "Request to bk has 2 ads")
        self.assertFragmentIn(response, "Too low ads in bk response, required at least 5")

    def test_request_to_bk_contains_testids(self):
        '''Проверяем что в bk пробрасываются данные об экспериментах'''
        response = self.report.request_json(
            'place=prime&text=кресло-качалка&rids=213&bk-ads=1&debug=da'
            '&test-buckets=1,2,3&test_tag=test_123&test_bits=101010&reqid=987654321'
        )

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'Perform external request to',
                        '/code/520551',
                        '&test-bits=101010',
                        '&test-buckets=1%2C2%2C3',
                        '&test-tag=test_123',
                        '&reqid=987654321',
                    )
                ]
            },
        )

    def test_request_to_bk_contains_experiment_id(self):
        '''Есть параметр experiment-id который влияет на наличие/отсутствие выдачи
        https://st.yandex-team.ru/DIRECT-113686#5e57674ed2f0636c6dab8657
        флаг market_bk_experiment_id передается в запросе к yabs
        '''
        response = self.report.request_json(
            'place=prime&text=кресло-качалка&rids=213&bk-ads=1&debug=da'
            '&rearr-factors=market_bk_experiment_id=7702&reqid=987654321'
        )

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains('Perform external request to', '/code/520551', '&experiment-id=7702', '&reqid=987654321')
                ]
            },
        )

    def test_request_to_bk_contains_wizard_artefacts(self):
        '''в бк должно попадать много всего хорошего от реквизарда
        типа qtree и relev-factors и query-markup (и еще в проекте wizard-rules)
        qtree - общий а не qtree4market
        '''
        response = self.report.request_json('place=prime&text=кресло+с+подлокотниками&rids=213&bk-ads=1&debug=da')

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'Perform external request to',
                        '/code/520551',
                        '&qtree=cHicnZRPaBNREMZn3m7bx2talsRoWQyGXLoKQh',
                        '&relev-factors=Cpo%3D81;ad_cat%3DO;ad_cat_pb%3DOAE%2C;ad_filtr%3D10',
                        '&query-markup=%7B%22Tokens%22%3A+%5B%7B%22EndChar%22%3A+6%2C+%22Text%22%3A',
                        '&wizard-rules=%7B%22Market%22%3A%7B%22FoundMainCategories%22%3A%221%22%7D%7D',
                    )
                ]
            },
        )

    def test_request_to_bk_has_app_host(self):
        response = self.report.request_json('place=prime&text=кресло+с+подлокотниками&rids=213&bk-ads=1&debug=da')

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains(
                        'Perform external request to',
                        '/code/520551',
                        'app_host=%5B%7B%22adv-machine%22%3A%7B%22results%22%3A%5B%7B%22Sources%22%3A%5B%7B%22BannerID%22%3A1138766911%2C%',
                    )
                ]
            },
        )

    def test_request_to_bk_has_headers(self):

        response = self.report.request_json(
            'place=prime&text=кресло+с+подлокотниками&rids=213&bk-ads=1&debug=da',
            headers={'Cookie': 'mycookie=123', 'User-Agent': 'Mozilla'},
        )

        self.assertFragmentIn(
            response,
            {'search': NotEmpty(), 'bkAds': {'items': [{'url': 'http://ads_with_cookie.ru/for_mozilla/mycookie123'}]}},
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=кресло+с+подлокотниками&rids=213&bk-ads=1&debug=da',
            headers={'Cookie': 'mycookie=789', 'User-Agent': 'YaBro'},
        )

        self.assertFragmentIn(
            response,
            {'search': NotEmpty(), 'bkAds': {'items': [{'url': 'http://ads_with_cookie.ru/for_yabro/mycookie789'}]}},
            allow_different_len=False,
        )

    def test_request_to_bk_with_suggest_text(self):
        """Проверяем что при отсутствии параметра text поиск в БК будет производиться по suggest_text"""

        response = self.report.request_json('place=prime&hid=1&suggest_text=gamak dlya sada&rids=213&bk-ads=1&debug=da')
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('Perform external request to', '/code/520551', '&text=gamak+dlya+sada')]}
        )

        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'bkAds': {
                    'linkHead': 'https://yabs.yandex.ru/count/link_head',
                    'items': [
                        {'url': 'http://some_ads_url.ru/direct/gamak', 'count': '=direct_gamak'},
                    ],
                },
            },
        )

    def test_request_to_bk_with_category_name(self):
        """На бестекстовых запросах передает в bk имя категории"""
        response = self.report.request_json('place=prime&hid=1&rids=213&bk-ads=1&debug=da')
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('Perform external request to', '/code/520551', '&text=gamaki')]}
        )

        self.assertFragmentIn(
            response,
            {
                'search': NotEmpty(),
                'bkAds': {
                    'linkHead': 'https://yabs.yandex.ru/count/link_head',
                    'items': [
                        {'url': 'http://best-gamak.ru/gamak-dlya-tebya', 'count': '=gamak-dlya-tebya'},
                    ],
                },
            },
        )


if __name__ == '__main__':
    main()
