#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
from market.pylibrary.lite.matcher import Contains
from grpc import StatusCode


class T(env.AccessSuite):
    def test_publisher_contract(self):
        self.access.create_publisher(name='cat', description="white cat")
        self.access.create_publisher(name='dog', description="three color dog")

        # get
        response = self.access.get_publisher(name='cat')
        self.assertFragmentIn(response, {
            'description': 'white cat',
            'name': 'cat'
        })
        response = self.access.get_publisher(name='dog')
        self.assertFragmentIn(response, {
            'description': 'three color dog',
            'name': 'dog'
        })

        # list
        response = self.access.list_publishers()
        self.assertFragmentIn(response, {
            'publisher': [{
                'description': 'white cat',
                'name': 'cat'
            }, {
                'description': 'three color dog',
                'name': 'dog'
            }]})

        # update
        response = self.access.update_publisher(name='cat', description='red cat')
        self.assertFragmentIn(response, {
            'description': 'red cat',
            'name': 'cat'
        })

        # get
        response = self.access.get_publisher(name='cat')
        self.assertFragmentIn(response, {
            'description': 'red cat',
            'name': 'cat'
        })
        response = self.access.get_publisher(name='dog')
        self.assertFragmentIn(response, {
            'description': 'three color dog',
            'name': 'dog'
        })

        # list
        response = self.access.list_publishers()
        self.assertFragmentIn(response, {
            'publisher': [{
                'description': 'red cat',
                'name': 'cat'
            }, {
                'description': 'three color dog',
                'name': 'dog'
            }]})

    def test_publisher_idempotency(self):
        self.access.create_publisher(name='hello', idempotency_key='123')
        self.access.create_publisher(name='hello', idempotency_key='123')
        response = self.access.create_publisher(name='hello', idempotency_key='321', fail_on_error=False)
        self.assertEqual(response.code, StatusCode.ABORTED)
        self.common_log.expect(message=Contains("Publisher with name 'hello' already exists"), severity='ERRR')


if __name__ == '__main__':
    env.main()
