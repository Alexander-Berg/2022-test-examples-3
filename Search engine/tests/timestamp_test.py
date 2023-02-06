import urllib
import helpers

import search.base_search.inverted_index_storage.protos.inverted_index_storage_response_pb2 as response_proto
import search.base_search.common.protos.response_error_pb2 as error_info

# override index timestamp for all tests in this module
MODULE_SERVER_OPTIONS = {"timestamp" : 12345}


def test_overriden_timestamp(server_handle):
    res = urllib.urlopen("http://localhost:{}/yandsearch?info=get_shard_name".format(server_handle.get_port()))
    assert res.getcode() == 200
    assert res.read().split("-")[3] == str(MODULE_SERVER_OPTIONS["timestamp"])


def test_timestamp_mismatch_error(server_handle, shard_handle):
    request = helpers.request_for_shard_info(shard_handle.get_info())

    res = urllib.urlopen("http://localhost:{}/inverted_index_storage".format(server_handle.get_port()), request.SerializeToString())
    assert res.getcode() == 200
    helpers.check_response_is_error_with_reason(res.read(), error_info.EResponseErrorReason.TimestampMismatchResponseErrorReason)


def test_timestamp_mismatch_no_error_with_overriden_timestamp(server_handle, shard_handle):
    request = helpers.request_for_shard_info(shard_handle.get_info())
    request.Timestamp = MODULE_SERVER_OPTIONS["timestamp"]

    res = urllib.urlopen("http://localhost:{}/inverted_index_storage".format(server_handle.get_port()), request.SerializeToString())
    response = response_proto.TInvertedIndexStorageResponse()
    response.ParseFromString(res.read())
    assert res.getcode() == 200
    assert not response.HasField("ErrorInfo")
