import json

from pycommon import *

class TestShards:
    def setup(self):
        self.__host = Testing.host()
        self.__port = Testing.port()
        self.__raw = RawClient(self.__host, self.__port)
    def teardown(self):
        pass
    def test_xtable_shards_full(self):
        response = self.__raw.get("/xtable/shards_full")
        assert_equal(response.status, 200)
        shards_json = json.loads(response.body)
        assert_greater(len(shards_json['shards']), 0)
        for shard in shards_json['shards']:
            assert_in('id', shard)
            assert_in('start_gid', shard)
            assert_in('end_gid', shard)
            assert_in('master', shard)
            assert_in('replicas', shard)