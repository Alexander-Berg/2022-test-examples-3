# coding: utf-8

import os

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TEventMessage,
    TVideoRequest,
    TVideoResponse,
)
from ads.bsyeti.big_rt.py_test_lib import (
    extract_all_states_from_simple_table,
)
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import (
    TPicrobotState
)

from .conftest import (
    launch,
    video_request_event,
    video_response_event,
    pack_message,
    video_request_msg,
    launch_resharder,
)

BASE_YT_DIR = '//tmp'


def test_resharder(stand16, resharder_stand16):
    stand = stand16
    resharder_stand = resharder_stand16

    request_data = {}
    video_request_data = {}
    image_data = {}
    event_data = {}
    resharded_data = {}

    video_request_data = {0: [
        video_request_msg(
            url='http://www.example.com/new_vid1',
            namespace='direct',
            offer='https://example.com/offer'
        ),
        video_request_msg(
            url='https://www.example.com/new_vid1/',
            namespace='direct',
            offer='https://example.com/offer2'
        ),
        video_request_msg(
            url='https://www.m.Example.com/new_vid1',
            namespace='direct',
            offer='https://example.com/offer3'
        ),
        video_request_msg(
            url='https://example.com/new_vid1',
            namespace='turbo',
            offer='https://example.com/offer3'
        ),
        video_request_msg(
            url='https://example.com/new_vid2',
            namespace='direct',
            offer='https://example.com/offer4'
        ),
    ]}

    resharder_stand.video_request_yt_queue["queue"].write(video_request_data)
    launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data)
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, 0, 100)["rows"]

    # new_vid1 in 3rd shard, new_vid2 in 9th shard
    for i in range(16):
        if i not in (3, 9):
            assert len(resharded_data[i]) == 0
    assert len(resharded_data[3]) == 1
    assert len(resharded_data[9]) == 1


def test_processor_request(stand):
    data = {0: [
        pack_message(video_request_event(
            url='https://example.com/new_vid',
            namespace='direct',
            offer='https://example.com/offer'
        )),
        pack_message(video_request_event(
            url='https://example.com/new_vid',
            namespace='direct',
            offer='https://example.com/offer'
        )),  # second request will be skipped (not should crawl)
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    data = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)

    state = TPicrobotState()
    state.ParseFromString(data['https://example.com/new_vid'])

    assert len(state.VideoRequestInfo.Requests) == 1
    assert state.VideoRequestInfo.Requests[0].Namespace == 'direct'
    assert state.VideoRequestInfo.Requests[0].Timestamp > 0
    assert state.VideoRequestInfo.Requests[0].Url == 'https://example.com/new_vid'
    assert state.VideoRequestInfo.Requests[0].Offer.OfferId == 'https://example.com/offer'

    assert state.VideoRequestInfo.SendCount == 1
    assert state.VideoRequestInfo.SendTimestamp > 0
    assert state.VideoRequestInfo.NextAttemptTimestamp == state.VideoRequestInfo.SendTimestamp + 6 * 3600

    output_offset = 0
    result = stand.copier_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert len(result["rows"]) == 1
    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.VIDEO_REQUEST
    inner = TVideoRequest()
    inner.ParseFromString(event.Body)
    assert inner.Url == 'https://example.com/new_vid'
    assert inner.Namespace == 'direct'


def test_processor_response(stand):
    data = {0: [
        pack_message(video_response_event(
            url='https://example.com/new_vid',
            namespace='direct',
            offer='https://example.com/offer',
            creativeId=123
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    data = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)

    state = TPicrobotState()
    state.ParseFromString(data['https://example.com/new_vid'])

    assert not state.HasField('VideoRequestInfo')

    assert len(state.VideoInfo) == 1
    assert state.VideoInfo[0].Namespace == 'direct'
    assert state.VideoInfo[0].CreativeId == 123

    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert len(result["rows"]) == 1
    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.VIDEO_RESPONSE
    inner = TVideoResponse()
    inner.ParseFromString(event.Body)
    assert inner.Url == 'https://example.com/new_vid'
    assert inner.VideoInfo.Namespace == 'direct'
    assert inner.VideoInfo.CreativeId == 123


def test_processor_retry(stand):
    def send_video_request(stand):
        data = {0: [
            pack_message(video_request_event(
                url='https://example.com/new_vid',
                namespace='direct',
                offer='https://example.com/offer'
            )),
        ]}
        stand.input_yt_queue["queue"].write(data)
        launch(stand, data)

    def send_video_response(stand):
        data = {0: [
            pack_message(video_response_event(
                url='https://example.com/new_vid',
                namespace='direct',
                offer='https://example.com/offer',
                creativeId=0
            )),
        ]}
        stand.input_yt_queue["queue"].write(data)
        launch(stand, data)

    def assert_copier_request(result):
        assert len(result["rows"]) == 1
        event = TEventMessage()
        event.ParseFromString(result["rows"][0])
        assert event.Type == EMessageType.VIDEO_REQUEST
        inner = TVideoRequest()
        inner.ParseFromString(event.Body)
        assert inner.Url == 'https://example.com/new_vid'
        assert inner.Namespace == 'direct'

    def assert_video_response_in_result(result, i):
        event = TEventMessage()
        event.ParseFromString(result["rows"][i])
        assert event.Type == EMessageType.VIDEO_RESPONSE
        inner = TVideoResponse()
        inner.ParseFromString(event.Body)
        assert inner.Url == 'https://example.com/new_vid'
        assert inner.VideoInfo.Namespace == 'direct'

    def assert_video_response(result):
        assert len(result["rows"]) == 1
        assert_video_response_in_result(result, 0)

    def assert_two_video_responses(result):
        assert len(result["rows"]) == 2
        assert_video_response_in_result(result, 0)
        assert_video_response_in_result(result, 1)

    copier_offset = 0
    output_offset = 0

    # In: requesting video

    send_video_request(stand)

    # Out: causes request to copier

    result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(result["rows"])

    assert_copier_request(result)

    # In: providing failed response

    send_video_response(stand)

    # Out: causes 2 responses to output (first is response event itself, second is from request from state)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert_two_video_responses(result)

    # In: requesting video again

    send_video_request(stand)

    # Out: shouldn't crawl, no copier request

    result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(result["rows"])

    assert len(result["rows"]) == 0

    # Out: causes 2 responses to output (first is for request event, second is its processing)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert_two_video_responses(result)

    # Imitating wait for 6 hours

    os.environ['PICROBOT_RECRAWL_DELAY_NOT_USED'] = '1'

    # In: requesting video again

    send_video_request(stand)

    # Out: causes request to copier

    result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(result["rows"])

    assert_copier_request(result)

    # Out: causes single response

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert_video_response(result)
