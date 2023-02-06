from concurrent import futures
from google.protobuf.text_format import Parse

from tools import *

import grpc
import logging
import os
import travel.proto.commons_pb2 as commons_pb2
import travel.hotels.proto2.hotels_pb2 as hotels_pb2
import travel.hotels.proto2.bus_messages_pb2 as bus_messages_pb2
import travel.hotels.proto2.offer_search_service_pb2_grpc as offer_search_service_pb2_grpc
import google.protobuf.wrappers_pb2 as wrappers_pb2
import yatest.common

import random
from datetime import timedelta

READ_OC_LOG = False

LOG = logging.getLogger(__name__)


class SearcherSession(object):
    def __init__(self):
        self.actual_requests = []
        self.expected_requests = []

    def expect_request(self, partner_ids, **params):
        self.expected_requests.append((partner_ids, params))

    def add_actual_request(self, request):
        self.actual_requests.append(request)

    def check(self):
        expected_count = len(self.expected_requests)
        actual_count = len(self.actual_requests)
        if expected_count != actual_count:
            raise Exception('expected {} searcher requests but got {}'.format(expected_count, actual_count))
        [self._check_request(exp_ids, exp_params, actual)
            for (exp_ids, exp_params), actual in zip(self.expected_requests, self.actual_requests)]

    def _check_request(self, partner_ids, expected, actual):
        expected_ids = set(partner_ids)
        actual_ids = {'{}.{}'.format(s.HotelId.PartnerId, s.HotelId.OriginalId) for s in actual.Subrequest}
        if expected_ids != actual_ids:
            raise Exception('expected {} searcher subrequests but got {}'.format(expected_ids, actual_ids))
        [self._check_subrequest(expected, s) for s in actual.Subrequest]

    def _check_subrequest(self, expected, actual):
        for field, value in expected.items():
            assert value == getattr(actual, field)


class SearcherServer(offer_search_service_pb2_grpc.OfferSearchServiceV1Servicer):
    def __init__(self, port, message_bus):
        grpc_server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        offer_search_service_pb2_grpc.add_OfferSearchServiceV1Servicer_to_server(self, grpc_server)
        grpc_server.add_insecure_port('[::]:{}'.format(port))
        grpc_server.start()
        self.grpc_server = grpc_server
        self.port = port
        self.message_bus = message_bus
        self.now = message_bus.now
        self.session = None
        self.unexpected_request_count = 0
        self.offercache = None

    def set_offercache(self, oc):
        self.offercache = oc

    def stop(self):
        self.grpc_server.stop(0)
        assert self.session is None
        if self.unexpected_request_count > 0:
            raise Exception("Had %s unexpected requests" % self.unexpected_request_count)

    def start_session(self):
        assert self.session is None
        self.session = SearcherSession()
        return self.session

    def finish_session(self):
        assert self.session is not None
        session = self.session
        self.session = None
        try:
            session.check()
        except Exception as e:
            if READ_OC_LOG:
                log = self.offercache.read_log().split('\n')
                log = '\n'.join(log[-30:])
                raise Exception("Problem: %s, offercache log: \n%s" % (str(e), log))
            else:
                raise

    def SearchOffers(self, request, context):
        assert request.Sync is False, 'sync requests to searcher are not allowed'
        if self.session is None:
            # gRPC will catch exception, and test won't fail, if no additional actions done
            self.unexpected_request_count += 1
            raise Exception('no searcher session activated, but SearchOffers got')
        self.session.add_actual_request(request)

        for subrequest in request.Subrequest:
            response = hotels_pb2.TSearchOffersRsp(Placeholder={})
            yt_response = bus_messages_pb2.TSearcherMessage(Request=subrequest, Response=response)
            self.message_bus.write('ru.yandex.travel.hotels.TSearcherMessage', [yt_response])

        subrequest_count = len(request.Subrequest)
        placeholder = hotels_pb2.TPlaceholder()
        subresponse = hotels_pb2.TSearchOffersRsp(Placeholder=placeholder)
        grpc_response = hotels_pb2.TSearchOffersRpcRsp(Subresponse=[subresponse] * subrequest_count)
        return grpc_response

    def Ping(self, request, context):
        return hotels_pb2.TPingRpcRsp(IsReady=True)

    @staticmethod
    def message_from_file(fn):
        with open(fn) as f:
            text = f.read()
        message = bus_messages_pb2.TSearcherMessage()
        return Parse(text, message)

    def generate_random_message(self, idx, checkin, nights, occup, p_id, op_ids):
        checkout = checkin + timedelta(days=nights)
        req = hotels_pb2.TSearchOffersReq(
            HotelId=hotels_pb2.THotelId(PartnerId=p_id, OriginalId="random"),
            CheckInDate=format_date(checkin),
            CheckOutDate=format_date(checkout),
            Occupancy=occup,
            Currency=commons_pb2.C_RUB,
            RequestClass=hotels_pb2.RC_INTERACTIVE,
            Id="Req.Random.%s" % idx
        )
        resp = hotels_pb2.TSearchOffersRsp(Offers=hotels_pb2.TOfferList())

        for y in range(random.randint(15, 30)):
            op_id = random.choice(op_ids)
            price = hotels_pb2.TPriceWithDetails(Amount=random.randint(10, 15), Currency=commons_pb2.C_RUB)
            resp.Offers.Offer.add(
                Id="Offer.Random.%s.%s" % (idx, y),
                OperatorId=op_id,
                Price=price,
                Capacity='==%s' % occup,
                Pansion=hotels_pb2.PT_BB,
                FreeCancellation=wrappers_pb2.BoolValue(value=True)
            )
        return bus_messages_pb2.TSearcherMessage(Request=req, Response=resp)

    def write_initial_messages(self, oc_app):
        messages_dir = yatest.common.source_path('travel/hotels/offercache/tests/initial_messages')
        messages = []
        for fn in os.listdir(messages_dir):
            LOG.info("Reading message file %s", fn)
            messages.append(SearcherServer.message_from_file(os.path.join(messages_dir, fn)))
        idx = 0
        for checkin, nights, occup, ages in iterate_random_params():
            for p_id, op_ids in oc_app.partner2operators.items():
                messages.append(self.generate_random_message(idx, checkin, nights, occup, p_id, op_ids))
                idx += 1
        LOG.info("Generated %s random messages" % idx)
        self.message_bus.write('ru.yandex.travel.hotels.TSearcherMessage', messages)
