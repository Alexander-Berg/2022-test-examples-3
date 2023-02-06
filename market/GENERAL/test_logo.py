#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import  # noqa
import market.media_adv.incut_search.mt.env as env

from market.media_adv.incut_search.beam.incut import IncutModelsList
from market.media_adv.incut_search.beam.model import ModelWithBid
from market.media_adv.incut_search.beam.media_element import Logo, ColoredText
from market.media_adv.incut_search.beam.image import Image


class T(env.MediaAdvIncutSearchSuite):
    """
    Тесты по работе формированию лого
    """

    @classmethod
    def prepare_title_empty(cls):
        cls.content.incuts += [
            IncutModelsList(
                hid=101 + it_incut,
                vendor_id=1 + it_incut,
                datasource_id=1 + it_incut,
                id=1 + it_incut,  # incut_id
                url="test_document_{}".format(it_incut),
                models=[  # model list fields
                    ModelWithBid(
                        model_id=1 + i,
                    )
                    for i in range(0, 6)
                ],
                bid=90,
                logo=Logo(
                    id=90,
                    text=ColoredText(
                        text='logo title' if it_incut else '',  # врезка 0 без заголовка, врезка 1 с заголовком
                    ),
                    image=Image(
                        url="image_url",
                        width=800,
                        height=600,
                    ),
                    click_url='click url',
                    pixel_url='pixel_url',
                    bid=67,
                ),
            )
            for it_incut in range(0, 2)
        ]

    def test_title_empty(self):
        """
        При пустом заголовке Лого в документе врезки, должно подставляться дефолтное значение
        @see https://st.yandex-team.ru/MEDIAADV-106
        """

        response_1 = self.request(
            {
                'hid': 101,
            }
        )
        self.assertFragmentIn(
            response_1,
            {
                'mediaElement': {
                    '1': {
                        'text': {
                            'text': 'Идеи для покупок',  # в документе пустой заголовок - значение по умолчанию
                        },
                        'type': 'Logo',
                        'clickUrl': 'click url',
                        'pixelUrl': 'pixel_url',
                        'image': {
                            'url': 'image_url',
                            'width': 800,
                            'height': 600,
                        },
                    },
                },
            },
        )

        response_2 = self.request(
            {
                'hid': 102,
            }
        )
        self.assertFragmentIn(
            response_2,
            {
                'mediaElement': {
                    '1': {
                        'text': {
                            'text': 'logo title',  # тот заголовок, что и передавали
                        },
                        'type': 'Logo',
                    }
                },
            },
        )
