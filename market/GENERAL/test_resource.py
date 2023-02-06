#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty, Capture, Contains
from grpc import StatusCode


class T(env.AccessSuite):
    def test_resource_contract(self):
        publisher_name = 'metals exchange'
        self.access.create_publisher(name=publisher_name)
        self.access.create_resource(publisher_name=publisher_name, name='gold', description='very valued')

        # get resource
        response = self.access.get_resource(name='gold')
        self.assertFragmentIn(response, {
            'description': 'very valued',
            'name': 'gold',
            'publisher_name': publisher_name
        })

        # update resource, check inside
        self.access.update_resource(name='gold', description='very valued++', publisher_name=publisher_name)

        # create another resource
        self.access.create_resource(publisher_name=publisher_name, name='silver', description='valued')

        # list resources
        response = self.access.list_resources(publisher_name=publisher_name)
        self.assertFragmentIn(response, {
            'resource': [{
                'description': 'very valued++',
                'name': 'gold',
                'publisher_name': publisher_name
            }, {
                'description': 'valued',
                'name': 'silver',
                'publisher_name': publisher_name
            }]})

        # delete resources
        for resource_name in ('gold', 'silver'):
            self.access.delete_resource(name=resource_name)

        response = self.access.list_resources(publisher_name=publisher_name)
        self.assertFragmentIn(response, {'resource': []})

        response = self.access.get_resource(name='gold', fail_on_error=False)
        self.assertEqual(response.code, StatusCode.NOT_FOUND)

    def test_resource_paging(self):
        publisher_name = 'metals exchange2'
        self.access.create_publisher(name=publisher_name)
        for i in range(5):
            self.access.create_resource(publisher_name=publisher_name, name=str(i))

        response = self.access.list_resources(publisher_name=publisher_name, page_size=3)
        next_page = Capture()
        self.assertFragmentIn(response, {
            'next_page_token': NotEmpty(capture=next_page),
            'resource': [
                {'name': '0'},
                {'name': '1'},
                {'name': '2'}
            ]})

        response = self.access.list_resources(publisher_name=publisher_name, page_size=3, page_token=next_page.value)
        self.assertFragmentIn(response, {
            'resource': [
                {'name': '3'},
                {'name': '4'}
            ]})

    def test_resource_idempotency(self):
        publisher_name = 'res_id'
        self.access.create_publisher(name=publisher_name)
        self.access.create_resource(publisher_name=publisher_name, name='hello', idempotency_key='123')
        self.access.create_resource(publisher_name=publisher_name, name='hello', idempotency_key='123')

        response = self.access.create_resource(publisher_name=publisher_name, name='hello', idempotency_key='321', fail_on_error=False)
        self.assertEqual(response.code, StatusCode.ABORTED)
        self.common_log.expect(message=Contains("Resource 'hello' already exists"), severity='ERRR')


if __name__ == '__main__':
    env.main()
