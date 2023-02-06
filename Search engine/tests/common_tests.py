import urllib
import search.base_search.inverted_index_storage.protos.inverted_index_storage_response_pb2 as response_proto
import search.base_search.common.protos.response_error_pb2 as error_info
import search.base_search.common.protos.docs_tier_pb2 as docs_tier

import helpers


def test_info_request_shard_name(server_handle, shard_handle):
    res = urllib.urlopen("http://localhost:{}/yandsearch?info=get_shard_name".format(server_handle.get_port()))
    assert res.getcode() == 200
    info = shard_handle.get_info()
    shard_name = "{}-{}-{}-{}".format(docs_tier.EDocsTier.Name(info.Tier), info.Partition, info.Shard, info.Timestamp)
    assert res.read() == shard_name


def test_tier_mismatch_error(server_handle, shard_handle):
    request = helpers.request_for_shard_info(shard_handle.get_info())
    request.Tier = docs_tier.EDocsTier.PlatinumTier0

    res = urllib.urlopen("http://localhost:{}/inverted_index_storage".format(server_handle.get_port()), request.SerializeToString())
    assert res.getcode() == 200
    helpers.check_response_is_error_with_reason(res.read(), error_info.EResponseErrorReason.TierMismatchResponseErrorReason)


def test_shard_mismatch_error(server_handle, shard_handle):
    request = helpers.request_for_shard_info(shard_handle.get_info())
    request.Shard = shard_handle.get_info().Shard - 1

    res = urllib.urlopen("http://localhost:{}/inverted_index_storage".format(server_handle.get_port()), request.SerializeToString())
    assert res.getcode() == 200
    helpers.check_response_is_error_with_reason(res.read(), error_info.EResponseErrorReason.ShardMismatchResponseErrorReason)


def test_timestamp_mismatch_error(server_handle, shard_handle):
    request = helpers.request_for_shard_info(shard_handle.get_info())
    request.Timestamp = shard_handle.get_info().Timestamp - 1

    res = urllib.urlopen("http://localhost:{}/inverted_index_storage".format(server_handle.get_port()), request.SerializeToString())
    assert res.getcode() == 200
    helpers.check_response_is_error_with_reason(res.read(), error_info.EResponseErrorReason.TimestampMismatchResponseErrorReason)


def test_request_correct_response(server_handle, shard_handle):
    terms_with_hits = {}
    for i in xrange(10):
        t = shard_handle.get_random_term()
        terms_with_hits[t[0]] = t[1]
    request = helpers.request_for_shard_info(shard_handle.get_info())
    terms = sorted(terms_with_hits.keys())
    terms.insert(0, 0)  # 0 is not a valid hash, we should get an empty string for it
    request.PantherTermHashes.extend(terms)

    res = urllib.urlopen("http://localhost:{}/inverted_index_storage".format(server_handle.get_port()), request.SerializeToString())
    assert res.getcode() == 200
    response = response_proto.TInvertedIndexStorageResponse()
    response.ParseFromString(res.read())
    assert not response.HasField("ErrorInfo")
    assert response.PantherBlobs[0] == ""
    hits = shard_handle._deserialize_hits(response.PantherBlobs)
    assert len(hits) == len(terms)
    assert len(hits[0]) == 0
    for i in xrange(1, len(hits)):
        hits[i] == terms_with_hits[terms[i]]
