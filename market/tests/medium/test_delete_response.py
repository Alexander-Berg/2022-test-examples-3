# coding: utf-8

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TEventMessage,
    TDeleteRequest,
)

from .conftest import (
    pack_message,
    request_event,
    launch,
    image_event,
    delete_event,
    generate_mds_json,
    delete_response_event,
)


BASE_YT_DIR = '//tmp'


def test_delete_response(stand):
    # request images in different namespaces
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpictesting',
            offer='https://example.com/offer'
        ))
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 1

    # provide mds_json for some namespaces
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='marketpictesting',
                group_id=456,
                name='pic_test',
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 1

    data = {0: [
        pack_message(delete_event(
            url='https://example.com/new_img1',
            namespace='marketpictesting',
            group_id=456,
            image_name='pic_test',
        )),
        pack_message(delete_event(
            url='https://example.com/new_img1',
            namespace='marketpictesting',
            group_id=456,
            image_name='pic_test',
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    copier_offset = 0
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    assert len(copier_result["rows"]) == 1
    event = TEventMessage()
    event.ParseFromString(copier_result["rows"][0])
    assert event.Type == EMessageType.DELETE_REQUEST

    data = {0: [
        pack_message(delete_response_event(
            url='https://example.com/new_img1',
            namespace='marketpictesting',
            group_id=456,
            image_name='pic_test',
            isDeleted=True,
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 0
    assert len(copier_result["rows"]) == 0

    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpictesting',
            offer='https://example.com/offer'
        ))
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 1
    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.SAMOVAR_REQUEST


def test_delete_ignored_mds_info(stand):
    # request images
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpic',
            offer='https://example.com/offer'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 1

    # provide mds_json for some namespaces
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='marketpic',
                group_id=123,
                name='pic',
            )
        ))
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 1

    # provide mds_json for same namespaces
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='marketpic',
                group_id=123,
                name='pic_diff',
            )
        )),
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='marketpic',
                group_id=123,
                name='pic',
            )
        ))
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    copier_offset = 0
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])
    assert len(copier_result["rows"]) == 1

    requests = set()
    for row in copier_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.DELETE_REQUEST
        inner = TDeleteRequest()
        inner.ParseFromString(event.Body)
        requests.add((inner.Url, inner.Namespace, inner.GroupId, inner.ImageName))

    expected = set([
        ('https://example.com/new_img1', 'marketpic', 123, 'pic_diff'),
    ])
    assert requests == expected
