#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.global_vendors import GlobalVendor
from core.matcher import Absent, ElementCount
from core.types.categories_stats import VendorCategoryItems
from core.types.popular_vendors import CategoryPopularVendors


def get_brand_picture(brand_id):
    return {152826: 'avatars.mds.yandex.net/picture_nikon/orig'}[brand_id]


def get_pictures_json(brand_id):
    def get_picture_url(index):
        orig_picture = get_brand_picture(brand_id)
        if not orig_picture.endswith('/orig'):
            return orig_picture
        return orig_picture[: -4] + str(index) + 'hq'

    def get_size_json(index):
        sizes = {1: 50, 2: 100, 3: 75, 4: 150, 5: 200, 6: 250, 7: 120, 8: 240, 9: 500}
        size = sizes[index]
        return {'containerHeight': size,
                'containerWidth': size,
                'height': size,
                'width': size,
                'url': get_picture_url(index)}

    return [get_size_json(index) for index in range(1, 9)]


def add_region_to_req(request, region):
    return request + ('&region={}'.format(region) if region else '')


def names_to_obj_list(names_list):
    return [{'name': name} for name in names_list]


def vendor_offers_in_simferopol(vendor):
    return abs(1012 - vendor) % 10


def vendor_offers_in_kiev(vendor):
    return vendor % 10


def vendor_offers_in_spb(vendor):
    return 1015 - vendor


