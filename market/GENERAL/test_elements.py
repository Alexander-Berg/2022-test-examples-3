#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.seo.experiments.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty


class T(env.ExperimentsSuite):
    def test_create_elements(self):
        """Проверка корректности создания элементов страниц"""
        response = self.experiments.request_json('elements')
        self.assertFragmentNotIn(response, [
            {
                'id': NotEmpty(),
                'name': 'element_1',
            },
            {
                'id': NotEmpty(),
                'name': 'element_2',
            },
        ])

        self.experiments.request_json('elements?name=element_1', method='POST')
        self.experiments.request_json('elements?name=element_2', method='POST')

        response = self.experiments.request_json('elements')
        self.assertFragmentIn(response, [
            {
                'id': NotEmpty(),
                'name': 'element_1',
            },
            {
                'id': NotEmpty(),
                'name': 'element_2',
            },
        ])

    def test_delete_element(self):
        """Проверка удаления элемента страницы"""
        response = self.experiments.request_json('elements?name=element_3', method='POST')
        idx_for_deletion = response.root['id']

        self.experiments.request_json('elements?name=element_4', method='POST')

        response = self.experiments.request_json('elements')
        self.assertFragmentIn(response, [
            {
                'id': NotEmpty(),
                'name': 'element_3',
            },
            {
                'id': NotEmpty(),
                'name': 'element_4',
            },
        ])

        self.experiments.request_json('elements/{}'.format(idx_for_deletion), method='DELETE')
        response = self.experiments.request_json('elements')
        self.assertFragmentNotIn(response, {
            'id': NotEmpty(),
            'name': 'element_3'
        })


if __name__ == '__main__':
    env.main()
