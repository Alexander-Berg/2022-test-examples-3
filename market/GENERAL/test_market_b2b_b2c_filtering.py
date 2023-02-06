#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import (
    TestCase,
    main,
)

from core.types.sku import (
    BlueOffer,
    MarketSku,
)

from core.types import Offer

from unittest import skip


class T(TestCase):
    _offers_data = []
    _b2c_enabled_options = [
        (True, '&rearr-factors=enable_b2c_filtering=1'),
        (True, ''),
        (False, '&rearr-factors=enable_b2c_filtering=0'),
    ]

    @classmethod
    def prepare_white_ofers(cls):
        for is_b2b in (True, False):
            for is_b2c in (True, False):
                title = 'тереби{}b2b{}b2c'.format(
                    ' ' if is_b2b else ' не ',
                    ' и ' if is_b2c else ' не ',
                )
                cls._offers_data.append((title, is_b2b, is_b2c))
                cls.index.offers += [Offer(title=title, is_b2b=is_b2b, is_b2c=is_b2c)]

    @classmethod
    def prepare_blue_ofers(cls):
        market_sku_id = 1
        for is_b2b in (True, False):
            for is_b2c in (True, False):
                title = 'тереби синий{}b2b{}b2c'.format(
                    ' ' if is_b2b else ' не ',
                    ' и ' if is_b2c else ' не ',
                )
                cls._offers_data.append((title, is_b2b, is_b2c))
                cls.index.mskus += [
                    MarketSku(
                        sku=market_sku_id,
                        title=title,
                        blue_offers=[
                            BlueOffer(title=title, is_b2b=is_b2b, is_b2c=is_b2c),
                        ],
                    )
                ]
                market_sku_id += 1

    def test_b2b_flow(self):
        '''
        Проверяем, что для b2b пользователя только b2b офферы отображаются
        '''
        base_search = 'place=prime&text=тереби&debug=da&available-for-business=1'
        for _, param in self._b2c_enabled_options:
            response = self.report.request_json(base_search + param)
            for title, is_b2b, _ in self._offers_data:
                if is_b2b:
                    self.assertFragmentIn(response, {'results': [{'titles': {'raw': title}}]})
                else:
                    self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': title}}]})

    @skip(
        'https://st.yandex-team.ru/MARKETOUT-46377 Кажется сразу не работало с поиском по ску. Офферы внутри скухи не фильтруются по b2b|b2c'
    )
    def test_b2c_flow(self):
        '''
        Проверяем, что для b2с пользователя только b2с офферы отображаются
        !!! Тест не работает с поиском по ску, несмотря на то что флаг уже выкачен в проде
        test_b2b_flow работает т.к. при available-for-business=1 поиск по ску не выкачен еще
        '''
        for b2c_filtering_enabled, b2c_filtering_param in self._b2c_enabled_options:
            for b2c_user_param in ('&available-for-business=0', ''):
                response = self.report.request_json(
                    'place=prime&text=тереби&debug=da' + b2c_user_param + b2c_filtering_param
                )
                for title, _, is_b2c in self._offers_data:
                    if not b2c_filtering_enabled or is_b2c:
                        self.assertFragmentIn(response, {'results': [{'titles': {'raw': title}}]})
                    else:
                        self.assertFragmentNotIn(response, {'results': [{'titles': {'raw': title}}]})


if __name__ == '__main__':
    main()
