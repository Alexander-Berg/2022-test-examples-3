# coding: utf-8

import os
import copy

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TCopierRequest,
    TEventMessage,
    TImageResponse,
    TMetaUpdate,
)
from ads.bsyeti.big_rt.py_test_lib import (
    extract_all_states_from_simple_table,
)
from market.idx.datacamp.picrobot.processor.proto.state_pb2 import (
    TPicrobotState
)
from robot.protos.crawl.compatibility.feeds_pb2 import TFeedExt

from .conftest import (
    pack_message,
    request_event,
    launch,
    image_event,
    generate_mds_json,
    copier_response,
    pack_messages,
    meta_update_event,
    bump_event
)

BASE_YT_DIR = '//tmp'


def test_multi_namespace(stand):
    # request images in different namespaces
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer2'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo2',
            offer='https://example.com/offer2'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo3',
            offer='https://example.com/offer3'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert len(result["rows"]) == 1
    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.SAMOVAR_REQUEST
    inner = TFeedExt()
    inner.ParseFromString(event.Body)
    assert inner.Url == 'https://example.com/new_img1'

    # provide mds_json for some namespaces
    # provide request and mds_json for another img
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo'
            )
        )),
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo2'
            )
        )),
        pack_messages([
            request_event(
                url='https://example.com/new_img4',     # no need to request this image, because the content in the same pack
                namespace='turbo4',
                offer='https://example.com/offer4'
            ),
            image_event(
                url='https://example.com/new_img4',
                mds_json=generate_mds_json(
                    namespace='turbo4'
                )
            ),
        ])
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    responses = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE

        inner = TImageResponse()
        inner.ParseFromString(event.Body)

        responses.add((inner.Offer.OfferId, inner.MdsInfo.MdsId.Namespace))

    expected = set([
        ('https://example.com/offer', 'turbo'),
        ('https://example.com/offer2', 'turbo'),
        ('https://example.com/offer2', 'turbo2'),
        ('https://example.com/offer4', 'turbo4')
    ])

    assert responses == expected


