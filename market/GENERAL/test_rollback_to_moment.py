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
NodeOptions = TConsumer.TOptions.TNode
TaggedNodeOptions = TConsumer.TOptions.TTaggedNode
ResourceOptions = TConsumer.TOptions.TResource


class T(env.AccessSuite):
    def test_global_rollback_to_moment(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_1 --global --update.to_moment 1500

        There is only one single-node consumer 'rollbacker_1' and only one resource 'rollbackable_1' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_1')
        self.access.create_resource(publisher_name='rollbackable_provider_1', name='rollbackable_1')
        self.access.create_consumer('rollbacker_1')

        target_v1 = {'rollbackable_1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
        target_v2 = {'rollbackable_1': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}
        target_after_rollback = {'rollbackable_1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}

        # Create version 1.0.0
        self.access.create_version(resource_name='rollbackable_1')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_1',
            state={'resource': {'rollbackable_1': Cr(wait={'for_install': True})}}
        )

        # Wait for version 1.0.0
        self._poll_node_target(
            consumer_name='rollbacker_1',
            node_id='0',
            prev_target=EmptyDict(),
            next_target=target_v1
        )

        # Create version 2.0.0
        self.access.create_version(resource_name='rollbackable_1')

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_1',
            state={'resource': {
                'rollbackable_1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Wait for version 2.0.0
        self._poll_node_target(
            consumer_name='rollbacker_1',
            node_id='0',
            prev_target=target_v1,
            next_target=target_v2
        )

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_1',
            state={'resource': {
                'rollbackable_1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_1',
            options={
                'global': {'update': {'to_moment': {'seconds': 1500}}}
            }
        )

        # Wait for rolled back version
        self._poll_node_target(
            consumer_name='rollbacker_1',
            node_id='0',
            prev_target=target_v2,
            next_target=target_after_rollback
        )

    def test_node_rollback_to_moment(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_2 --node 0 --update.to_moment 1500

        There is only one consumer 'rollbacker_2' with two nodes and two resources 'rollbackable_2.1' and
        'rollbackable_2.2' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_2')
        self.access.create_resource(publisher_name='rollbackable_provider_2', name='rollbackable_2.1')
        self.access.create_resource(publisher_name='rollbackable_provider_2', name='rollbackable_2.2')
        self.access.create_consumer('rollbacker_2')

        target_v1 = {
            'rollbackable_2.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
            'rollbackable_2.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
        }
        target_v2 = {
            'rollbackable_2.1': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_2.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }
        node_0_target_after_rollback = {
            # Both versions will be rolled back:
            'rollbackable_2.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_2.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }

        # Create version 1.0.0 of resources
        self.access.create_version(resource_name='rollbackable_2.1')
        self.access.create_version(resource_name='rollbackable_2.2')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_2',
            state={'resource': {
                'rollbackable_2.1': Cr(wait={'for_install': True}),
                'rollbackable_2.2': Cr(wait={'for_install': True}),
            }}
        )
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_2',
            state={'resource': {
                'rollbackable_2.1': Cr(wait={'for_install': True}),
                'rollbackable_2.2': Cr(wait={'for_install': True}),
            }}
        )

        # Wait for version 1.0.0
        self._poll_nodes_target(consumer_name='rollbacker_2', node_ids=['0', '1'], prev_target=EmptyDict(), next_target=target_v1)

        # Create version 2.0.0 of resources
        self.access.create_version(resource_name='rollbackable_2.1')
        self.access.create_version(resource_name='rollbackable_2.2')

        # Version 1.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_2',
            state={'resource': {
                'rollbackable_2.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                ),
                'rollbackable_2.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Version 1.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_2',
            state={'resource': {
                'rollbackable_2.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                ),
                'rollbackable_2.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Wait for version 2.0.0
        self._poll_nodes_target(consumer_name='rollbacker_2', node_ids=['0', '1'], prev_target=target_v1, next_target=target_v2)

        # Version 2.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_2',
            state={'resource': {
                'rollbackable_2.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                ),
                'rollbackable_2.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Version 2.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_2',
            state={'resource': {
                'rollbackable_2.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                ),
                'rollbackable_2.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_2',
            options={
                'node': {
                    '0': NodeOptions(update={'to_moment': {'seconds': 1500}}),
                }
            }
        )

        # Wait for rolled back version on node '0'
        self._poll_node_target(consumer_name='rollbacker_2', node_id='0', prev_target=target_v2, next_target=node_0_target_after_rollback)

        # Check that the version wasn't changed on node '1'
        response = self.access.get_consumer_node(consumer_name='rollbacker_2', node_id='1')
        self.assertFragmentIn(response, target_v2)

    def test_node_resource_rollback_to_moment(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_3 --node 0 --resource rollbackable_3.1 --update.to_moment 1500

        There is only one consumer 'rollbacker_3' with two nodes and two resources 'rollbackable_3.1' and
        'rollbackable_3.2' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_3')
        self.access.create_resource(publisher_name='rollbackable_provider_3', name='rollbackable_3.1')
        self.access.create_resource(publisher_name='rollbackable_provider_3', name='rollbackable_3.2')
        self.access.create_consumer('rollbacker_3')

        target_v1 = {
            'rollbackable_3.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
            'rollbackable_3.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
        }
        target_v2 = {
            'rollbackable_3.1': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_3.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }
        node_0_target_after_rollback = {
            # Will be rolled back:
            'rollbackable_3.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},

            # Will stay unchanged:
            'rollbackable_3.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }

        # Create version 1.0.0 of resources
        self.access.create_version(resource_name='rollbackable_3.1')
        self.access.create_version(resource_name='rollbackable_3.2')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_3',
            state={'resource': {
                'rollbackable_3.1': Cr(wait={'for_install': True}),
                'rollbackable_3.2': Cr(wait={'for_install': True}),
            }}
        )
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_3',
            state={'resource': {
                'rollbackable_3.1': Cr(wait={'for_install': True}),
                'rollbackable_3.2': Cr(wait={'for_install': True}),
            }}
        )

        # Wait for version 1.0.0
        self._poll_nodes_target(consumer_name='rollbacker_3', node_ids=['0', '1'], prev_target=EmptyDict(), next_target=target_v1)

        # Create version 2.0.0 of resources
        self.access.create_version(resource_name='rollbackable_3.1')
        self.access.create_version(resource_name='rollbackable_3.2')

        # Version 1.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_3',
            state={'resource': {
                'rollbackable_3.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                ),
                'rollbackable_3.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Version 1.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_3',
            state={'resource': {
                'rollbackable_3.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                ),
                'rollbackable_3.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Wait for version 2.0.0
        self._poll_nodes_target(consumer_name='rollbacker_3', node_ids=['0', '1'], prev_target=target_v1, next_target=target_v2)

        # Version 2.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_3',
            state={'resource': {
                'rollbackable_3.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                ),
                'rollbackable_3.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Version 2.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_3',
            state={'resource': {
                'rollbackable_3.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                ),
                'rollbackable_3.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_3',
            options={
                'node': {
                    '0': NodeOptions(resource={
                        'rollbackable_3.1': ResourceOptions(update={'to_moment': {'seconds': 1500}}),
                    })
                }
            }
        )

        # Wait for rolled back version on node '0'
        self._poll_node_target(consumer_name='rollbacker_3', node_id='0', prev_target=target_v2, next_target=node_0_target_after_rollback)

        # Check that the version wasn't changed on node '1'
        response = self.access.get_consumer_node(consumer_name='rollbacker_3', node_id='1')
        self.assertFragmentIn(response, target_v2)

    def test_resource_rollback_to_moment(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_4 --resource rollbackable_4.1 --update.to_moment 1500

        There is only one consumer 'rollbacker_4' with two nodes and two resources 'rollbackable_4.1' and
        'rollbackable_4.2' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_4')
        self.access.create_resource(publisher_name='rollbackable_provider_4', name='rollbackable_4.1')
        self.access.create_resource(publisher_name='rollbackable_provider_4', name='rollbackable_4.2')
        self.access.create_consumer('rollbacker_4')

        target_v1 = {
            'rollbackable_4.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
            'rollbackable_4.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
        }
        target_v2 = {
            'rollbackable_4.1': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_4.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }
        target_after_rollback = {
            # Will be rolled back:
            'rollbackable_4.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},

            # Will stay unchanged:
            'rollbackable_4.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }

        # Create version 1.0.0 of resources
        self.access.create_version(resource_name='rollbackable_4.1')
        self.access.create_version(resource_name='rollbackable_4.2')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_4',
            state={'resource': {
                'rollbackable_4.1': Cr(wait={'for_install': True}),
                'rollbackable_4.2': Cr(wait={'for_install': True}),
            }}
        )
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_4',
            state={'resource': {
                'rollbackable_4.1': Cr(wait={'for_install': True}),
                'rollbackable_4.2': Cr(wait={'for_install': True}),
            }}
        )

        # Wait for version 1.0.0
        self._poll_nodes_target(consumer_name='rollbacker_4', node_ids=['0', '1'], prev_target=EmptyDict(), next_target=target_v1)

        # Create version 2.0.0 of resources
        self.access.create_version(resource_name='rollbackable_4.1')
        self.access.create_version(resource_name='rollbackable_4.2')

        # Version 1.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_4',
            state={'resource': {
                'rollbackable_4.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                ),
                'rollbackable_4.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Version 1.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_4',
            state={'resource': {
                'rollbackable_4.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                ),
                'rollbackable_4.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Wait for version 2.0.0
        self._poll_nodes_target(consumer_name='rollbacker_4', node_ids=['0', '1'], prev_target=target_v1, next_target=target_v2)

        # Version 2.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_4',
            state={'resource': {
                'rollbackable_4.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                ),
                'rollbackable_4.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Version 2.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_4',
            state={'resource': {
                'rollbackable_4.1': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                ),
                'rollbackable_4.2': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_4',
            options={
                'resource': {
                    'rollbackable_4.1': ResourceOptions(update={'to_moment': {'seconds': 1500}}),
                }
            }
        )

        # Wait for rolled back version on nodes
        self._poll_nodes_target(consumer_name='rollbacker_4', node_ids=['0', '1'], prev_target=target_v2, next_target=target_after_rollback)

    def test_tagged_node_resource_rollback_to_moment(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_5 --tag dc:vla --resource rollbackable_5.1 --update.to_moment 1500

        There is only one consumer 'rollbacker_5' with two nodes and two resources 'rollbackable_5.1' and
        'rollbackable_5.2' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_5')
        self.access.create_resource(publisher_name='rollbackable_provider_5', name='rollbackable_5.1')
        self.access.create_resource(publisher_name='rollbackable_provider_5', name='rollbackable_5.2')
        self.access.create_consumer('rollbacker_5')

        target_v1 = {
            'rollbackable_5.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
            'rollbackable_5.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
        }
        target_v2 = {
            'rollbackable_5.1': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_5.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }
        node_0_target_after_rollback = {
            # Will be rolled back:
            'rollbackable_5.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},

            # Will stay unchanged:
            'rollbackable_5.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }

        # Create version 1.0.0 of resources
        self.access.create_version(resource_name='rollbackable_5.1')
        self.access.create_version(resource_name='rollbackable_5.2')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_5',
            state={
                'resource': {
                    'rollbackable_5.1': Cr(wait={'for_install': True}),
                    'rollbackable_5.2': Cr(wait={'for_install': True}),
                },
                'tag': ['dc:vla']
            }
        )
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_5',
            state={
                'resource': {
                    'rollbackable_5.1': Cr(wait={'for_install': True}),
                    'rollbackable_5.2': Cr(wait={'for_install': True}),
                },
                'tag': ['dc:sas']
            }
        )

        # Wait for version 1.0.0
        self._poll_nodes_target(consumer_name='rollbacker_5', node_ids=['0', '1'], prev_target=EmptyDict(), next_target=target_v1)

        # Create version 2.0.0 of resources
        self.access.create_version(resource_name='rollbackable_5.1')
        self.access.create_version(resource_name='rollbackable_5.2')

        # Version 1.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_5',
            state={
                'resource': {
                    'rollbackable_5.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    ),
                    'rollbackable_5.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    )
                },
                'tag': ['dc:vla']
            }
        )

        # Version 1.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_5',
            state={
                'resource': {
                    'rollbackable_5.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    ),
                    'rollbackable_5.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    )
                },
                'tag': ['dc:sas']
            }
        )

        # Wait for version 2.0.0
        self._poll_nodes_target(consumer_name='rollbacker_5', node_ids=['0', '1'], prev_target=target_v1, next_target=target_v2)

        # Version 2.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_5',
            state={
                'resource': {
                    'rollbackable_5.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    ),
                    'rollbackable_5.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    )
                },
                'tag': ['dc:vla']
            }
        )

        # Version 2.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_5',
            state={
                'resource': {
                    'rollbackable_5.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    ),
                    'rollbackable_5.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    )
                },
                'tag': ['dc:sas']
            }
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_5',
            options={
                'tagged': [
                    TaggedNodeOptions(
                        tag=['dc:vla'],
                        node=NodeOptions(resource={
                            'rollbackable_5.1': ResourceOptions(update={'to_moment': {'seconds': 1500}}),
                        })
                    )
                ]
            }
        )

        # Wait for rolled back version on node '0'
        self._poll_node_target(consumer_name='rollbacker_5', node_id='0', prev_target=target_v2, next_target=node_0_target_after_rollback)

        # Check that the version wasn't changed on node '1'
        response = self.access.get_consumer_node(consumer_name='rollbacker_5', node_id='1')
        self.assertFragmentIn(response, target_v2)

    def test_tagged_node_rollback_to_moment(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_6 --tag dc:vla --update.to_moment 1500

        There is only one consumer 'rollbacker_6' with three nodes and two resources 'rollbackable_6.1' and
        'rollbackable_6.2' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_6')
        self.access.create_resource(publisher_name='rollbackable_provider_6', name='rollbackable_6.1')
        self.access.create_resource(publisher_name='rollbackable_provider_6', name='rollbackable_6.2')
        self.access.create_consumer('rollbacker_6')

        target_v1 = {
            'rollbackable_6.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
            'rollbackable_6.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}},
        }
        target_v2 = {
            'rollbackable_6.1': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_6.2': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }
        node_target_after_rollback = {
            'rollbackable_6.1': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
            'rollbackable_6.2': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}},
        }

        # Create version 1.0.0 of resources
        self.access.create_version(resource_name='rollbackable_6.1')
        self.access.create_version(resource_name='rollbackable_6.2')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(wait={'for_install': True}),
                    'rollbackable_6.2': Cr(wait={'for_install': True}),
                },
                'tag': ['dc:vla']
            }
        )
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(wait={'for_install': True}),
                    'rollbackable_6.2': Cr(wait={'for_install': True}),
                },
                'tag': ['dc:vla']
            }
        )
        self.access.update_consumer_node(
            node_id='2',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(wait={'for_install': True}),
                    'rollbackable_6.2': Cr(wait={'for_install': True}),
                },
                'tag': ['dc:sas']
            }
        )

        # Wait for version 1.0.0
        self._poll_nodes_target(consumer_name='rollbacker_6', node_ids=['0', '1', '2'], prev_target=EmptyDict(), next_target=target_v1)

        # Create version 2.0.0 of resources
        self.access.create_version(resource_name='rollbackable_6.1')
        self.access.create_version(resource_name='rollbackable_6.2')

        # Version 1.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    ),
                    'rollbackable_6.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    )
                },
                'tag': ['dc:vla']
            }
        )

        # Version 1.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    ),
                    'rollbackable_6.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    )
                },
                'tag': ['dc:vla']
            }
        )

        # Version 1.0.0 is loaded on node '2'
        self.access.update_consumer_node(
            node_id='2',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    ),
                    'rollbackable_6.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                        in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                    )
                },
                'tag': ['dc:sas']
            }
        )

        # Wait for version 2.0.0
        self._poll_nodes_target(consumer_name='rollbacker_6', node_ids=['0', '1', '2'], prev_target=target_v1, next_target=target_v2)

        # Version 2.0.0 is loaded on node '0'
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    ),
                    'rollbackable_6.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    )
                },
                'tag': ['dc:vla']
            }
        )

        # Version 2.0.0 is loaded on node '1'
        self.access.update_consumer_node(
            node_id='1',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    ),
                    'rollbackable_6.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    )
                },
                'tag': ['dc:vla']
            }
        )

        # Version 2.0.0 is loaded on node '2'
        self.access.update_consumer_node(
            node_id='2',
            consumer_name='rollbacker_6',
            state={
                'resource': {
                    'rollbackable_6.1': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    ),
                    'rollbackable_6.2': Cr(
                        wait={'for_install': True},
                        in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                        in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                    )
                },
                'tag': ['dc:sas']
            }
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_6',
            options={
                'tagged': [
                    TaggedNodeOptions(
                        tag=['dc:vla'],
                        node=NodeOptions(update={
                            'to_moment': {'seconds': 1500},
                        })
                    )
                ]
            }
        )

        # Wait for rolled back version on node '0'
        self._poll_node_target(consumer_name='rollbacker_6', node_id='0', prev_target=target_v2, next_target=node_target_after_rollback)

        # Wait for rolled back version on node '1'
        self._poll_node_target(consumer_name='rollbacker_6', node_id='1', prev_target=target_v2, next_target=node_target_after_rollback)

        # Check that the version wasn't changed on node '2'
        response = self.access.get_consumer_node(consumer_name='rollbacker_6', node_id='2')
        self.assertFragmentIn(response, target_v2)

    def test_select_the_oldest_version(self):
        """Simulate and check correctness of command:
        actl --consumer rollbacker_7 --global --update.to_moment 500

        There is only one single-node consumer 'rollbacker_7' and only one resource 'rollbackable_7' with versions:
          * 1.0.0, load_timestamp = 1000
          * 2.0.0, load_timestamp = 2000
        """

        self.access.create_publisher(name='rollbackable_provider_7')
        self.access.create_resource(publisher_name='rollbackable_provider_7', name='rollbackable_7')
        self.access.create_consumer('rollbacker_7')

        target_v1 = {'rollbackable_7': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
        target_v2 = {'rollbackable_7': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}
        target_after_rollback = {'rollbackable_7': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}

        # Create version 1.0.0
        self.access.create_version(resource_name='rollbackable_7')
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_7',
            state={'resource': {'rollbackable_7': Cr(wait={'for_install': True})}}
        )

        # Wait for version 1.0.0
        self._poll_node_target(
            consumer_name='rollbacker_7',
            node_id='0',
            prev_target=EmptyDict(),
            next_target=target_v1
        )

        # Create version 2.0.0
        self.access.create_version(resource_name='rollbackable_7')

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_7',
            state={'resource': {
                'rollbackable_7': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_install={'1.0.0': Cv(done=True, done_time={'seconds': 1000})},
                    in_load={'1.0.0': Cv(done=True, done_time={'seconds': 1000})}
                )
            }}
        )

        # Wait for version 2.0.0
        self._poll_node_target(
            consumer_name='rollbacker_7',
            node_id='0',
            prev_target=target_v1,
            next_target=target_v2
        )

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='rollbacker_7',
            state={'resource': {
                'rollbackable_7': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True, done_time={'seconds': 1000}), '2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_install={'2.0.0': Cv(done=True, done_time={'seconds': 2000})},
                    in_load={'2.0.0': Cv(done=True, done_time={'seconds': 2000})}
                )
            }}
        )

        # Simulate actl call
        self.access.update_consumer(
            name='rollbacker_7',
            options={
                'global': {'update': {'to_moment': {'seconds': 500}}}
            }
        )

        # Wait for rolled back version
        self._poll_node_target(
            consumer_name='rollbacker_7',
            node_id='0',
            prev_target=target_v2,
            next_target=target_after_rollback
        )

    def _poll_nodes_target(self, consumer_name, node_ids, prev_target, next_target, allowed_errors=10):
        for node_id in node_ids:
            self._poll_node_target(
                consumer_name=consumer_name,
                node_id=node_id,
                prev_target=prev_target,
                next_target=next_target,
                allowed_errors=allowed_errors
            )


if __name__ == '__main__':
    env.main()
