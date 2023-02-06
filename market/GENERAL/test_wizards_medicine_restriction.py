#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CardVendor,
    CategoryRestriction,
    DeliveryBucket,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalModel,
    RegionalRestriction,
    Shop,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import Contains, NotEmpty, EmptyList
from core.types.card import CardCategory

# Тест проверяет правильность обработки колдунщиками товаров из категорий, имеющих ограничение medicine

# queries and corresponded wizard rules
# NonRegionQuery: gradusnic Termo2000 в интернет-магазине
# NonBuyStopWordsQuery: gradusnic Termo2000
GRADUSNIC = (
    'thrermometer AND6252 с доставкой в Москве',
    '%7B%22CommercialMx%22%3A%20%7B%22commercialMx%22%3A%20%220.56243%22%2C%20%22NavRank%22%3A%20%220.371483%22%2C%20%22TovarModelRank%22%3A%20%220.180709%22%7D%2C%20%22Wares%22%3A%20%5B%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22org%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.8499994874%7D%2C%20%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22geo%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.9999993443%7D%2C%20%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22site%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.46999982%7D%5D%2C%20%22MarketTires%22%3A%20%7B%22RuleResult%22%3A%20%220%22%7D%2C%20%22Market%22%3A%20%7B%22qtree4market%22%3A%20%22cHic1VS_axRREJ55u2teXoIsOZRjq3NRs1gtqWIqPZvDQo5Dgmyh55ngWaiwUQhXLTYuVqKSQoOFej_BjQqChR4pjVhcwDJY-m_E2d97lyUo2LhwzO28me9975tvn7goZjmoUIQSM5gJc6CBDmdgAZbiPBhgwjmlolThCtShiY8RniO8QviA8AWBnq8II1zRfjBxLYLj1ObDTd-0r6_cX7tz64aGpQR4OgMMFfCBm5sbP9-yGDptm9jDhEUs_0KOKmhpkQ4Gmlh9ykKC9kmRrhWR1mARauhY0DhCfWAUmrjO7HJulWuxthfXNThFNr9mSI0plWkY_gEN5w0pwDDzMJjrWdg-iBF0nM7rkNy2N86uxThzEEgQ7T2KOsnKVJbKennVvn13wTRNX1Ys_bGuSV-erhdCWe_FNZGsT-RI1hMiXStCRD1lzdehBSFnXeX7-5LqOHKRlWRDMcHfwBpI3HHUY_622oYkKn_nvUOc10WxPOE8JTgqISbqKDnq7HmbCWrYcVAYqXyehEFNCQ4fiVKINNFFmE70GB9-PMo6RsN8iKI2wVSO5xifXQ6I4hhRdymmKeePj5VPkUVQC5b1GTN8_JeAb_TttiBmooeOYiXJJ-6PJ8rLft4A67ucDuuT_A-HtcPE1QkJxO6D0cddZ_R5NMzA5g3szbPjMXCmKc_Ne-EtkamKRvc6vSbSxXcZQ3OSkY2GoaXt5UzVMPliZffF9o7FBlv08yzWo9ij2KXYpdih2KHYp9in2N7KXivkCrTP5uIq7ktv-5uFAwt7FnYt7FjYHzcUkqH-NzvJ9PYIC0ImqmxuhvPaFGdz0qXqaphlSRaj7GxQS8fhR20f6DenvE-X%22%2C%20%22MatchedFlags%22%3A%20%5B%22stop_word%22%2C%20%22buy_stop_word%22%5D%2C%20%22NotFoundRate%22%3A%20%220.33%22%2C%20%22MaxWeight%22%3A%20%22316392846%22%2C%20%22NonRegionQuery%22%3A%20%22thrermometer%20AND6252%20%5Cu0432%20%5Cu0438%5Cu043d%5Cu0442%5Cu0435%5Cu0440%5Cu043d%5Cu0435%5Cu0442%20%5Cu043c%5Cu0430%5Cu0433%5Cu0430%5Cu0437%5Cu0438%5Cu043d%5Cu0435%22%2C%20%22AvgWeight%22%3A%20%22105892501.33%22%2C%20%22FoundCities%22%3A%20%22213%22%2C%20%22MarketIntent%22%3A%20%224.26%22%2C%20%22RuleResult%22%3A%20%223%22%2C%20%22NonBuyStopwordsQuery%22%3A%20%22thrermometer%20AND6252%22%2C%20%22MinWeight%22%3A%20%223716%22%2C%20%22WordNotFound%22%3A%20%221%22%7D%7D',  # noqa
)
MED_GRADUSNIC = (
    'med thermometer BWell XT25 брендовый в Санкт-Петербурге',
    '%7B%22CommercialMx%22%3A%20%7B%22commercialMx%22%3A%20%220.686867%22%2C%20%22NavRank%22%3A%20%220.361812%22%2C%20%22TovarModelRank%22%3A%20%220.188207%22%7D%2C%20%22Wares%22%3A%20%5B%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22org%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.8599994779%7D%2C%20%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22geo%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.9999993443%7D%2C%20%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22site%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.46999982%7D%5D%2C%20%22MarketTires%22%3A%20%7B%22RuleResult%22%3A%20%220%22%7D%2C%20%22Market%22%3A%20%7B%22qtree4market%22%3A%20%22cHic1VXRbwxBGP--2TnGKNm0qZxJyPW2OBLJhpcSD5SHxoNUSVP7IFUt9YBkkTR9cYi4ECJIJYgI2msbriWaeKCReEE87D0ID-IR_wUzO7t7273VkIjEJZeZ_eabb3_z-377G76TNzAwIQs5UiA2NIKAPKyDDbA5jEMBbNia6ch0Qg_0wiBeRbiFcB_hKcJLBPl7g-Bhvygj3x2UY3KbKmccHegXmItKGrGS0AGq5OCj1y1hSZWeqGpDG7Z3MDRBqNU8FNDGzutEY3HXcRXNqii0QRcpVRwc7Vsgs6HQ1MfkSNacKBiDOESGCSNFBPku8YHw3gTSRYfdA_2nThw7clDhxVwAeFEK4DsjXx6TEHJtXxrwr-gjryUl8bfy2loWg1Ng0YHwDAq5256aVXLIaKX-rH0LTSJQT0DgGn16106rMR9f7uq0HUZptDIXXYzXJ-jzSkxS43XvgHv0-Abbtv-Q12hfGq_bNa0nw5yA1ms0oLWF19YicdRQsyEYBo05v4SdNouYJTmjkLEhb7IfPwyzWKQyQlVEvc-5RxhrfvzN3SGuEN6TkM4C74U34z0TuDaPhXmE_ulKQ3i6YEva0cpaMkFGcLAHoV7OIw9Wsn5xvyOv3jlkvFKvBm9aikDPXkSz5_6MCFI9H868GTnjJgrqzXpP_ShGe1TmuTBWvSS1AUMoe44Bf1RxUQDnEZUU3Ww9tEPconxfPUXVy79B0ffbIytjHKk9aRxNG5IjIoKMdI7USky1dRxtjDiyIo6siCMr4siKOLJiHFkRR1bEkRVxZMnvAt0zqTje_lMMZ-sx0NLtfwtCiaWXJOQyabBi0WxWjRUjBu_4s3to_luoO6G-jO8lsmJkP5kU7X2u3Imq6h310jPat0nlocj47hIIrynQXZ7r8C9uo9Are4MvR1xA3pVASkOjDM9OfaA4B2hpcwiTpvsjaV8lPVh2yLfGxbb-qQcfb3CPD0OIJK8tW1kgasOLmkVNKpv1ntaa9Zz-xWa9JXx_ggJePevNVIvS5GZjZdMa9vDGsrBwbFOaX3zWnhrLSnpGK68tTsduDC3uWX1nuN2xrNnkBzU5Jf_SYcantNOU5ViW45gcx-Q4IccJ5UBTcaf2v9JNqXUzpbsVaVs46eC4g2UHxxycmCuomBH_N3Ki8ukitnDfCRqXMxTNjDfzjw-2rLjZv6ctB-vV222Z0-TnkMbFjHUtZNho7Ooc0FFZJxFtCOpRttQF_ShB6Ef57p8cH0m_%22%2C%20%22MatchedFlags%22%3A%20%5B%22stop_word%22%2C%20%22buy_stop_word%22%5D%2C%20%22NotFoundRate%22%3A%20%220.25%22%2C%20%22MaxWeight%22%3A%20%22316392846%22%2C%20%22NonRegionQuery%22%3A%20%22med%20med%20thermometer%20BWell%20XT25%20%5Cu0432%20%5Cu0438%5Cu043d%5Cu0442%5Cu0435%5Cu0440%5Cu043d%5Cu0435%5Cu0442%20%5Cu043c%5Cu0430%5Cu0433%5Cu0430%5Cu0437%5Cu0438%5Cu043d%5Cu0435%22%2C%20%22AvgWeight%22%3A%20%2279488037.50%22%2C%20%22FoundCities%22%3A%20%22213%22%2C%20%22MarketIntent%22%3A%20%223.62%22%2C%20%22RuleResult%22%3A%20%223%22%2C%20%22NonBuyStopwordsQuery%22%3A%20%22med%20thermometer%20BWell%20XT25%22%2C%20%22MinWeight%22%3A%20%223716%22%2C%20%22WordNotFound%22%3A%20%221%22%7D%7D',  # noqa
)

