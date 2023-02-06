# coding: utf-8
import jinja2
import json
import os
import pytest
import re
import six
import threading
import time
import yatest.common

from flask import Flask, request

from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    execute_cli,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
)

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TCopierRequest,
    TCopierResponse,
    TVideoRequest,
    TVideoResponse,
    TEventMessage,
    TMetaUpdate,
    TDeleteRequest,
    TDeleteResponse,
)

from yt.wrapper import ypath_join


BASE_YT_DIR = '//tmp'

VIDEO_ID = '629737d60559a77a4afec77b025'
CREATIVE_ID = 1122686348
GOOD_OFFER_ID = '123'
BAD_OFFER_ID = '321'
OAUTH_TOKEN = 'FIDASDFNIORJPW'


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    shard_count = 1
    yt_cluster = standalone_yt_cluster.get_proxy_address()

    test_id = re.sub(r'[^\w\d]', '_', request.node.name)
    input_queue_path = ypath_join(BASE_YT_DIR, 'input_queue_' + test_id)
    input_queue_path_namespace = ypath_join(BASE_YT_DIR, 'input_queue_namespace_' + test_id)
    output_queue_path = ypath_join(BASE_YT_DIR, 'output_queue_' + test_id)
    consuming_system_path = ypath_join(BASE_YT_DIR, 'test_consuming_system_' + test_id)
    consuming_system_path_namespace = ypath_join(BASE_YT_DIR, 'test_consuming_system_namespace_' + test_id)
    global_log = 'global_' + test_id + '.log'
    yt_log = 'global_' + test_id + '.log'

    yt_client = standalone_yt_cluster.get_yt_client()

    input_yt_queue = create_yt_queue(yt_client, input_queue_path, shard_count)
    input_yt_queue_namespace = create_yt_queue(yt_client, input_queue_path_namespace, shard_count)
    output_yt_queue = create_yt_queue(yt_client, output_queue_path, shard_count)

    input_queue_consumer = 'copier'
    input_queue_consumer_namespace = 'copier_namespace'
    execute_cli(['consumer', 'create', input_yt_queue['path'], input_queue_consumer, '--ignore-in-trimming', '0'])
    execute_cli(['consumer', 'create', input_yt_queue_namespace['path'], input_queue_consumer_namespace, '--ignore-in-trimming', '0'])

    local_mds_port = port_manager.get_port()

    return make_namedtuple('CopierTestStand', **locals())


def make_stateful_config(stand, worker_minor_name):
    with open(yatest.common.source_path('market/idx/datacamp/picrobot/copier/test_config.json.j2')) as f:
        conf_s = jinja2.Template(f.read()).render(
            yt_cluster=os.environ['YT_PROXY'],
            global_log=os.path.join(yatest.common.output_path(), stand.global_log),
            yt_log=os.path.join(yatest.common.output_path(), stand.yt_log),
            port=stand.port_manager.get_port(),
            input_queue_path=stand.input_queue_path,
            input_queue_path_namespace=stand.input_queue_path_namespace,
            cs_path=stand.consuming_system_path,
            cs_path_namespace=stand.consuming_system_path_namespace,
            shard_count=stand.shard_count,
            output_queue_path=stand.output_queue_path,
            queue_consumer=stand.input_queue_consumer,
            queue_consumer_namespace=stand.input_queue_consumer_namespace,
            mds_host='localhost:' + str(stand.local_mds_port),
        )
    return make_json_file(conf_s, name_template='copier_config_{json_hash}.json')


class StatefulProcess(BulliedProcess):
    def __init__(self, config_path):
        super(StatefulProcess, self).__init__(
            launch_cmd=[yatest.common.binary_path('market/idx/datacamp/picrobot/copier/copier'), '-c', config_path]
        )