class T(TestCase):

    @classmethod
    def prepare(cls):
        green_nodes = [
            NavigationNode(nid=1000, hid=90401, parent_nid=0),
            NavigationNode(nid=2000, hid=200, parent_nid=1000),
            NavigationNode(nid=2100, hid=210, parent_nid=2000),
            NavigationNode(nid=2200, hid=220, parent_nid=2000),
            NavigationNode(nid=2300, hid=230, parent_nid=2000),
            NavigationNode(nid=3000, hid=300, parent_nid=1000),
            NavigationNode(nid=3100, hid=310, parent_nid=3000),
            NavigationNode(nid=3200, hid=320, parent_nid=3000),
            NavigationNode(nid=3300, hid=330, parent_nid=3000),
            NavigationNode(nid=3400, hid=340, parent_nid=3000),
            NavigationNode(nid=4000, hid=400, parent_nid=1000),
            NavigationNode(nid=4100, hid=410, parent_nid=4000),
            NavigationNode(nid=4200, hid=420, parent_nid=4000),
            NavigationNode(nid=4300, hid=430, parent_nid=4000)
        ]

        cls.index.navigation_trees += [NavigationTree(code='green', nodes=green_nodes)]

        cls.index.global_vendors += [
            GlobalVendor(vendor_id=100500, name='Рога и копыта'),
            GlobalVendor(vendor_id=100501, name='Бренд с одной категорией'),
            GlobalVendor(vendor_id=100502, name='Бренд на синем'),
            GlobalVendor(vendor_id=152826, name='Nikon',
                         site='http://www.nikon.ru', description='nikon desc',
                         picture=get_brand_picture(152826),
                         brandzone_logo='nikon logo'),
            GlobalVendor(vendor_id=1001, name='Legm'),
            GlobalVendor(vendor_id=1002, name='legn'),
            GlobalVendor(vendor_id=1003, name='LEGP'),
            GlobalVendor(vendor_id=1004, name='Lego 123'),
            GlobalVendor(vendor_id=1005, name='012'),
            GlobalVendor(vendor_id=1006, name='111'),
            GlobalVendor(vendor_id=1007, name='ёёё'),
            GlobalVendor(vendor_id=1008, name='112'),
            GlobalVendor(vendor_id=1009, name='Большевик'),
            GlobalVendor(vendor_id=1010, name='~aaa'),
            GlobalVendor(vendor_id=1011, name='^bbb'),
            GlobalVendor(vendor_id=1012, name='*ccc'),
            GlobalVendor(vendor_id=1013, name='красный октябрь'),
            GlobalVendor(vendor_id=1014)
        ]
        cls.index.global_vendors += [
            GlobalVendor(vendor_id=vid, name='blue vendor '+str(vid)) for vid in range(2000, 2020)
        ]

        cls.index.vendor_categories_offers += [
            VendorCategoryItems(vendor_id=100500, hid=210, regional_items={213: 100, 2: 200}),
            VendorCategoryItems(vendor_id=100500, hid=220, regional_items={213: 300, 146: 50}),
            VendorCategoryItems(vendor_id=100501, hid=310, regional_items={213: 100})
        ]

        for vendor in range(1001, 1015):
            cls.index.vendor_categories_offers += [
                VendorCategoryItems(vendor_id=vendor, hid=210, regional_items={213: vendor % 10}),
                VendorCategoryItems(vendor_id=vendor, hid=220, regional_items={213: (1015 - vendor) % 10}),
                VendorCategoryItems(vendor_id=vendor, hid=340,
                                    regional_items={143: vendor_offers_in_kiev(vendor),
                                                    146: vendor_offers_in_simferopol(vendor),
                                                    2: vendor_offers_in_spb(vendor)
                                                    }
                                    )
            ]

        cls.index.vendor_categories_shows += [
            VendorCategoryItems(vendor_id=100500, hid=340, regional_items={143: 0}),  # нужно для регистрации "большого" региона
            VendorCategoryItems(vendor_id=1012, hid=340, regional_items={146: 7000}),
            VendorCategoryItems(vendor_id=1004, hid=340, regional_items={146: 500}),
            VendorCategoryItems(vendor_id=1013, hid=340, regional_items={146: 50}),
            VendorCategoryItems(vendor_id=1014, hid=340, regional_items={2: 5000}),
            VendorCategoryItems(vendor_id=1009, hid=330, regional_items={143: 2500}),
            VendorCategoryItems(vendor_id=1008, hid=320, regional_items={143: 3000}),
        ]

        cls.index.blue_vendor_categories_offers += [
            VendorCategoryItems(vendor_id=100502, hid=310, regional_items={143: 150}),
            VendorCategoryItems(vendor_id=2001, hid=200, regional_items={213: 100}),
            VendorCategoryItems(vendor_id=2003, hid=200, regional_items={213: 200}),
        ]

        cls.index.popular_vendors = [
            CategoryPopularVendors(hid=340, vendors=(1001, 1002, 1003), regions=(143, 146)),
            CategoryPopularVendors(hid=340, vendors=(1004, 1005, 1006), regions=(2,)),
            CategoryPopularVendors(hid=340, vendors=(1003, 1004, 1005), regions=(10000,)),
            CategoryPopularVendors(hid=300, vendors=(1010, 1011, 1012), regions=(2, 143)),
            CategoryPopularVendors(hid=340, vendors=(2005, 2006, 2007), regions=(143,), is_blue=True),
            CategoryPopularVendors(hid=330, vendors=(2007, 2008), regions=(143,), is_blue=True),
            CategoryPopularVendors(hid=340, vendors=(2007,), regions=(146,), is_blue=True),
            CategoryPopularVendors(hid=200, vendors=(2004, 2002), regions=(213,), is_blue=True),
        ]

        # проверка похожих вендоров
        cls.index.global_vendors += [
            GlobalVendor(vendor_id=vid) for vid in range(4000, 4004)
        ]

        cls.index.vendor_categories_offers += [
            # У вендора 4000 в Москве есть офферы в категориях 410 и 420. В Питере есть офферы в категориях 410, 420 и 430
            VendorCategoryItems(vendor_id=4000, hid=410, regional_items={213: 100, 2: 50}),
            VendorCategoryItems(vendor_id=4000, hid=420, regional_items={213: 200, 2: 100}),
            VendorCategoryItems(vendor_id=4000, hid=430, regional_items={2: 200}),

            # У вендора 4001 в Москве есть офферы в категориях 410 и 420. В Питере в них же, и еще в категории 420 в Симферополе
            VendorCategoryItems(vendor_id=4001, hid=410, regional_items={213: 200, 2: 100}),
            VendorCategoryItems(vendor_id=4001, hid=420, regional_items={213: 100, 2: 100, 146: 50}),

            # У вендора 4002 в Москве есть офферы только в категории 410. В Питере есть офферы в 410 и 430, и еще в категории 410 в Симферополе
            VendorCategoryItems(vendor_id=4002, hid=410, regional_items={213: 1000, 2: 300, 146: 500}),
            VendorCategoryItems(vendor_id=4002, hid=430, regional_items={2: 600}),

            # у вендора 4003 есть офферы только в 410 в Cимферополе
            VendorCategoryItems(vendor_id=4003, hid=410, regional_items={146: 100}),
        ]

        cls.index.precalc_brands = True

    def test_small_brand_info(self):
        response = self.cataloger.request_json('GetBrandInfo?id={}'.format(100500))
        self.assertFragmentIn(response, {
            'entity': 'vendor',
            'id': 100500,
            'name': 'Рога и копыта',
            'slug': 'roga-i-kopyta',
            'logo': {
                'entity': 'picture',
                'url': Absent(),
                'thumbnails': Absent()
            },
        })

    def test_big_brand_info(self):
        brand_id = 152826
        response = self.cataloger.request_json('GetBrandInfo?id={}'.format(brand_id))
        self.assertFragmentIn(response, {
            'entity': 'vendor',
            'id': brand_id,
            'name': 'Nikon',
            'slug': 'nikon',
            'website': 'http://www.nikon.ru',
            'description': 'nikon desc',
            'hasArticle': False,
            'logo': {
                'entity': 'picture',
                'url': get_brand_picture(brand_id),
                'thumbnails': get_pictures_json(brand_id)
            }
        })

    def test_categories_count(self):
        region_to_offers = dict()
        region_to_offers[213] = 400
        region_to_offers[2] = 200
        region_to_offers[146] = 50
        region_to_offers[0] = 650
        region_to_offers[10000] = region_to_offers[0]
        for region in (0, 213, 2, 146, 10000):
            response = self.cataloger.request_json(add_region_to_req('GetBrandInfo?id=100500', region))
            self.assertFragmentIn(response, {
                'offersCount': region_to_offers[region],  # вендорные офферы региональны
                'categoriesCount': 2,  # вендорные категории нет,
                'categories': Absent()  # если несколько товарных категорий, то они не показываются
            })

    def test_brand_with_one_category(self):
        for region in (0, 213, 2, 146, 10000):
            response = self.cataloger.request_json(add_region_to_req('GetBrandInfo?id=100501', region))
            self.assertFragmentIn(response, {
                'offersCount': (0 if region in (2, 146) else 100),
                'categoriesCount': 1,
                'categories': [  # если всего одна товарная категория, то она показывается
                    {
                        'entity': 'category',
                        'id': 310,
                        'name': 'tovar category 310'
                    }
                ],
            })

    def test_color_stats(self):
        def gen_req(brand_id, color=None):
            req = 'GetBrandInfo?id={}'.format(brand_id)
            return req + '&rgb={}'.format(color) if color is not None else req

        # 100501 есть в зеленом, и отсутствует в синем, а 100502 наоборот
        for brand_id, color in ((100501, 'blue'), (100502, 'green')):
            response = self.cataloger.request_json(gen_req(brand_id, color))
            self.assertFragmentIn(response, {
                'offersCount': 0,
                'categoriesCount': 0,
            })

        response = self.cataloger.request_json(gen_req(100501))
        self.assertFragmentIn(response, {
            'offersCount': 100,
            'categoriesCount': 1,
        })
        response = self.cataloger.request_json(gen_req(100502, 'blue'))
        self.assertFragmentIn(response, {
            'offersCount': 150,
            'categoriesCount': 1,
        })

    def test_popular_brands_blue_main_page(self):
        ''' Сначала идёт выдача из заданного списка популярных брендов, затем - дополняется остальными вендорами на основании статистики. '''

        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=200, region=213, count=10, blue=True))
        # всего 4 бренда, 2 за счёт приоритета, 2 за счёт статистик
        self.assertFragmentIn(response, {'result': ElementCount(4)})
        self.assertFragmentIn(response, [
            {  # первым идёт 2004, как задано в списке популярных
                'id': 2004,
                'name': 'blue vendor 2004'
            },
            {  # вторым идёт 2002, как задано в списке популярных
                'id': 2002,
                'name': 'blue vendor 2002'
            },
            {  # третьим идёт 2003, т.к. у него статистика больше
                'id': 2003,
                'name': 'blue vendor 2003'
            },
            {  # четвёртым и последним идёт 2001, у него статистика меньше
                'id': 2001,
                'name': 'blue vendor 2001'
            },
        ], preserve_order=True)

    def test_sort_by_name_leaf(self):
        response = self.cataloger.request_json('GetBrandsByLetter?hid=210&rgb=green&format=json')
        self.assertFragmentIn(response, [
            {
                'id': 1012,
                'name': '*ccc',
                'slug': 'ccc'
            },
            {
                'id': 1011,
                'name': '^bbb',
                'slug': 'bbb'
            },
            {
                'id': 1005,
                'name': '012',
                'slug': '012'
            },
            {
                'name': '111'
            },
            {
                'name': '112'
            },
            {
                'name': 'Legm'
            },
            {
                'name': 'legn'
            },
            {
                'name': 'Lego 123'
            },
            {
                'name': 'LEGP'
            },
            {
                'name': 'Большевик'
            },
            {
                'name': 'ёёё'
            },
            {
                'name': 'красный октябрь'
            },
            {
                'name': 'Рога и копыта'
            }
        ], preserve_order=True)
        self.assertFragmentNotIn(response, {'name': 'Бренд с одной категорией'})

    def test_sort_by_name_not_leaf(self):
        response = self.cataloger.request_json('GetBrandsByLetter?rgb=green&format=json')
        order = ('*ccc', '^bbb', '012', '111', '112', 'Legm', 'legn', 'Lego 123', 'LEGP',
                 'Большевик', 'Бренд с одной категорией', 'ёёё', 'красный октябрь', 'Рога и копыта')
        self.assertFragmentIn(response, [{'name': vendor} for vendor in order], preserve_order=True)

    def test_sort_by_popularity_leaf(self):
        def hid_to_selection(hid):
            if hid == 210:  # сортировка по vendor % 10
                return [100500] + range(1009, 1000, -1)
            if hid == 220:  # сортировка по (1015 - vendor) % 10
                [100500, 1006, 1007, 1008, 1009, 1010, 1001, 1002, 1003, 1004]
            return []

        for hid in (210, 220,):
            response = self.cataloger.request_json('GetBrandsByLetter?hid={}&rgb=green&format=json&how=dpop&region=213'.format(hid))
            self.assertFragmentIn(response, [{'id': vendor} for vendor in hid_to_selection(hid)], preserve_order=True)
            # проверка отсутствия вендора без офферов
            if hid == 210:
                self.assertFragmentNotIn(response, {'id': 1010})
            elif hid == 220:
                self.assertFragmentNotIn(response, {'id': 1005})

    def test_sort_by_popularity_not_leaf(self):
        # в 200 суммируются офферы 210 и 220, у всех вендоров 1001-1012 одинаковое значение
        response = self.cataloger.request_json('GetBrandsByLetter?hid=200&rgb=green&format=json&how=dpop&region=213')
        for vendor in range(1001, 1014):
            self.assertFragmentIn(response, [{'id': 100500}, {'id': vendor}], preserve_order=True)
        self.assertFragmentNotIn(response, {'id': 100501})

        # во всем дереве, добавляется популярный бренд 100501
        response = self.cataloger.request_json('GetBrandsByLetter?rgb=green&format=json&how=dpop&region=213')
        # for vendor in range(1001, 1014):
        #    self.assertFragmentIn(response, [{'id': 100500}, {'id': vendor}], preserve_order=True)
        #    self.assertFragmentIn(response, [{'id': 100501}, {'id': vendor}], preserve_order=True)

    def test_page_and_count(self):
        def gen_req(page=None, count=None):
            page_param = '&page={}'.format(page) if page is not None else ''
            count_param = '&count={}'.format(count) if count is not None else ''
            return 'GetBrandsByLetter?rgb=green&format=json' + page_param + count_param

        vendors = [v for k, v in self.cataloger.request_json(gen_req()) if k.endswith('/id')]
        for on_page in (1, 2, 3, 4, 5, 20, 50):
            pages_count = (len(vendors) - 1) / on_page + 1
            for page in range(1, pages_count + 2):
                response = self.cataloger.request_json(gen_req(count=on_page, page=page))
                beg_pos = (min(page, pages_count) - 1) * on_page
                end_pos = beg_pos + on_page - 1
                self.assertFragmentIn(response, [{'id': vendor} for vendor in vendors[beg_pos: end_pos]])

    def _test_groupped_brands(self, region):
        brands_groups = dict()
        # все бренды, начинающиеся на цифры и странные символы
        brands_groups['#'] = ('*ccc', '^bbb', '~aaa', '012', '111', '112')
        brands_groups['L'] = ('Legm', 'legn', 'Lego 123', 'LEGP')
        brands_groups['V'] = ('vendor_1014', 'vendor_4000', 'vendor_4001', 'vendor_4002', 'vendor_4003')
        brands_groups['Б'] = ('Большевик', 'Бренд с одной категорией')
        brands_groups['Ё'] = ('ёёё',)
        brands_groups['К'] = ('красный октябрь',)
        brands_groups['Р'] = ('Рога и копыта',)

        response = self.cataloger.request_json('GetBrandsByLetter?format=json&region={}&group=1'.format(region))
        self.assertFragmentIn(response, {'result': ElementCount(len(brands_groups))})
        for letter in brands_groups.keys():
            self.assertFragmentIn(response, {'letter': letter, 'brands': ElementCount(len(brands_groups[letter]))})
        self.assertFragmentIn(response, [
            {
                'entity': 'letterGroup',
                'letter': letter,
                'brands': names_to_obj_list(brands_groups[letter])
            }
            for letter in ('#', 'L', 'V', 'Б', 'Ё', 'К', 'Р')
        ], preserve_order=True)

    def test_groupped_brands(self):
        '''
        Большинство офферов в Москве, но GetBrandsByLetter смотрит в нулевом регионе.
        Проверка, что между Москвой и Питером нет разницы в списке брендов.
        '''
        self._test_groupped_brands(2)
        self._test_groupped_brands(213)

    def test_brands_by_letter(self):
        def test_letters(letters, brands):
            for letter in letters:
                req = 'GetBrandsByLetter?format=json&letter_code={}'.format(letter)
                if brands is None:
                    with self.assertRaises(RuntimeError):
                        self.cataloger.request_json(req)
                    return
                response = self.cataloger.request_json(req)
                self.assertFragmentIn(response, {'result': ElementCount(len(brands))})
                self.assertFragmentIn(response, names_to_obj_list(brands), preserve_order=True)

        # символ без вендоров
        test_letters((34, 75, 1040), None)
        # странные символы и цифры
        test_letters((35,), ('*ccc', '^bbb', '~aaa', '012', '111', '112'))
        # 76 - L, 108 - l
        test_letters((76, 108), ('Legm', 'legn', 'Lego 123', 'LEGP'))
        # 1041 - Б, 1073 - б
        test_letters((1041, 1073), ('Большевик', 'Бренд с одной категорией'))
        # 1025 - Ё, 1105 - ё
        test_letters((1025, 1105), ('ёёё',))

    def test_sort_by_popularity_with_letter(self):
        response = self.cataloger.request_json('GetBrandsByLetter?hid=210&format=json&how=dpop&letter_code=76')
        self.assertFragmentIn(response, names_to_obj_list(('Lego 123', 'LEGP', 'legn', 'Legm')), preserve_order=True)

    def _gen_popular_brands_request(self, hid, region, count=None, blue=False):
        req = 'GetPopularBrands?hid={}&format=json&region={}'.format(hid, region)
        if count:
            req += '&n={}'.format(count)
        if blue:
            req += '&rgb=blue'
        return req

    def _gen_vendors_offers_json(self, vendors):
        return [{'id': vendor_id, 'offersCount': offers} for vendor_id, offers in vendors]

    def test_popular_brands_green_leaf_category_and_regions(self):
        '''Проверка метода GetPopularBrands для листовой категории в листовом регионе
        '''

        response_143 = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=143, count=5))
        response_146 = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=146, count=5))
        response_2 = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=2, count=5))

        for response in (response_2, response_146, response_143):
            self.assertFragmentIn(response, {'result': ElementCount(5)})

        # В регионах 143 и 146 в категории 210 забиты бренды 1001, 1002, 1003. А в категории 2 бренды 1004, 1005, 1006
        # Они будут показываться в начале, не зависимо от количества показов и офферов
        top_143 = [(1001, 1), (1002, 2), (1003, 3)]
        top_146 = [(1001, 1), (1002, 0), (1003, 9)]
        top_2 = [(1004, 11), (1005, 10), (1006, 9)]

        # Недостающие вендоры выбираются из статистики. В первую очередь сравнивается количество показов в категории,
        # а во вторую количество офферов.

        # В Киеве показов нет ни у одного вендора, а больше всего офферов у вендоров 1009 и 1008
        list_143 = top_143 + [(1009, vendor_offers_in_kiev(1009)), (1008, vendor_offers_in_kiev(1008))]
        self.assertFragmentIn(response_143, self._gen_vendors_offers_json(list_143), preserve_order=True)

        # В Симферополе больше всего показов вендора 1012, а затем у 1004. Теперь сортируется по показам
        list_146 = top_146 + [(1012, vendor_offers_in_simferopol(1012)), (1004, vendor_offers_in_simferopol(1004))]
        self.assertFragmentIn(response_146, self._gen_vendors_offers_json(list_146), preserve_order=True)

        # Проверяем зависимость топ-списка от региона. Показы есть только у вендора 1014
        list_2 = top_2 + [(1014, vendor_offers_in_spb(1014)), (1001, vendor_offers_in_spb(1001))]
        self.assertFragmentIn(response_2, self._gen_vendors_offers_json(list_2), preserve_order=True)

    def _test_popular_brands_in_hid_340_in_big_region(self, region):
        def offers_count(vendor):
            return vendor_offers_in_kiev(vendor) + vendor_offers_in_simferopol(vendor) + vendor_offers_in_spb(vendor)

        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=region, count=10))
        self.assertFragmentIn(response, {'result': ElementCount(10)})
        self.assertFragmentIn(response, [
            {  # В регионе 10000 (вся земля) забиты вендора 1003, 1004, 1005
                'id': 1003,
                'offersCount': offers_count(1003)
            },
            {
                'id': 1004,
                'offersCount': offers_count(1004)
            },
            {
                'id': 1005,
                'offersCount': offers_count(1005)
            },
            {  # далее 3 вендора с показами
                'id': 1012,  # 7000 показов в Симферополе
                'offersCount': offers_count(1012)
            },
            {
                'id': 1014,  # 5000 показов в Питере
                'offersCount': offers_count(1014)
            },
            {
                'id': 1013,  # 50 показов в Симферополе
                'offersCount': offers_count(1013)
            },
            {  # далее по убыванию офферов
                'id': 1006,
                'offersCount': offers_count(1006)  # 21
            },
            {
                'id': 1007,
                'offersCount': offers_count(1007)  # 20
            },
            {
                'id': 1008,
                'offersCount': offers_count(1008)  # 19
            },
            {
                'id': 1009,
                'offersCount': offers_count(1009)  # 18
            }
        ], preserve_order=True)

    def test_popular_brands_green_leaf_category_and_not_leaf_region(self):
        '''Проверка метода GetPopularBrands для листовой категории в не листовом регионе
        '''

        for region in (10000, 10001, 166):  # Земля, Евразия, СНГ
            self._test_popular_brands_in_hid_340_in_big_region(region)

        # TODO:
        # В MARKETKGB-1181 была бага с неправилным выбором забитых вендоров для региона 10000.
        # Ее не чинили, но в этих тестах воспроизвести не удалось. В светлом будущем можно еще постараться.

    def test_popular_brands_green_not_leaf_category(self):
        '''Проверка метода GetPopularBrands для не листовой категории в листовом регионе
        '''

        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=300, region=143, count=8))
        self.assertFragmentIn(response, [
            {  # в начале прибитые вендора 1010, 1011, 1012
                'id': 1010,
                'offersCount': vendor_offers_in_kiev(1010)  # все офферы из подузла 340
            },
            {
                'id': 1011,
                'offersCount': vendor_offers_in_kiev(1011)
            },
            {
                'id': 1012,
                'offersCount': vendor_offers_in_kiev(1012)
            },
            {  # затем с наибольшим количеством показов
                'id': 1008,  # из подузла 320
                'offersCount': vendor_offers_in_kiev(1008)
            },
            {
                'id': 1009,  # из подузла 330
                'offersCount': vendor_offers_in_kiev(1009)
            },
            {  # дальше по уменьшению офферов
                'id': 1007,
                'offersCount': vendor_offers_in_kiev(1007)
            },
            {
                'id': 1006,
                'offersCount': vendor_offers_in_kiev(1006)
            },
            {
                'id': 1005,
                'offersCount': vendor_offers_in_kiev(1005)
            }
        ], preserve_order=True)

    def test_popular_brands_blue(self):
        '''Проверка списка популярных брендов для синего
        '''

        # в начале идут вендора 2005, 2006, 2007
        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=143, count=10, blue=True))
        self.assertFragmentIn(response, {'result': ElementCount(3)})

        # в заданном порядке
        self.assertFragmentIn(response, [
            {
                'id': 2005,
            },
            {
                'id': 2006,
            },
            {
                'id': 2007,
            }
        ], preserve_order=True)

        # другой hid
        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=330, region=143, count=10, blue=True))
        self.assertFragmentIn(response, {'result': ElementCount(2)})
        self.assertFragmentIn(response, [
            {
                'id': 2007,
            },
            {
                'id': 2008,
            }
        ], preserve_order=True)

        # в другом регионе - пусто
        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=2, count=10, blue=True))
        self.assertFragmentIn(response, {'result': ElementCount(0)})

        # синие приоритеты на белую выдачу не влияют (в выдаче только белые вендоры)
        response = self.cataloger.request_json(self._gen_popular_brands_request(hid=340, region=143, count=9, blue=False))
        self.assertFragmentIn(response, [
            {'id': vid} for vid in range(1001, 1009)
        ], preserve_order=False)

    def _similar_brands_req(self, vendor, region):
        return 'GetSimilarBrands?id={}&format=json&region={}'.format(vendor, region)

    def test_similar_brands_213(self):
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4000, region=213))
        # В Москве вендор 4000 имеет офферы в категориях 410 и 420. Вендор 4001 тоже имеет оферы в них обеих,
        # пусть и в другой пропорции. Вендор 4002 имеет офферы только в 410. В списке похожих он должен быть после 4001
        self.assertFragmentIn(response, [{'id': vid} for vid in (4001, 4002)], preserve_order=True)

        # аналогично 4001 похож на 4000
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4001, region=213))
        self.assertFragmentIn(response, [{'id': vid} for vid in (4000, 4002)], preserve_order=True)

        # 4000 и 4001 похожи на 4002, но порядок может быть любой
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4002, region=213))
        self.assertFragmentIn(response, [{'id': vid} for vid in (4000, 4001)])

        # проверка count
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4000, region=213) + '&count=1')
        self.assertFragmentIn(response, {'result': ElementCount(1)})

    def test_similar_brands_2(self):
        '''В Питере сложнее. Вендор 4000 имеет две общие категори как с 4001, так и с 4002. Пересечения разные.
        Но 4000 все же больше похож на 4002, т.к в их пересекающихся категориях у них абсолютно одинаковое соотношение офферов.
        А у 4000 и 4001 соотношения офферов в пересекающихся категориях разнятся.
        '''

        response = self.cataloger.request_json(self._similar_brands_req(vendor=4000, region=2))
        self.assertFragmentIn(response, [{'id': vid} for vid in (4002, 4001)], preserve_order=True)

        # 4001 похож и на 4000, и на 4002. Но на первый похож больше, т.к с ним пересекается в двух категориях, а с 4002 в одной
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4001, region=2))
        self.assertFragmentIn(response, [{'id': vid} for vid in (4000, 4002)], preserve_order=True)

        # 4002 тоже больше похож на 4000, т.к с ним пересекается в двух категориях, а с 4001 в одной
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4002, region=2))
        self.assertFragmentIn(response, [{'id': vid} for vid in (4000, 4001)], preserve_order=True)

    def test_similar_brands_146(self):
        # В Симферополе есть вендора 4001, 4002 и 4003.
        # 4002 и 4003 имеют общую категорию
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4002, region=146))
        self.assertFragmentIn(response, [{'id': 4003}])
        self.assertFragmentIn(response, {'result': ElementCount(1)})

        response = self.cataloger.request_json(self._similar_brands_req(vendor=4003, region=146))
        self.assertFragmentIn(response, [{'id': 4002}])
        self.assertFragmentIn(response, {'result': ElementCount(1)})

        # 4001 не имеет общих категорий ни с кем
        response = self.cataloger.request_json(self._similar_brands_req(vendor=4001, region=146))
        self.assertFragmentIn(response, {'result': ElementCount(0)})

    def test_similar_brands_blue(self):
        # В синем вообще не считаются похожие вендора. У вендоров 2001 и 2003 полностью совпадают
        # синие офферы в Москве, но выдача пустая
        response = self.cataloger.request_json(self._similar_brands_req(vendor=2001, region=213) + '&rgb=blue')
        self.assertFragmentIn(response, {'result': ElementCount(0)})


if __name__ == '__main__':
    main()
