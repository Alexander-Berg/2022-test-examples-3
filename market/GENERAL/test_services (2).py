#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.seo.experiments.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty


class T(env.ExperimentsSuite):
    def test_create_services(self):
        """Проверка корректности создания сервисов"""
        response = self.experiments.request_json('services')
        self.assertFragmentNotIn(response, [
            {
                'id': NotEmpty(),
                'name': 'service1',
            },
            {
                'id': NotEmpty(),
                'name': 'service2',
            },
        ])

        self.experiments.request_json('services?name=service1', method='POST')
        self.experiments.request_json('services?name=service2', method='POST')

        response = self.experiments.request_json('services')
        self.assertFragmentIn(response, [
            {
                'id': NotEmpty(),
                'name': 'service1',
            },
            {
                'id': NotEmpty(),
                'name': 'service2',
            },
        ])

    def test_delete_services(self):
        """Проверка удаления сервисов"""
        response = self.experiments.request_json('services?name=service3', method='POST')
        idx_for_deletion = response.root['id']

        self.experiments.request_json('services?name=service4', method='POST')

        response = self.experiments.request_json('services')
        self.assertFragmentIn(response, [
            {
                'id': NotEmpty(),
                'name': 'service3',
            },
            {
                'id': NotEmpty(),
                'name': 'service4',
            },
        ])

        self.experiments.request_json('services/{}'.format(idx_for_deletion), method='DELETE')
        response = self.experiments.request_json('services')
        self.assertFragmentNotIn(response, {
            'id': NotEmpty(),
            'name': 'service3'
        })


if __name__ == '__main__':
    env.main()
