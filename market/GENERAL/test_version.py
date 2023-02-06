#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
import market.access.server.proto.resource_pb2 as resource_pb2
from market.pylibrary.lite.matcher import NotEmpty, Capture


class T(env.AccessSuite):
    def test_version_contract(self):
        publisher_name = 'ver_contract'
        self.access.create_publisher(name=publisher_name)
        self.access.create_resource(publisher_name=publisher_name, name='shops.dat')

        # create version
        response = self.access.create_version(resource_name='shops.dat', description='first ver', create_ts=25)
        self.assertFragmentIn(response, {
            'create_time': NotEmpty(),
            'description': 'first ver',
            'number': '1.0.0',
            'resource_name': 'shops.dat'
        })

        # get version
        response = self.access.get_version(resource_name='shops.dat', number='1.0.0')
        self.assertFragmentIn(response, {
            'create_time': NotEmpty(),
            'description': 'first ver',
            'number': '1.0.0',
            'resource_name': 'shops.dat'
        })

        # create next version
        self.access.create_version(resource_name='shops.dat', description='first ver', create_ts=25, prev_version='1.0.0')

        # list versions
        response = self.access.list_versions(resource_name='shops.dat')
        self.assertFragmentIn(response, {
            'version' : [{
                'create_time': NotEmpty(),
                'number': '2.0.0',
                'previos_version_number': '1.0.0',
                'resource_name': 'shops.dat'
            }, {
                'create_time': NotEmpty(),
                'description': 'first ver',
                'number': '1.0.0',
                'resource_name': 'shops.dat'
            }]
        })

    def test_version_sequence(self):
        publisher_name = 'metals exchange4'
        self.access.create_publisher(name=publisher_name)
        self.access.create_resource(publisher_name=publisher_name, name='svn')

        response = self.access.create_version(resource_name='svn', create_ts=25)
        self.assertFragmentIn(response, {'number': '1.0.0'})

        response = self.access.create_version(resource_name='svn', create_ts=25, prev_version='1.0.0')
        self.assertFragmentIn(response, {'number': '2.0.0'})

        response = self.access.create_version(
            resource_name='svn',
            create_ts=25,
            prev_version='2.0.0',
            change=resource_pb2.TVersion.CHANGE_MINOR
        )
        self.assertFragmentIn(response, {'number': '2.1.0'})

        response = self.access.create_version(
            resource_name='svn',
            create_ts=25,
            prev_version='2.1.0',
            change=resource_pb2.TVersion.CHANGE_PATCH
        )
        self.assertFragmentIn(response, {'number': '2.1.1'})

        response = self.access.create_version(
            resource_name='svn',
            create_ts=25,
            prev_version='2.1.1',
            change=resource_pb2.TVersion.CHANGE_MAJOR
        )
        self.assertFragmentIn(response, {'number': '3.0.0'})

        response = self.access.create_version(
            resource_name='svn',
            create_ts=25,
            prev_version='2.1.1',
            change=resource_pb2.TVersion.CHANGE_MINOR
        )
        self.assertFragmentIn(response, {'number': '2.2.0'})

        # list versions
        response = self.access.list_versions(resource_name='svn')
        self.assertFragmentIn(response, {
            'version': [{
                'number': '3.0.0',
                'previos_version_number': '2.1.1'
            }, {
                'change': 'CHANGE_MINOR',
                'number': '2.2.0',
                'previos_version_number': '2.1.1'
            }, {
                'change': 'CHANGE_PATCH',
                'number': '2.1.1',
                'previos_version_number': '2.1.0'
            }, {
                'change': 'CHANGE_MINOR',
                'number': '2.1.0',
                'previos_version_number': '2.0.0'
            }, {
                'number': '2.0.0',
                'previos_version_number': '1.0.0'
            }, {
                'number': '1.0.0',
            }]
        }, preserve_order=True)

    def test_version_paging(self):
        publisher_name = 'metals exchange5'
        self.access.create_publisher(name=publisher_name)
        self.access.create_resource(publisher_name=publisher_name, name='paging')

        version = None
        for i in range(5):
            response = self.access.create_version(
                resource_name='paging',
                create_ts=25,
                prev_version=version.value if version else None
            )
            version = Capture()
            self.assertFragmentIn(response, {'number': NotEmpty(capture=version)})

        response = self.access.list_versions(resource_name='paging', page_size=3)
        self.assertFragmentIn(response, {
            'next_page_token': 'CAMSCGJuVnNiQSws',
            'version': [
                {'number': '5.0.0'},
                {'number': '4.0.0'},
                {'number': '3.0.0'}
            ]
        })

        response = self.access.list_versions(resource_name='paging', page_size=3, page_token='CAMSCGJuVnNiQSws')
        self.assertFragmentIn(response, {
            'version': [
                {'number': '2.0.0'},
                {'number': '1.0.0'}
            ]
        })

    def test_version_idempotency(self):
        publisher_name = 'ver_idemp'
        self.access.create_publisher(name=publisher_name)
        self.access.create_resource(publisher_name=publisher_name, name='idemp')

        # add version once
        response = self.access.create_version(resource_name='idemp', create_ts=25, idempotency_key='1')
        version = Capture()
        self.assertFragmentIn(response, {'number': NotEmpty(capture=version)})

        # add version again
        response = self.access.create_version(resource_name='idemp', create_ts=25, idempotency_key='1')
        retry_version = Capture()
        self.assertFragmentIn(response, {'number': NotEmpty(capture=retry_version)})
        self.assertEqual(version.value, retry_version.value)

        # and again, with another metadata
        response = self.access.create_version(resource_name='idemp', create_ts=25, idempotency_key='2')
        retry_version = Capture()
        self.assertFragmentIn(response, {'number': NotEmpty(capture=retry_version)})
        self.assertNotEqual(version.value, retry_version.value)


if __name__ == '__main__':
    env.main()
