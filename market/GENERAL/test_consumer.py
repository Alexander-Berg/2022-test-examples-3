#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
import market.access.server.proto.consumer_pb2 as consumer_pb2
from grpc import StatusCode


TResource = consumer_pb2.TConsumerNode.TState.TResource
TVersion = consumer_pb2.TConsumerNode.TState.TResource.TVersion


class T(env.AccessSuite):
    def test_consumer_contract(self):
        # create
        response = self.access.create_consumer('chita')
        self.assertFragmentIn(response, {'name': 'chita'})

        # get
        response = self.access.get_consumer(name='chita')
        self.assertFragmentIn(response, {
            'name': 'chita'
        })

        # update
        response = self.access.update_consumer(name='chita')
        self.assertFragmentIn(response, {
            'name': 'chita'
        })

        # list
        self.access.create_consumer('chita2')
        response = self.access.list_consumers()
        self.assertFragmentIn(response, {
            'consumer': [{
                'name': 'chita'
            }, {
                'name': 'chita2'
            }]
        })

    def test_consumer_idempotency(self):
        self.access.create_consumer(name='gita', ik='123')
        self.access.create_consumer(name='gita', ik='123')
        response = self.access.create_consumer(name='gita', ik='321', fail=False)
        self.assertEqual(response.code, StatusCode.ABORTED)

    def test_consumer_node_contract(self):
        # create
        self.access.create_consumer(name='vasily')

        response = self.access.update_consumer_node(consumer_name='vasily', node_id='25')
        self.assertFragmentIn(response, {'id': '25'})

        # get
        response = self.access.get_consumer_node(consumer_name='vasily', node_id='25')
        self.assertFragmentIn(response, {'id': '25'})

        # update
        response = self.access.update_consumer_node(
            consumer_name='vasily',
            node_id='25',
            state={
                'resource': {
                    'gold': TResource(
                        in_download={'1.0.0': TVersion(done=False)}
                    )
                }
            }
        )

        self.assertFragmentIn(response, {
            'id': '25',
            'state': {
                'resource': {
                    'gold': {
                        'in_download': {
                            '1.0.0': {
                                'done': False,
                                'error': ''
                            }
                        }
                    }
                }
            }
        })

        # list
        self.access.update_consumer_node(consumer_name='vasily', node_id='26')
        response = self.access.list_consumer_nodes('vasily')
        self.assertFragmentIn(response, {
            'consumer_node': [{
                'id': '25',
                'state': {
                    'resource': {
                        'gold': {
                            'in_download': {
                                '1.0.0': {
                                    'done': False,
                                    'error': '',
                                }
                            }
                        }
                    }
                }
            }, {
                'id': '26',
                'state': {
                    'resource': {}
                }
            }]
        })

    def test_consumer_defaults(self):
        self.access.update_consumer_node('0', 'fly')
        response = self.access.get_consumer('fly')
        self.assertFragmentIn(response, {
            'name': 'fly',
            'options': {
                'global': {
                    'retention': {
                        'download_count': 5,
                        'install_count': 1
                    }
                }
            }
        })


if __name__ == '__main__':
    env.main()
