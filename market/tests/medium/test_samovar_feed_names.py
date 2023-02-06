# coding: utf-8

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TEventMessage,
)

from robot.protos.crawl.compatibility.feeds_pb2 import TFeedExt

from .conftest import (
    pack_message,
    request_event,
    launch
)

BASE_YT_DIR = '//tmp'


def test_feed_names(stand):
    # request images in different namespaces
    # first 2 ns have special feed name in config all other must be data-camp-images
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='yabs_performance',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img2',
            namespace='goods_pic',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img3',
            namespace='marketpic',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img4',
            namespace='marketpictesting',
            offer='https://example.com/offer'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert len(result["rows"]) == 4
    actual_reqs = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.SAMOVAR_REQUEST
        inner = TFeedExt()
        inner.ParseFromString(event.Body)
        actual_reqs.add((inner.Url, inner.FeedName))

    expected = set([
        ('https://example.com/new_img1', 'data-camp-images-yabs_performance'),
        ('https://example.com/new_img2', 'data-camp-images-goods'),
        ('https://example.com/new_img3', 'data-camp-images'),
        ('https://example.com/new_img4', 'data-camp-images'),
    ])
    assert actual_reqs == expected
