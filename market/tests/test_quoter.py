#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.quoter.tests.env as env
from market.pylibrary.lite.matcher import AlmostEqual, EmptyList


class T(env.QuoterSuite):

    def test_get_quota(self):
        try:
            self.quoter.get_quota(service_name='test_service', client_name='test_get_quota', resource_name='cpu')
        except RuntimeError as e:
            assert str(e) == 'Server error: StatusCode.NOT_FOUND ()'
        else:
            raise AssertionError('NOT_FOUND status code was expected')

        self.quoter.update_usage(service_name='test_service', client_name='test_get_quota', resource_name='cpu', current_usage=100)
        self.quoter.sync()

        response = self.quoter.get_quota(service_name='test_service', client_name='test_get_quota', resource_name='cpu')
        self.assertFragmentIn(response, {
            'limit': 300.0,
            'total_usage': AlmostEqual(100, delta=1)
        })

    def test_list_quotas(self):
        response = self.quoter.list_quotas(service_name='test_service', client_name='test_list_quota')
        self.assertFragmentIn(response, {
            'items': EmptyList()
        })

        self.quoter.update_usage_batch(service_name='test_service', client_name='test_list_quota', items=[{'resource': 'cpu', 'current_usage': 200}, {'resource': 'rps', 'current_usage': 100}])
        self.quoter.sync()

        response = self.quoter.list_quotas(service_name='test_service', client_name='test_list_quota')
        self.assertFragmentIn(response, {
            'items': [{
                'name': 'cpu',
                'quota': {
                    'limit': 300.0,
                    'total_usage': AlmostEqual(200, delta=1)
                }
            },
            {
                'name': 'rps',
                'quota': {
                    'limit': 200.0,
                    'total_usage': AlmostEqual(100, delta=1)
                }
            }]
        })

    def test_update_usage(self):
        self.quoter.update_usage(service_name='test_service', client_name='test_update_usage', resource_name='cpu', current_usage=100)
        self.quoter.update_usage(service_name='test_service', client_name='test_update_usage', resource_name='cpu', current_usage=150)
        self.quoter.update_usage(service_name='test_service', client_name='test_update_usage', resource_name='cpu', current_usage=100)
        self.quoter.sync()

        response = self.quoter.update_usage(service_name='test_service', client_name='test_update_usage', resource_name='cpu', current_usage=0)
        self.assertFragmentIn(response, {
            'limit': 300.0,
            'total_usage': AlmostEqual(350, delta=1)
        })

    def test_update_usage_batch(self):
        self.quoter.update_usage_batch(service_name='test_service', client_name='test_update_usage_batch', items=[{'resource': 'cpu', 'current_usage': 100}, {'resource': 'cpu', 'current_usage': 110}])
        self.quoter.update_usage_batch(service_name='test_service', client_name='test_update_usage_batch', items=[{'resource': 'cpu', 'current_usage': 150}, {'resource': 'rps', 'current_usage': 100}])
        self.quoter.update_usage_batch(service_name='test_service', client_name='test_update_usage_batch', items=[])
        self.quoter.sync()

        response = self.quoter.update_usage_batch(service_name='test_service', client_name='test_update_usage_batch', items=[])
        self.assertFragmentIn(response, {
            'items': [{
                'name': 'cpu',
                'quota': {
                    'limit': 300.0,
                    'total_usage': AlmostEqual(360, delta=1)
                }
            },
                {
                    'name': 'rps',
                    'quota': {
                        'limit': 200.0,
                        'total_usage': AlmostEqual(100, delta=1)
                    }
                }]
        })

    def test_overused_filter(self):
        self.quoter.update_usage_batch(service_name='test_service', client_name='test_overused_filter',
                                       items=[{'resource': 'cpu', 'current_usage': 400},
                                              {'resource': 'rps', 'current_usage': 100},
                                              {'resource': 'other', 'current_usage': 500}])
        self.quoter.sync()
        response = self.quoter.list_quotas(service_name='test_service', client_name='test_overused_filter', only_overused=True)
        self.assertFragmentIn(response, {
            'items': [{
                'name': 'cpu',
                'quota': {
                    'limit': 300.0,
                    'total_usage': AlmostEqual(400, delta=1)
                }
            },
            {
                'name': 'other',
                'quota': {
                    'limit': 400.0,
                    'total_usage': AlmostEqual(500, delta=1)
                }
            }]
        })
        self.assertFragmentNotIn(response, {
            'name': 'rps'
        })

    def test_json(self):
        self.quoter.update_usage(service_name='test_service', client_name='test_json', resource_name='cpu', current_usage=100, use_json=True)
        self.quoter.update_usage_batch(service_name='test_service', client_name='test_json', items=[{'resource': 'cpu', 'current_usage': 200},
                                                                                                    {'resource': 'rps', 'current_usage': 100}], use_json=True)
        self.quoter.sync(use_json=True)

        response = self.quoter.get_quota(service_name='test_service', client_name='test_json', resource_name='cpu', use_json=True)
        self.assertFragmentIn(response, {
            'limit': 3000,
            'total_usage': AlmostEqual(300, delta=1)
        })

        response = self.quoter.list_quotas(service_name='test_service', client_name='test_json', use_json=True)
        self.assertFragmentIn(response, {
            'items': [{
                'name': 'cpu',
                'quota': {
                    'limit': 3000,
                    'total_usage': AlmostEqual(300, delta=1)
                }
            },
            {
                'name': 'rps',
                'quota': {
                    'limit': 2000,
                    'total_usage': AlmostEqual(100, delta=1)
                }
            }]
        })


if __name__ == '__main__':
    env.main()
