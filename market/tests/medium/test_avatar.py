# coding: utf-8

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TCopierRequest,
    TEventMessage,
    TImageResponse,
)
from .conftest import (
    pack_message,
    request_event,
    launch,
    image_event,
    image_msg,
    generate_mds_json,
    request_msg,
    launch_resharder,
)

from robot.protos.crawl.compatibility.feeds_pb2 import TFeedExt
from market.idx.datacamp.picrobot.proto.picture_zora_context_pb2 import TPictureZoraContext

BASE_YT_DIR = '//tmp'


def test_avatar_url(stand):
    # При запросе url из аватарницы должны выдавать copier request
    data = {0: [
        pack_message(request_event(
            url='https://avatars.mds.yandex.net/get-mpic/5277894/img_id1434962606095117823.jpeg/9',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='//avatars.mds.yandex.net/get-mpic/5277894/img_id1434962606095117823.jpeg/10',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
        pack_message(request_event(
            url='avatars.mds.yandex.net/get-mpic/5277894/img_id1434962606095117823.jpeg/11',
            namespace='turbo',
            offer='https://example.com/offer'
        )),
    ]}
    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    # проверяем что нет SAMOVAR_REQUEST
    output_offset = 0
    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 0

    # проверяем что появился COPIER_REQUEST
    copier_offset = 0
    copier_result = stand.copier_yt_queue["queue"].read(0, copier_offset, 100)
    copier_offset += len(copier_result["rows"])

    assert len(copier_result["rows"]) == 3

    copier_requests = set()
    for row in copier_result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_REQUEST

        inner = TCopierRequest()
        inner.ParseFromString(event.Body)

        copier_requests.add((inner.AvatarsUrl, inner.Context.MdsNamespace))

    expected = set([
        ('https://avatars.mds.yandex.net/get-mpic/5277894/img_id1434962606095117823.jpeg/9', 'turbo'),
        ('https://avatars.mds.yandex.net/get-mpic/5277894/img_id1434962606095117823.jpeg/10', 'turbo'),
        ('https://avatars.mds.yandex.net/get-mpic/5277894/img_id1434962606095117823.jpeg/11', 'turbo'),
    ])

    assert copier_requests == expected


def test_mdsinfo_sizes(stand):
    # request images in different namespaces
    data = {0: [
        pack_message(request_event(
            url='https://example.com/new_img1',
            namespace='turbo',
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
        pack_message(
            image_event(
                url='https://example.com/new_img1',
                mds_json=generate_mds_json(
                    namespace='turbo'
                )
            )
        )
    ]}

    stand.input_yt_queue["queue"].write(data)
    launch(stand, data)

    result = stand.output_yt_queue["queue"].read(0, output_offset, 100)
    output_offset += len(result["rows"])
    assert len(result["rows"]) == 1

    for row in result["rows"]:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.IMAGE_RESPONSE

        inner = TImageResponse()
        inner.ParseFromString(event.Body)

        assert len(inner.MdsInfo.Sizes) == 4
        sizes = set()
        containers = set()
        aliases = set()
        for size in inner.MdsInfo.Sizes:
            aliases.add(size.Alias)
            sizes.add((size.Width, size.Height))
            containers.add((size.ContainerWidth, size.ContainerHeight))
        assert sizes == set([(600, 600), (300, 300), (123, 200), (899, 1199)])
        assert containers == set([(600, 600), (300, 300), (180, 240), (900, 1200)])
        assert aliases == set(['XXL', 'big', 'orig', 'small'])


def test_ya_disk_url(stand16, resharder_stand16, yadisk_server):
    '''Тест проверяет что url ya disk будет взят из контекста который мы передали в samovar.'''
    stand = stand16
    resharder_stand = resharder_stand16

    request_data = {}
    video_request_data = {}
    image_data = {}
    event_data = {}
    offsets = {i: 0 for i in range(stand.shard_count)}

    request_data = {0: [
        request_msg(
            url='https://disk.yandex.ru/i/FlnHSZYznw2R6w',
            namespace='marketpic',
            offer='https://example.com/offer'
        ),
    ]}

    resharder_stand.request_yt_queue["queue"].write(request_data)
    launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data)
    resharded_data = {}
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, offsets[i], 100)["rows"]
        offsets[i] += len(resharded_data[i])

    # launch processor, should request img in samovar
    launch(stand, resharded_data, yadisk_server)
    event_processed = 0
    for i in range(stand.shard_count):
        for row in stand.output_yt_queue["queue"].read(i, 0, 100)["rows"]:
            event = TEventMessage()
            event.ParseFromString(row)
            if event.Type == EMessageType.SAMOVAR_REQUEST:
                inner = TFeedExt()
                inner.ParseFromString(event.Body)
                assert inner.Url == 'https://downloader.disk.yandex.ru/disk/a6addca0843f3da59c00733d2eed83991a79fff8fe193a7a6517bd1f443d0d5b/60e5d1c0/...'
                ctx = TPictureZoraContext()
                ctx.ParseFromString(inner.FeedContext.BytesValue)
                assert ctx.ShortYaDiskUrl == 'https://disk.yandex.ru/i/FlnHSZYznw2R6w'
                event_processed += 1
    assert event_processed == 1

    # provide image response
    image_data = {0: [
        image_msg(
            url='https://downloader.disk.yandex.ru/disk/a6addca0843f3da59c00733d2eed83991a79fff8fe193a7a6517bd1f443d0d5b/60e5d1c0/...',
            mds_json=generate_mds_json(namespace='marketpic', name='pic1'),
            short_ya_disk_url='https://disk.yandex.ru/i/FlnHSZYznw2R6w',
            compress=True
        )
    ]}
    resharder_stand.image_yt_queue["queue"].write(image_data)
    launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data)
    for i in range(stand.shard_count):
        resharded_data[i] = stand.input_yt_queue["queue"].read(i, offsets[i], 100)["rows"]
        offsets[i] += len(resharded_data[i])

    # launch processor, should provide image response
    launch(stand, resharded_data)
    event_processed = 0
    for i in range(stand.shard_count):
        for row in stand.output_yt_queue["queue"].read(i, 0, 100)["rows"]:
            event = TEventMessage()
            event.ParseFromString(row)
            if event.Type == EMessageType.IMAGE_RESPONSE:
                inner = TImageResponse()
                inner.ParseFromString(event.Body)
                assert inner.Offer.OfferId == 'https://example.com/offer'
                assert inner.Url == 'https://disk.yandex.ru/i/FlnHSZYznw2R6w'
                assert inner.MdsInfo.MdsId.Namespace == 'marketpic'
                assert inner.MdsInfo.MdsId.ImageName == 'pic1'
                event_processed += 1

    assert event_processed == 1
