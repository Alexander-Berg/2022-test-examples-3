#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def test_admin_action_globalstats(self):
        response = self.report.request_json('admin_action=globalstats')
        self.assertFragmentIn(
            response,
            {
                "BallocCurSize": NotEmpty(),
                "BallocCurSizeHR": NotEmpty(),
                "BallocGCSize": NotEmpty(),
                "BallocGCSizeHR": NotEmpty(),
                "ExternalRequestHistoryCacheHits": NotEmpty(),
                "ExternalRequestHistoryCacheAccesses": NotEmpty(),
                "ExternalRequestHistoryRemoteCacheHits": NotEmpty(),
                "ExternalRequestHistoryRemoteCacheAccesses": NotEmpty(),
                "StartupTime": NotEmpty(),
                "GlobalStartupTime": NotEmpty(),
                "AnonymousMemoryConsumptionAfterGlobal": NotEmpty(),
                "MetaActiveThreadCount": NotEmpty(),
                "BaseActiveThreadCount": NotEmpty(),
                "RelevanceCalcerActiveThreadCount": NotEmpty(),
                "ConcurrentPoolThreadCount": NotEmpty(),
                "MetaQueueLength": NotEmpty(),
                "MaxBaseQueueLength": NotEmpty(),
                "LFAllocSize": NotEmpty(),
                "NehHttpOutputConnectionCount": NotEmpty(),
                "NetConnectCount": NotEmpty(),
                "NetAcceptCount": NotEmpty(),
                "NetClusterPeerRecvBytes": NotEmpty(),
                "NetClusterPeerSendBytes": NotEmpty(),
                "NetLogBrokerRecvBytes": NotEmpty(),
                "NetLogBrokerSendBytes": NotEmpty(),
                "NetOtherIncomingRecvBytes": NotEmpty(),
                "NetOtherIncomingSendBytes": NotEmpty(),
                "NetOtherOutgoingRecvBytes": NotEmpty(),
                "NetOtherOutgoingSendBytes": NotEmpty(),
            },
        )


if __name__ == '__main__':
    main()
