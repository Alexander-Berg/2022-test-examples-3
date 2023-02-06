# -*- coding: utf-8 -*-

from common.tester.testcase import TestCase
from travel.rasp.morda.morda.views.thread import get_route_uid_by_thread_uid


class TestGetRouteUidByThreadUid(TestCase):
    thread_uid_route_uid_pairs = [
        ('EI-5090_2_c476_12', 'EI-5090_c476_12'),
        ('EY-8084_2_c1111_5', 'EY-8084_c1111_5'),
        ('2653k_1_f9752758t9692685_76', '2653k_f9752758t9692685_76'),
        ('440_4_f9815053t9821740_175__as-175', '440_f9815053t9821740_175'),
        ('444', None),
        ('', None),
        ('as-', None),
        ('1_as-', None),
    ]

    def test_get_route_uid_by_thread_uid(self):
        for thread_uid, route_uid in self.thread_uid_route_uid_pairs:
            assert route_uid == get_route_uid_by_thread_uid(thread_uid)