def launch(stand, data, input_yt_queue=None, input_queue_consumer=None):
    config = make_stateful_config(stand, '0')
    if input_yt_queue is None:
        input_yt_queue = stand.input_yt_queue
    if input_queue_consumer is None:
        input_queue_consumer = stand.input_queue_consumer
    launch_bullied_processes_reading_queue(
        [StatefulProcess(config)],
        input_yt_queue,
        input_queue_consumer,
        data,
        timeout=60
    )


def generate_mds_json(namespace, name='pic123', group_id=456, update_prohibited=False):
    data = {
        'group-id': group_id,
        'imagename': name,
        'sizes': {
            'orig': {
                'path': '/get-{}/{}/{}/orig'.format(namespace, name, group_id)
            }
        }
    }
    if update_prohibited:
        data = {
            'attrs': data,
            'description': 'update is prohibited'
        }
    return json.dumps(data)


def generate_direct_json(handler, error=False):
    data = {
        'result': {
            'AddResults': [{
                'Id': VIDEO_ID if handler == 'advideos' else CREATIVE_ID
            }]
        }
    }
    if error:
        data = {
            'error': {
                'error_code': 1000,
            }
        }
    return json.dumps(data)


def copier_request(url, avatars_url, name, namespace):
    inner = TCopierRequest()
    inner.Url = url
    inner.AvatarsUrl = avatars_url
    inner.Context.MdsKey = name
    inner.Context.MdsNamespace = namespace

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.COPIER_REQUEST
    event.Body = inner.SerializeToString()
    event.Key = url

    return event.SerializeToString()


def video_request(url, namespace, offer_id):
    inner = TVideoRequest()
    inner.Url = url
    inner.Namespace = namespace
    inner.Offer.OfferId = offer_id

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.VIDEO_REQUEST
    event.Body = inner.SerializeToString()
    event.Key = url

    return event.SerializeToString()


def meta_update_request(url, namespace, group_id, name):
    inner = TMetaUpdate()
    inner.Url = url
    inner.MdsId.Namespace = namespace
    inner.MdsId.GroupId = group_id
    inner.MdsId.ImageName = name

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.META_UPDATE
    event.Body = inner.SerializeToString()
    event.Key = url

    return event.SerializeToString()


def delete_request(url, namespace, group_id, image_name):
    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.DELETE_REQUEST
    event.Key = url
    msg = TDeleteRequest()
    msg.Url = url
    msg.Namespace = namespace
    msg.GroupId = group_id
    msg.ImageName = image_name
    event.Body = msg.SerializeToString()
    return event.SerializeToString()


def test_copier(stand):
    data = {0: [
        copier_request(
            url='https://example.com/new_img1',
            avatars_url='http://avatars.mds.yandex.net/get-turbo/111/pic222/orig',
            name='pic123',
            namespace='turbo2',
        ),
        copier_request(
            url='https://example.com/new_img1',
            avatars_url='http://avatars.mds.yandex.net/get-turbo/111/pic222/orig',
            name='pic123',
            namespace='turbo3',
        ),
        copier_request(
            url='https://example.com/new_img1',
            avatars_url='http://avatars.mds.yandex.net/get-turbo/111/pic222/orig',
            name='pic123prohibited',
            namespace='turbo4',
        ),
    ]}
    stand.input_yt_queue['queue'].write(data)

    avatar_requests = []
    avatars_app = Flask(__name__)

    @avatars_app.route('/<action>/<name>', methods=['GET'])
    def avatars_request(action='', name=''):
        avatar_requests.append(request.full_path)
        namespace = action.split('-', 1)[1]
        if 'prohibited' in name:
            return generate_mds_json(namespace, 123, name, True), 403
        else:
            return generate_mds_json(namespace, 123, name), 200

    thread = threading.Thread(
        target=avatars_app.run,
        kwargs={
            'host': '::',
            'port': stand.local_mds_port,
            'threaded': True,
        }
    )
    thread.setDaemon(True)
    thread.start()

    # Wait for mocks to start.
    time.sleep(3)

    launch(stand, data)

    expected_avatar_requests = [
        six.ensure_text('/put-turbo2/pic123?url=http%3A//avatars.mds.yandex.net/get-turbo/111/pic222/orig'),
        six.ensure_text('/put-turbo3/pic123?url=http%3A//avatars.mds.yandex.net/get-turbo/111/pic222/orig'),
        six.ensure_text('/put-turbo4/pic123prohibited?url=http%3A//avatars.mds.yandex.net/get-turbo/111/pic222/orig'),
    ]

    assert sorted(avatar_requests) == sorted(expected_avatar_requests)

    result = stand.output_yt_queue['queue'].read(0, 0, 100)
    urls = []
    codes = []
    for row in result['rows']:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_RESPONSE
        inner = TCopierResponse()
        inner.ParseFromString(event.Body)
        urls.append(inner.Url)
        assert six.ensure_binary('/get-turbo') in inner.MdsJson
        assert inner.Context.MdsNamespace.startswith('turbo')
        codes.append(inner.MdsHttpCode)

    assert urls == [
        'https://example.com/new_img1',
        'https://example.com/new_img1',
        'https://example.com/new_img1',
    ]

    assert sorted(codes) == [200, 200, 403]


