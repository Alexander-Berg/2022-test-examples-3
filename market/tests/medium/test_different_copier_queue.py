# coding: utf-8

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TCopierRequest,
    TEventMessage,
    TImageResponse,
    TMetaUpdate,
    TDeleteRequest,
)

from robot.protos.crawl.compatibility.feeds_pb2 import TFeedExt

from .conftest import (
    pack_message,
    request_event,
    launch,
    image_event,
    generate_mds_json,
    pack_messages,
    meta_update_event,
    delete_event,
)

BASE_YT_DIR = '//tmp'


def test_copier_request(stand):
    # request images
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpic',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpictesting',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='copier_separate_namespace',
            offer='https://example.com/offer'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    copier_offset = 0
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    copier_namespace_offset = 0
    copier_namespace_result = stand.copier_queue_namespace_yt_queue["queue"].read(0, copier_namespace_offset, 100)
    copier_namespace_offset += len(copier_namespace_result["rows"])

    assert len(result["rows"]) == 1
    assert len(copier_result["rows"]) == 0
    assert len(copier_namespace_result["rows"]) == 0

    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.SAMOVAR_REQUEST
    inner = TFeedExt()
    inner.ParseFromString(event.Body)
    assert inner.Url == 'https://example.com/new_img1'

    # provide mds_json
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='marketpic'
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    copier_namespace_result = stand.copier_queue_namespace_yt_queue["queue"].read(0, copier_namespace_offset, 100)
    copier_namespace_offset += len(copier_namespace_result["rows"])

    # should provide response for marketpic namespace and request copier to move into another namespaces
    assert len(result["rows"]) == 1
    responses = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE

        inner = TImageResponse()
        inner.ParseFromString(event.Body)

        responses.add((inner.Offer.OfferId, inner.MdsInfo.MdsId.Namespace))

    expected = set([
        ('https://example.com/offer', 'marketpic'),
    ])
    assert responses == expected

    # check copier common queue
    assert len(copier_result["rows"]) == 1
    copier_requests = set()
    for row in copier_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_REQUEST

        inner = TCopierRequest()
        inner.ParseFromString(event.Body)

        copier_requests.add((inner.Url, inner.Context.MdsNamespace))

    expected = set([
        ('https://example.com/new_img1', 'marketpictesting'),
    ])
    assert copier_requests == expected

    # check copier_namespace queue
    assert len(copier_namespace_result["rows"]) == 1
    copier_requests = set()
    for row in copier_namespace_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_REQUEST

        inner = TCopierRequest()
        inner.ParseFromString(event.Body)

        copier_requests.add((inner.Url, inner.Context.MdsNamespace))

    expected = set([
        ('https://example.com/new_img1', 'copier_separate_namespace'),
    ])
    assert copier_requests == expected


def test_meta_updates(stand):
    # request images in different namespaces
    data = {0: [
        pack_messages([
            request_event(
                url='https://example.com/new_img1',
                namespace='marketpic',
                offer='https://example.com/offer'
            ),
            image_event(
                url='https://example.com/new_img1',
                mds_json=generate_mds_json(
                    namespace='marketpic',
                    group_id=456,
                    name='pic_update',
                )
            ),
            meta_update_event(
                url='https://example.com/new_img1',
                namespace='marketpic'
            ),
            meta_update_event(
                url='https://example.com/new_img2',
                namespace='copier_separate_namespace',
                group_id=555,
                name='pic_new',
            ),
        ]),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    copier_offset = 0
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    copier_namespace_offset = 0
    copier_namespace_result = stand.copier_queue_namespace_yt_queue["queue"].read(0, copier_namespace_offset, 100)
    copier_namespace_offset += len(copier_namespace_result["rows"])

    updates = set()
    assert len(copier_result["rows"]) == 1
    for row in copier_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.META_UPDATE

        inner = TMetaUpdate()
        inner.ParseFromString(event.Body)

        updates.add((inner.Url, inner.MdsId.Namespace, inner.MdsId.GroupId, inner.MdsId.ImageName))

    expected = set([
        ('https://example.com/new_img1', 'marketpic', 456, 'pic_update'),
    ])
    assert updates == expected
    assert len(copier_namespace_result["rows"]) == 1

    updates = set()
    assert len(copier_namespace_result["rows"]) == 1
    for row in copier_namespace_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.META_UPDATE

        inner = TMetaUpdate()
        inner.ParseFromString(event.Body)

        updates.add((inner.Url, inner.MdsId.Namespace, inner.MdsId.GroupId, inner.MdsId.ImageName))

    expected = set([
        ('https://example.com/new_img2', 'copier_separate_namespace', 555, 'pic_new'),
    ])
    assert updates == expected


def test_delete_requests(stand):
    data = {0: [
        pack_messages([
            request_event(
                url='https://example.com/new_img1',
                namespace='copier_separate_namespace',
                offer='https://example.com/offer'
            ),
            image_event(
                url='https://example.com/new_img1',
                mds_json=generate_mds_json(
                    namespace='copier_separate_namespace',
                    group_id=555,
                    name='pic_new',
                )
            ),
            delete_event(
                url='https://example.com/new_img1',
                namespace='copier_separate_namespace',
                group_id=555,
                image_name='pic_new',
            ),
            delete_event(
                url='https://example.com/new_img1',
                namespace='copier_separate_namespace',
                group_id=555,
                image_name='pic_new',
            ),
        ]),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    copier_offset = 0
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    copier_namespace_offset = 0
    copier_namespace_result = stand.copier_queue_namespace_yt_queue["queue"].read(0, copier_namespace_offset, 100)
    copier_namespace_offset += len(copier_namespace_result["rows"])

    assert len(copier_result["rows"]) == 0
    assert len(copier_namespace_result["rows"]) == 1
    requests = set()
    for row in copier_namespace_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.DELETE_REQUEST
        inner = TDeleteRequest()
        inner.ParseFromString(event.Body)
        requests.add((inner.Url, inner.Namespace, inner.GroupId, inner.ImageName))

    expected = set([
        ('https://example.com/new_img1', 'copier_separate_namespace', 555, 'pic_new'),
    ])
    assert requests == expected
