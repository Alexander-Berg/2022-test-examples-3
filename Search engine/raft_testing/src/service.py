import os
import sys
from pysyncobj import replicated_sync

from search.horadric2.proto.structures import unistat_pb2

from search.martylib.raft import BaseRaftConsumer, traced_replicated, exception_handler
from search.martylib.unistat.utils import clean_data, merge_data
from search.martylib.modules import ServiceAndLoopModule
from search.martylib.threading_utils.interval import Interval

from search.raft_testing.services.raft import RaftInterface
from search.raft_testing.proto import structures_pb2


class ReplicatedList(BaseRaftConsumer):

    def __init__(self):
        super().__init__('main-list')
        self.storage = []

    @exception_handler
    @replicated_sync
    @traced_replicated
    def append(self, data):
        self.storage.append(data)

    @exception_handler
    @replicated_sync
    @traced_replicated
    def pop(self):
        self.storage.pop()

    @exception_handler
    @replicated_sync
    @traced_replicated
    def clear(self):
        self.storage.clear()


class RaftTester(RaftInterface):

    storage = ReplicatedList()

    def __init__(self):
        self.loop = Interval(sleep_interval=1, target=lambda: self.add_random_data(structures_pb2.AddRandomDataRequest(), None))

    def add_random_data(self, request, context):
        request.size = request.size or 1024

        self.storage.append(os.urandom(request.size), timeout=30)

    def clear_data(self, request, context):
        self.storage.clear()

    def unistat(self, request=None, context=None):
        data = super().unistat(request, context)
        additional_data = unistat_pb2.UnistatData()

        additional_data.numerical['data-size_ammv'] = sum(map(len, self.storage.storage))

        return merge_data(data, clean_data(additional_data))


class RaftTesterModule(ServiceAndLoopModule):

    @classmethod
    def get_loops(cls):
        yield cls._get_service().loop

    @classmethod
    def get_service(cls):
        return RaftTester()