def test_video(stand):
    data = {0: [
        video_request(
            url='https://example.com/new_vid1',
            namespace='direct',
            offer_id=GOOD_OFFER_ID
        ),
        video_request(
            url='invalid',
            namespace='direct',
            offer_id=BAD_OFFER_ID
        ),
    ]}
    stand.input_yt_queue['queue'].write(data)

    os.environ['DIRECT_OAUTH_TOKEN'] = OAUTH_TOKEN

    direct_requests = []
    direct_app = Flask(__name__)

    @direct_app.route('/json/v5/<handler>', methods=['POST'])
    def direct_request(handler=''):
        body = six.ensure_text(request.data)
        token = request.headers['Authorization']
        direct_requests.append((handler, token, body))
        error = False
        if 'invalid' in body:
            error = True
        return generate_direct_json(handler, error), 200

    thread = threading.Thread(
        target=direct_app.run,
        kwargs={
            'host': '::',
            'port': stand.local_mds_port,
            'threaded': True,
        }
    )
    thread.setDaemon(True)
    thread.start()

    # Wait for mocks to start.
    time.sleep(3)

    launch(stand, data)

    auth = 'Bearer ' + OAUTH_TOKEN
    assert sorted(direct_requests) == [
        ('advideos', auth, '{"method":"add","params":{"AdVideos":[{"Url":"https:\\/\\/example.com\\/new_vid1"}]}}'),
        ('advideos', auth, '{"method":"add","params":{"AdVideos":[{"Url":"invalid"}]}}'),
        ('creatives', auth, '{"method":"add","params":{"Creatives":[{"VideoExtensionCreative":{"VideoId":"' + VIDEO_ID + '"}}]}}')
    ]

    result = stand.output_yt_queue['queue'].read(0, 0, 100)
    assert len(result['rows']) == 2

    for row in result['rows']:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.VIDEO_RESPONSE

        inner = TVideoResponse()
        inner.ParseFromString(event.Body)
        if inner.Offer.OfferId == GOOD_OFFER_ID:
            assert inner.VideoInfo.CreativeId == CREATIVE_ID
            assert inner.VideoInfo.Namespace == 'direct'
            assert inner.VideoInfo.AdVideosHttpCode == 200
            assert inner.VideoInfo.CreativesHttpCode == 200
        else:
            assert inner.VideoInfo.CreativeId == 0
            assert inner.VideoInfo.Namespace == 'direct'
            assert inner.VideoInfo.AdVideosHttpCode == 200
            assert inner.VideoInfo.CreativesHttpCode == 0
            assert inner.VideoInfo.AdVideosErrors.Errors[0] == 1000