NONMED_GRADUSNIC = (
    'nonmed gradusnic Termo2000 в интернет-магазине в Москве',
    '%7B%22CommercialMx%22%3A%20%7B%22commercialMx%22%3A%20%220.516731%22%2C%20%22NavRank%22%3A%20%220.371812%22%2C%20%22TovarModelRank%22%3A%20%220.176544%22%7D%2C%20%22Wares%22%3A%20%5B%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22org%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.8999994397%7D%2C%20%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22geo%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.9999993443%7D%2C%20%7B%22Intent%22%3A%20%22unknown%22%2C%20%22Type%22%3A%20%22site%22%2C%20%22IntWght%22%3A%200.0%2C%20%22Weight%22%3A%200.46999982%7D%5D%2C%20%22MarketTires%22%3A%20%7B%22RuleResult%22%3A%20%220%22%7D%2C%20%22Market%22%3A%20%7B%22qtree4market%22%3A%20%22cHic1VTPa9RQEJ55eWlfX4uELsqS03ZRGwQh9FR70vVSPEgpUjQHWWuL9dCWpgplT8GLiydR6UGLB3W73QW3KggedOnRioftQfBQPPpv1EnysknTtFLwYmCZzfz48s038568IgcEGJCHArOYDYNgQhHOwQiMRX6wwIaL-rg-AdehDHP4GOE5wiuEDwhfEOj5itDBGfMnyhsKTlCZD9ezsLgwPztjYqGL2pNAhXHwUefWVn-9ZRGuqkmh2zCKpSWBBpgqoQgW2jjxlIW03PNSBfJIARiFSVZtOVibpi-aYOWmBVk2vGxpc7jC3KED6eg5ECX7KRUmmIdAvMwfTJZTrfXdcW_N3F9euHvb7w4Lqr2-v7UX12V1-BuDFuOkdJenZRw7lLlbysyqOqzWOijHdK_BTAz_gInDSiA7C-NISc9mVWjVWutQXd9joCszWKzrtVl3fnHEtu1j6tqty9L1cijrvShHyfqEK1mHZBzLg6IesxYrUIGQc9EQe3ua4Xk8zwrc0u0Mj_9Jp6kJzzNO-kTMVU2OH--kHXHO6iinUsuoB80TYlcvPUOv3dZaFzWsOCiVVrpEUqGpB3IomXJKpaIM3V2F9q9DNNwyqvE-RDmZYsqjyUa984Ao7iNaHYto8uyBstIZWho0g3Cx3w4f_yXgq26qCkRMiuGOsYLmE_fHo_ycvs8tcL7zeFif-D8c1jaTN1MSyJ0HnY87Xudzp52AzRrYm2enIuBEUdZ-74b3RiJLje51fHHEwXeJFRckI-u0wyV3pxJZ7e4Z5tUXW9sOa27Sr-WwDbIbZOtk62TXya6TbZBtkK1tJi8a2gp0L2Ti6tWXra1vDjYd3HCw7uC6g439C4W0UP_bOnF6e4Q5yYkqG-wXYrJX4KB2dWI29FJOyjugcrk44fpAfwB-rn47%22%2C%20%22MatchedFlags%22%3A%20%5B%22stop_word%22%2C%20%22buy_stop_word%22%5D%2C%20%22NotFoundRate%22%3A%20%220.50%22%2C%20%22MaxWeight%22%3A%20%22316392846%22%2C%20%22NonRegionQuery%22%3A%20%22nonmed%20gradusnic%20Termo2000%20%5Cu0432%20%5Cu0438%5Cu043d%5Cu0442%5Cu0435%5Cu0440%5Cu043d%5Cu0435%5Cu0442%20%5Cu043c%5Cu0430%5Cu0433%5Cu0430%5Cu0437%5Cu0438%5Cu043d%5Cu0435%22%2C%20%22AvgWeight%22%3A%20%22158517587.50%22%2C%20%22FoundCities%22%3A%20%22213%22%2C%20%22MarketIntent%22%3A%20%224.89%22%2C%20%22RuleResult%22%3A%20%223%22%2C%20%22NonBuyStopwordsQuery%22%3A%20%22nonmed%20gradusnic%20Termo2000%22%2C%20%22MinWeight%22%3A%20%223716%22%2C%20%22WordNotFound%22%3A%20%222%22%7D%7D',  # noqa
)


