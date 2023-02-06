import search.base_search.inverted_index_storage.protos.inverted_index_storage_request_pb2 as request_proto
import search.base_search.inverted_index_storage.protos.inverted_index_storage_response_pb2 as response_proto

def request_for_shard_info(info):
    request = request_proto.TInvertedIndexStorageRequest()
    request.Tier = info.Tier
    request.Shard = info.Shard
    request.Timestamp = info.Timestamp
    return request


def check_response_is_error_with_reason(responseBody, reason):
    response = response_proto.TInvertedIndexStorageResponse()
    response.ParseFromString(responseBody)
    assert response.HasField("ErrorInfo")
    assert response.ErrorInfo.Reason == reason
