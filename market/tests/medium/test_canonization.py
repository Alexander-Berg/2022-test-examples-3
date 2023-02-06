# import pytest
from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TEventMessage,
    TImageResponse,
)
from .conftest import (
    launch,
    image_msg,
    generate_mds_json,
    request_msg,
    copier_response,
    launch_resharder,
)

BASE_YT_DIR = '//tmp'


def test_canonization(stand16, resharder_stand16):
    stand = stand16
    resharder_stand = resharder_stand16

    request_data = {}
    video_request_data = {}
    image_data = {}
    event_data = {}
    resharded_data = {}
    offsets = {i: 0 for i in range(stand.shard_count)}

    request_data = {0: [
        request_msg(
            url='http://www.example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        ),
        request_msg(
            url='https://www.example.com/new_img1/',
            namespace='turbo',
            offer='https://example.com/offer2'
        ),
        request_msg(
            url='https://www.m.Example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer3'
        ),
        request_msg(
            url='https://example.com/new_img1',
            namespace='turbo2',
            offer='https://example.com/offer3'
        ),
        request_msg(
            url='https://example.com/new_img2',
            namespace='turbo',
            offer='https://example.com/offer4'
        ),
    ]}

    resharder_stand.request_yt_queue["queue"].write(request_data)
    launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data)
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, offsets[i], 100)["rows"]
        offsets[i] += len(resharded_data[i])

    # launch processor, should request img in samovar
    launch(stand, resharded_data)

    # provide image response
    image_data = {0: [
        image_msg(
            url='http://www.example.com/new_img1',
            mds_json=generate_mds_json(namespace='turbo', name='pic1'),
            compress=True
        )
    ]}
    resharder_stand.image_yt_queue["queue"].write(image_data)
    launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data)
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, offsets[i], 100)["rows"]
        offsets[i] += len(resharded_data[i])

    # launch processor, should request copier and provide responses
    launch(stand, resharded_data)

    # provide copier response
    event_data = {0: [
        copier_response(
            url='http://www.example.com/new_img1',
            mds_json=generate_mds_json(namespace='turbo2', name='pic2'),
        ).SerializeToString()
    ]}
    resharder_stand.event_yt_queue["queue"].write(event_data)
    launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data)
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, offsets[i], 100)["rows"]
        offsets[i] += len(resharded_data[i])

    # launch processor last time, should provide response for another mds namespace
    launch(stand, resharded_data)

    responses = []
    for i in range(stand.shard_count):
        for row in stand.output_yt_queue["queue"].read(i, 0, 100)["rows"]:
            event = TEventMessage()
            event.ParseFromString(row)
            if event.Type != EMessageType.IMAGE_RESPONSE:
                continue  # skip samovar request events

            inner = TImageResponse()
            inner.ParseFromString(event.Body)

            responses.append((inner.Offer.OfferId, inner.Url, inner.MdsInfo.MdsId.Namespace, inner.MdsInfo.MdsId.ImageName))

    assert sorted(responses) == [
        ('https://example.com/offer', 'http://www.example.com/new_img1', 'turbo', 'pic1'),
        ('https://example.com/offer2', 'https://www.example.com/new_img1/', 'turbo', 'pic1'),
        ('https://example.com/offer3', 'https://example.com/new_img1', 'turbo2', 'pic2'),
        ('https://example.com/offer3', 'https://www.m.Example.com/new_img1', 'turbo', 'pic1'),
    ]

    # check sharding:
    # all input events related to the same url (according to canonization) MUST be in the same shard
    # with current sharding algorithm and current shard count, all events related to https://example.com/new_img1 happend to be in 12th shard
    # https://example.com/new_img2 - in 3rd shard
    # требует вычитвания всех сообщеий за период теста
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, 0, 100)["rows"]
    for i in range(16):
        if i not in (3, 12):
            assert len(resharded_data[i]) == 0
    assert len(resharded_data[12]) >= 3  # resharder can pack 4 image requests into one message + 2 separete image data messages
    assert len(resharded_data[3]) == 1  # 1 image request
