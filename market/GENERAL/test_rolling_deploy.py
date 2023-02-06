#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
from market.access.server.proto.consumer_pb2 import TConsumerNode, TConsumer
from market.pylibrary.lite.matcher import EmptyDict


Cr = TConsumerNode.TState.TResource
Cv = TConsumerNode.TState.TResource.TVersion
Tr = TConsumerNode.TTarget.TResource
Tv = TConsumerNode.TTarget.TResource.TVersion

TResourceOptions = TConsumer.TOptions.TResource
TResourceUpdateOptions = TConsumer.TOptions.TResource.TUpdate
TUpdateOptions = TConsumer.TOptions.TGlobal.TUpdate
TWindowConstraint = TConsumer.TOptions.TGlobal.TUpdate.TWindowConstraint


class T(env.AccessSuite):
    def test_rolling(self):
        self.access.create_publisher(name='miner')
        self.access.create_resource(publisher_name='miner', name='gold')
        self.access.create_version(resource_name='gold')
        self.access.create_consumer('rolling_argonavt')
        self.access.update_consumer(
            name='rolling_argonavt',
            options={
                'resource': {'gold': TResourceOptions(update=TResourceUpdateOptions(respect_download_window=True, respect_install_window=True))},
                'global' : {
                    'update': TUpdateOptions(
                        download_window=TWindowConstraint(node_count=2),
                        install_window=TWindowConstraint(node_count=2),
                    )
                }
            }
        )

        nodes = ['0', '1', '2']
        for node in nodes:
            self.access.update_consumer_node(node, 'rolling_argonavt')
            response = self.access.get_consumer_node(node, 'rolling_argonavt')
            self.assertFragmentIn(response, {'state': {'resource': {}}})

        # Signal that nodes are watching for updates
        for node in nodes:
            self.access.update_consumer_node(
                node_id=node,
                consumer_name='rolling_argonavt',
                state={'resource': {'gold': Cr(wait={'for_watch': True})}}
            )

        # test that scheduler orders to install & download desired resource for nodes in window
        for node in ['0', '1']:
            self._poll_node_target(
                consumer_name='rolling_argonavt',
                node_id=node,
                prev_target=EmptyDict(),
                next_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
            )

        # test that last node does not recieved target
        self.assertFalse(self._poll_node_target_once(
            consumer_name='rolling_argonavt',
            node_id='2',
            prev_target=EmptyDict(),
            next_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}},
            allow_different_len=False))

        # test that scheduler moves rolling window
        for node in ['0', '1']:
            self.access.update_consumer_node(
                node_id=node,
                consumer_name='rolling_argonavt',
                state={'resource': {
                    'gold': Cr(
                        wait={'for_watch': True},
                        in_download={'1.0.0': Cv(done=True)},
                        in_install={'1.0.0': Cv(done=True)},
                        in_load={'1.0.0': Cv(done=True)}
                    )
                }}
            )

        self._poll_node_target(
            consumer_name='rolling_argonavt',
            node_id='2',
            prev_target=EmptyDict(),
            next_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
        )

        # check that sheduler does not start new deployment before finishing active one.
        self.access.create_version(resource_name='gold')
        self.assertFalse(self._poll_node_target_once(
            consumer_name='rolling_argonavt',
            node_id='0',
            prev_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}},
            next_target={'gold': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}},
            allow_different_len=False))

        # check that sheduler starts new rolling deploy
        self.access.update_consumer_node(
            node_id='2',
            consumer_name='rolling_argonavt',
            state={'resource': {
                'gold': Cr(
                    wait={'for_watch': True},
                    in_download={'1.0.0': Cv(done=True)},
                    in_install={'1.0.0': Cv(done=True)},
                    in_load={'1.0.0': Cv(done=True)}
                )
            }}
        )
        self._poll_node_target(
            consumer_name='rolling_argonavt',
            node_id='0',
            prev_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}},
            next_target={'gold': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}
        )

if __name__ == '__main__':
    env.main()
