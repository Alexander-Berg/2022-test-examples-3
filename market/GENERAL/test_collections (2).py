#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.seo.experiments.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty


class T(env.ExperimentsSuite):
    def test_create_collections(self):
        """Проверка корректности создания коллекций"""
        response = self.experiments.request_json('collections')
        self.assertFragmentNotIn(response, [
            {
                'id': NotEmpty(),
                'name': 'collection_1',
            },
            {
                'id': NotEmpty(),
                'name': 'collection_2',
            },
        ])

        self.experiments.request_json('collections?name=collection_1', method='POST')
        self.experiments.request_json('collections?name=collection_2', method='POST')

        response = self.experiments.request_json('collections')
        self.assertFragmentIn(response, [
            {
                'id': NotEmpty(),
                'name': 'collection_1',
            },
            {
                'id': NotEmpty(),
                'name': 'collection_2',
            },
        ])

    def test_delete_collection(self):
        """Проверка удаления коллекции"""
        response = self.experiments.request_json('collections?name=collection_3', method='POST')
        idx_for_deletion = response.root['id']

        self.experiments.request_json('collections?name=collection_4', method='POST')

        response = self.experiments.request_json('collections')
        self.assertFragmentIn(response, [
            {
                'id': NotEmpty(),
                'name': 'collection_3',
            },
            {
                'id': NotEmpty(),
                'name': 'collection_4',
            },
        ])

        self.experiments.request_json('collections/{}'.format(idx_for_deletion), method='DELETE')
        response = self.experiments.request_json('collections')
        self.assertFragmentNotIn(response, {
            'id': NotEmpty(),
            'name': 'collection_3'
        })


if __name__ == '__main__':
    env.main()
