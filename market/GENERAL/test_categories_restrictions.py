#!/usr/bin/env python
# -*- coding: utf-8 -*-

from core.testcase import TestCase, main
from core.types.navigation_info import NavigationNode, NavigationTree
from core.types.categories_restrictions import CategoryRestriction, RegionalRestriction, RegionRestrNote, CategoryRestrNote
from core.matcher import Absent


def gen_tree_request(nid=None, explicit_content=None, region=None):
    nid_param = '&nid={}'.format(nid) if nid is not None else ''
    return gen_request('GetNavigationTree?depth=5{}'.format(nid_param),
                       explicit_content=explicit_content,
                       region=region)


def gen_node_request(nid, explicit_content=None, region=None):
    return gen_request('GetNavigationNode?nid={}'.format(nid),
                       explicit_content=explicit_content,
                       region=region)


def gen_request(request_begin, explicit_content, region):
    request = '{}&format=json'.format(request_begin)
    if explicit_content is not None:
        if isinstance(explicit_content, str):
            value = explicit_content
        else:
            value = ','.join(explicit_content)
        request += '&show_explicit_content={}'.format(value)
    if region is not None:
        request += '&region={}'.format(region)
    return request


class T(TestCase):

    @classmethod
    def prepare(cls):
        nodes = [
            NavigationNode(nid=1000, hid=100, parent_nid=0, short_name='Все товары'),
            NavigationNode(nid=2000, hid=0, parent_nid=1000, short_name='Здоровье'),
            NavigationNode(nid=3000, hid=0, parent_nid=1000, short_name='Досуг и развлечения'),
            NavigationNode(nid=4000, hid=0, parent_nid=1000, short_name='Напитки'),

            NavigationNode(nid=2100, hid=210, parent_nid=2000, short_name='Оптика'),
            NavigationNode(nid=2110, hid=211, parent_nid=2100, short_name='Очки'),
            NavigationNode(nid=2120, hid=212, parent_nid=2100, short_name='Линзы'),

            NavigationNode(nid=2200, hid=220, parent_nid=2000, short_name='Лекарства и БАДы'),
            NavigationNode(nid=2210, hid=221, parent_nid=2200, short_name='Лекарства'),
            NavigationNode(nid=2211, hid=100500, parent_nid=2210, short_name='От простуды'),
            NavigationNode(nid=2212, hid=100501, parent_nid=2210, short_name='От аллергии'),
            NavigationNode(nid=2220, hid=222, parent_nid=2200, short_name='БАДы'),
            NavigationNode(nid=2221, hid=100502, parent_nid=2220, short_name='Для похудания'),

            NavigationNode(nid=3100, hid=0, parent_nid=3000, short_name='Для компании'),
            NavigationNode(nid=3110, hid=311, parent_nid=3100, short_name='Алкоголь'),
            NavigationNode(nid=3200, hid=320, parent_nid=3000, short_name='Товары для взрослых'),
            NavigationNode(nid=3210, hid=321, parent_nid=3200, short_name='Эротическая одежда'),
            NavigationNode(nid=3220, hid=322, parent_nid=3200, short_name='Презервативы'),

            NavigationNode(nid=4100, hid=410, parent_nid=4000, short_name='Энергетики'),
            NavigationNode(nid=4110, hid=411, parent_nid=4000, short_name='Яблочный сок'),
        ]
        cls.index.navigation_trees += [NavigationTree(code='green', nodes=nodes)]

        # в России есть правило для лекарств со всеми подкатегориями
        rest_for_russia = RegionalRestriction([RegionRestrNote(225)])
        medicine_rest = CategoryRestriction(name='medicine',
                                            categories=[CategoryRestrNote(221)],
                                            regional_restrictions=[rest_for_russia])

        # в России есть правило на товары для взрослых, но не для всех подкатегорий
        adult_rest = CategoryRestriction(name='adult',
                                         categories=[CategoryRestrNote(320, include_subtree=False),
                                                     CategoryRestrNote(321)],
                                         regional_restrictions=[rest_for_russia])

        # алкоголь во всей России, кроме одного села
        alcohol_regions_rest = [RegionRestrNote(225, include_subtree=False),
                                RegionRestrNote(3),  # центральный округ с подрегионами
                                RegionRestrNote(17),  # С-З с подрегинами
                                RegionRestrNote(26),  # Крым с подрегионами
                                RegionRestrNote(40, include_subtree=False),  # начало "спуска" до села
                                RegionRestrNote(11095, include_subtree=False),
                                RegionRestrNote(99621, include_subtree=False),
                                RegionRestrNote(168863, include_subtree=False)
                                ]
        alcohol_rest = CategoryRestriction(name='alcohol',
                                           categories=[CategoryRestrNote(311)],
                                           regional_restrictions=[RegionalRestriction(alcohol_regions_rest)])

        # запрет энергетиков в центральном округе
        energy_rest = CategoryRestriction(name='energy_drinks',
                                           categories=[CategoryRestrNote(410)],
                                           regional_restrictions=[RegionalRestriction([RegionRestrNote(3)])])

        cls.index.categories_restrictions += [medicine_rest, adult_rest, alcohol_rest, energy_rest]

        cls.__saved_restrictions = [medicine_rest, adult_rest, alcohol_rest]

    # В файле задаются правила показа категорий для отдельных регионов.
    # При show_explicit_content=REST_NAME это правило будет игнориться.
    # Но только для регионов из файла. Для остальных регионов это правило
    # проверяется всегда

    def test_categories_not_in_rules(self):
        # Оптика, БАДы, Развлечения показываются всегда и везде
        for region in (2, 3, 143, 146, 225, 135312, None):
            for explicit_content in ('medicine', 'adult', 'alcohol', 'aaa', None):
                request = gen_tree_request(explicit_content=explicit_content, region=region)
                response = self.cataloger.request_json(request)
                self.assertFragmentIn(response, {
                    'id': 1000,
                    'category': {
                        'id': 100
                    },
                    'name': 'Все товары',
                    'navnodes': [
                        {
                            'id': 2000,
                            'category': Absent(),
                            'name': 'Здоровье',
                            'navnodes': [
                                {
                                    'id': 2100,
                                    'category': {
                                        'id': 210
                                    },
                                    'name': 'Оптика',
                                    'navnodes': [
                                        {
                                            'id': 2110,
                                            'category': {
                                                'id': 211
                                            },
                                            'name': 'Очки'
                                        },
                                        {
                                            'id': 2120,
                                            'category': {
                                                'id': 212
                                            },
                                            'name': 'Линзы'
                                        }
                                    ]
                                },
                                {
                                    'id': 2200,
                                    'category': {
                                        'id': 220
                                    },
                                    'name': 'Лекарства и БАДы',
                                    'navnodes': [
                                        {
                                            'id': 2220,
                                            'category': {
                                                'id': 222
                                            },
                                            'name': 'БАДы',
                                            'navnodes': [
                                                {
                                                    'id': 2221,
                                                    'category': {
                                                        'id': 100502
                                                    },
                                                    'name': 'Для похудания'
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            'id': 3000,
                            'category': Absent(),
                            'name': 'Досуг и развлечения',
                            'navnodes': [
                                {
                                    'id': 3100,
                                    'category': Absent(),
                                    'name': 'Для компании'
                                }
                            ]
                        }
                    ]
                })

    def test_regions_not_in_list(self):
        # Правило для лекарств указано для России с подрегионами. Для остальных
        # регионов правило не выполняется никогда
        for region in (143, 10001, None):  # Киев, весь мир, не учитывать регион
            request = gen_tree_request(region=region)
            response = self.cataloger.request_json(request)
            self.assertFragmentIn(response, {'id': 2210})

            request = gen_tree_request(explicit_content='medicine', region=region)
            response = self.cataloger.request_json(request)
            self.assertFragmentIn(response, {'id': 2210})

    def test_medicine_for_russia(self):
        # В России лекарства можно показывать только при наличии show_explicit_content=medicine
        for region in (213, 2, 1, 3, 146, 135312, 225):
            request = gen_tree_request(region=region)
            response = self.cataloger.request_json(request)
            self.assertFragmentNotIn(response, {'id': 2210})

            request = gen_tree_request(explicit_content='medicine', region=region)
            response = self.cataloger.request_json(request)
            self.assertFragmentIn(response, {'id': 2210})

    def test_adult_for_russia(self):
        # В России категорию "Товары для взрослых" можно показывать
        # только с show_explicit_content=adult. Подкатегории проверяются отдельно.
        request = gen_tree_request(region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentNotIn(response, {'id': 3200})

        request = gen_tree_request(explicit_content='adult', region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {'id': 3200})

    def test_include_categories_subtree(self):
        # Правило medicine для категории имеет include_subtree=True
        # Подкатегории лекарств без show_explicit_content не показывается
        with self.assertRaises(RuntimeError):
            self.cataloger.request_json(gen_node_request(nid=2211, region=213))
        # c show_explicit_content=medicine показываются
        request = gen_node_request(nid=2211, explicit_content='medicine', region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 2211,
            'category': {
                'id': 100500
            },
            'name': 'От простуды'
        })
        request = gen_node_request(nid=2212, explicit_content='medicine', region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 2212,
            'category': {
                'id': 100501
            },
            'name': 'От аллергии'
        })

    def test_exclude_categories_subtree_1(self):
        # Правило adult для категории имеет include_subtree=False.
        # Подкатегория 3210 не показывается из-за отдельного указания в правиле,
        with self.assertRaises(RuntimeError):
            self.cataloger.request_json(gen_node_request(nid=3210, region=213))
        # c show_explicit_content=adult показывается
        request = gen_node_request(nid=3210, explicit_content='adult', region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 3210,
            'category': {
                'id': 321
            },
            'name': 'Эротическая одежда'
        })

        # а подкатегория 3220 показывается и без show_explicit_content=adult
        response = self.cataloger.request_json(gen_node_request(nid=3220, region=213))
        self.assertFragmentIn(response, {
            'id': 3220,
            'category': {
                'id': 322
            },
            'name': 'Презервативы'
        })

    def test_exclude_categories_subtree_2(self):
        russia = [213, 1, 2, 3, 225, 10174, 17, 146, 121220,
                  977, 26, 168863, 99621, 11095, 40]
        # без show_explicit_content=alcohol алкоголь в России не должен показываться
        # кроме 135312 - у всех его родителей include_subtree == False
        for region in russia:
            with self.assertRaises(RuntimeError):
                self.cataloger.request_json(gen_node_request(nid=3110, region=region))

        # с show_explicit_content=alcohol алкоголь должен показываться
        # во всей России, включая село 135312, на которое ограничение не распространяется
        for region in russia:
            request = gen_node_request(nid=3110, explicit_content='alcohol', region=region)
            response = self.cataloger.request_json(request)
            self.assertFragmentIn(response, {
                'id': 3110,
                'category': {
                    'id': 311
                },
                'name': 'Алкоголь'
            })

    def test_show_explicit_with_two_values(self):
        # Товары для взрослых и алкоголь в Москве не показываются.
        request = gen_tree_request(nid=3000, region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 3000,
            'name': 'Досуг и развлечения',
            'navnodes': [
                {
                    'id': 3100,
                    'name': 'Для компании',
                    'isLeaf': False,  # похоже на багу. Когда откидываются
                                      # скрытые категории, нелистовые узлы
                                      # могут превращатья в листовые
                    'navnodes': Absent()  # тут нет узлов, и похоже этого достаточно
                }
            ]
        })
        # товаров для взрослых нет
        self.assertFragmentNotIn(response, {'id': 3200})

        request = gen_tree_request(nid=3000, explicit_content='adult,alcohol', region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 3000,
            'navnodes': [
                {
                    'id': 3100,
                    'navnodes': [
                        {
                            'id': 3110,
                            'name': 'Алкоголь'
                        }
                    ]
                },
                {
                    'id': 3200,
                    'name': 'Товары для взрослых'
                },
            ]
        })

    def test_reload_restrictions(self):
        # Cейчас энергетики под запретом, поэтому в выдаче только сок
        request = gen_tree_request(nid=4000, region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 4000,
            'name': 'Напитки',
            'navnodes': [
                {
                    'id': 4110,
                    'name': 'Яблочный сок',
                }
            ]
        },
        allow_different_len=False)
        self.assertFragmentNotIn(response, {'id': 4100})  # Нет энергетиков

        # Уберем ограничение на энергетики, обновим данные
        # Тк теперь кеш сбрасывается, старые ограничения из него не придут
        self.index.change_restrictions(new_restrictions=self.__saved_restrictions)
        self.cataloger.request_json('ReloadData?format=json')

        request = gen_tree_request(nid=4000, region=213)
        response = self.cataloger.request_json(request)
        self.assertFragmentIn(response, {
            'id': 4000,
            'name': 'Напитки',
            'navnodes': [
                {
                    'id': 4110,
                    'name': 'Яблочный сок',
                },
                {
                    'id': 4100,
                    'name': 'Энергетики',
                },
            ]
        },
        allow_different_len=False)

if __name__ == '__main__':
    main()
