#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
from market.access.puller.mt.env import AccessPullerSuite, main
from market.pylibrary.lite.matcher import Contains
from grpc import StatusCode


class T(AccessPullerSuite):
    @classmethod
    def prepare(cls):
        cls.access_puller.config.StoragePullers.Dummy = True

    def test_puller_contract(self):
        deps1 = [{
            'resource_name': 'dep1'
        }, {
            'resource_name': 'dep2'
        }]
        deps2 = [{
            'resource_name': 'dep3'
        }, {
            'resource_name': 'dep4'
        }]
        mds1 = {
            'key': 'one',
            'host': 'https://mds.y-t.ru:123',
            'bucket': 'first',
        }
        mds2 = {
            'key': 'two',
            'host': 'http://mds.google.com:321',
            'bucket': 'second',
        }
        mds3 = {
            'prefix': 'prefix/three/',
            'host': 'http://mds.google.com:456',
            'bucket': 'third',
            'key_regex': 'my_cool_key.*\\d{2}',
        }
        self.access_puller.create_resource_puller(resource_name='my_resource', name='gold', dependency=deps1, mds=mds1, period='345s')
        self.access_puller.create_resource_puller(resource_name='resource_for_regex', name='regex_puller', dependency=deps1, mds=mds3, period='345s')

        # get resource
        response = self.access_puller.get_resource_puller(name='gold')
        self.assertFragmentIn(response, {
            'name': 'gold',
            'resource_name': 'my_resource',
            'mds': mds1,
            'period': '345s',
            'dependency': deps1
        })

        response = self.access_puller.get_resource_puller(name='regex_puller')
        self.assertFragmentIn(response, {
            'name': 'regex_puller',
            'resource_name': 'resource_for_regex',
            'mds': mds3,
            'period': '345s',
            'dependency': deps1
        })

        # update resource
        response = self.access_puller.update_resource_puller(name='gold', resource_name='they_resource', mds=mds2, dependency=deps2, period='678s')
        self.assertFragmentIn(response, {
            'name': 'gold',
            'resource_name': 'they_resource',
            'mds': mds2,
            'period': '678s',
            'dependency': deps2
        })

        # create another resource
        self.access_puller.create_resource_puller(resource_name='some_resource', name='silver')

        # list resources
        response = self.access_puller.list_resource_pullers()
        self.assertFragmentIn(response, {
            'pullers': [{
                'name': 'gold',
                'resource_name': 'they_resource',
                'dependency': deps2,
                'mds': mds2,
                'period': '678s'
            }, {
                'name': 'silver',
                'resource_name': 'some_resource',
            }]})

        # delete resources
        for puller_name in ('gold', 'silver'):
            self.access_puller.delete_resource_puller(name=puller_name)

        response = self.access_puller.list_resource_pullers()
        self.assertFragmentNotIn(response, {
            'pullers': [{
                'name': 'gold'
            }]})
        self.assertFragmentNotIn(response, {
            'pullers': [{
                'name': 'silver'
            }]})
        response = self.access_puller.get_resource_puller(name='gold', fail_on_error=False)
        self.assertEqual(response.code, StatusCode.NOT_FOUND)

    def test_resource_idempotency(self):
        self.access_puller.create_resource_puller(resource_name='our_resource', name='plumbum', idempotency_key='123')
        self.access_puller.create_resource_puller(resource_name='our_resource', name='plumbum', idempotency_key='123')
        response = self.access_puller.create_resource_puller(
            resource_name='our_resource',
            name='plumbum',
            idempotency_key='321',
            fail_on_error=False
        )
        self.assertEqual(response.code, StatusCode.ABORTED)
        self.common_log.expect(message=Contains("ResourcePuller 'plumbum' already exists"), severity='ERRR')


if __name__ == '__main__':
    main()
