import random
import os.path
import os
import time
import shard_util
import google.protobuf.text_format as text_format
import search.base_search.common.protos.shard_info_pb2 as shard_info
import search.base_search.common.protos.docs_tier_pb2 as docs_tier


class Shard:
    _random = None
    _shard_util_binary = ""
    _dir = ""
    _shard = None
    _info = None

    def __init__(self, seed, working_dir):
        self._random = random.Random()
        self._random.seed(seed)

        assert os.path.isdir(working_dir)
        self._dir = os.path.join(working_dir, "shard")
        os.mkdir(self._dir)

        self._init_shard()
        self._write_shard()

    def get_directory(self):
        return self._dir

    def get_random_term(self):
        ind = self._get_random_number(0, len(self._shard) - 1)
        return self._shard[ind]

    def get_info(self):
        return self._info

    def _get_random_number(self, a=0, b=0xFFffFFff):
        return self._random.randint(a, b)

    def _init_shard(self, max_kishka_length=1000):
        self._shard = []
        num_hashes = self._get_random_number(1, 1000)
        hashes = set()
        while len(hashes) < num_hashes:
            hashes.add(self._get_random_number(1, 0xFFffFFffFFffFFff))
        for h in sorted(hashes):
            kishka_length = self._get_random_number(1, max_kishka_length)
            hits = []
            for i in xrange(kishka_length):
                hits.append((self._get_random_number(), self._get_random_number()))
            hits.sort()
            self._shard.append((h, hits))

        self._info = shard_info.TShardInfo()
        self._info.Tier = docs_tier.EDocsTier.WebTier1
        self._info.Partition = self._get_random_number(1, 20)
        self._info.Shard = self._get_random_number(1, 20)
        self._info.Timestamp = int(time.time())
        self._info.RobotRevision = 123
        self._info.RobotBranch = "abc"

    def _write_shard(self):
        shard_util.create_shard(self._shard, self._dir)
        with open(os.path.join(self._dir, "shard.pb.txt"), "w") as f:
            f.write(text_format.MessageToString(self._info))

    def _deserialize_hits(self, hitsContainer):
        deserialized = []
        for hits in hitsContainer:
            deserialized.append(shard_util.deserialize_hits(hits))
        return deserialized