class T(TestCase):
    """
    Набор тестов для колдунщиков при работе с категориями товаров,
    для которых определены ограничения.
    Отдельно проверяются доработки для лекарственных категорий (MARKETOUT-8176)
    Для ограничения show_offers (NMarketReport::TRestriction::ShowContent) = false колдунщики должны рендериться (MARKETOUT-10559)
    Но при этом не должны выводиться офферные врезки
    """

    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(
                rid=77,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[Region(rid=213, name='Москва', genitive='Москвы', locative='Москве', preposition='в')],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[225],
                name="Good Internet Shop",
                tariff="FREE",
                pickup_buckets=[5001],
            ),
            Shop(fesh=2, priority_region=213, regions=[225], name="Apteka 36.6", tariff="FREE"),
            Shop(fesh=3, priority_region=213, regions=[225], name="Galamart", tariff="FREE"),
            Shop(fesh=4, priority_region=213, regions=[255], online=False, pickup_buckets=[5004]),
        ]

        cls.index.outlets += [
            Outlet(fesh=4, region=213, point_type=Outlet.FOR_STORE, point_id=1),
            Outlet(fesh=4, region=213, point_type=Outlet.FOR_PICKUP, point_id=2),
            Outlet(fesh=1, region=213, point_type=Outlet.FOR_PICKUP, point_id=3),  # for geo in market_offers_wizard
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5004,
                fesh=4,
                carriers=[99],
                options=[PickupOption(outlet_id=1), PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=3)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1, name="medicine termometers", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, name="room termometers", output_type=HyperCategoryType.GURU),
        ]

        cls.index.vendors += [
            Vendor(vendor_id=30, hids=[1, 2], name="Beurer"),
            Vendor(vendor_id=31, hids=[1], name="LittleDoctor"),
            Vendor(vendor_id=33, hids=[1, 2], name="Termo2000"),
            Vendor(vendor_id=32, hids=[2], name="Pisla"),
        ]

        cls.index.cards += [
            CardCategory(hid=1, vendor_ids=[30, 31, 32]),
            CardCategory(hid=2, vendor_ids=[30, 32, 33]),
            CardVendor(vendor_id=30),
            CardVendor(vendor_id=31),
            CardVendor(vendor_id=32),
            CardVendor(vendor_id=33),
        ]

        cls.index.models += [
            Model(hyperid=10010, hid=1, title="termometer Beurer FT45", vendor_id=30),
            Model(hyperid=10011, hid=1, title="termometer B.Well WT05", vendor_id=40),
            Model(hyperid=10020, hid=2, title="termometer HAMA T350", vendor_id=41),
            Model(hyperid=10021, hid=2, title="termometer RST 02309", vendor_id=42),
            # for implicit model
            Model(
                hyperid=10030, hid=1, title="gradusnik LittleDoctor MEDICINE (adult)", group_hyperid=90030, vendor_id=31
            ),
            Model(
                hyperid=10031,
                hid=1,
                title="gradusnik LittleDoctor MEDICINE (child) monkey",
                group_hyperid=90031,
                vendor_id=31,
            ),
            Model(
                hyperid=10032,
                hid=1,
                title="gradusnik LittleDoctor MEDICINE (child) donkey",
                group_hyperid=90031,
                vendor_id=31,
            ),
            Model(hyperid=10040, hid=2, title="gradusnic Pisla kitchen RED APPLE", group_hyperid=90040, vendor_id=32),
            Model(hyperid=10041, hid=2, title="gradusnic Pisla kitchen BLACK GRAPE", group_hyperid=90040, vendor_id=32),
            Model(hyperid=10042, hid=2, title="gradusnic Pisla kitchen KITTY", group_hyperid=90042, vendor_id=32),
        ]

        cls.index.regional_models += [
            RegionalModel(
                hyperid=10010, rids=[213], offers=100, geo_offers=20
            ),  # для того чтобы показывался в модельном колдунщике "geo": {"title": "..."}
            RegionalModel(
                hyperid=10020, rids=[213], offers=100, geo_offers=20
            ),  # для того чтобы показывался в модельном колдунщике "geo": {"title": "..."}
        ]

        gradusnic_title, _ = GRADUSNIC
        med_gradusnic_title, _ = MED_GRADUSNIC
        nonmed_gradusnic_title, _ = NONMED_GRADUSNIC

        cls.index.models += [
            Model(hyperid=10050, hid=1, title=med_gradusnic_title, group_hyperid=90050, vendor_id=33),
            Model(hyperid=10051, hid=1, title=med_gradusnic_title, group_hyperid=90051, vendor_id=33),
            Model(hyperid=10052, hid=1, title=med_gradusnic_title, group_hyperid=90051, vendor_id=33),
            Model(hyperid=10060, hid=2, title=nonmed_gradusnic_title, group_hyperid=90060, vendor_id=33),
            Model(hyperid=10061, hid=2, title=nonmed_gradusnic_title, group_hyperid=90061, vendor_id=33),
            Model(hyperid=10062, hid=2, title=nonmed_gradusnic_title, group_hyperid=90061, vendor_id=33),
        ]

        cls.index.offers += [
            Offer(hyperid=10010, title="electronic medicine termometer Beurer FT45 1", fesh=1, ts=1),
            Offer(hyperid=10010, title="electronic medicine termometer Beurer FT45 2", fesh=2, ts=2),
            Offer(hyperid=10011, title="electronic medicine termometer B.Well WT05 1", fesh=2, ts=3),
            Offer(hyperid=10011, title="electronic medicine termometer B.Well WT05 2", fesh=4, ts=4),
            Offer(hyperid=10020, title="alcohol room termometer 1", fesh=2, ts=5),
            Offer(hyperid=10021, title="alcohol room termometer 2", fesh=3, ts=6),
            Offer(hyperid=10021, title="alcohol room termometer 3", fesh=4, ts=7),
            Offer(hyperid=10020, title="alcohol room termometer 4", fesh=1, ts=8),
            Offer(hyperid=10030, fesh=1),
            Offer(hyperid=10031, fesh=2),
            Offer(hyperid=10032, fesh=1),
            Offer(hyperid=10040, fesh=3),
            Offer(hyperid=10041, fesh=3),
            Offer(hyperid=10042, fesh=3),
            Offer(hyperid=10050, title=med_gradusnic_title, fesh=1),
            Offer(hyperid=10051, title=med_gradusnic_title, fesh=1),
            Offer(hyperid=10052, title=med_gradusnic_title, fesh=1),
            Offer(hyperid=10060, title=nonmed_gradusnic_title, fesh=1),
            Offer(hyperid=10061, title=nonmed_gradusnic_title, fesh=1),
            Offer(hyperid=10062, title=nonmed_gradusnic_title, fesh=1),
        ]

        # для порядка во врезке
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.8)

        cls.index.hypertree += [
            HyperCategory(hid=13, name="phones", visual=False, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=345, name="drugs", visual=False, output_type=HyperCategoryType.GURU),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name="medicine", hids=[1], regional_restrictions=[RegionalRestriction(show_offers=False)]
            )
        ]

    def test_model_wizard(self):

        # модель в медицинской категории
        response = self.report.request_bs(
            "place=parallel&text=Beurer+FT45&rids=213&ignore-mn=1&show_explicit_content=medicine"
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "category_name": "Medicine termometers",
                        "title": {"__hl": {"raw": True, "text": "Termometer Beurer FT45"}},
                    }
                ]
            },
        )

        # Не передаем врезку
        self.assertFragmentIn(response, {"market_model": [{"showcase": {"items": EmptyList()}}]}, preserve_order=False)

        response = self.report.request_bs("place=parallel&text=B.Well+WT05&rids=213&ignore-mn=1")
        self.assertFragmentIn(response, {"market_model": NotEmpty()})

        # модель не в медицинской категории
        response = self.report.request_bs("place=parallel&text=HAMA+T350&rids=213&ignore-mn=1")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "category_name": "Room termometers",
                        "showcase": {"items": NotEmpty()},
                        "price": NotEmpty(),
                        "title": {"__hl": {"raw": True, "text": "Termometer HAMA T350"}},
                    }
                ]
            },
        )
        self.assertFragmentIn(response, {"sitelinks": NotEmpty()})

    # MARKETOUT-9059
    def test_model_wizard_show_explicit_content_all(self):
        """
        Тот же запрос, что и выше в `test_model_wizard`, но с =all вместо =medicine
        """
        # модель в медицинской категории
        response = self.report.request_bs(
            "place=parallel&text=Beurer%20FT45&rids=213&ignore-mn=1&show_explicit_content=all"
        )
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "category_name": "Medicine termometers",
                        "title": {"__hl": {"raw": True, "text": "Termometer Beurer FT45"}},
                    }
                ]
            },
        )
        # Не передаем врезку
        self.assertFragmentIn(response, {"market_model": [{"showcase": {"items": EmptyList()}}]}, preserve_order=False)

    def test_implicit_model(self):

        # модель из медицинской категории
        response = self.report.request_bs(
            "place=parallel&text=gradusnic%20LittleDoctor%20medicine&rids=213&ignore-mn=1&show_explicit_content=medicine"
        )
        self.assertFragmentIn(response, {"market_implicit_model": NotEmpty()})

        response = self.report.request_bs(
            "place=parallel&text=gradusnic%20LittleDoctor%20medicine&rids=213&ignore-mn=1"
        )
        self.assertFragmentIn(response, {"market_implicit_model": NotEmpty()})

        # модель не из медицинской категории
        response = self.report.request_bs("place=parallel&text=gradusnic%20Pisla%20kitchen&rids=213&ignore-mn=1")
        self.assertFragmentIn(response, {"market_implicit_model": NotEmpty()})

    # MARKETOUT-9059
    def test_implicit_model_all(self):
        """
        Тот же запрос что и в `test_implicit_model`, но с =all
        """
        # модель из медицинской категории
        response = self.report.request_bs(
            "place=parallel&text=gradusnic%20LittleDoctor%20medicine&rids=213&ignore-mn=1&show_explicit_content=all"
        )
        self.assertFragmentIn(response, {"market_implicit_model": NotEmpty()})

    def test_implicit_model_bad_words_in_title(self):

        request = "place=parallel&text={0}&rids=213&ignore-mn=1&show_explicit_content=medicine&wizard-rules={1}&rearr-factors=market_parallel_wizard=1"

        # модель из медицинской категории - проверяем что title не содержит стоп-слов
        text, rules = MED_GRADUSNIC
        response = self.report.request_bs(request.format(text, rules))
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "title": "\7[Med thermometer BWell XT25 в Москве\7]",
                    }
                ]
            },
        )

        # модель не из медицинской категории - выводим в title запрос как есть

        text, rules = NONMED_GRADUSNIC
        response = self.report.request_bs(request.format(text, rules))
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "title": "\7[Nonmed gradusnic Termo2000 в интернет магазине в Москве\7]",
                    }
                ]
            },
        )

    def test_offers_wizard(self):

        # офферы из немедицинской категории - возможна доставка из магазинов
        response = self.report.request_bs("place=parallel&text=alcohol%20room&rids=213&ignore-mn=1")
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "title": "\7[Alcohol room\7] в Москве",
                        "text": [
                            {
                                "__hl": {
                                    "text": "3 магазина. Выбор по параметрам. Доставка из магазинов Москвы и других регионов.",
                                    "raw": True,
                                }
                            }
                        ],
                        "geo": {"title": Contains("Адреса магазинов")},
                    }
                ]
            },
        )

        # офферы из медицинской и немедецинской категории - не пишем про доставку, пишем про выбор в магазинах
        response = self.report.request_bs(
            "place=parallel&text=termometer&rids=213&ignore-mn=1&show_explicit_content=medicine"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "title": "\7[Termometer\7] в Москве",
                        "text": [
                            {
                                "__hl": {
                                    "text": "3 магазина. Выбор по параметрам. Выбор в магазинах Москвы и других регионов.",
                                    "raw": True,
                                }
                            }
                        ],
                        "geo": {"title": Contains("Адреса магазинов")},
                    }
                ]
            },
        )

        response = self.report.request_bs("place=parallel&text=termometer&rids=213&ignore-mn=1")
        self.assertFragmentIn(response, {"market_offers_wizard": NotEmpty()})

        # для офферов из медицинской категории пишем про выбор в аптеках. также не пишем про доставку.
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        response = self.report.request_bs(
            "place=parallel&text=electronic%20medicine%20gradusnik&rids=213&ignore-mn=1&show_explicit_content=medicine"
            "&rearr-factors=market_offers_wiz_top_offers_threshold=0;"
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "title": "\7[Electronic medicine\7] gradusnik в Москве",
                        "text": [
                            {
                                "__hl": {
                                    "text": "2 аптеки. Выбор по параметрам. Выбор в аптеках Москвы и других регионов.",
                                    "raw": True,
                                }
                            }
                        ],
                        "geo": {"title": Contains("Адреса аптек")},
                    }
                ]
            },
        )

        # market_parallel_reject_restricted_offers=0 отключает фильтрацию медицинских оферов на базовом
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        response = self.report.request_bs(
            "place=parallel&text=electronic%20medicine%20termometer&rids=213&ignore-mn=1"
            "&rearr-factors=market_parallel_reject_restricted_offers=0;market_offers_wiz_top_offers_threshold=0;"
        )
        self.assertFragmentIn(response, {"market_offers_wizard": NotEmpty()})

    def test_offers_wizard_bad_words_in_title(self):
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        request = (
            "place=parallel&text={0}&rids=213&ignore-mn=1&show_explicit_content=medicine&wizard-rules={1}"
            "&rearr-factors=market_parallel_wizard=1;market_offers_wiz_top_offers_threshold=0;"
        )

        # офферы из немедицинской категории - заголовок отображается как есть
        text, rules = NONMED_GRADUSNIC
        response = self.report.request_bs(request.format(text, rules))
        self.assertFragmentIn(
            response,
            {"market_offers_wizard": [{"title": "\7[Nonmed gradusnic Termo2000 в интернет магазине в Москве\7]"}]},
        )

        # офферы из медицинской и немедицинской категорий - убираются заказать, купить и т.д.
        text, rules = GRADUSNIC
        response = self.report.request_bs(request.format(text, rules))
        self.assertFragmentIn(
            response, {"market_offers_wizard": [{"title": "Thrermometer AND6252 \7[в интернет магазине в Москве\7]"}]}
        )

        # офферы из медицинской категории - убираются заказать, купить и т.д.
        text, rules = MED_GRADUSNIC
        response = self.report.request_bs(request.format(text, rules))
        self.assertFragmentIn(
            response, {"market_offers_wizard": [{"title": "\7[Med thermometer BWell XT25 в\7] Москве"}]}
        )

    def test_ext_category(self):
        # запрос к немедицинской категории
        response = self.report.request_bs("place=parallel&text=room%20termometer&rids=213&ignore-mn=1")
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        # Популярные модели. Описание и отзывы. Сравнение характеристик и цен
                        "text": [{"__hl": {"text": Contains("моделей"), "raw": True}}],
                        "showcase": {"items": [{"label": {"text": Contains("модел")}}]},  # 100 моделей
                        "category_name": "Room termometers",
                    }
                ]
            },
        )

        # запрос к медицинской категории
        response = self.report.request_bs(
            "place=parallel&text=medicine%20termometer&rids=213&ignore-mn=1&show_explicit_content=medicine"
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        # Популярные виды. Описание и отзывы. Сравнение характеристик и цен
                        "text": [{"__hl": {"text": Contains("видов"), "raw": True}}],
                        "showcase": {"items": [{"label": {"text": Contains("вид")}}]},  # 100 видов
                        "category_name": "Medicine termometers",
                    }
                ]
            },
        )

        response = self.report.request_bs("place=parallel&text=medicine%20termometer&rids=213&ignore-mn=1")
        self.assertFragmentIn(response, {"market_ext_category": NotEmpty()})

    def test_offers_incut(self):
        """Проверяем, что врезка не отдается, если найдены оферы в категориях с ограничениями.
        Если выставлен флаг market_parallel_reject_restricted_offers,
        то оферы отфильтровываются на базовых, а врезка показывается.
        """

        query = (
            'place=parallel&text=termometer&rids=213&rearr-factors=market_parallel_allow_not_prescription_offers=0;'
            'market_parallel_improved_warning_filtering=0;'
        )
        response = self.report.request_bs_pb(query + 'market_parallel_reject_restricted_offers=0;')
        self.assertFragmentIn(response, {"market_offers_wizard": {"offer_count": 6}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})

        # market_parallel_reject_restricted_offers=1 по умолчанию
        response = self.report.request_bs(query)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "offer_count": 3,
                        "showcase": {
                            "items": [
                                # 3 - пропущен, т.к. в оффлайн-магазине
                                {"title": {"text": {"__hl": {"text": "alcohol room termometer 4", "raw": True}}}},
                                {"title": {"text": {"__hl": {"text": "alcohol room termometer 2", "raw": True}}}},
                                {"title": {"text": {"__hl": {"text": "alcohol room termometer 1", "raw": True}}}},
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_medicine_models(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name="medicine", hids=[101], regional_restrictions=[RegionalRestriction(show_offers=False)]
            )
        ]

        cls.index.models += [
            Model(hyperid=90001, ts=90001, hid=101, title="Лекарство 1"),
            Model(hyperid=90002, ts=90002, hid=101, title="Лекарство 2"),
            Model(hyperid=90003, ts=90003, hid=101, title="Лекарство 3"),
            Model(hyperid=90004, ts=90004, hid=101, title="Лекарство 4"),
            Model(hyperid=90011, ts=90011, hid=102, title="Не лекарство 1"),
            Model(hyperid=90012, ts=90012, hid=102, title="Не лекарство 2"),
            Model(hyperid=90013, ts=90013, hid=102, title="Не лекарство 3"),
            Model(hyperid=90014, ts=90014, hid=102, title="Не лекарство 4"),
        ]

        cls.index.offers += [
            Offer(hyperid=90001, price=101, title="Лекарство 1"),
            Offer(hyperid=90002, price=101, title="Лекарство 2"),
            Offer(hyperid=90003, price=101, title="Лекарство 3"),
            Offer(hyperid=90004, price=101, title="Лекарство 4"),
            Offer(hyperid=90011, price=102, title="Не лекарство 1"),
            Offer(hyperid=90012, price=102, title="Не лекарство 2"),
            Offer(hyperid=90013, price=102, title="Не лекарство 3"),
            Offer(hyperid=90014, price=102, title="Не лекарство 4"),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90001).respond(0.90)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90002).respond(0.89)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90003).respond(0.88)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90004).respond(0.87)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90011).respond(0.86)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90012).respond(0.85)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90013).respond(0.84)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 90014).respond(0.83)

    def test_medicine_models(self):
        request = 'place=parallel&text=лекарство&rearr-factors=market_parallel_reject_medicine_models=1'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'Не лекарство 1'}}},
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'Не лекарство 2'}}},
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'Не лекарство 3'}}},
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'Не лекарство 4'}}},
                            },
                        ]
                    }
                }
            },
        )

        request = 'place=parallel&text=лекарство&show_explicit_content=medicine'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                'market_implicit_model': {
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'Лекарство 1'}}},
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'Лекарство 2'}}},
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'Лекарство 3'}}},
                            },
                            {
                                'title': {'text': {'__hl': {'text': 'Лекарство 4'}}},
                            },
                        ]
                    }
                }
            },
        )


if __name__ == "__main__":
    main()
