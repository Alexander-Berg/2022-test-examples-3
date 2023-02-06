#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    Model,
    ModelDescriptionTemplates,
    Offer,
    YamarecPlaceReasonsToBuy,
)
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # семена -> семени, семенной, семечки, semen, semena
        # лавра -> lavra, лавровый лист, монастырь, лавр, лавровое дерево, лавры
        # благородного -> благородно, благородность, благородство, инертные
        # лавра благородного -> лавровый лист, лавровое дерево
        cls.reqwizard.on_request('купить семена лавра благородного не дорого в Москве').respond(
            qtree='cHic7VpraBxVFL73zmw6XrdhSayGxWKIoouoBPFHUQyigsUfUuoDWX-0jS1G0ApbERUf21bttqnWB_hDi_hKrLVuYkxwGptNW8RHEDuj0vaHitU_RRRUEESUeh8zd-6cuTObTVufKXQzu3POnXPOd-553aHX0ryzsNDehbpxifSiDlREPeh8dDG6NO-gAmK_oxLqRVfkluaWoZvRSjSAn8ToOYxexmgMoymM2L8PMfLw6uL7Fl1NJZvD2PhyeX-9N-HtYf-nvNEi7u7B3cHibdriaCniiw8cPkzCxWOM4EG9aAm-8jPi4AIqxuh6UAn34mWvECloZQumNLrfJR62BC23a8_v_ahMdtTLZLjezwQpopLV77C_xBstdQZXe9RvrrgiReI_Gl55E-yKFnDR9hremPgVKx5OuTH8zR8sWQPoXlw5h8Zk7eKycmlQIEHngHMvqtxHHUnlb-vCgbxWbd8ME3ekTIZG-vNs0Ta5RFNRKbuy-WJKbHnfYve3KZ4pKeD9zKBVjBgAxU9tugoASb13vVFvzK8CGHMGGMeO1JWTaHwmFL-xBIoaFcRwI6ZOeFchaNX2zvyl4NVMUvw9fvQoc-rIXJqTTDOTDHE58HHKsVs984lMf47EaGjeLDUYFf6c05zqqTb6EHCqLu8tscTbXoMv4o0zr27wbzEXyxtcbPuzX72pYkXqMiaPeyQnPC6VB_rfJfR0A6nr7dZUzjMTtUl2qXall6Y-IGXjH7VMD_IHxYMkxLhazu2ou8zn7GH2R3zuY6hZYpNPqQ3PQwO_RuyaixRiuE0LCRPK1zja7YzW8Wvy2TF6qdqE_jvg1T0WKU8hgffI57nZPAmPxLq0_sbAD3OMshHTrHOuEqk1ifToQI65SdldOLVY9KYZYknIgaVBoH0R0xvAnuDwTcT8nxj8_5MRFWEFQ9LT7Sv7hKOL25FT45WEOfVZVPwsPAsvIcvxY-XIG2Mi9uQdq-B0kW6rlOtFPQudhwtVnPKV76_yj8SpOovQ3vuuLn5JaH9mPRCVGsdbDXyPm1cD54BiIApYSAWs2aVp_I9I0z02N23JLn9gOU8GJh-36K0ZJnebmPzDg0cKJpu7JpvvT1Zgbms2d4NoeW6qzeXGCeni2Lgp2JRoQafyN_hbY5REUT6ooYhiKO5kOf01mdd1JK05INnZApLHWDkkgfzBoncAIHWteC7ZrYHpNMuRCWYToj9LRBO0ENXzaXuMREcsKiKkjZ8jNLGe2jJhRpsp20Mio-2su9Nl-zXxOSw-X2ef_cWCUzyNR9jYSjCLnV6gQPbRjJynJ21TZutsIY_I60GNv2nWSPGAV5UHPG3R24AHKKv7m7zJ2GZeYNrMR7-wQvgBpwn87RJ8QAmhX0EBgRb-RmbKeEcZDymDTyozT7K0mF2AujLzVJ7C4AkNc4DVnyGvJ2exQSHMroK5AXiDojcFpp-xE0TcrzG9EcCUW7fmzjVrNXRMTdL-n2r5EB3JYALlQYGJvB9A8UwIxWJq89-1vce8oIjXio13QaWPSraofKwF_YEIlVLZ89YxZRcwZbG8YPznCaUJU5oApd9XSo9jejNQuk08bFWTBBOLSQGLSe_rhN4BAVT8QhrcUKqTWp37XUK1AWxUZNQKFXnBojdB9O5YdU9lVRP0YnpIDpMau-WekgRQjQuo_J35d3MtKj2Q2qoN1bWqjdP0QZoWIOf8VyX4N5fJi2-X8VvxJdasZpynsCWs29feJhaxxGpike7EIlUgZpJifZwiBhkpEAbZLwvCuPjtArqUhsMpMpuhVcbI6nOL3gnTbNTVsk_Wf_FifPZ5FnKb3OJwkGchLQy2u1juhDSJbjDInUFhE_X9rXWAMjA2TFwnpbtKZNfU7krlft0STfqqVyx6C4CVj29cf72_gfmKaq1sA5y_VnMhmIonY28rGgjeoBwZiXtaCuODKz4xgtsySF4YzGLikyBRi_pbmfZjMUwzk55Iq5vmJM1JmF9pybTgHDtmFapVmzWQdsmSDSTY9wdIuO-nCL0LYNrBioSGKPOYVv6gX_W3apHgFAO4wzOTKhgYuE04T8vW0kANEWeli4EKDC0Da0dIS9vCnRV1gdx2W8P9EuwrPFvcY6WLtGhdZfHtmF4Ppw88MGs2NG2QXc-MtKvhA6c3We0yOXzgt2HeO5OKn43dmyn0z6jQ757I0P9Hk9DP_k_MPfRzbpNd1lvJ0M9poS-VaIIkZXD4RpMkYbMksU_kiH0nNUd09v99E7i55IgDJDkq8cZZ01Hl4dVr6HnCVMr-fjAaT-l8JtS_DMZTOh1EfALT2H0wkVBRGgMEW2t4GmAaddyZpcV4_o2lxoMHLLoi7ZTJH2xWgP92VJlf4zIZ_wiJnzH5g9D0m_TTHZAcZbcJD3dOfHKU3e_jWYc7XJbheiDPXN1gtoc8Up4eqpnNHIAMKWY-Ys9H7PmI_R-I2PZ85z3fec933sez2_6VnTff999FB7mHCL0bgHqa6fUGLRjQZtvVuIAJ7c9lJDbSQ-TXUSNZdN7EB43SaGnbsLO_u0BbOc6v9Ga8uBGdYwar8zOxWMVkC2MfVGOO9witYmDsokkpMWHQ5x0Lm5k8YxmT4cel4TO4oPkfphnEWnacjnw_BCOCAE49UCy-bZv1tEMa9gtl2BlCH2j2_hGXNKgk0JzfPgoXyRgmpfIYQlkq7V9abqSY951caN6hXPLA2nP5lmGtwwaxISZaOrCGzCZz7rSDdA5oTbU5IEmpzVlZAklBbT7izpStoRH3f_3i1YkvUDKakVkUKBpi2QWK8lwfOwXpudOYLje8hKUfr5tewfp480r9FSzjmfoK9QpW4iBdvoLlRr4FXsFKEmxMfUfLZu1OW8lm37ZgxsZV6TjDwcVOx1q0ef8vfYvHD73U140u5NJJCtanBxR4ET109uWLnzj7ojQKsmjXNUcuXzy-58yIokM8hXRQx1ne5uAOct2yLTgfPNp22itIfmWlhPzKxPwTIQI8OQ,,',  # noqa
            non_region_query='купить семена лавра благородного не дорого',
            non_buy_stop_words_query='семена лавра благородного не дорого',
        )

        cls.index.models += [Model(hyperid=9438239, title='Набор юный химик')]

        cls.index.offers += [
            Offer(title='Лавр благородный. Семена'),
            Offer(title='сушеный лавровый лист'),
            Offer(
                title='Костюм детский "Рыцарь на белом коне"',
                descr='костюм благородного рыцаря с символом лавра на щите',
            ),
            Offer(title='эксперимент: инертные газы', hyperid=9438239),
        ]

    def test_not_found_words(self):
        """Под флагом market_show_not_found_words=1 отображается блок notFoundWords
        слова матчатся с учетом синонимов по тайтлу и дескрипшену
        для схлопнутых моделей берутся notFoundWords от схлопнутого оффера
        стоп-слова и регион в запросе не учитываются
        """

        response = self.report.request_json(
            'place=prime&text=купить семена лавра благородного не дорого в Москве'
            '&rearr-factors=market_show_not_found_words=1'
        )

        # найдены все слова из запроса - блок notFoundWords не выводится
        self.assertFragmentIn(
            response,
            {'titles': {'raw': 'Лавр благородный. Семена'}, 'description': '', 'notFoundWords': NoKey('notFoundWords')},
            allow_different_len=False,
            preserve_order=True,
        )
        # лавр благородный -> лавровый лист
        self.assertFragmentIn(
            response,
            {'titles': {'raw': 'сушеный лавровый лист'}, 'description': '', 'notFoundWords': ['семена']},
            allow_different_len=False,
            preserve_order=True,
        )
        # тайтл не содержит слов из запроса, но они нашлись в описании оффера
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Костюм детский "Рыцарь на белом коне"'},
                'description': 'костюм благородного рыцаря с символом лавра на щите',
                'notFoundWords': ['семена'],
            },
            allow_different_len=False,
            preserve_order=True,
        )
        # благородный -> инертный
        self.assertFragmentIn(
            response,
            {'titles': {'raw': 'эксперимент: инертные газы'}, 'description': '', 'notFoundWords': ['семена', 'лавра']},
            allow_different_len=False,
            preserve_order=True,
        )

        # при схлопывании модель получает информацию о не найденных словах в схлопнутом оффере
        response = self.report.request_json(
            'place=prime&text=купить семена лавра благородного не дорого в Москве'
            '&rearr-factors=market_show_not_found_words=1&allow-collapsing=1'
        )

        self.assertFragmentIn(
            response,
            {'titles': {'raw': 'Набор юный химик'}, 'description': '', 'notFoundWords': ['семена', 'лавра']},
            allow_different_len=False,
            preserve_order=True,
        )

        # без флага market_show_not_found_words=1 нет блока notFoundWords
        response = self.report.request_json('place=prime&text=купить семена лавра благородного не дорого в Москве')
        self.assertFragmentIn(
            response,
            {'titles': {'raw': 'сушеный лавровый лист'}, 'description': '', 'notFoundWords': NoKey('notFoundWords')},
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_extra_cases(cls):

        cls.index.offers += [
            Offer(
                title='Матовый силиконовый чехол для Apple iPhone SE, 5s, 5 (темно-синий)',
                descr='Чехол для мобильного телефона',
                hid=2392540,
            )
        ]

        # здесь проблема с нахождением предлога для
        # в одном из запросов для это стоп-слово которое находится, а во втором это не стоп-слово
        cls.reqwizard.on_request('Матовый силиконовый чехол для iPhone SE, 5s, 5 синий').respond(
            qtree='cHic7Rx9cB1Ffffu3stlE-JNQiF9kjYEHZ5YhgzNGzoMbbXWmYqKtWrVNwyW2kAj0GLeWEtngNeW0k-a0iJYBqFQUkLBJG0JpKRfIFYsMt6DPwCnfvzhlBl1QB1HhWEG93b39vbr3kf6QktMZpLc7f527_f9--1v9w5djerdc7yGZtAK01Y7aAQp0AYuAZeDK-td4AHcDtKgHXwuMS8xH3wHLAJL4TYIHoTgMQj2Q3AYAvzzMgQ-XJJ62UY3IjrMxcOC6Rr8Q_5gYY1_xN9f2OwfTMFWPnuNMDuYB4LZl548xWdXRirPagcz4JzXLBd6IKVAtoE0bIfzd1sU2-4-CykQzeShM8ACmM8m-vqHj53IOr343-KkZ6VA2l7s4lktfxBfIXzl-Af8I6TVSlmFHtJq4dYhf4i0Qgx7MOwnkNooDTK4OhQ-qbCWz741nL2wllzT1s0cJ3GeJnUef5iPWBfCEXybvZpUo38U08_5oGK_FKyEqzA_8xBgCaTWOmilIsrzCqv9YX8E_z6PJzgsihS2MpnWGWTq_9EOZRozhUm2H1DZxoxQZXzCQjGQsbJu9epSqYAn-jhNss3eOalGHW78aYqiBbst9D1FC1BhvT9UWIfJH5EknzBI_r7GUPDCKJOwhyARtgClCrgHIqGXC9XegOVp9fVnrd5-xusRIg1KZTy9IacRptzhsox4ZHFeQ8ZVOjt-eLpJ4dIeiBYqXEr6z_gjhR6JQ7aBQ_NDBrEBOnOcOXMIbxhAxBe4yMJ8uRCxDsISOMNaANdnAXZk2I9riK6x0bdVRLtuXbp8WaeEaNKA6JrpHFM6wiTGh6jNMgiG6o5QhJci1tEMcUcgPWtDfxb2hshSSV2cw1oIV1rdF2ng9obefoG2AGi2BgQ3EF3Q5lxcg-UH6QVIwYvZUy7UJ8grz_iCDrIxa-06kIX7-GPInEvI5JA-pRY_zu5adiN9DhaCxYQwBNHXFSFYuc6UNU2QgGWSwAAPjRjexP6FhPu4U-V8CuHGZsCwj6hzMXUtyP7lI2In0f1cJ-meJuB9J_qqgjbM4FwB482QhgRpKCH9PscZZnSU7TltmFTMsExbXTv_yRDkWa6xCiwKtXc71DHIEb4BCQOZbccdjkFOx8Cak8GjApGpPJuMYM7MsoAnHKk70DU6V7AxFWfLyaJsgYQtoDhb2PN_7aDrNA8dBKrD-Pcglk9RB73lsshB80EmhE7azEFzKNVBP2sjoTc26p7rJXC-Fkbdw1pkbMCZihv2lREfG3C_W9jgD2qRtx5fJfH8pxVbMR5Fx8RmXHJkdoo8h2I2fJoZHm3bEka0wlYhJ-iJy07k8NAWhCi32Wq104l29e4c904vD8u8DZQtKdxZnkXuILmD7E7uw3eBpmX_i1OBSeDYbXNTp0xhleSvwhLFFFT_9lc7CqtkgEmlV0MWVwmEqs6zEOvgmgzkmMJyPpo9SDlDwNXuVuSyXHtQ8iKQeZGEwHcnoDHtZHfVhqRvrUU5hfRz2XTDhXygVoWNgWoKjEAmRrywfXLICeN4E1-2uIQvRniVS_01SIAjqTO-LmyJ8rONL72aTT7aO7B_-Hi25tG-4P8JpvL7x9R8i5nV6ZjxiJT003mG-VWE62EJrgH3utilxdF5pCpLAHq9eZS0up7NKZCS3kCiBNNEKllYi2VySKLNvPD5qJc4FdJZWCPNQp-ygdBJ-XAwxBzr9hAftZVjuSaUKraMsQw-TaOgUNPa0Nl3T0NGuzYmOsDgooas0EXhpeGtiotqFKoLOL4HaiM4qFqDg3ro_j_83Ao9lGG4yT89Tv22AVr1Tj9GBiBh6XhUXDpCskyPFufcprFOMJcv6GNPnBSUkBry7VXoupMAWDU3NQLREoVvdbmum7tuWr5s-YrbfpCCn2mD6SLVFIll4kgTr75EWCVCqWnuRUjsjU94OT04XqdBttcJ9WCHg5apeoBDwZD_HMnhaBVmqIQeHHmGR2zDYBNl99OE1ACtasElyABkVvjuQ5YBWEpmnb6B4RNZu3dgePEFXm3qPJLKSiO0lPZcry7VIMOU4T_PZOFoFD5H0_tAT5zsEe4vnrbQDWptWK6oCTpiKg0f3-Xy0rA80KQfz1EvoUCqunEPRAqEubKkZ35Vry_FMDBvhwx8x9INDUee_YF7Y5WxkcLmEob28J8uj9ytNtjEyH-E7laDNhiaDhRjaA9ADZiW9aDI_AHM_IExLes1CWW9oFAji8DxHCyC1VwE71roh1paPug_j1VoCEeX1Tg5GsRhdXOJtHyTHSXlhtEmKbwLWVJugFflcC_lrQKnKjblLZWJPYaK3cTzDgPqZeQdVAY-9yNHDHlHpEGFLfgam3JFeYc-3CSCo8wQdGiTAHSoanuWaOlRQhSaZ6Es_X3CbaUsfSWB5qFwt88qZxewyB7gARt9V121f39p58rlN4t7f6YSsyQUNsYkiFdYlZlCGKrMtKPsKrMCbqgyTzPM2ZuF_XL5N5xytgZdYU16rj6BVnAmc3QGFeeo0IxnsSM0LtRnUSrbQnH3aYi-qUjNWXLzbdeLBUTHILN3_vPg-aHIyACTwK4m8iLdqrQ-i0gz9wPFZCWkoZ774Ye2l887zVark7Zp6UjQ7yTW7_dqQpfxdk0V9fu4ZSi4RuFX0HFTwfWku03ZEYsJu2_JO2KGcNuGUMkwW3zX7KMOr_Jm1CMQfUNVuaBCIm1FmTZC_lLHNS6AN22ZzaIaF3SrG2ZTEWkuZ7tsta0XINl2GZjYLftIdst0Sy9p-y62_ad4EXknRF9TZGjfsLy7RAX5OJdfAGwS3lVEdkFvJDiqYFNQ0BqnX_JiOkk81c5EiO36akbiByGabyJdcFGjod2pBu0TtnU22pZkSUliSa9C16O6aShc1dOjB1xuZafc0sD4wpUExoS4to0J8VNI6o5ZSgCFnn2W6056Z0nH3NQjFrpeDeR4jXKwcBcpRoh1OFMkf_zUAcRjeTTORMwAi-URlBrLN-I4HfWePTUIF_MMZI85oQ70O1X0T4cg-oq6d94leifTdr5fz3fOu0y8XkxYDbtUk08j2MX5at2zXbZ2TP8tnYR-q_sC5HTd0nmTpE820yfxPMTdFvqWgn5Cc2AmzfnNk-eFNCRiDeAxqjMJo_-ahRKhGwCVupqAxDna-MrcCKjAjbjE7N4MzI6oEM6fdTfCVV-qFJdcJEoDTVx8HTI_IsCptreumO3t7R91iaSpMrsj_kxAs4zSCGXtz5KhdW5OVtE6iwToEuv4iQBdraNid-gOskP0LqajRR_w4iLsKHLiqkM8WtQRc-JqG0Rf1k9cgTN43sqwHpCOlyiuB2H7-AW3j30T9jG-7ON205FEsXwwtmfvDuhry-AkJyhevjjD5zgrXlFTK5qIMuPWigxRJlMqylTzXO-4iDJvWaF9_MpC1yrkuFRcOXGN7JZKbvkg43lcmthyGJXOVlTDugRqiU4wh4C1mQ-O17TPm4Aq0-fLDFOUWeUPebub71TdW8362MMWWlSsAFC8kj_-1_-p9ZZeoQ92cEtFuMefaucV-gDexJNeyhPSr3Lji4g089WpfU__C1nYJ6qMfvpFOulCFlRT2TT89M7dcqW_ewpKBgDkgE_xE7rlRMgx0tJYIQj6-X8hhFL2WqLqMv7ttSwt3Z4I41S-mlo6kced-TxuB32xLMhT-HKkI1fKWUs5B4Y3sb-DLkg6wnSqOVqPdJjTqajwPqGUE0qpKGWmQqXMFFPKjEEpM9VQyvf4ke23DYdjaAgW9ohGG4PvGvsYXH6e47gI0w6yj9qhQfbYVTTIHsOOT67Ujk_xpR4kSz1Q8VKPqed9Bow69cWvjNGeWo6R0TFcSTHSXMIUBDuFXEEy70q2VBDRz71cRj-lMgpsDDAiRi-jZ6D2RqslLVxHU9OySU0LVr-mxYsIt-s1hIxWQ6ikhmFVUsMoUh5AXj09kLItj8WFf1IPQLRAcYl2rjNT4kiK5BIDeJPmXUFPZuBexubtDmPzJ1HQWnKTnGK7C7qA4rrVUJtZWKI2M8jzbLiwiMGuYCi-DiODXVHGCyj1BMUt_FD2-4ZvVPhHCz3Bq5eFHgFX06rgzdd-y7VAGGV8L9JiqwIOZTiRF3XGnMh7EYZA8pslm7J2b_9R9t6Y-j5Z09nzlqB2RICK43f8wNXLEGXV8hcjWHwbzHSY9K5jM3nxKxxiksQiWvsKQVQxzES8K-LvOhY3lRc25Ze7YojbC9183qUGcT_NBdR8JvgKQfTK_5lIaQJkG3DofgKGO-0_0b-XYBVWlzDeUzzaYtj44IY71YOVLQg3lnGsEmPagDF1snfzAPYvq4oBrPrlZPt0PxJx1ganBs_DgnhCCE476KFBSb9zmRLhX9btnDE0ZdimlhaZggSgdGCimJ7VgckjKE4EprMkMFFxjNPARIn7N38L988O6tS-vHeAvH601t-H_x8s8XbloYtDCpVxJjr_Tl--VSBVco_bSIGI-ypM1tnbP3w06zxB_u4hf5_Ef5k-8dppkRdjPx7fgUmU9fKu9tWXUX0SoZKvAkZSKvpVQKx4jV5jUBtJuHmWZZxKxH6AaEjSOpOXe-yfU6P3Yfggk8q951AnF0GZ3ofhnTFO7qXYjxTFaGAWPl_lTxY1fUxUdbSfLCr_ebRtFJ8s6u5CjN_a5xz2DAifcmiSEIXsYaP_lAPRfXy3CU5F5MMOjee7MNXkTp6E3rjoqpa6VU2zW8GlgeJSCJxEcYigteWKGz6tQjgMwpm09cUfzWyZPv-KOIgkhbhVh3A5xM6dk2e1PHvd8CwJwhGekr72E7Nacptumq1ARHPUvrF7Zsu2FfNkiKSXlGlZeWqVDOEKc1w6ffrslr69_gwJAvNPnmPK--lojibCU6exznUX1LhWo33N_Otpq8VbodDqekhs7aSt9Z4nzsBaG3hrCFvPJIj7ugG9DcQl3AY8EW4Rfpxwi1eG4m3wAOE2oJTcYsX5H5uKsKY,',  # noqa
            non_region_query='матовый силиконовый чехол для iphone se, 5s, 5 синий',
            non_buy_stop_words_query='матовый силиконовый чехол для iphone se, 5s, 5 синий',
        )

        cls.reqwizard.on_request('Матовый силиконовый чехол для синего iPhone SE, 5s, 5').respond(
            qtree='cHic7RxrcF1FeXfPuTenm5A5k1AIV1JC0CFiGTOQO3QYoFjrDCJirVr1jKOlNtBYaDF3BktnlPQFfdFAQVAGoTxSQsEkbcmQkr5QrFjKeC78EJ2KjqP9owPiOCIMI-7Z3bNnX-c-wo0tMZlpcs7ut7vffu_v2z3FV-MG7zS_sQW0wQ7UCZpADrSDC8BF4NIGD_iAtIMO0AmuzFyVWQC-BhaDZfAuCB6A4FEI9kBwAALy8yIEIVyae9HBN2A2zCPDoukaw_3hSHFteDDcU9wS7svBNjF7nTQ7uApEsy87fkLMro3U1uoEc-C8V5AHfZDTINtBB-yECx5DDNveQYQ1iBa66BywEPYFmcGhscNHA3eA_FmS9VEOdDhLPDIrCkfIEyZPbrg3PEhbUQ4V-2krIq2j4ShthQR2X9xPIY1RBmT0tD9eqbhOzL4tnr24jj6z1i0CJ3meZn2ecEyM2BDDUXxb_LpcU3iI7F_QQcd-GVgFVxN69kFAOJBb5-JVGivPKK4Jx8Jx8u85MsEBmaWwjfO03sLT8A9OzNOUKWy8fY_xNmWEzuOjCKdApvK6za_P5SKamOMMzrb4p-WaTLipJymaFDyG8Lc0KcDFO8LR4gay_XGF8xkL5-9tihkvjbIxexRSZktQOoP7IZZ6BVOdjYSfaHAoQANDnNbjlBtsl-n7jSmNyc5dwcuERkjQGnKqstnJ4h3NGpV2QrxIo1I2fCYcL_YrFHIsFFoQE4gPMInjzptHacMBErrAxYjQ5VzMOyhJ4By0EN4RAGLIiB03EP2Pi7s1RBuoVB8gEkpkUEE3a0F37Ph6YZ-VkTambnYp4gqcztaPYqW7JeqO-JrswFsFevc7hPkMbMyi0b8K3J3kD_39_JLT_QzxCLFeHzB0r5HYQi_uUzSw2aqBjaTfK24MRwzdbiBPWYb4xLWX4FFyTKpNV3XfLbEOw2zMahkqX4-1bY11prhNsjr9afZPE8C1Dv6qrik9Ny9buaK7rOitvVioChthE7kHmdPgEFzY7omF7ULMO1ogFzO0cSiAA7GsMQKcXyBow1Wo9zwD3Nk4MCQpVwQ01wCCG6kxMuZcUkeIAtkDyMHz-SrnmhP0aWt82gTZFKAdewO4WyxD51xKJ4dslRlkOadnxQ1sHcIExJkwCvEXNSagQncOzZY4gGwcGBa6T-Bt5F9EqU86dcrnMGm0KjfqbcXOLx6WO6nwFLpp92wJ79vw5zW0YZ4EqwRvjjSkSEMF6XcFzjBvouzMaydbJQTLt9d3ip88RZ4Hu6vB4lh6t0MTgwKlG1AwUMl2xBUYFEwM0Lw8GRWxTKfZWRgW7CSLaCKQ-v5EqHK8JFUgpQooTRW2fHvk1rwW1OZ0ZDr1t9O82_w-OLHXKKDISm_IR_QN0jfI30hfhG_wbxI_zASHb52fO2HzxTTolfIamyf-21-dxBfTATbCrIHcGVMI3ZtdgXmHcFFAtQM8UGQhhxJoRIaytw17PEAfUTgPOecziSltd6M9drjBjhnx1rfNwAVt66fz6caKfZGnKG6KvI1ECGwjxPPbz4opYR1vo8tWj9LFCq9TaagOS3A03ibPxa1JULfphZeD7CMDw3vGjgR1jwxGf49yL7ZnUj1ybTyl6ZnHlUyBzTMmnhJcDyhwjaTXI1FK2j4P1iRvYM9bJhwVOGIHSqQccZRimslli-sIT_Yre7NnS__rvKjKfRbXKrOwVTbSfTI67IsxJ7I9KkZtE1iujblKNGMy48nmCezQkNo4fuudja16bQ_WLSZqFMUmiuSTN2smqkkqSZCQPRIbyUDNsBioB-97_acotlCW4Tb79Diz2xZo3Tp9D1uApHzzkJxvQprbJxm90GkiE9zkS_LYn8YFNUoWdHsZet5MAFbPz41DvFSjW32h58ae5StXrLzl1u_k4MfbYUeJEoxCMnmkjVafpaSSofTQ5Dws96YHKWI_xHt3gGDAjeXgHhev0OWAuILR8FmalrHSzWgZOTj4jPDYlsG2nd3nMCkwoXUpuABbgNKyU2QBVrJUd3B47GjgDAyPLTnbn5E7g2anyggjSz3dr881qjAV2M-TWW2agM0x5D6SEzc4KOzF0whfrxeU1TKcJCO2evKRHZ6oJ6sDbfLxLLMSGqQuG3dCrEHYy1Fm5FfzolQKAV8XhuMYxN_QCFj37WXdy5etvFExGjbaKUYjHmUj2xJKtRhCNxZX4rhH6E91KbJiSVzfJZakz4kl5A1kWhLiWvdE9pvXC8eLW8pYkof-eFHiT4zBti2_FfsTA9piSUygFEtyPzSAWbETytI1TIg3PKnFzmap2BmxQJWxiAVusEaw4E2Ev2vkHSPhc0RHRon7XEOivxESN2wpk3dsdpKswzLaxoU3Ic86LPA6H-5mtNXgdM1ltGU8cSZRc5tFYGVBvYLAivEgFIbyoCWwSiSouJU8E1tVVWBlDrex4BBXBBPaxgATqtamM8mtyrDCMJ2MpL_PeG2MpC9l8FU4PgNFlZyNljgZ3evgr-tlCWIVV0VmOCkf2eqeClP4GBsjXuKlTwZhKX2yjopLnxq4pfQ52zLnQACH1JpkPOVcA7rKQul8cwKjCkrn6I7KoEn1k8ziJGica86ilVuliuPTEH9Z45q79MZbryNiICqlroVnb7z9wJkxy-gAG8Oupvyi3Tq3PoFps7ADpXgleUffe_99x-_rc1tQm9vhsNqYJN9ZIt9PiUrZjyH-grY55_qVvWXKZEdEGT4Ctu3rMrqvqDfZFjurmoWj1rSDKtXPZym2_xIZw5_No6vGKJAllmRduJv83VcmEtx_fnKxQBln28PfHX6xQIHULdoRB2sQaYfNgbtraOxQ4D5Bf--kv58kv7nhEgauRBD_4TiGylSUaBjH0RMq31Rz7SHhUslrD0TwPN8jgvdOxuvzeEU5g79pnIHHR4uy1NlOwB_9xznJEbgYZBO5d1x-BC6gdHFrx1JnSgT5QupJaYoEBvC5D3Biamfuh0NUT-ET094ezOltlJ52Dktlp2YFUcgXm3jZicn-bkSyxz-9k5-fexjh63TRJ1zdV1xPU285gbQJ_-Mn9mIh_ck4m_QP8wsgCZQu_ZsglnpPnYwb-5gkpIddz2fmYsitYdi4H-Jr9NO9HjlitB04hg3icK8nPV2HPXrU0YFhj6ArunO7GnKQ_d_UTfePes_Gbs9N3csVG-RwGySf2N6O8Fc09DPstB-Uvjh07Mkz4j1kUg_7H2Uyk7Ge9V-BM_GROag22oy2OM8YX92RO6giMotEyA1ei9SOitARZFR7GxLRV-qiZTMGZaCNiq8yKipwuu5tKKV7u4YmnC83V6d30W0hGc0K8mRG2p9kY-3ckq2hdq5xzLPm5DZLKRZNX2ap1WWWH5gGsku2LrbbD--JShPsKnEnpEu-_dCVcifkLog_Z94JASfxRohpbNTrE5rpaSD68XOhH7un9WNq6Yf1epB8125Srwfl9ppFhuiumaSgp-BNM4sGlXPgDdNeZiprkcXL5Mt5mVrePJwSXua3KNaPXyLjVNJj7KK2Id6VVy64FYNsgn2cBbYCRt9nG67jXdJuqUxwg0CkWQxOl7RP2YCqk-dPWqaosOQb0_YxcWxxdy2PLR5CeHGpAkBbyeLX1M__c3cg_CX9hCA6zivn4R5_qlMcEETwNpoMMJrQfp0an8G0WWSnzp1DzwdwUBYZs0qnVLtoQnUOn0bUCW9Xvx3pnYWzEYBWb7TdR63EQ06SlKYyQZLP_wsmlNPXMlWXqa-vFUnp9kzsp_pqKaXTcdzJj-PuYZ--RHGKSEe6CuWMtRJzEHgb-btYQtIVh1MtST7SZQ-ngPiGYloop4VSE8p8lUKZLyWUeYtQ5mshlI84sVD2OzUUyn7LqUeh3KlH6XQH0nQHVJ3ucBbda8Go20wAVYx2zhAYWZXjUoaRoRazMOyW_KUi4tUcKzRQHr0N42OFv0C8MCVW_aBR0vrJj5Iqj0Tjne8S0vkjJp2RhgG-84lL5zMQX2staZUiYrmKlkMrWrD2FS3pO0GjgpA3KgjVVDBQNRWMEsWBBr-R3Uu6q4-wi_zk7meCKhtEp9CdL3MzSTGIEbxNUC9hl5NILyfzdpeT-SM4ai1jFGNsd0APMFy3WSozi8pUZkZElA0XlTBVt3AUX4WJqbqlgo8tGimKW8X93Hct_4lDeKjYH31mWOwvc4HltVd-LaRAGmX9BhDxnEBAWW6wJJ0pN1h-BmMg9SuKzYEzMHSI3yDRb5Y0nzpfxBkXBBg7fifu3b0IcaAXv_iG5S-fbPcK1x--XJS-4iE2Tixmla8YRGfD5Vh0JfTdwG2y9nGi-iFTyuZ2Qa8vui4VKcR97M6kHs1EX0kn3ySfjIAmQtbvAMETwiH-0PyeGxXXlFHeEyLOILDpbp106v9VRysmjRX8Nx0EU59g6ga3Cwf2T1RDB1b7YrLzwT9iP0Wdk-83EUY8ITknEq0v0OW7kC_j_lXZLlhdU54faRmeKQoAyjsmhum01T9FrH4TZccUtfp0c-RtMzwH049Vm870YK7ZO2sm_s15l7XWr26e2wYujNBjEERZBETU2nrJ9R_TIVwO4c48duyay1svXnBJGkSWQdysQ7jSHM_OylzRWti8XIXI-lkVj1UnVqsQ2Mdilbc6zp7bOrgrnCMgmulu3aZ6z1tY56Em59oF17FWJFqh1EqyZLm1m7U2-k3yDLzVF60xbAOnLRnRC9grIhuQXj3fk18j5KVXkgXJr8RVy6_RevSVsPS_UOFFWA,,',  # noqa
            non_region_query='матовый силиконовый чехол для синего iphone se, 5s, 5',
            non_buy_stop_words_query='матовый силиконовый чехол для синего iphone se, 5s, 5',
        )

        # здесь проблема в нахождении apple
        # http://hamzard.yandex.net:8891/wizard?lr=213&rearr=disable-request-region%3D1;market_use_exp_synonyms%3D1&rwr=off%3AEntitySearch%2CVideo%2CImages&text=%D1%87%D0%B5%D1%85%D0%BB%D1%8B%20%D0%B4%D0%BB%D1%8F%20%D0%BC%D0%BE%D0%B1%D0%B8%D0%BB%D1%8C%D0%BD%D1%8B%D1%85%20%D1%82%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%D0%BE%D0%B2%20%D0%BE%D1%82%20Apple%20%D1%81%D0%B8%D0%BD%D0%B8%D0%B9&format=json
        cls.reqwizard.on_request('чехлы для мобильных телефонов от Apple синий').respond(
            qtree='cHic7Vx7jBXVGZ8zM3d3OCz0ZtfV5UZg3ZpyK9AS-s-mUWiofxj_qEgfsdMXrmwFoWKXpgGTxssrLuzqWmsqosFGyopQd5eF1QvsLlSsVQjt3D4kNrRWa0lMjVqrVowJPY-ZM2fO-Wbm3vW6QLoY8N45v-_Mme_7zvc8c_H1uM7JZGuajGaUN-cZ9UbOaDGuMuYbX6xzjKxBrht5Y57xpcx1mcXGTcZSYzm6DxnbkfEYMgaRMYIM8ud5ZHhoWe45E38XczKHkNHpcOlub6i02Ttc6sqhZjFzRprZuM6gMy8_7QQTS0TKLeYZrWjRS8hBWSMnoVqMPJqHFu80-QI7WrA02EQHjVZjidFWQ8iMfMNyZ63R0YMEaNQ73MTWQkBW59Hjrrm73zV39bfhrJmzvcPeQN5qcwit6Q2LT0X2ycyZpc3BJ2-IfMJZRGhGvUF2FQkaitwYXCt15Rv82cnN89ZyYy2603RQARmECblehL-hMLLGO0AepocyETX7XLQALi4OmOgT6Ay0Fy1i_PMBIe_QUpPw7grsDzCWoFZzCbrbDVkXXejTGbxaWWi9N0yeaZ9XJHN0eyOlrtLmyKInAYs-87lg1QA1pAIHbfYIAFpVhZctrKG8g0LcqOBae_qLrr27v_iCa_X2F4lYbCKWIW-ECKuOiKiGEjEVwOSWtrffGxUC7WFXTYYfEgI_SD5NJVin1OkNaHg2J8FL1xVaWW0MoTaU_l5xv2IyjaaeSF5taSObyclmCHIUerLKVyTmNJl6B-sof5UN0iovz07KXeodIWKLiE7lMtXGjqt0AZc2w5te0txXTF1zSxvIDQ4Tk7CJTDdC93Cq5vZNFpqrU0Oa-w9uvAC0qrlbEJ4aRSlGqrfff7aAm2MzUw2JZkrwWF9wKo8fRfirCo_JLUsbIlw1Aa4WlgRcZXjIhi1gbGTDqgWbidnlcuzX2wh_XVlh5uY77ljVHlki5K2G5wdL5ASQrLu5rDnAX-XPAvEuwPx6EwpMUSfxOX3BQrn4Zq0hoqglQkH8g5FDs5hIzI45Kr3Z2eeiXTo9RZNHNv1HfsEGnPR6sndGyN-DxPsnPnb350MnLYigZz9t-U5aoFT9fsrC0qhskTO7-4pEwe1d5H9tlxAjNZWaAh-rG9paYmj9sTJM2cVhmO2E-_CVFUET21D2_fi17sAUlO6VzHdPnLOLbp-WKc5d2QJqMputfGae9pUGKE7cYOJXqkHuUdNxGg3jzmtzfSb-tqKytbcsb1-5fNW6HPpsC8r7ClsLKOwjP__bk2agswEVpLDH-GYNIOp2_TIORsiGQ_KGnUTYZ61olzco-WDn0LrILkRkz7YAkxQk40QxrTEY_zYdgRDX_Ei6D6UM93iLTXmVN9wC2YSNxtF11-beNPHtWqA24A0Sqz4qhehh7A85ux1_nx_GaRoxxNR_-95OR6vWgHpxDRQTuj-INDAP4dH5DOFRZGtwEdjueiGCt0z8Q0UEl5CnOERs11BpPflvozdQKkSEgAEhbLUCGYDUkBTe4lIA8aocfsp5q-Dg9Kga0UcKh1n0MQeDS0-LP4QMPDOQwaiJ79CiPqFBpW7yuegdStkGEZMCkEMiOBKEfRoaEoCOUgXQp8cKFQmAM72YLgrF4gcs_WvGaeYsPZHB1-GgVmCWU0NIqCDst_A31cSXWMO1q1fJ1YOaNKH4NJAgTpg8-eUI1crPxf6AsL2xURW11Z_W4Fbnrj7FoM8B5tzlolB6zGsEUy7U0BXEhXSCa_UJtrjmL_a7aF90jvZlhHQSmcNacfutbBYrXMYV-iyKo5JCyl8h_DU1zl-2at3NcjBpAzJ787_bLxOBPiWABHY9D_TpsCqt2ZhdFnYgSVaSd8w6585Z2ULBJuGGnbd4uCHpt0n0u3dKYDIemFJF_T6dwbcqnJpKzNoBlloNlDaUumU9h-KZFwduCmZXKCHmneF1EgWpmpxhGyuI0NxsOXaSag63HIOfaPycFNeOLY4OSjnDQTxLk1iGtgh6Q-ROnK6TRcRsnN1Vo6KuPmJvkVgL8osp_DlGIripZNShZaQYro1WpfjCP3eNkXP8-Ubl5N-fkWgE20-hpAzOPyWd3mni78UUgIkvS82pH6hXK8CEClLroWgFmKBUlU6u7nLv-UmGhpYUGqpFh4umuvuSiVcqC80qZaAB2cY7wIoHTgivrNFCon2Zi1bDVl4d69Pi04bqi5nFp3msLTc1NtWcUDlXFEdlE0d1ygkc1W8cvDSu--IVJZcFbb1I-CSRQSJ6sTbaf_GKCf0XrxiTxHXXEgEKkOJ0nj3pWn0HRogcLFGLv_iNKb9WFHp4sblTXpYq1wEOR3Bw74QjL5QOhCY_JUTgo91QsODfOaT2HSTX1dKWC7ncCKR49kTWXO2smbL0XZFVvFrNrOLdDL5LkU-utJHV1QcJSwokth8Qvc8hOcOYAgiqs0vcKWGWhGwjgUoV23ycAI5xHB9ZCUSRPgJt6h53rV39xbYrs1NyM1gXAaTUugqXZz-Vu1THTrSFq2aUGypfpRKe_jY-2_CPmyRmGxPnTS6-jORfJl6j1tDJzhxmBeENUt9eykqgIvrbe0XfHqSH1OB1v4oO4VWF6EIkJVBw5697PweDix6XHKVmIkeZyFEmcpRUdziRo1Sao1DLshcFluUhhG9QLIv1_dUdkkmBvNdzXwjsCQVDhuRqZkfoaNh94I5rBqZX47xWtB1vsqbCMWEH9zlVDP-fTD2ESSx_jxz2Q_nZfQ83xxzCpNQQa_rBQ5gUDTX3NVCMzf1z6oFNei7oSIXHNBsm4vFPwABVcExTCuK2mfgWRV_rRJJVJJoqBW9Qq_eNVzOBpkboIB3t5UFbBKdqJ8md5WEpTjty3G8UIKY6gjWa74owLmJnU2rOil1LjqZMVkTYLuoynaYetv9gdduKVe2SLYE4eP-DtgjbOQHEu52cdz5Cbb7OwBk2IG1j1oduZ_t4Do3rOWF4wLEQtZC0161CKjoDGTGxdhlFK0VNKy1a6eRJRSsdDZ610VAXTtGKs_R-0yk4nKfnEF6lNoSUBxiVOAr1gyIc1Yghfm7yO0IqVuXmbKxBQr1aH00pSUoEO5lAlYOgj-oyxJRXxLGu31v4xwpPqEkk5oRYjAPEsBS9p1i-dVDizGSAM-_8pz3gS8wEEHeO8_MsMRQqj4jpjUHGnb917T3E27r24-zfXvbvE9z_Uv82kFDrSkt0tLOqFSU66d5pRhbnmsj99edN9E9Cxp12IOP3LdwOHNsYpQcQvUFWVDUST20Ub5MPbUh0kEg3WOLQhoRURfkZrABi4qkBUwHGlUibsrXEaPGD1j5aE2mWOHwcjpeRPcGqMT451ccuMQpVGBFuZa8JqEKk-ZuiCr9rDVUhQgepwtPIV4UIUlWFe9J64R_bh1TUDwf4N-GWq-qWaer7gWDpGeBst1rvSz3bvenoNWFzTiNOqIcCaOAwTlo1dJxPFNP3mbRll3GemLO-a1LA-rPVzOP3ZYAz-mqQMpSWx38U_y4l7G4OxLxLqbsc4E27WLfzl_Q0PnQ9E69cnnfHN8Zc_rSJb0s-IsYsT-IRsa2iF6ORJrxBqWEr94rja3O0U2JlWJzkekANs0avZwJrdKqaR_HvB7qrLFUYpuF02sv8f3pMvKYiUUHi_KXfXQ1Ren1GGhznyGbiRORYIsDqnIgEdH0i6DkPQQ9n_THB-n3pr87zKm4S6zvffQeFvNeoId7vh1-dByq6m5H2vvp5LuwGXOwSNYWzFu7QjjJE18xCppTXAbf9cU54kgEghxj5gemfZADwKiufNzGIi31fm1ddjrASQYRKi9easnWaMHkMyOOtyiPA8W7Vlv22dYw27BGvOG8zcZvmbQcIG49Q5kTeck4_GhESQtJ_PHC4AqUfjXCCwUhJ1A8uaUm0oxVLE4iXxOzOvl8fd9EeF-120eMu6nXRE9IrWfQd2Y4rJcrRyPwonD_CLJzFecPdwZhFt0430Pco3UMnTGn3vvbQbNH34AQJDPIRgG3xRy6cIgtlkO32WQGDHrHwtxQGOWzNURZB7-Tt-OcfhCIJGohLg9yICIzKp7sRFmPS65HjzCmqcJugldidDx99YTyXAQjsfeEMXrP1qiK1XiQH20hysWjzAKoqDs8SVcUoHSS6t_0CcxSpCvA5CyuIinsEnFWCpf8nuXslfY1475K-NlGwl6SU1tmoy9YRxTubES29M5nYn4MZimgd5HYee2em-nMwQ7DKnbUjPwczpKtbC5YGY4pKz8b-ZEyMBrroUJV_QKbhIlHVsf6ATPn349fG8AMyHSuwz2_5PCWLyXv7pZi8QY3E_aNsRmQf9ZQdgTPdJ9-2opmY_Y5B_WUOyjU4duM9z7x3zfQ1W1cubDbmUsUNEDU-oqbxOz2LFky_Yc-8KMLMmj5iWiO9On3tmTujCDtrizneuLF1wXTDKKiI8C7vnbtroY6okRCv3Lg2ETGtseeZ967WEcTh-AjUOG3z7IXTP5x2srXZmjt38CcLOYJwR8yBT-28evqMD_PhHA0-PyY7zpJax6q3vrK4YyuqZzyoqcfkao1j1puLF3OoLaCIQm_diur8CWxnaofBvzpkTdJXugD2lUjrfyWFIZE,',  # noqa
            non_region_query='чехлы для мобильных телефонов от apple синий',
            non_buy_stop_words_query='чехлы для мобильных телефонов от apple синий',
        )
        # http://hamzard.yandex.net:8891/wizard?lr=213&rearr=disable-request-region%3D1;market_use_exp_synonyms%3D1&rwr=off%3AEntitySearch%2CVideo%2CImages&text=%D1%87%D0%B5%D1%85%D0%BB%D1%8B%20%D0%B4%D0%BB%D1%8F%20%D0%BC%D0%BE%D0%B1%D0%B8%D0%BB%D1%8C%D0%BD%D1%8B%D1%85%20%D1%82%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD%D0%BE%D0%B2%20Apple%20%D1%81%D0%B8%D0%BD%D0%B8%D0%B9&format=json
        cls.reqwizard.on_request('чехлы для мобильных телефонов Apple синий').respond(
            qtree='cHic7Vx7jBXVGZ8zM3d3OAK92XV1uRFcV1OvAi2BfzaNQkP5w_hHJaRt7PSlq1sWoUKXpgGShuUVFnZxrTVV1JAGyoJQd5eF1YvsgxZLBWM7tw-JDa3VWhLTRq1FK8aEnsfMmTPnfDNz73pdIF0MeO_M95058_u-8z3PufhOPNnJZKvqjQaUN-cYNUbOaDRuNeYaX5jsGFmDXDfyxhzji5k7MouNu417jFb0MDKeQMZuZAwgYxgZ5M-LyPDQ_bmTJv425mwOYaPD4eJWb7C4xTtW7MyhBjFyRhrZuMOgI7eedYKBJSblEXOMJrTwVeSgrJGTqBqNPJqDFu8x-QTbGrF0s57eNJqMJUZzFWEz8rWtzhqjrRsJohHvWD2bCyGyOo6fds39fa65t68ZZ82c7R3z-vNWs0N4TW9IfCqwT2bOLG4JPnmD5BPOIsIz4g2wq0jwUMpNwbViZ77WH508PG-1GmvQOtNB7cggIOR6EP6aAmSVd4S8TDcFETX4KFoAiosDEH0GHUB74UKGn08QYofuMQl2N2D_BoMENZlL0FY3hC460ecyeKUy0RpviLzTIa9AxujyhoudxS2RSU8CJn3uc8GsAW5IBY7a7BUAalUVXrOwRuUdFeJG7a51oK_g2vv7Cqdcq6evQMRiE7EMesNEWJOJiKooE1MBTB5pe4e9ESHQbnbVZPSDQuBHyaephNYpdnj9Gj0bk9BL1xVeWW0MoTaU_yHxvEIyj6aeSJ5tcRMbyclmCOUI9Gblz0iMaTL1DuZR-ixrpVlel52Uu8YbJWKLiE5FmWpj2626gItb4EUvae7rpq65xY3kAceISdhMhhumazhVc3uvEpqrc0Oa-3duvABqVXO3ITw1SqUYqZ4-_90CNMdmpmoTzZTAWJ9wKsbvIvxVBePMvatWrWiJwAr5gqG5AaycAUKyiyPJCXzwfhKANx_z6_UoWOgdxKL3BhPl4Ny8mrxoNXllxD8YOXQze2GzbZbKb3b0umivzk-pySub_iufsgEXuIFo5jD5e5T41sTX7vp86AIFE_TuZy3fBQoqVXuetbB0V7Z3mf29BaI-9l7yv-ariQmYSheaT6ubsWpixvx7JRiKK8Ps2QnP4TMrgAastuTn8WtdwUIrPiQZx-44VxJ1ro1TnPXZdlRvNlj5zBztK3X_TtzN6FeqMu5x03HqDGPdolyvib-p6Gj1fa0ty1tXrM2hWxpR3tfQakBDn_rpX58xAyUNuCANPcFXZ0Cirs8v4eAOWWFIXqGTCF7WshZ5RZIPdg6tjSw7RBZpIzBIuxSrUJqmGBr_MW2B1Fb_QHoO5QwXdaNNscobbjtZdXXG8bWLcm-b-EEt7un3BoiRHJEi3jCUhnzHrr_NDcMejRkC9d--89Cp1eVPnaJGFBMJP4Y0Yh4Ro0sZEaPIWuAisN0NQgTvmPj7igiuJm_xPDFWg8UN5L9NXn-xPSIEDAhhuxXIAOSGpPAOlwJIr8rhxxxbhQ7ONirhzFMQZs58FgannubOhQw8M5DBiIlXaUGU0KBiF_lc8J5PWQYRkwKwQyIYDaIojRoSgE6lCqBXDw7KEgAHvZAuCsXEB5D-JeM0cEhfyuA7cJB6m6Wk5AkJ-WELf13NI4k1XLNyhZyMV6UJxeeBBPGSyXNJTqFa-dnYvyFsb2wYRW31jRq51bG3VzHos4Ax97oolB7zGsGQCzTqMgJBOsAifYBtrvmzwy46FB2j5X7COomMYS17cCkbxQqncYM-iuKopBjyFwh_RZGaff-KtffK0aMNyOzt_z5xbSAyxgAJ7E4mL3ZbldZMzC4LO5AkK8k7Zp2LF61se7tNwg07b_FwQ9Jvk-h3z5TAZDw6pYL6fTaDlypITSVm7QjLVPqLG4tdsp5D8cwr_XcHoyucEHjneNlBoVRNzpCNFYrQ3Gw78TLVHG45Bj7VgDkpkB1b4BxURoaCAJbmhIzaItQbI0_ifB0sBGb32VM1LurqI_YWibkgvzbB32M4QjeV3HVoVSYGtZGK1DL4584xIsffb0TOpf0RiUaw9RRKyuD4KdW1PSb-Tkw9lfiy1CT60Rq1oEq4ILUejBZUCZWq0snFUu49P83Q0JJCQ7UGecUUS1818XJlolmlqtIv23gHmHH_S8Ira7yQaF_jotVoyy829WrxaW3lxczi0zzWppsam2pOqJQriqOyiaM64wSO6tcOvieumeEVJJcFLb1I-CSxQSJ6pTrazvAKCe0MrxCTxHVVEwEKIsXpvPCya_UeGSZysERp-8o3pvxaQejhleZOeR2qVAc4FKGDWxGc8nIp6GvyU0IEfrcLChb8J4fcvoPkulrcdjnXF4EUz57ImiudNVNIz4us4o1KZhXnM3i9Ip9ccRMrpA8QSNpJbN8vWomDcoYxBRBUR6d4UsIoCdlGApcqtrk4gTjGcXxsJTBFGge0R3ratfb2FZpvyk7JzWBtA5BTayNcl_1M7hqddqLLWjGjXFv-LJXw9Dfx2Ya_eyMx25jYvnHlZST_NPFqtYZOVuYQKwhvlNrgUlYCFdHfPSja4CA_pAZv-VV0iF5ViE5EUgKF7tI1w2dhcNLjkqNUTeQoEznKRI6S6g4ncpRycxRqWQ6iwLLsRPguxbJY313ZJpkUyHudnBfYE0oMGZLbmB2hd8PuA3dcMzC9Gue1ou14kzUVTgg7eMipYPj_TOqeRmL5u-WwH8rPHn6yIWZPI-WGoOkD9zRSaqi5rxHF2Nw_pe5_pBuBRsvc9Vg7EY9_CgaojF2PUhD3uInvU_R1skiyCkRTpeANavX-641MoKkRPkhHe3jQFqFTtZPkzvJtKU4bPe03ChBTHQGN5rsiwEXsbErNWbFrydGUyYoIT4i6TIeph-3fW9m8bEWLZEsgBB95zBZhO2eAsNvDsfMp1ObrDJxhN6RlzPrQLWwdz6JxPWcMdzS2Ry0k7XWrJGVteoyYWLuEopWipuUWrXT2pKKVTg3utdGoLp-iFYf0EdNpdzimFxFeoTaElBcYkRCF-kERRDVmCM_NfkdIpVXRnIk1klCvNkRTSpISwU4mUOUg6KO6DIHyutjW9TsL_1DBhJpEYk6IxThCDEvBe5blW0clZK4CkHnvPy0BLjEDQOic5vtZYjhUjIjpjaGM23Dr2geIt3XtfezfHvbv09z_Uv_Wn1DrSkt0tM2pZSU66d5pRhbn6snz9fdN9E9Cxh12IOMPLNwCbNsYoRsQvQFWVDUSd20UHpA3bUh8kEg3WmLThkSpivKzWCGIiaf6TYUwrkRan60mRovvrPapNZFmicPH4f0SsidYNcYnp_rEJUahCsPCrRw0AVWINH9TVOG3TaEqRPggVXgO-aoQoVRVYUdaL_wT-5Cy-uEAfhNuuaJumaa-HwpIzwF7u9V6X-re7s3Hbw-bcxpzQj0UoAY246RVQ8d5RzE9HqRNu4T9xBz6zkkB9BcqmccfygB79NUgZTAtj_84_mgi7G6OxBxN1F0OcHAt1u38OT2ND13PxAnGS-74xpjLnzXxA8lbxJjlSdwitl30YjTWhAOJGm35XnF8bY62S6wEi5NcD6hi1uitTGCNzlRyK_4jQHeVpQpDNJxOOxv_x93imIrEBYnz5353NaTS6zPSzXGObCZ2RI4lAqzMjkhA1yeCnksQ9HDoTwjoD6WfROdV3CToO86_h0LsNW4I-8PwSXSgorsFace_L3FhN0CxU9QULli4TdvKEJ0zC5lSjgM-_odZ4U4GgB0C8kPT38kA0KtQvmhikC72gDavuoyyEkGES4vX6rOTNWHyGJDHW-VHgOPdqi35eHWMNhwQR5wfN3Gz5m37CYyjFJzIKef0rREhIyT9fYHDFVT61ggnuBkpifrBJS2JtjVhaQBxSMzu6P3laRcdcNF-F-1zUY-LnpaOZNEzsm03SZwjkfFROH4ELCfr5A13FwOLLp0uoO9R3EEHTGn3vrlzpuh7cIYEgHwKwLb4dy6fIgsFyHZ7rQCgpyz8DQUgh805ChF0Jm_XP34vFEnwQCgNcCMiaFSctiIs7knHI8cZKapwm6GZ2B1PHj81ntMABPaBcAZv2npVkVovkoNtIrlYtHkAVRWHbhZVxSgfJLp3_QJzlFIV4EkLKxRl9wg4VALS_5PcvZy-Rrx3SZ-bKNhLUkrrbOAsJop3ISNaeucysb__MhjROsjt7H7vevX3XwZhlbtgR37_ZVBXt0Ys3YwpKr0Q-xsxMRrooucr_IsxtVeIqo71F2NKfx6_NoZfjGlbhn285f2ULCbv6ZNi8lo1Eve3shmRddRdcgTOdJ98246ux-x3DGqudVCu1rHrdvzq_dunr96-fEGDMZsqbkBR5VNU1X2re-H86XcdmBOlMLOmTzGtjl6dvubcuiiFnbXFGDt3Tps_3TDaVYrwKe9fXL9Ap6iSKG6ZtzqRYlodPnPjbToFcTg-BaqbPW_egukfTXu5qcGaPXvgRws4BUFHGmPPbdNnfJQPx6j18bjKcZZUO1aN9eXFbdtRDcOgqgaTq1WOWWMuXsxJbUGKKOnS7WiyP4DtTG0z-Fc6J-krnQD7SqT1P7DS_Jg,',  # noqa
            non_region_query='чехлы для мобильных телефонов apple синий',
            non_buy_stop_words_query='чехлы для мобильных телефонов apple синий',
        )

    def test_found_stop_words(self):
        """Cлово "для" в одном случае - стоп-слово - а в другом - нет. Слово для не должно появляться в ненайденных"""
        response = self.report.request_json(
            'place=prime&text=Матовый силиконовый чехол для iPhone SE, 5s, 5 синий&hid=2392540'
            '&rearr-factors=market_show_not_found_words=1'
        )
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Матовый силиконовый чехол для Apple iPhone SE, 5s, 5 (темно-синий)'},
                'notFoundWords': NoKey('notFoundWords'),
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=Матовый силиконовый чехол для синего iPhone SE, 5s, 5&hid=2392540'
            '&rearr-factors=market_show_not_found_words=1'
        )
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Матовый силиконовый чехол для Apple iPhone SE, 5s, 5 (темно-синий)'},
                'notFoundWords': NoKey('notFoundWords'),
            },
            allow_different_len=False,
        )

    def test_not_found_apple(self):
        """Слово apple почему-то игнорируется пантерой в первом запросе и нормально находится во втором"""

        response = self.report.request_json(
            'place=prime&text=чехлы для мобильных телефонов от Apple синий&hid=2392540'
            '&rearr-factors=market_show_not_found_words=1'
        )
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Матовый силиконовый чехол для Apple iPhone SE, 5s, 5 (темно-синий)'},
                'notFoundWords': ["apple"],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=чехлы для мобильных телефонов Apple синий&hid=2392540'
            '&rearr-factors=market_show_not_found_words=1'
        )
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Матовый силиконовый чехол для Apple iPhone SE, 5s, 5 (темно-синий)'},
                'notFoundWords': NoKey('notFoundWords'),
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_correct_model_not_found_words(cls):
        """Для модели список ненайденных слов корректируется по ее описанию и параметрам"""

        cls.reqwizard.on_request(text='air pods model беспроводной с микрофоном и хорошим звуком').respond(
            qtree='cHicvZhfaBxFHMdnZjd3m_ESllyicTEaY7GnVAyCUiTWWvsQfJBQsOiC0J6pJrTplYtiKYpX0tIkLVZQfNA8VGv-gpc0afCS5p8V0RbF3aIVEYuvYkH6UOyT-JuZ3b3ZvVlj2pJA9nZ2fr9vZj6_Pzcb-hxNGTVmbSNqxhnSiuqQhVrQw-gx9GTKQCaC5yiDWtHWqvaqDvQi2oW68HsYfYTRpxhNY7SIEfx8i5GDO60hQjuocDPAjclpu7vzFm4OJDVJErUjJtlV2OUrMuuIaCvajLctYgObyGLTLSiDW3HH-0SsJd9G2dNGzBQ3ox243ybDxWwCrFEmnTXgk2zszWjZpEksLG6QhTdmtC50kOSfiXgP2OTUjI3PhAX2dIJfNQho3ftf4xIa1-IS90YkCjbyvdn8IWKQAkawQ-sUoS9E8OgHcp29jA9u9gDpCkCX5lZ0HxH3UDFaEIz4vAISPA5DmvRWqVVCSldAYt6oDEk_VZxZCvyrWWg6KyHpXItL3EP5uhrZuphGGZFxEEmIVgjdGUFU1ZPr3LMvxKhKweify0FeCg8Vo18FI2EQhbSFiuc3m0svVfj_v2yqhmzS9sXgE9LNFdLxWfa3Tt-KIGx0zjiz7mFn2S04S840_J51FuE6F6KaUlD9biagGiuiAn1V46BjfTz2p332P2EaaxtsmhRKtjZRXLa1UXYZYZdxuOhjxZWLtjZcXMlSAKg7U-5JIJiC-wQIzDhLPMmJRZxZfofhbo6Hg90t-CXg9vl27rtwx7XcPun-uOQfeDmlwOuoP8v-PoQF53-P2RdIzQUFFbMvbwNT8gbWYWNza9siZB72Mu8qoa9HMq_eWXBKzjzbunsElswCuhDKOqrIuh__1PysUwqoMu4PUdpK-2i2HcfUjNoF0dD6IZnIeDHcHp2p4C4eix8ICoB0FmwJZdqz7AsAHvdyZBNVLjq2V_q4LyZpdwR3rXsUJJhMPwiGQScVoK9M3kj6pCO-KsaXEpxxxDJKt5CgYYtypkPb0kYnS7Y-Xiyt8OuX2XozadU65yAfj4F1ZbbLWOHrLTbrK7JWlfWwmpvK9FDNNJoJqw6CNAVqwbqlypxd1zXKK7sTVmZK7ON6x_quUE2x3qQQ92VIdZ8h0zQhG2h5B5Xld1t2ITxKgbraN73KboTKkl_wYl-8pB-MVMDqxfwVoa9EijnlfOFMu33OPO-ZqFzJCUUlFz-o8Qs55KcqY1e0ypBdtIj7MDX8-dvZGgXb0upNknPcQEOLXI1iS43xjlnAjaRZy1S13tqQvaoY_ohhs__SDaMBbX10u_WbTl-OBMvgmXzWPWzhh1pw5j8O8_POZwk_VIGXKkzXxBkqsImG6AgLkTcHZyRBRu8fWrlQeWy95a8vLav6-sIQoyeUq2AnMjxh4zEbj9p4xMbjNh4Oosb8jqmWLzJsrLiuO4gB-fH6gpTSWDeISTLI_oFAxtUdyGy3FgntrGgPC_wsOeucc0qhrFP1h6FxvdwfJEdV4l30-4NkF00-VpnStLoy82_LVu4JqZEsQ5gnIMxjkzYZhc-RIn8lFcRmvTYsmrR3nHVPBM28fHA9GReF8NEUiOqmDkR_YUQROrTd-obQbIQo7enem8-9mtuf6wnxVB1Rhz688jnxiUqOKp6XBU_JKvrq2UKrg0kJJX-97OEsN-WfpZJAkKZreEfF8CLZrhZZ4789sPzCCWwN0wC2gwHbG5juirCt3tuVy-d6u7rDaKtXQ1v2U5EdEGTLRlGw91PDn4vjuo2W3QObtWENwaAmzSAYDeL7qM4Y1N1lYCtt0Ab68wNPNb3ZsmNzM3qEbURYJMyEZ3E3WJxua_r-kw1PhywAr2eRaBg4f31LU_LCG2EL-JuBxsnz19uarn39eNkizdeh191hGDuSBqnTnu_YI56S4CmWnlIzJT_dPYhT3k50ozaPxBA6lDyE8pKHbFPSMAWKfAiI_gVv6z5N'  # noqa
        )
        cls.index.hypertree += [
            HyperCategory(hid=9402, has_groups=True, range_fields=["Type", "Microphone", "Impedans"])
        ]
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=9401,
                micromodel="наушники {Type}",
                friendlymodel=["{Type}", "{Microphone:с микрофоном}", "{Impedans:Impedans}"],
                model=[("Технические характеристики", {"Тип": "{Type}"})],
                seo="{return $Type; #exec}",
            )
        ]

        cls.index.gltypes += [
            GLType(
                hid=9401,
                param_id=940101,
                name=u"Тип",
                xslname="Type",
                gltype=GLType.ENUM,
                values=[GLValue(1, text='беспроводные'), GLValue(2, text='проводные'), GLValue(3, text='гарнитура')],
            ),
            GLType(hid=9401, param_id=940102, name=u"Микрофон", xslname="Microphone", gltype=GLType.BOOL),
            GLType(
                hid=9401, param_id=940103, name=u"Импеданс", xslname="Impedans", gltype=GLType.NUMERIC, unit_name="Ом"
            ),
        ]

        cls.index.models += [
            Model(
                hid=9401,
                title='air pods model',
                hyperid=20001,
                glparams=[
                    GLParam(param_id=940101, value=1),
                    GLParam(param_id=940102, value=0),
                    GLParam(param_id=940103, value=250),
                ],
            ),
            Model(
                hid=9401,
                title='air pods wire model',
                hyperid=20002,
                glparams=[GLParam(param_id=940101, value=2), GLParam(param_id=940102, value=1)],
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition()
            .add(
                hyperid=20001,
                reasons=[
                    {
                        "factor_name": "Хороший звук",
                        "type": "consumerFactor",
                        "factor_priority": "1",
                    }
                ],
            )
        ]

    def test_correct_model_not_found_words(self):
        """Список не найденных слов для моделей корректируется по ее описанию и причинам купить"""

        response = self.report.request_json(
            'place=prime&text=air pods model беспроводной с микрофоном и хорошим звуком&hid=9401'
            '&show-models-specs=friendly&rearr-factors=market_show_not_found_words=1'
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "air pods wire model"},
                "description": "наушники проводные",  # description сгенерированный гумофулом
                "specs": {
                    "friendly": ["проводные", "с микрофоном", "--"]
                },  # friendly параметры отображаются на сниппете
                # с микрофоном - выбыло из списка не найденных слов
                "notFoundWords": ["беспроводной", "хорошим", "звуком"],
            },
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "titles": {"raw": "air pods model"},
                "description": "наушники беспроводные",  # description сгенерированный гумофулом
                "specs": {"friendly": ["беспроводные", "250"]},  # friendly параметры отображаются на сниппете
                "reasonsToBuy": [{"factor_name": "Хороший звук"}],  # причины купить тоже отоброжаются на сниппете
                # беспроводные хорошим звуком - выбыли из списка не найденных слов (предлоги учтутся как стоп-слова)
                "notFoundWords": ["микрофоном"],
            },
        )


if __name__ == '__main__':
    main()
