#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Book, GLParam, GLType, GLValue, HyperCategory, Model, ModelDescriptionTemplates, Offer
from core.testcase import TestCase, main
from core.types.picture import to_mbo_picture


class T(TestCase):
    @classmethod
    def prepare_single_model_parameters(cls):
        # Создаем шаблон описания модели
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=10,
                friendlymodel=[
                    ("Ретина", "телефон {Retina#ifnz}{Retina:с ретиной}{#endif}"),
                    ("Диагональ экрана", "{DisplaySize}"),
                    ("ОС", "{OS}"),
                    ("Дополнительная информация", "{AdditionalInfo}"),
                ],
            )
        ]

        # Создаем фильтры, на которые ссылаемся в шаблоне
        cls.index.gltypes += [
            GLType(hid=10, param_id=100, xslname="Retina", gltype=GLType.BOOL),
            GLType(hid=10, param_id=101, xslname="DisplaySize", gltype=GLType.NUMERIC),
            GLType(
                hid=10,
                param_id=102,
                xslname="OS",
                gltype=GLType.ENUM,
                values=[GLValue(value_id=1, text='iOS'), GLValue(value_id=2, text='Android')],
            ),
            GLType(hid=10, param_id=103, xslname="AdditionalInfo", gltype=GLType.STRING),
        ]

        # Создаем модель с фильтрами
        cls.index.models += [
            Model(
                hid=10,
                title="single iphone",
                hyperid=1,
                glparams=[
                    GLParam(param_id=100, value=1),
                    GLParam(param_id=101, value=4.7),
                    GLParam(param_id=102, value=1),
                    GLParam(param_id=103, string_value="Наушники EarPods в комплекте"),
                ],
            )
        ]

    def test_single_model_parameters(self):
        """Характеристики из friendly-карточки в колдунщике одиночной модели.
        https://st.yandex-team.ru/MARKETOUT-13503
        """

        response = self.report.request_bs('place=parallel&text=single+iphone')
        self.assertFragmentIn(
            response,
            {"market_model": [{"parameters": ["телефон с ретиной", "4.7", "iOS", "Наушники EarPods в комплекте"]}]},
        )

    @classmethod
    def prepare_book_parameters(cls):
        cls.index.books += [
            Book(
                isbn='978-5-902719-15-1',
                author="lognick",
                publisher='Market.Report',
                publishing_year='2021',
                description='Очередной бестселлер от нашумевшего автора',
            )
        ]

    def test_book_parameters(self):
        """Характеристики книжки в модельном колдунщике.
        Описание прокидывается отдельным полем long_description.
        Для книжек проставляется признак is_book=1.

        https://st.yandex-team.ru/MARKETOUT-13503
        """
        response = self.report.request_bs('place=parallel&text=9785902719151')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "parameters": ["9785902719151", "lognick", "Market.Report", "2021"],
                    }
                ]
            },
        )

    @classmethod
    def prepare_single_model_colors(cls):
        cls.index.hypertree += [HyperCategory(hid=30)]

        # Готовим одиночную модель для колдунщика одиночной модели
        cls.index.models += [
            Model(hyperid=5000, title="color odinochnaya model", hid=30),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=500,
                hid=30,
                cluster_filter=1,
                subtype='color',
                values=[
                    GLValue(1, code='#FF0000', tag='red', text='красный'),
                    GLValue(2, code='#00FF00', tag='green', text='зелёный'),
                    GLValue(3, code='#0000FF', tag='blue', text='синий'),
                ],
            )
        ]

        # Привязываем «цветные» офферы к модели
        cls.index.offers += [
            Offer(hyperid=5000, glparams=[GLParam(param_id=500, value=1)]),  # красный
            Offer(hyperid=5000, glparams=[GLParam(param_id=500, value=2)]),  # зелёный
        ]

    @classmethod
    def prepare_model_pictures(cls):
        '''
        Подготовка для проверки нескольких картинок в модельном колдунщике
        MARKETOUT-13312
        '''
        cls.index.models += [
            Model(
                title='Picturesmodel',
                proto_add_pictures=[
                    to_mbo_picture('//avatars.mds.yandex.net/get-mpic/13312/model_1/orig#100#100'),
                    to_mbo_picture('//avatars.mds.yandex.net/get-mpic/13312/model_2/orig#100#100'),
                    to_mbo_picture('//avatars.mds.yandex.net/get-mpic/13312/model_3/orig#100#100'),
                ],
            ),
        ]

    def test_model_pictures(self):
        '''
        Проверка нескольких картинок в модельном колдунщике
        MARKETOUT-13312
        '''

        default_size = "8"
        pic_sizes = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '10', None, 'wrong'}

        for quality in ('', 'hq'):
            for pic_size in pic_sizes:
                response_size = pic_size if pic_size and pic_size != 'wrong' else default_size

                # Проверка картинок модели
                request = "place=parallel&text=Picturesmodel"
                if pic_size:
                    request += "&rearr-factors=market_model_wizard_pictures_size={}{}".format(pic_size, quality)
                response = self.report.request_bs(request)
                self.assertFragmentIn(
                    response,
                    {
                        "pictures": [
                            "//avatars.mds.yandex.net/get-mpic/13312/model_1/{}hq".format(response_size),
                            "//avatars.mds.yandex.net/get-mpic/13312/model_2/{}hq".format(response_size),
                            "//avatars.mds.yandex.net/get-mpic/13312/model_3/{}hq".format(response_size),
                        ]
                    },
                )

    @classmethod
    def prepare_book_description_length(cls):
        """Подготовка данных для проверки ограничения длины снипета для книг"""
        cls.index.books += [
            Book(isbn='978-5-389-07947-2', description='A' * 10),
            Book(isbn='978-5-389-12042-6', description='B' * 150),
            Book(isbn='978-5-389-06171-2', description='C' * 200),
        ]

    def test_book_description_length(self):
        """Проверка ограничения длины снипета для книг
        https://st.yandex-team.ru/MARKETOUT-13681
        """
        MAX_DESCRIPTION_LENGTH = 150
        # Проверка описания длинной менее 150 символов
        response = self.report.request_bs('place=parallel&text=9785389079472')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "text": [{"__hl": {"raw": True, "text": "AAAAAAAAAA"}}],
                    }
                ]
            },
        )
        # Проверка описания длинной 150 символов
        response = self.report.request_bs('place=parallel&text=9785389120426')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "text": [{"__hl": {"raw": True, "text": "B" * MAX_DESCRIPTION_LENGTH}}],
                    }
                ]
            },
        )
        # Проверка ограничения описания длинной более 150 символов
        response = self.report.request_bs('place=parallel&text=9785389061712')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "text": [{"__hl": {"raw": True, "text": "C" * (MAX_DESCRIPTION_LENGTH - 3) + "..."}}],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
