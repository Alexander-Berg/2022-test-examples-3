# coding: utf-8
import os
import StringIO
from hamcrest.core.base_matcher import BaseMatcher

from market.proto.indexer import awaps_pb2
from market.pylibrary.snappy_protostream import pbsn_reader

_AWAPS_OFFERS_MAGIC = 'AWOF'


class HasCountOfAwapsOffer(BaseMatcher):
    def __init__(self, expected_offers_count):
        self.expected_offers_count = expected_offers_count
        self.count = 0

    def _matches(self, env):
        s3_bucket = env.s3_bucket

        for file, content in s3_bucket.files.iteritems():
            name = os.path.basename(file)
            if name.startswith(env.offers_prefix):
                for offer in pbsn_reader(StringIO.StringIO(content), _AWAPS_OFFERS_MAGIC, awaps_pb2.Offer):
                    self.count += 1

        return self.count == self.expected_offers_count

    def describe_to(self, description):
        msg = 'Count of offers is equal'
        description.append_text(msg)

    def describe_mismatch(self, env, description):
        msg = 'different count of offers in s3 bucket({0}) and expected({1})'.format(self.count, self.expected_offers_count)
        description.append_text(msg)