# copier support many input queues
# check that we can consume messages from second queue via other consuming system
def test_copier_namespace_queue(stand):
    data = {0: [
        copier_request(
            url='https://example.com/new_img1',
            avatars_url='http://avatars.mds.yandex.net/get-turbo/111/pic222/orig',
            name='pic123',
            namespace='turbo2',
        ),
        copier_request(
            url='https://example.com/new_img1',
            avatars_url='http://avatars.mds.yandex.net/get-turbo/111/pic222/orig',
            name='pic123',
            namespace='turbo3',
        ),
        copier_request(
            url='https://example.com/new_img1',
            avatars_url='http://avatars.mds.yandex.net/get-turbo/111/pic222/orig',
            name='pic123prohibited',
            namespace='turbo4',
        ),
    ]}
    stand.input_yt_queue_namespace['queue'].write(data)

    avatar_requests = []
    avatars_app = Flask(__name__)
    @avatars_app.route('/<action>/<name>', methods=['GET'])
    def avatars_request(action='', name=''):
        avatar_requests.append(request.full_path)
        namespace = action.split('-', 1)[1]
        if 'prohibited' in name:
            return generate_mds_json(namespace, 123, name, True), 403
        else:
            return generate_mds_json(namespace, 123, name), 200

    thread = threading.Thread(
        target=avatars_app.run,
        kwargs={
            'host': '::',
            'port': stand.local_mds_port,
            'threaded': True,
        }
    )
    thread.setDaemon(True)
    thread.start()
    # Wait for mocks to start.
    time.sleep(3)

    launch(stand, data, stand.input_yt_queue_namespace, stand.input_queue_consumer_namespace)

    expected_avatar_requests = [
        six.ensure_text('/put-turbo2/pic123?url=http%3A//avatars.mds.yandex.net/get-turbo/111/pic222/orig'),
        six.ensure_text('/put-turbo3/pic123?url=http%3A//avatars.mds.yandex.net/get-turbo/111/pic222/orig'),
        six.ensure_text('/put-turbo4/pic123prohibited?url=http%3A//avatars.mds.yandex.net/get-turbo/111/pic222/orig'),
    ]

    assert sorted(avatar_requests) == sorted(expected_avatar_requests)

    result = stand.output_yt_queue['queue'].read(0, 0, 100)
    urls = []
    codes = []
    for row in result['rows']:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_RESPONSE
        inner = TCopierResponse()
        inner.ParseFromString(event.Body)
        urls.append(inner.Url)
        assert six.ensure_binary('/get-turbo') in inner.MdsJson
        assert inner.Context.MdsNamespace.startswith('turbo')
        codes.append(inner.MdsHttpCode)

    assert urls == [
        'https://example.com/new_img1',
        'https://example.com/new_img1',
        'https://example.com/new_img1',
    ]

    assert sorted(codes) == [200, 200, 403]


def test_meta_update(stand):
    data = {0: [
        meta_update_request(
            url='https://example.com/new_img1',
            group_id=567,
            name='pic123',
            namespace='turbo2',
        )
    ]}
    stand.input_yt_queue['queue'].write(data)

    avatar_requests = []

    avatars_app = Flask(__name__)

    @avatars_app.route('/<action>/<groupid>/<name>', methods=['GET'])
    def avatars_request(action='', groupid='', name=''):
        avatar_requests.append(request.full_path)
        namespace = action.split('-', 1)[1]
        return generate_mds_json(namespace, int(groupid), name), 200

    thread = threading.Thread(
        target=avatars_app.run,
        kwargs={
            'host': '::',
            'port': stand.local_mds_port,
            'threaded': True,
        }
    )
    thread.setDaemon(True)
    thread.start()

    # Wait for mocks to start.
    time.sleep(3)

    launch(stand, data)

    expected_avatar_requests = [
        six.ensure_text('/getimageinfo-turbo2/567/pic123?'),
    ]

    assert sorted(expected_avatar_requests) == sorted(avatar_requests)

    result = stand.output_yt_queue['queue'].read(0, 0, 100)
    urls = []
    codes = []
    for row in result['rows']:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.COPIER_RESPONSE
        inner = TCopierResponse()
        inner.ParseFromString(event.Body)
        urls.append(inner.Url)
        assert six.ensure_binary('/get-turbo2') in inner.MdsJson
        assert inner.Context.MdsNamespace.startswith('turbo')
        assert inner.ForceUpdate
        codes.append(inner.MdsHttpCode)

    assert urls == [
        'https://example.com/new_img1',
    ]
    assert sorted(codes) == [200]