def test_copier_request(stand):
    # request images in different namespaces
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer2'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo2',
            offer='https://example.com/offer2'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo3',
            offer='https://example.com/offer3'
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

    assert len(result["rows"]) == 1
    assert len(copier_result["rows"]) == 0

    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.SAMOVAR_REQUEST
    inner = TFeedExt()
    inner.ParseFromString(event.Body)
    assert inner.Url == 'https://example.com/new_img1'

    # provide mds_json for some namespaces
    # provide request and mds_json for another img
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo'
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])

    # should provide response for turbo namespace and request copier to move into another namespaces
    responses = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE

        inner = TImageResponse()
        inner.ParseFromString(event.Body)

        responses.add((inner.Offer.OfferId, inner.MdsInfo.MdsId.Namespace))

    expected = set([
        ('https://example.com/offer', 'turbo'),
        ('https://example.com/offer2', 'turbo'),
    ])

    assert responses == expected

    copier_requests = set()
    for row in copier_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_REQUEST

        inner = TCopierRequest()
        inner.ParseFromString(event.Body)

        copier_requests.add((inner.Url, inner.Context.MdsNamespace))

    expected = set([
        ('https://example.com/new_img1', 'turbo2'),
        ('https://example.com/new_img1', 'turbo3'),
    ])

    assert copier_requests == expected

    # next step: provide copier responses
    data = {0: [
        pack_message(copier_response(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo2'
            )
        )),
        pack_message(copier_response(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo3'
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])

    assert len(copier_result["rows"]) == 0

    responses = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE

        inner = TImageResponse()
        inner.ParseFromString(event.Body)

        responses.add((inner.Offer.OfferId, inner.MdsInfo.MdsId.Namespace))

    expected = set([
        ('https://example.com/offer2', 'turbo2'),
        ('https://example.com/offer3', 'turbo3'),
    ])

    assert responses == expected


def test_meta_updates(stand):
    # request images in different namespaces
    data = {0: [
        pack_messages([
            request_event(
                url='https://example.com/new_img1',
                namespace='turbo',
                offer='https://example.com/offer'
            ),
            image_event(
                url='https://example.com/new_img1',
                mds_json=generate_mds_json(
                    namespace='turbo',
                    group_id=456,
                    name='pic_update',
                )
            ),
            meta_update_event(
                url='https://example.com/new_img1',
                namespace='turbo'
            ),
            meta_update_event(
                url='https://example.com/new_img2',
                namespace='turbo2',
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

    updates = set()

    for row in copier_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.META_UPDATE

        inner = TMetaUpdate()
        inner.ParseFromString(event.Body)

        updates.add((inner.Url, inner.MdsId.Namespace, inner.MdsId.GroupId, inner.MdsId.ImageName))

    expected = set([
        ('https://example.com/new_img1', 'turbo', 456, 'pic_update'),
        ('https://example.com/new_img2', 'turbo2', 555, 'pic_new'),
    ])

    assert updates == expected


# def test_ignore_not_requested(stand):
#     # request img
#     data = {0: [
#         pack_message(request_event(
#             url='https://example.com/new_img1',
#             namespace='turbo',
#             offer='https://example.com/offer'
#         )),
#     ]}

#     stand.input_yt_queue["queue"].write(data)
#     launch(stand, data)

#     # provide requested
#     data = {0: [
#         pack_message(image_event(
#             url='https://example.com/new_img1',
#             mds_json=generate_mds_json(
#                 namespace='turbo'
#             )
#         )),
#     ]}
#     stand.input_yt_queue["queue"].write(data)
#     launch(stand, data)

#     # provide extra
#     data = {0: [
#         pack_message(image_event(
#             url='https://example.com/new_img1',
#             mds_json=generate_mds_json(
#                 namespace='turbo2'
#             )
#         )),
#         pack_message(image_event(
#             url='https://example.com/new_img2',
#             mds_json=generate_mds_json(
#                 namespace='turbo'
#             )
#         )),
#     ]}
#     stand.input_yt_queue["queue"].write(data)
#     launch(stand, data)

#     data = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)

#     img1State = TPicrobotState()
#     img1State.ParseFromString(data['https://example.com/new_img1'])

#     assert len(img1State.MdsInfo) == 1
#     assert img1State.MdsInfo[0].MdsId.Namespace == 'turbo'
#     # появится сгенерированная deleteMeta
#     # assert data['https://example.com/new_img2'] == ''  # empty state, ideally there should be no key at all


def test_bump_retry(stand):
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer2'
        )),
        pack_message(request_event(
            url='https://example.com/new_img2',
            namespace='turbo',
            offer='https://example.com/offer2'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    env = copy.copy(os.environ)
    env.update({'PICROBOT_TEST_TIMESTAMP': '1500000000'})
    launch(stand, data, env=env)

    result = stand.output_yt_queue["queue"].read(0, 0, 100)
    output_offset = len(result["rows"])

    requested_urls = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.SAMOVAR_REQUEST
        inner = TFeedExt()
        inner.ParseFromString(event.Body)
        url = inner.Url
        assert url not in requested_urls
        requested_urls.add(url)
    assert requested_urls == set([
        'https://example.com/new_img1',
        'https://example.com/new_img2'
    ])

    data = extract_all_states_from_simple_table(stand.yt_client, stand.state_table_path, decompress=True)

    img1State = TPicrobotState()
    img1State.ParseFromString(data['https://example.com/new_img1'])

    next_ts = img1State.DownloadMeta.NextAttemptTimestamp + 1

    data = {0: [
        pack_message(bump_event(
            url='https://example.com/new_img1'
        ))
    ]}

    stand.input_yt_queue["queue"].write(data)
    env.update({'PICROBOT_TEST_TIMESTAMP': str(next_ts)})
    launch(stand, data, env=env)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    requested_urls = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.SAMOVAR_REQUEST
        inner = TFeedExt()
        inner.ParseFromString(event.Body)
        url = inner.Url
        assert url not in requested_urls
        requested_urls.add(url)
    assert requested_urls == set([
        'https://example.com/new_img1',
    ])


def test_report_errors(stand):
    # request img
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpic',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='marketpic',
            offer='https://example.com/offer2'
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)
    result = stand.output_yt_queue["queue"].read(0, 0, 100)
    output_offset = len(result["rows"])

    # provide error
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json='',
            http_code=503,
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    offers = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE
        inner = TImageResponse()
        inner.ParseFromString(event.Body)
        assert inner.HttpCode == 503
        assert inner.MdsInfo.MdsId.Namespace == 'marketpic'
        offers.add(inner.Offer.OfferId)
    assert offers == set([
        'https://example.com/offer',
        'https://example.com/offer2'
    ])

    # provide ok result
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

    offers = set()
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE
        inner = TImageResponse()
        inner.ParseFromString(event.Body)
        assert inner.HttpCode == 200
        assert inner.MdsInfo.MdsId.Namespace == 'marketpic'
        offers.add(inner.Offer.OfferId)
    assert offers == set([
        'https://example.com/offer',
        'https://example.com/offer2'
    ])


def test_multi_request_for_single_offer(stand):
    # request images in different namespaces
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo_new',
            offer='https://example.com/offer'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    assert len(result["rows"]) == 1
    event = TEventMessage()
    event.ParseFromString(result["rows"][0])
    assert event.Type == EMessageType.SAMOVAR_REQUEST
    inner = TFeedExt()
    inner.ParseFromString(event.Body)
    assert inner.Url == 'https://example.com/new_img1'

    # provide mds_json for some namespaces
    data = {0: [
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo'
            )
        )),
        pack_message(image_event(
            url='https://example.com/new_img1',
            mds_json=generate_mds_json(
                namespace='turbo_new'
            )
        )),
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])

    responses = set()
    assert len(result["rows"]) == 2
    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE

        inner = TImageResponse()
        inner.ParseFromString(event.Body)

        responses.add((inner.Offer.OfferId, inner.MdsInfo.MdsId.Namespace))

    expected = set([
        ('https://example.com/offer', 'turbo'),
        ('https://example.com/offer', 'turbo_new'),
    ])

    assert responses == expected
