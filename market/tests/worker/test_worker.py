import conftest
import yt.wrapper as yt
import time
import requests
import concurrent.futures
import kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api as pqlib
from market.amore.proto import market_amore_service_pb2 as autostrategies_pb
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from worker_utils import parse_amore_data, parse_amore_data_with_datasource_id
from worker_utils import StrategyType


LOGBRKER_WAIT_TIMEOUT = 30


def _shop2feed(shop_id):
    return 1000000 + shop_id


def _shop2Business(shop_id):
    return 2000000 + shop_id


class TestPipeline:
    def _pl_insert_row(self, timestamp, shop_id, autostrategy_id, offer_id, type, bid):
        table = self.fix.worker_cfg_pb.PriceLabs.Table
        yt.insert_rows(table, [{'timestamp': timestamp,
                                'shop_id': shop_id,
                                'autostrategy_id': autostrategy_id,
                                'feed_id': _shop2feed(shop_id),
                                'offer_id': str(offer_id),
                                'business_id': _shop2Business(shop_id),
                                'type' : type,
                                'bid' : bid
                                }])

    def _pl_insert_blue_vendor_row(self, timestamp, vendor_id, shop_id, autostrategy_id, offer_id, datasource_id):  # will be two vendor tables: white and blue

        table = self.fix.worker_cfg_pb.PriceLabsVendors.Table
        yt.insert_rows(table, [{'timestamp': timestamp,
                                'shop_id': shop_id,
                                'id': vendor_id,
                                'autostrategy_id': autostrategy_id,
                                'feed_id': _shop2feed(vendor_id),
                                'offer_id': str(offer_id),
                                'business_id': _shop2Business(shop_id),
                                'datasource_id': datasource_id,
                                'type' : 0,
                                'bid' : 0
                                }])

    def _read_logbroker_messages(self, timeout, read_beru_data=False):
        messages = []

        print("[TEST] processing logbroker messages")

        while True:
            try:
                if self.lb_last_event is None:
                    self.lb_last_event = self.fix.lb_consumer.next_event()
                result = self.lb_last_event.result(timeout=timeout)
                self.lb_last_event = None
            except concurrent.futures.TimeoutError:
                break

            if result.type == pqlib.ConsumerMessageType.MSG_DATA:
                print("[TEST] MSG_DATA")
                for batch in result.message.data.message_batch:
                    for message in batch.message:
                        messages.append(message.data)
                self.fix.lb_consumer.commit(result.message.data.cookie)
            else:
                pass
        ret = dict()

        for data in messages:
            print("[TEST] 1")
            message = DatacampMessage()
            message.ParseFromString(data)
            for batch in message.united_offers:
                print("[TEST] 2")
                for united in batch.offer:
                    print("[TEST] 3")
                    for offer in united.service.values():
                        shop_id = offer.identifiers.shop_id
                        offer_id = offer.identifiers.offer_id
                        feed_id = offer.identifiers.feed_id
                        business_id = offer.identifiers.business_id
                        print("[TEST] read offer: business_id -{} feed_id-{} offer_id-{} shop_id -{}".format(business_id, feed_id, offer_id, shop_id))
                        amore_as_bundle = parse_amore_data(offer.bids.amore_data.value) if not read_beru_data else parse_amore_data_with_datasource_id(offer.bids.amore_beru_vendor_data.value)
                        k = (shop_id, offer_id, feed_id)
                        assert k not in ret
                        ret[k] = amore_as_bundle
        return ret

    def test_local_worker(self, local_worker_fixture: conftest.LocalWorker):
        self.lb_last_event = None
        self.fix = local_worker_fixture
        self.fix.clear_pg_db()

        cpo = autostrategies_pb.TAutostrategies.CpoParams
        drr = autostrategies_pb.TAutostrategies.DrrParams
        positional = autostrategies_pb.TAutostrategies.PositionalParams
        cpa = autostrategies_pb.TAutostrategies.CpaParams

        ts1 = int(1000 * time.time())
        ts2 = ts1 - 999

        # Add offers
        self._pl_insert_row(ts1, 1, 11, 2, 0, 0)  # CPO
        self._pl_insert_row(ts1, 1, 11, 3, 0, 0)  # CPO
        self._pl_insert_row(ts1, 1, 12, 4, 0, 0)  # DRR
        self._pl_insert_row(ts2, 2, 21, 5, 0, 0)  # CPO
        self._pl_insert_row(ts2, 2, 22, 6, 0, 0)  # Positional
        self._pl_insert_row(ts2, 2, 13, 7, 0, 0)  # CPA
        self._pl_insert_row(ts2, 2, 0, 18, 1, 300)  # Campaign
        self._pl_insert_row(ts2, 3, 0, 19, 1, 300)  # Campaign

        as_pb = autostrategies_pb.TAutostrategies()
        shop1 = as_pb.shops.add(shop_id=1, n_offers=3, ts_create=ts1)
        shop1.as_params.add(uid=11).cpo.CopyFrom(cpo(cpo=22))
        shop1.as_params.add(uid=12).drr.CopyFrom(drr(drr=33))

        shop2 = as_pb.shops.add(shop_id=2, n_offers=4, ts_create=ts2)
        shop2.as_params.add(uid=21).cpo.CopyFrom(cpo(cpo=14))
        shop2.as_params.add(uid=22).positional.CopyFrom(positional(position=2, max_bid=15))
        shop2.as_params.add(uid=13).cpa.CopyFrom(cpa(cpa=31))

        as_pb.shops.add(shop_id=3, n_offers=1, ts_create=ts2)

        api_port = self.fix.api_cfg_pb.Core.Server.Port

        r = requests.post('http://localhost:' + str(api_port) + '/post_request?what=add_proto',
                          data=as_pb.SerializeToString())
        assert r.status_code == 200

        lb_offers = self._read_logbroker_messages(timeout=LOGBRKER_WAIT_TIMEOUT)
        assert len(lb_offers) == 8

        # (shop_id, offfer_id, feed_id)
        k = (2, '18', _shop2feed(2))
        assert k in lb_offers

        assert lb_offers[k].Id == 0
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpa
        assert lb_offers[k].Production.Cpa == 300

        # (shop_id, offfer_id, feed_id)
        k = (3, '19', _shop2feed(3))
        assert k in lb_offers

        assert lb_offers[k].Id == 0
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpa
        assert lb_offers[k].Production.Cpa == 300

        # (shop_id, offfer_id, feed_id)
        k = (1, '2', _shop2feed(1))
        assert k in lb_offers
        assert lb_offers[k].Id == 11
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpo
        assert lb_offers[k].Production.Cpo == 22

        k = (1, '3', _shop2feed(1))
        assert k in lb_offers
        assert lb_offers[k].Id == 11
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpo
        assert lb_offers[k].Production.Cpo == 22

        k = (1, '4', _shop2feed(1))
        assert k in lb_offers
        assert lb_offers[k].Id == 12
        assert lb_offers[k].Experimental.as_type == StrategyType.Drr  # See market/amore/service/worker2/src/pipeline_filters/compute_task_optim_drr.cpp:118
        assert lb_offers[k].Production.as_type == StrategyType.Drr
        assert lb_offers[k].Production.Drr == 33

        k = (2, '5', _shop2feed(2))
        assert k in lb_offers
        assert lb_offers[k].Id == 21
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpo
        assert lb_offers[k].Production.Cpo == 14

        k = (2, '6', _shop2feed(2))
        assert k in lb_offers
        assert lb_offers[k].Id == 22
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 2
        assert lb_offers[k].Production.MaxBid == 15

        k = (2, '7', _shop2feed(2))
        assert k in lb_offers
        assert lb_offers[k].Id == 13
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpa
        assert lb_offers[k].Production.Cpa == 31

        # update shop 2
        as_pb = autostrategies_pb.TAutostrategies()
        shop2 = as_pb.shops.add(shop_id=2, n_offers=2, ts_create=ts2)
        shop2.as_params.add(uid=21).cpo.CopyFrom(cpo(cpo=11))
        shop2.as_params.add(uid=22).positional.CopyFrom(positional(position=1, max_bid=19))
        shop2.as_params.add(uid=13).cpa.CopyFrom(cpa(cpa=29))
        # Если не добавить строчку выше, то результаты выглядят странно: падает assert len(lb_offers) == 2

        api_port = self.fix.api_cfg_pb.Core.Server.Port
        r = requests.post('http://localhost:' + str(api_port) + '/post_request?what=add_proto',
                          data=as_pb.SerializeToString())
        assert r.status_code == 200

        lb_offers = self._read_logbroker_messages(timeout=LOGBRKER_WAIT_TIMEOUT)
        assert len(lb_offers) == 4

        k = (2, '5', _shop2feed(2))
        assert k in lb_offers
        assert lb_offers[k].Id == 21
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpo
        assert lb_offers[k].Production.Cpo == 11

        k = (2, '6', _shop2feed(2))
        assert k in lb_offers
        assert lb_offers[k].Id == 22
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 1
        assert lb_offers[k].Production.MaxBid == 19

        k = (2, '7', _shop2feed(2))
        assert k in lb_offers
        assert lb_offers[k].Id == 13
        assert lb_offers[k].Experimental.as_type == StrategyType.Disabled
        assert lb_offers[k].Production.as_type == StrategyType.Cpa
        assert lb_offers[k].Production.Cpa == 29

        """
        Test blue vendor bids
        """

        # self, timestamp, vendor_id, shop_id, autostrategy_id, offer_id, datasource_id
        self._pl_insert_blue_vendor_row(ts2, 3, 233, 31,  7, 123)  # Positional
        self._pl_insert_blue_vendor_row(ts2, 3, 233, 32,  8, 321)  # Positional one strategy but two shops
        self._pl_insert_blue_vendor_row(ts2, 3, 666, 32,  9, 4321)  # Positional one strategy but two shops
        self._pl_insert_blue_vendor_row(ts2, 4, 666, 33, 10, 5321)  # Positional

        as_pb = autostrategies_pb.TAutostrategies()

        shop1 = as_pb.shops.add(shop_id=3, n_offers=3, ts_create=ts2)
        shop1.as_params.add(uid=31).positional.CopyFrom(positional(position=1, max_bid=100))
        shop1.as_params.add(uid=32).positional.CopyFrom(positional(position=2, max_bid=50))

        api_port = self.fix.api_cfg_pb.Core.Server.Port
        r = requests.post('http://localhost:' + str(api_port) + '/post_request?what=add_vendor',
                          data=as_pb.SerializeToString())
        assert r.status_code == 200

        as_pb = autostrategies_pb.TAutostrategies()

        shop1 = as_pb.shops.add(shop_id=4, n_offers=1, ts_create=ts2)
        shop1.as_params.add(uid=33).positional.CopyFrom(positional(position=1, max_bid=10))

        api_port = self.fix.api_cfg_pb.Core.Server.Port
        r = requests.post('http://localhost:' + str(api_port) + '/post_request?what=add_vendor',
                          data=as_pb.SerializeToString())
        assert r.status_code == 200

        lb_offers = self._read_logbroker_messages(timeout=LOGBRKER_WAIT_TIMEOUT, read_beru_data=True)
        assert len(lb_offers) == 4

        # (shop_id, offfer_id, feed_is)
        k = (233, '7', _shop2feed(3))
        assert k in lb_offers
        assert lb_offers[k].Id == 31
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 1
        assert lb_offers[k].Production.MaxBid == 100
        assert lb_offers[k].DatasourceId == 123

        k = (233, '8', _shop2feed(3))
        assert k in lb_offers
        assert lb_offers[k].Id == 32
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 2
        assert lb_offers[k].Production.MaxBid == 50
        assert lb_offers[k].DatasourceId == 321

        k = (666, '9', _shop2feed(3))
        assert k in lb_offers
        assert lb_offers[k].Id == 32
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 2
        assert lb_offers[k].Production.MaxBid == 50
        assert lb_offers[k].DatasourceId == 4321

        k = (666, '10', _shop2feed(4))
        assert k in lb_offers
        assert lb_offers[k].Id == 33
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 1
        assert lb_offers[k].Production.MaxBid == 10
        assert lb_offers[k].DatasourceId == 5321

        # update ony 31 strategy, 32 - deleted
        as_pb = autostrategies_pb.TAutostrategies()

        shop1 = as_pb.shops.add(shop_id=3, n_offers=3, ts_create=ts2)
        shop1.as_params.add(uid=31).positional.CopyFrom(positional(position=42, max_bid=42))

        r = requests.post('http://localhost:' + str(api_port) + '/post_request?what=add_vendor',
                          data=as_pb.SerializeToString())
        assert r.status_code == 200

        lb_offers = self._read_logbroker_messages(timeout=LOGBRKER_WAIT_TIMEOUT, read_beru_data=True)
        assert len(lb_offers) == 3

        # (shop_id, offfer_id, feed_is)
        k = (233, '7', _shop2feed(3))
        assert k in lb_offers
        assert lb_offers[k].Id == 31
        assert lb_offers[k].Production.as_type == StrategyType.Positional
        assert lb_offers[k].Production.Position == 42
        assert lb_offers[k].Production.MaxBid == 42
        assert lb_offers[k].DatasourceId == 123

        k = (233, '8', _shop2feed(3))
        assert k in lb_offers
        assert lb_offers[k].Production.as_type == StrategyType.Disabled

        k = (666, '9', _shop2feed(3))
        assert k in lb_offers
        assert lb_offers[k].Production.as_type == StrategyType.Disabled