def test_delete_request(stand):
    data = {0: [
        delete_request(
            url='https://example.com/new_img1',
            group_id=567,
            image_name='pic123',
            namespace='turbo2',
        )
    ]}
    stand.input_yt_queue['queue'].write(data)

    avatar_requests = []
    avatars_app = Flask(__name__)
    @avatars_app.route('/<action>/<groupid>/<name>', methods=['GET'])
    def avatars_request(action='', groupid='', name=''):
        avatar_requests.append(request.full_path)
        namespace = action.split('-', 1)[1]
        return generate_mds_json(namespace, int(groupid), name), 200

    thread = threading.Thread(
        target=avatars_app.run,
        kwargs={
            'host': '::',
            'port': stand.local_mds_port,
            'threaded': True,
        }
    )
    thread.setDaemon(True)
    thread.start()

    # Wait for mocks to start.
    time.sleep(3)

    launch(stand, data)

    expected_avatar_requests = [
        six.ensure_text('/delete-turbo2/567/pic123?'),
    ]

    assert sorted(expected_avatar_requests) == sorted(avatar_requests)

    result = stand.output_yt_queue['queue'].read(0, 0, 100)
    for row in result['rows']:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.DELETE_RESPONSE
        inner = TDeleteResponse()
        inner.ParseFromString(event.Body)
        assert inner.Namespace == 'turbo2'
        assert inner.Url == 'https://example.com/new_img1'
        assert inner.IsDeleted is True
        assert inner.MdsHttpCode == 200


def test_delete_request_202(stand):
    data = {0: [
        delete_request(
            url='https://example.com/new_img1',
            group_id=567,
            image_name='pic123',
            namespace='turbo2',
        )
    ]}
    stand.input_yt_queue['queue'].write(data)

    avatar_requests = []
    avatars_app = Flask(__name__)
    @avatars_app.route('/<action>/<groupid>/<name>', methods=['GET'])
    def avatars_request(action='', groupid='', name=''):
        avatar_requests.append(request.full_path)
        namespace = action.split('-', 1)[1]
        return generate_mds_json(namespace, int(groupid), name), 202

    thread = threading.Thread(
        target=avatars_app.run,
        kwargs={
            'host': '::',
            'port': stand.local_mds_port,
            'threaded': True,
        }
    )
    thread.setDaemon(True)
    thread.start()

    # Wait for mocks to start.
    time.sleep(3)

    launch(stand, data)

    expected_avatar_requests = [
        six.ensure_text('/delete-turbo2/567/pic123?'),
        six.ensure_text('/delete-turbo2/567/pic123?'),
    ]

    assert sorted(expected_avatar_requests) == sorted(avatar_requests)

    result = stand.output_yt_queue['queue'].read(0, 0, 100)
    for row in result['rows']:
        event = TEventMessage()
        event.ParseFromString(row)
        assert event.Type == EMessageType.DELETE_RESPONSE
        inner = TDeleteResponse()
        inner.ParseFromString(event.Body)
        assert inner.Namespace == 'turbo2'
        assert inner.Url == 'https://example.com/new_img1'
        assert inner.IsDeleted is False
        assert inner.MdsHttpCode == 202
