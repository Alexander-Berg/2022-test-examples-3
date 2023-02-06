#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, GLValue, Model, Offer, VCluster
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_format(cls):
        """
        Создаем один оффер с одним и тем же размером из трех размерных сеток -- итого 3 параметра
        Описываем размеры и размерные сетки
        """
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.gltypes += [
            GLType(
                param_id=201,
                hid=101,
                gltype=GLType.ENUM,
                subtype='size',
                cluster_filter=True,
                unit_param_id=202,
                name='size',
                values=[
                    GLValue(value_id=1, text='32', unit_value_id=2),
                    GLValue(value_id=2, text='34', unit_value_id=2, filter_value_red=False),
                    GLValue(value_id=3, text='L', unit_value_id=1),
                    GLValue(value_id=4, text='XL', unit_value_id=1),
                    GLValue(value_id=5, text='Tumba', unit_value_id=3, filter_value=False),
                    GLValue(value_id=6, text='Yumba', unit_value_id=3, filter_value=False),
                    GLValue(value_id=7, text='9', unit_value_id=4),
                    GLValue(value_id=8, text='9.5', unit_value_id=4),
                ],
            ),
            GLType(
                param_id=202,
                hid=101,
                gltype=GLType.ENUM,
                name='size_units',
                positionless=True,
                values=[
                    GLValue(value_id=1, text='INT', filter_value_red=False),
                    GLValue(value_id=2, text='RU', default=True),
                    GLValue(value_id=3, text='African'),
                    GLValue(value_id=4, text='US', default_red=True),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='boots',
                hid=101,
                vclusterid=1000000001,
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=201, value=4),
                    GLParam(param_id=201, value=6),
                ],
            ),
        ]

        cls.index.vclusters += [VCluster(title='boots', hid=101, vclusterid=1000000001)]

    def test_format_json(self):
        """
        Фиксируем json-формат глобальных и локальных фильтров
        Должен быть такой же, как и при выдаче старых размеров, кроме:
        - в глобальных параметрах отображаются только параметры с флажком filter_value="true" (gl_mbo.pbuf.sn)
        - в локальных параметрах отображаются все параметры (здесь: из сетки African)
        """
        expected_global_filters = {
            'search': {},
            'filters': [
                {
                    'id': '201',
                    'type': 'enum',
                    'name': 'size',
                    'subType': 'size',
                    'kind': 2,
                    'position': 1,
                    'noffers': 1,
                    'defaultUnit': 'RU',
                    'units': [
                        {
                            'values': [{'initialFound': 1, 'unit': 'INT', 'found': 1, 'value': "XL", 'id': "4"}],
                            'unitId': 'INT',
                            'id': '1',
                        },
                        {
                            'values': [{'initialFound': 1, 'unit': 'RU', 'found': 1, 'value': '34', 'id': '2'}],
                            'unitId': 'RU',
                            'id': '2',
                        },
                    ],
                }
            ],
        }

        unexpected_unit_filters = {'filters': [{'id': '202'}]}

        expected_product_filters = {
            'entity': 'product',
            'filters': [
                {
                    'id': '201',
                    'type': 'enum',
                    'name': 'size',
                    'subType': 'size',
                    'kind': 2,
                    'position': 1,
                    'noffers': 1,
                    'defaultUnit': 'RU',
                    'units': [
                        {
                            'values': [{'initialFound': 1, 'unit': 'INT', 'found': 1, 'value': "XL", 'id': "4"}],
                            'unitId': 'INT',
                            'id': '1',
                        },
                        {
                            'values': [{'initialFound': 1, 'unit': 'RU', 'found': 1, 'value': '34', 'id': '2'}],
                            'unitId': 'RU',
                            'id': '2',
                        },
                    ],
                }
            ],
        }

        expected_offer_filters = {
            'entity': 'offer',
            'filters': [
                {
                    'id': '201',
                    'type': 'enum',
                    'name': 'size',
                    'subType': 'size',
                    'kind': 2,
                    'position': 1,
                    'noffers': 1,
                    'defaultUnit': 'RU',
                    'units': [
                        {
                            'values': [{'initialFound': 1, 'unit': 'African', 'found': 1, 'value': 'Yumba', 'id': '6'}],
                            'unitId': 'African',
                            'id': '3',
                        },
                        {
                            'values': [{'initialFound': 1, 'unit': 'INT', 'found': 1, 'value': 'XL', 'id': '4'}],
                            'unitId': 'INT',
                            'id': '1',
                        },
                        {
                            'values': [{'initialFound': 1, 'unit': 'RU', 'found': 1, 'value': '34', 'id': '2'}],
                            'unitId': 'RU',
                            'id': '2',
                        },
                    ],
                }
            ],
        }

        response = self.report.request_json('place=prime&text=boots&hid=101')
        self.assertFragmentIn(response, expected_global_filters)
        self.assertFragmentIn(response, expected_offer_filters, allow_different_len=False)
        self.assertFragmentIn(response, expected_product_filters, allow_different_len=False)

        # проверяем, что параметр сетки не отдается фронту
        self.assertFragmentNotIn(response, unexpected_unit_filters)

        response = self.report.request_json('place=productoffers&hid=101&hyperid=1000000001')
        self.assertFragmentIn(response, expected_global_filters)
        self.assertFragmentIn(response, expected_offer_filters, allow_different_len=False)

        # проверяем, что параметр сетки не отдается фронту
        self.assertFragmentNotIn(response, unexpected_unit_filters)

    def test_format_xml(self):
        """
        Фиксируем xml-формат глобальных и локальных фильтров
        Должен быть такой же, как и при выдаче старых размеров, кроме:
        - в глобальных параметрах отображаются только параметры с флажком filter_value="true" (gl_mbo.pbuf.sn)
        - в локальных параметрах отображаются все параметры (здесь: из сетки African)

        Важно! Для place=visual на текущий момент нельзя протестировать локальные параметры кластера (!), т.к. они
         опираются на региональные статистики по фильтрам, которые не реализованы в лайте и не будут, т.к. place=visual
         умирает. Лишний параметр там появиться не может, т.к. там рисуется то, что есть в статистике.
        """
        expected_global_filters = '''
            <gl_filters>
                <filter default_unit="RU" id="201" name="size" noffers="1" position="1" sub_type="size" type="enum">
                    <value found="1" id="2" initial-found="1" unit="RU" value="34"/>
                    <value found="1" id="4" initial-found="1" unit="INT" value="XL"/>
                </filter>
            </gl_filters>
        '''

        _ = '<filter id="202"/>'

        _ = '''
            <offer>
                <gl_filters>
                    <filter default_unit="RU" id="201" name="size" noffers="1" position="1" sub_type="size" type="enum">
                        <value found="1" id="2" initial-found="1" unit="RU" value="34"/>
                        <value found="1" id="4" initial-found="1" unit="INT" value="XL"/>
                        <value found="1" id="6" initial-found="1" unit="African" value="Yumba"/>
                    </filter>
                </gl_filters>
            </offer>
        '''
        requests = ['place=modelinfo&hid=101&hyperid=1000000001&rids=0']

        for request in requests:
            response = self.report.request_xml(request)
            self.assertFragmentIn(response, expected_global_filters, allow_different_len=False)

        self.error_log.ignore(
            "can not parse yandexmarket cookie's shows on page(bad lexical cast: source type value could not be interpreted as target)"
        )

    @classmethod
    def prepare_value_order(cls):
        """
        Создаем гуру-лайт енум параметр, в значениях указываем порядок их отображения на фронте
         (диапазон может быть не непрерывным)
        Создаем другой гуру-лайт енум параметр, в значениях которого указываем порядок, но в последнем значении "забываем"
        Создаем сетки: интернациональную, римскую, текстовую
        Создаем оффер с двумя параметрами, описывающими его размер
        Создаем оффер с параметром из текстовой сетки
        """
        cls.index.gltypes += [
            GLType(
                param_id=205,
                hid=102,
                gltype=GLType.ENUM,
                subtype='size',
                cluster_filter=True,
                unit_param_id=206,
                name='size',
                values=[
                    GLValue(value_id=5, text='XS', unit_value_id=1, position=2),
                    GLValue(value_id=6, text='XL', unit_value_id=1, position=6),
                    GLValue(value_id=7, text='L', unit_value_id=1, position=4),
                    GLValue(value_id=8, text='M', unit_value_id=1, position=3),
                    GLValue(value_id=9, text='I', unit_value_id=2, position=1),
                    GLValue(value_id=10, text='IC', unit_value_id=2, position=99),
                    GLValue(value_id=11, text='V', unit_value_id=2, position=5),
                    GLValue(value_id=12, text='C', unit_value_id=2, position=100),
                ],
            ),
            GLType(
                param_id=207,
                hid=102,
                gltype=GLType.ENUM,
                subtype='size',
                cluster_filter=True,
                unit_param_id=206,
                name='text-size',
                values=[
                    GLValue(value_id=1, text='aaa', unit_value_id=3, position=4),
                    GLValue(value_id=2, text='bbb', unit_value_id=3, position=3),
                    GLValue(value_id=3, text='ccc', unit_value_id=3, position=2),
                    GLValue(value_id=4, text='ddd', unit_value_id=3, position=None),  # position is absent
                ],
            ),
            GLType(
                param_id=206,
                hid=102,
                gltype=GLType.ENUM,
                name='size_units',
                position=None,
                values=[
                    GLValue(value_id=1, text='INT'),
                    GLValue(value_id=2, text='ROMAN'),
                    GLValue(value_id=3, text='TEXT'),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='shoes',
                hid=102,
                hyperid=601,
                glparams=[
                    GLParam(param_id=205, value=5),
                    GLParam(param_id=205, value=6),
                    GLParam(param_id=205, value=7),
                    GLParam(param_id=205, value=8),
                    GLParam(param_id=205, value=9),
                    GLParam(param_id=205, value=10),
                    GLParam(param_id=205, value=11),
                    GLParam(param_id=205, value=12),
                    GLParam(param_id=206, value=1),
                    GLParam(param_id=206, value=2),
                    GLParam(param_id=206, value=3),
                ],
            ),
            Offer(
                title='papers',
                hid=102,
                hyperid=601,
                glparams=[
                    GLParam(param_id=206, value=1),
                    GLParam(param_id=207, value=1),
                    GLParam(param_id=207, value=2),
                    GLParam(param_id=207, value=3),
                    GLParam(param_id=207, value=4),
                ],
            ),
        ]

        cls.index.models += [Model(title='shoes', hid=102, hyperid=601)]

    def test_value_order(self):
        """
        Проверяем порядок следования значений параметров:
        он должен совпадать с порядком по-возрастанию позиций значений параметра (см. выше)
        Одного запроса в прайм достаточно, т.к. логика в репорте работает одна и та же
        """
        response = self.report.request_json('place=prime&text=shoes&hid=102')
        self.assertFragmentIn(
            response,
            {
                'units': [
                    {
                        'values': [
                            {'unit': 'INT', 'value': 'XS', 'id': '5'},
                            {'unit': 'INT', 'value': 'M', 'id': '8'},
                            {'unit': 'INT', 'value': 'L', 'id': '7'},
                            {'unit': 'INT', 'value': 'XL', 'id': '6'},
                        ],
                        'unitId': 'INT',
                        'id': '1',
                    },
                    {
                        'values': [
                            {'unit': 'ROMAN', 'value': 'I', 'id': '9'},
                            {'unit': 'ROMAN', 'value': 'V', 'id': '11'},
                            {'unit': 'ROMAN', 'value': 'IC', 'id': '10'},
                            {'unit': 'ROMAN', 'value': 'C', 'id': '12'},
                        ],
                        'unitId': 'ROMAN',
                        'id': '2',
                    },
                ]
            },
            preserve_order=True,
        )

    def test_value_without_position_fallback(self):
        """
        Проверяем порядок следования значений параметров:
        он должен быть в алфавитном порядке, т.к. у значения ddd нет параметра position
        """
        response = self.report.request_json('place=prime&text=papers&hid=102')
        self.assertFragmentIn(
            response,
            {
                'units': [
                    {
                        'values': [
                            {'unit': 'TEXT', 'value': 'aaa', 'id': '1'},
                            {'unit': 'TEXT', 'value': 'bbb', 'id': '2'},
                            {'unit': 'TEXT', 'value': 'ccc', 'id': '3'},
                            {'unit': 'TEXT', 'value': 'ddd', 'id': '4'},
                        ],
                        'unitId': 'TEXT',
                        'id': '3',
                    }
                ]
            },
            preserve_order=True,
        )

    def test_value_without_position_fallback_popular_filters_exp(self):
        """
        Проверяем, что эксперимент  market_popular_gl_filters_on_search=1 не сломал размеры (то же, что test_value_without_position_fallback)
        """
        response = self.report.request_json(
            'place=prime&text=papers&hid=102&rearr-factors=market_popular_gl_filters_on_search=1'
        )
        self.assertFragmentIn(
            response,
            {
                'units': [
                    {
                        'values': [
                            {'unit': 'TEXT', 'value': 'aaa', 'id': '1'},
                            {'unit': 'TEXT', 'value': 'bbb', 'id': '2'},
                            {'unit': 'TEXT', 'value': 'ccc', 'id': '3'},
                            {'unit': 'TEXT', 'value': 'ddd', 'id': '4'},
                        ],
                        'unitId': 'TEXT',
                        'id': '3',
                    }
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_original_params(cls):
        """
        Создаем русскую размерную сетку с двумя значениями размеров: 32 и 34 и интернациональную с XL
        Создаем оффер с размерами 32 и 34, и с оригинальными значениями 32 и XL
        Создаем оффер с размерами 32 и 34 и без оригинальных значений.
        """
        cls.index.gltypes += [
            GLType(
                param_id=210,
                hid=103,
                gltype=GLType.ENUM,
                subtype='size',
                cluster_filter=True,
                unit_param_id=211,
                name='size',
                values=[
                    GLValue(value_id=1, text='32', unit_value_id=2),
                    GLValue(value_id=2, text='34', unit_value_id=2),
                    GLValue(value_id=3, text='XL', unit_value_id=3),
                ],
            ),
            GLType(
                param_id=211,
                hid=103,
                gltype=GLType.ENUM,
                name='size_units',
                position=None,
                values=[
                    GLValue(value_id=2, text='RU', default=True),
                    GLValue(value_id=3, text='INT'),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='with-originals',
                hid=103,
                vclusterid=1000000003,
                glparams=[
                    GLParam(param_id=210, value=1),
                    GLParam(param_id=210, value=2),
                ],
                original_glparams=[GLParam(param_id=210, value=1), GLParam(param_id=210, value=3)],
            ),
            Offer(
                title='without-originals',
                hid=103,
                vclusterid=1000000003,
                glparams=[
                    GLParam(param_id=210, value=1),
                    GLParam(param_id=210, value=3),
                ],
            ),
        ]

        cls.index.vclusters += [VCluster(title='pants', hid=103, vclusterid=1000000003)]

        cls.settings.is_archive_new_format = True

    def test_original_params(self):
        """
        Проверяем, что в выдаче присутствует информация об оригинальном размере (32 и XL) у оффера с оригинальными
        значениями, и что оригинальных значений не показывается у оффера без оригинальных значений.
        Выдача экспериментальная, т.е. не обсуждаемая с фронтом и имеет цель, что репорт МОЖЕТ пробросить
        оригинальный размер
        """
        response = self.report.request_json('place=productoffers&hid=103&hyperid=1000000003')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'titles': {
                        'raw': "with-originals",
                    },
                    'experimentalOriginalParams': [{'param': 'size', 'value': '32'}, {'param': 'size', 'value': 'XL'}],
                },
                {
                    'entity': 'offer',
                    'titles': {
                        'raw': "without-originals",
                    },
                    'experimentalOriginalParams': Absent(),
                },
            ],
        )


if __name__ == '__main__':
    main()
