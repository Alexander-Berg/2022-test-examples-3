#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.types.autogen import Const

from core.matcher import Regex
from core.logs import ErrorCodes


class T(TestCase):
    @classmethod
    def prepare(cls):
        # Numeration rules:
        # - hid = {101, 102}
        # - glparam = {201, 202}
        # - glvalue = {301, 302, 303, 401, 402}

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.offers = [
            Offer(
                hid=101, title="offer1", glparams=[GLParam(param_id=201, value=302), GLParam(param_id=202, value=401)]
            ),
            Offer(hid=102, title="offer2", glparams=[GLParam(param_id=203, value=1)]),
            Offer(hid=101, title="offer3"),
        ]
        cls.index.gltypes = [
            GLType(param_id=201, hid=101, gltype=GLType.ENUM, values=[301, 302, 303]),
            GLType(param_id=202, hid=101, gltype=GLType.ENUM, values=[401, 402]),
            GLType(param_id=203, hid=102, gltype=GLType.BOOL),
        ]

    def __init__(self, name):
        TestCase.__init__(self, name)
        self.response = None

    def sendRequest(self, query):
        self.response = self.report.request_json(query)

    def assertOfferCountIs(self, count):
        self.assertFragmentIn(self.response, {"search": {"totalOffers": count}})

    def assertResponseContainsOffer(self, offer_name):
        self.assertFragmentIn(self.response, {"entity": "offer", "titles": {"raw": offer_name}})

    def assertUniversalFilterRequestsEqual(self, format, place, filters, request, offersCount=None):
        """
        Проверка одного запроса для универсального фильтра.
        Сравнивает на равенство два запроса: базовый запрос (вида имя=значение) и через универсальный фильтр (filter=имя:значение)
        """
        base = ''
        universal = ''
        for filter, value in filters.items():
            try:
                int(filter)
                baseFormat = 'glfilter={0}:{1}&'
            except ValueError:
                baseFormat = '{0}={1}&'

            base += baseFormat.format(filter, value)
            universal += 'filter={0}:{1}&'.format(filter, value)

        requestBase = request + '&{0}place={1}'.format(base, place)
        requestUniversal = request + '&{0}place={1}'.format(universal, place)

        if format == 'json':
            self.assertEqualJsonResponses(requestBase, requestUniversal, count_offers=offersCount)
        elif format == 'xml':
            self.assertEqualXmlResponses(requestBase, requestUniversal, count_offers=offersCount)

    def assertUniversalBooleanFilter(self, format, place, filter, request, offersCountTrue=None, offersCountFalse=None):
        """
        Проверка одного булевого универсального фильтра.
        Проверяет равенство выдачи для истинного и ложного значения
        """
        self.assertUniversalFilterRequestsEqual(
            format=format, place=place, filters={filter: 1}, request=request, offersCount=offersCountTrue
        )
        self.assertUniversalFilterRequestsEqual(
            format=format, place=place, filters={filter: 0}, request=request, offersCount=offersCountFalse
        )

    def test_valid_glfilter(self):
        self.sendRequest("place=prime&hid=101&glfilter=201:302")
        self.assertOfferCountIs(1)
        self.assertResponseContainsOffer("offer1")

    def test_multiple_valid_glfilters(self):
        self.sendRequest("place=prime&hid=101&glfilter=201:302&glfilter=202:401,402")
        self.assertOfferCountIs(1)
        self.assertResponseContainsOffer("offer1")

    def test_multiple_valid_glfilters_and_no_offer(self):
        self.sendRequest("place=prime&hid=101&glfilter=201:302&glfilter=202:402")
        self.assertOfferCountIs(0)

    def test_invalid_glfilter(self):
        self.sendRequest("place=prime&hid=101&glfilter=201:302;999:000")
        self.assertOfferCountIs(1)
        self.assertResponseContainsOffer("offer1")
        # 1 раз в основном запросе и 1 раз в подзапросе за фильтрами
        self.error_log.expect(
            "GlFactory returned null (wrong parameter or value ID?), glfilters: 201:302;999:000, offending filter: 999:000"
        ).times(2)

    def test_glfilter_with_empty_param_value(self):
        self.sendRequest("place=prime&hid=101&glfilter=201")
        self.assertOfferCountIs(2)
        self.error_log.expect("Parameter name or value is empty, glfilters: 201, offending filter: 201").once()

    def test_glfilter_with_invalid_param_name(self):
        self.sendRequest("place=prime&hid=101&glfilter=QQQ:302")
        self.assertOfferCountIs(2)
        self.error_log.expect("Parameter ID is not integer, glfilters: QQQ:302, offending filter: QQQ:302").once()

    def test_glfilter_with_invalid_param_value(self):
        self.sendRequest("place=prime&hid=101&glfilter=201:QQQ")
        self.assertOfferCountIs(2)
        self.error_log.expect(
            "GlFactory returned null (wrong parameter or value ID?), glfilters: 201:QQQ, offending filter: 201:QQQ"
        ).once()

    def test_invalid_glfilter_log_message(self):
        self.report.request_json('place=prime&text=testPaging&how=aprice&glfilter=201:456')
        self.error_log.expect('Error in glfilters syntax:').once()

    def test_ignore_exceptions_invalid_glfilter(self):
        self.report.request_json('place=prime&text=testPaging&how=aprice&glfilter=filter:subsidies:1')
        self.error_log.expect('Parameter ID is not integer').once()

    @classmethod
    def prepare_universal_filter_gl(cls):
        """
        Подготовка данных для проверки универсального фильтра.
        Универсальный параметр filter может принимать как glfilter, так и другие фильтры в формате FilterName:Value.
        Если FilterName является числом, то это glfilter.
        """

        cls.index.offers += [
            Offer(
                hid=151,
                title="u_offer151_1",
                glparams=[GLParam(param_id=601, value=302), GLParam(param_id=602, value=401)],
            ),
            Offer(hid=151, title="u_offer151_2"),
        ]
        cls.index.gltypes += [
            GLType(param_id=601, hid=151, gltype=GLType.ENUM, values=[301, 302, 303]),
            GLType(param_id=602, hid=151, gltype=GLType.ENUM, values=[401, 402]),
        ]

    def test_universal_filter_gl(self):
        """
        Проверка работоспособности glfilter, полученного через cgi-параметр filter
        Проверяется поштучная передача фильтра и комбинация фильтров
        """

        self.assertUniversalFilterRequestsEqual(
            format='json', place='prime', filters={'601': 302}, request='hid=151', offersCount=1
        )

        self.assertUniversalFilterRequestsEqual(
            format='json', place='prime', filters={'601': 302, '602': 401}, request='hid=151', offersCount=1
        )

    def test_universal_filter_duplicate(self):
        """
        Проверка вывода ошибки при дублировании параметров в filter и простом запросе.
        """
        errorMessage = 'Do not use filter and non-filter params with same name in one request: {}'

        def responseTemplate(code):
            return {'error': {'description': errorMessage.format(code)}}

        # Проверяем glfilter с разными значениями
        response = self.report.request_json('place=prime&filter=601:302&glfilter=602:401&hid=151', strict=False)
        self.assertFragmentIn(response, responseTemplate('glfilter'))
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=errorMessage.format('glfilter'))

        # Проверяем glfilter с одинаковыми значениями
        response = self.report.request_json('place=prime&filter=601:302&glfilter=602:302&hid=151', strict=False)
        self.assertFragmentIn(response, responseTemplate('glfilter'))
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=errorMessage.format('glfilter'))

        # Проверяем adult
        response = self.report.request_json('place=prime&filter=adult:1&adult=0&hid=151', strict=False)
        self.assertFragmentIn(response, responseTemplate('adult'))
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=errorMessage.format('adult'))

        # Проверяем несуществующий параметр (для программы нет различия в названиях параметра)
        response = self.report.request_json(
            'place=prime&filter=non-existing-filter:some_value&non-existing-filter=another_value&hid=151', strict=False
        )
        self.assertFragmentIn(response, responseTemplate('non-existing-filter'))
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=errorMessage.format('non-existing-filter'))

    def test_universal_filter_wrong_format(self):
        """
        Проверка вывода ошибки при ошибке формата параметров в универсальном фильтре
        """
        wrongNameMessage = 'Empty filter name: {}. Valid filter format is filter=name:value'
        wrongValueMessage = 'Missed filter value: {}. Valid filter format is filter=name:value'

        def responseTemplate(code):
            return {'error': {'description': code}}

        # Проверяем пустое имя фильтра
        response = self.report.request_json('place=prime&filter=:302', strict=False)
        self.assertFragmentIn(response, responseTemplate(wrongNameMessage.format(':302')))
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=wrongNameMessage.format(':302'))

        # Проверяем отсутствие символа ':' в фильтре
        response = self.report.request_json('place=prime&filter=name', strict=False)
        self.assertFragmentIn(response, responseTemplate(wrongValueMessage.format('name')))
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=wrongValueMessage.format('name'))

    def test_incorrect_value_in_param_defined_in_enum(self):
        wrongValueMessageRegex = Regex(
            ".*Key 'dprice' not found in enum NMarketReport::EDefaultHowOnGeo. Valid options are: 'model_card', 'search'.*"
        )

        _ = self.report.request_json(
            'place=geo&default-how-on-geo=dprice&show-book-now-only=1&geo-location=37.15,55.15&geo-sort-gran=2&numdoc=20&hyperid=301&rids=0&pp=18',
            strict=False,
        )
        self.error_log.expect(code=ErrorCodes.USER_SEARCH_COMMON, message=wrongValueMessageRegex)

    @classmethod
    def prepare_universal_filter_onstock(cls):
        """
        Подготовка данных для проверки фильтра "В продаже", полученного через универсальный фильтр.
        Имеется две модели, к одной из которых привязано предложение.
        """

        cls.index.models += [
            Model(hyperid=701, hid=153),  # with stock
            Model(hyperid=702, hid=153),  # w/o stock
        ]
        cls.index.offers += [Offer(hid=153, hyperid=701, title="u_offer153_1")]

    def test_universal_filter_onstock(self):
        """
        Проверка фильтра "В продаже".
        """
        self.assertUniversalBooleanFilter(
            format='json', place='prime', filter='onstock', request='hid=153', offersCountTrue=1, offersCountFalse=1
        )

    @classmethod
    def prepare_universal_filter_price(cls):
        """
        Подготовка данных для проверки фильтра "Цена", полученного через универсальный фильтр.
        Имеется два офера с разной ценой.
        """

        cls.index.offers += [
            Offer(hid=154, title="u_offer154_1", price=100),
            Offer(hid=154, title="u_offer154_2", price=50),
        ]

    def test_universal_filter_price(self):
        """
        Проверка фильтра "Цена".
        """

        # for place=prime
        self.assertUniversalFilterRequestsEqual(
            format='json',
            place='prime',
            filters={'mcpricefrom': 100, 'mcpriceto': 150},
            request='hid=154',
            offersCount=1,
        )

    @classmethod
    def prepare_universal_filter_adult(cls):
        """
        Подготовка данных для проверки фильтра "18+", полученного через универсальный фильтр.
        Имеется два офера: без флага и с ним.
        """

        cls.index.offers += [
            Offer(hid=155, title="u_offer155_1", adult=False),
            Offer(hid=155, title="u_offer155_2", adult=True, hyperid=1255),
        ]

    def test_universal_filter_adult(self):
        """
        Проверка фильтра "18+".
        """

        # for place=prime
        self.assertUniversalBooleanFilter(
            format='json', place='prime', filter='adult', request='hid=155', offersCountTrue=2, offersCountFalse=1
        )

    @classmethod
    def prepare_universal_filter_manufacturer_warranty(cls):
        """
        Подготовка данных для проверки фильтра "Гарантия производителя".
        """

        cls.index.offers += [
            Offer(
                hid=156, title="u_offer156_1", hyperid=1256, fesh=101, manufacturer_warranty=True, pickup_buckets=[5101]
            ),
            Offer(
                hid=156,
                title="u_offer156_2",
                hyperid=1256,
                fesh=102,
                manufacturer_warranty=False,
                pickup_buckets=[5102],
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=231, fesh=101),
            Outlet(point_id=232, fesh=102),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5101,
                fesh=101,
                carriers=[99],
                options=[PickupOption(outlet_id=231)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5102,
                fesh=102,
                carriers=[99],
                options=[PickupOption(outlet_id=232)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_universal_filter_manufacturer_warranty(self):
        """
        Проверка фильтра "Гарантия производителя".
        """

        # for place=prime
        self.assertUniversalBooleanFilter(
            format='json',
            place='prime',
            filter='manufacturer_warranty',
            request='hid=156',
            offersCountTrue=1,
            offersCountFalse=2,
        )

        # for place=productoffers
        self.assertUniversalBooleanFilter(
            format='json',
            place='productoffers',
            filter='manufacturer_warranty',
            request='hyperid=1256',
            offersCountTrue=1,
            offersCountFalse=2,
        )

        # for place=geo
        self.assertUniversalBooleanFilter(
            format='json',
            place='geo',
            filter='manufacturer_warranty',
            request='hyperid=1256&bsformat=2',
            offersCountTrue=1,
            offersCountFalse=2,
        )

    @classmethod
    def prepare_filters_output(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            ),
            Region(
                rid=10650,
                name='Брянская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=191, name='Брянск'),
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=900,
                name="Category with gl params #900",
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=901, name="Category with gl params #901", output_type=HyperCategoryType.GURU)
                ],
            ),
        ]
        cls.index.gltypes = [
            GLType(param_id=221, hid=900, gltype=GLType.ENUM, values=[301, 302, 303], vendor=True, cluster_filter=True),
            GLType(param_id=222, hid=900, gltype=GLType.ENUM, values=[400, 402], cluster_filter=True),
            GLType(param_id=223, hid=900, gltype=GLType.ENUM, values=[400, 402], vendor=True, cluster_filter=True),
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=901, values=[1, 2]),
        ]

        cls.index.shops += [
            Shop(
                fesh=101,
                cpa=Shop.CPA_REAL,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                priority_region=213,
                regions=[213],
            ),
            Shop(
                fesh=102,
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                priority_region=213,
                regions=[213],
            ),
            Shop(
                fesh=103,
                cpa=Shop.CPA_NO,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                priority_region=191,
                regions=[191],
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=900,
                title="offer_21",
                hyperid=4026,
                fesh=101,
                glparams=[
                    GLParam(param_id=221, value=302, gltype=GLType.ENUM),
                    GLParam(param_id=222, value=400, gltype=GLType.ENUM),
                ],
                price=1000,
                cpa=Offer.CPA_REAL,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
                manufacturer_warranty=0,
                pickup_buckets=[5101],
            ),
            Offer(
                hid=900,
                title="offer_22",
                hyperid=4026,
                fesh=102,
                glparams=[GLParam(param_id=221, value=303, gltype=GLType.ENUM)],
                price=500,
                delivery_options=[DeliveryOption(day_from=0, day_to=0, price=0)],
                manufacturer_warranty=1,
                discount=50,
                pickup_buckets=[5102],
            ),
            Offer(
                hid=900,
                title="offer_23",
                hyperid=4026,
                fesh=102,
                glparams=[GLParam(param_id=221, value=301, gltype=GLType.ENUM)],
                price=600,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
                pickup_buckets=[5102],
            ),
            Offer(
                hid=900,
                title="offer_24",
                hyperid=4026,
                fesh=101,
                glparams=[GLParam(param_id=222, value=402, gltype=GLType.ENUM)],
                price=1000,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
                pickup_buckets=[5101],
            ),
            Offer(
                hid=900,
                title="offer_24",
                hyperid=4026,
                fesh=101,
                glparams=[GLParam(param_id=223, value=402, gltype=GLType.ENUM)],
                price=1000,
                adult=1,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
                pickup_buckets=[5101],
            ),
            Offer(
                hid=900,
                title="offer_adult",
                hyperid=4026,
                fesh=101,
                glparams=[GLParam(param_id=223, value=400, gltype=GLType.ENUM)],
                price=1000,
                adult=1,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
                pickup_buckets=[5101],
            ),
            Offer(
                hid=900,
                title="offer_bryansk",
                hyperid=4026,
                fesh=103,
                glparams=[GLParam(param_id=223, value=402, gltype=GLType.ENUM)],
                price=1000,
                adult=1,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
            ),
            Offer(
                hid=900,
                title="offer_bryansk",
                hyperid=4026,
                fesh=103,
                glparams=[GLParam(param_id=223, value=400, gltype=GLType.ENUM)],
                price=1000,
                adult=1,
                delivery_options=[DeliveryOption(day_from=1, day_to=2, price=100)],
            ),
        ]

    def test_filter_outputs(self):
        """
        Заводим 2 гл фильтра, вешаем их на офферы таким образом, что у оффера 1 магазина нет одного из этих фильтров
        Проверяем, что все фильтры есть в выдаче для всех офферов по модели
        Проверяем, что все фильтры есть в выдаче для офферов с разными пользовательскими фильтрами
        """

        def check_no_filter_id(response, filter_id):
            self.assertFragmentNotIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {"id": filter_id},
                    ],
                },
            )

        def check_filters(query):
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {"id": "221"},
                        {"id": "222"},
                    ],
                },
            )
            # test no adult offer & another rids glparam
            check_no_filter_id(response, "223")

        for hid in ["&hid=900", "&hid_by_hyper_id=1"]:
            # без фильтра
            check_filters('place=productoffers&hyperid=4026{}&rids=213'.format(hid))

            # по магазину
            check_filters('place=productoffers&hyperid=4026{}&fesh=102&rids=213'.format(hid))

            # по максимальной цене
            check_filters('place=productoffers&hyperid=4026{}&mcpriceto=900&rids=213'.format(hid))

            # по минимальной цене
            check_filters('place=productoffers&hyperid=4026{}&qrfrom=5&rids=213'.format(hid))

            # cpa (ослабление фильтра cpa работает только при отключенном market_no_cpc_mode_if_cpa_real=0)
            check_filters(
                'place=productoffers&hyperid=4026{}&cpa=real&rids=213&rearr-factors=market_no_cpc_mode_if_cpa_real=0'.format(
                    hid
                )
            )

            # free-delivery
            check_filters('place=productoffers&hyperid=4026{}&free-delivery=1&rids=213'.format(hid))

            # delivery-interval
            check_filters('place=productoffers&hyperid=4026{}&free-delivery=1&rids=213&delivery_interval=1'.format(hid))

            # manufacturer_warranty
            check_filters(
                'place=productoffers&hyperid=4026{}&free-delivery=1&rids=213&manufacturer_warranty=1'.format(hid)
            )

            # filter-discount-only
            check_filters(
                'place=productoffers&hyperid=4026{}&free-delivery=1&rids=213&filter-discount-only=1'.format(hid)
            )

            # по гл-параметру
            check_filters('place=productoffers&hyperid=4026{}&fesh=102&rids=213&glfilter=221:303'.format(hid))

            # test rids filter -> just filters from offers in selected region. Check filters for adult offers.
            response = self.report.request_json('place=productoffers&hyperid=4026{}&rids=191&adult=1'.format(hid))
            self.assertFragmentIn(
                response,
                {
                    "search": {},
                    "filters": [
                        {"id": "223"},
                    ],
                },
            )

            check_no_filter_id(response, "221")
            check_no_filter_id(response, "222")


if __name__ == '__main__':
    main()
