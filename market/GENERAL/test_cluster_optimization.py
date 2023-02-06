#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Shop, VClusterTransition
from core.testcase import TestCase, main

from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=1),
            Shop(fesh=2, priority_region=2),
            Shop(fesh=3, priority_region=3),
            Shop(fesh=4, priority_region=4),
            Shop(fesh=5, priority_region=5),
            Shop(fesh=6, priority_region=6),
        ]

        cls.index.offers += [
            Offer(fesh=1, vclusterid=1000001101),
            Offer(fesh=2, vclusterid=1000001102),
            Offer(fesh=1, vclusterid=1000001103),
        ]
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001101, strong_id=1000001102, weak_ids=[1000001103]),
            VClusterTransition(src_id=1000001100, strong_id=1000001102, weak_ids=[1000001103]),
        ]

        cls.index.offers += [
            Offer(fesh=3, vclusterid=1000001104),
            Offer(fesh=3, vclusterid=1000001105),
            Offer(fesh=3, vclusterid=1000001106),
        ]
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001104, strong_id=1000001105, weak_ids=[1000001106]),
        ]

        cls.index.offers += [
            Offer(fesh=4, vclusterid=1000001107),
            Offer(fesh=5, vclusterid=1000001108),
            Offer(fesh=4, vclusterid=1000001109),
            Offer(fesh=4, vclusterid=1000001110),
            Offer(fesh=4, vclusterid=1000001110),
            Offer(fesh=4, vclusterid=1000001111),
            Offer(fesh=4, vclusterid=1000001111),
            Offer(fesh=4, vclusterid=1000001111),
        ]
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001107, strong_id=1000001108, weak_ids=[1000001109, 1000001110, 1000001111]),
        ]

        cls.index.offers += [
            Offer(fesh=6, vclusterid=1000001112),
            Offer(fesh=6, vclusterid=1000001116),
        ]
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001113, strong_id=1000001114, model_delete_timestamp=1459500000000),
            VClusterTransition(src_id=1000001115, strong_id=1000001112, model_delete_timestamp=1459500000000),
            VClusterTransition(src_id=1000001114, strong_id=1000001115, model_delete_timestamp=1459500000001),
        ]
        # this record will be dropped (double deletion)
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001113, strong_id=1000001116, model_delete_timestamp=1459500000001),
        ]

        # empty clusters
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001117, strong_id=1000001118, weak_ids=[1000001119]),
        ]

    def test_modelinfo_single_cluster_CO_enabled(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001101&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001102,
                'deletedId': 1000001101,
            },
        )

    def test_modelinfo_several_clusters_CO_enabled(self):
        response = self.report.request_json(
            'place=modelinfo&vclusterid=1000001101&vclusterid=1000001103&with-rebuilt-model=1&rids=0'
        )
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001102,
                'deletedId': 1000001101,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001103,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'type': 'cluster',
                'deletedId': 1000001103,
            },
        )

    def test_modelinfo_single_cluster_CO_disabled(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001101&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001101,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'type': 'cluster',
                'deletedId': 1000001101,
            },
        )

    def test_modelinfo_several_clusters_CO_disabled(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001101&vclusterid=1000001103&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001101,
            },
        )
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001103,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'type': 'cluster',
                'deletedId': 1000001101,
            },
        )

    def test_modelinfo_nonexistent_cluster(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001100&rids=0')
        self.assertFragmentNotIn(
            response,
            {
                'type': 'cluster',
            },
        )
        response = self.report.request_json('place=modelinfo&vclusterid=1000001100&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001102,
                'deletedId': 1000001100,
            },
        )

    def test_modelinfo_set_region_use_strong(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001104&with-rebuilt-model=1&rids=3')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001105,
                'deletedId': 1000001104,
            },
        )

    def test_modelinfo_set_region_use_weak(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001101&with-rebuilt-model=1&rids=1')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001103,
                'deletedId': 1000001101,
            },
        )

    def test_modelinfo_set_region_use_largest_weak(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001107&with-rebuilt-model=1&rids=4')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001111,
                'deletedId': 1000001107,
            },
        )

    def test_modelinfo_cluster_chains(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001113&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001112,
                'deletedId': 1000001113,
            },
        )
        response = self.report.request_json('place=modelinfo&vclusterid=1000001114&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001112,
                'deletedId': 1000001114,
            },
        )
        response = self.report.request_json('place=modelinfo&vclusterid=1000001115&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(
            response,
            {
                'type': 'cluster',
                'id': 1000001112,
                'deletedId': 1000001115,
            },
        )

    def test_modelinfo_empty_clusters(self):
        response = self.report.request_json('place=modelinfo&vclusterid=1000001117&rids=0')
        self.assertFragmentNotIn(
            response,
            {
                'type': 'cluster',
            },
        )
        response = self.report.request_json('place=modelinfo&vclusterid=1000001117&with-rebuilt-model=1&rids=0')
        self.assertFragmentNotIn(
            response,
            {
                'type': 'cluster',
            },
        )

    @classmethod
    def prepare_deleted_cluster_equals_new_cluster(cls):
        # По алгоритму кластер 1000001120 является наследником самого себя, т.к.
        # кластер 1000001121 пустой
        cls.index.offers += [
            Offer(fesh=1, vclusterid=1000001120),
        ]
        cls.index.vcluster_transitions += [
            VClusterTransition(src_id=1000001120, strong_id=1000001121, weak_ids=[1000001121, 1000001120]),
        ]

    def test_deleted_cluster_equals_new_cluster(self):
        """В случае, когда deletedId совпадает с изначальным кластером, не считать
        его наследником самого себя

        https://st.yandex-team.ru/MARKETOUT-30080
        """

        response = self.report.request_json('place=modelinfo&vclusterid=1000001120&with-rebuilt-model=1&rids=0')
        self.assertFragmentIn(response, {'type': 'cluster', 'id': 1000001120, 'deletedId': NoKey('deletedId')})


if __name__ == '__main__':
    main()
