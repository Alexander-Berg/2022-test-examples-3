# coding: utf-8
import gzip
import jinja2
import json
import logging
import os
import pytest
import re
import six
import time
import yatest.common

from six import BytesIO
from six.moves import BaseHTTPServer
from six.moves import socketserver as SocketServer
from six.moves.BaseHTTPServer import BaseHTTPRequestHandler
from threading import Thread

from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_simple_state_table,
    create_yt_queue,
    execute_cli,
    launch_bullied_processes_reading_queue,
    check_queue_is_read,
    make_json_file,
    make_namedtuple,
)

from library.python.framing.packer import Packer

from market.idx.datacamp.picrobot.proto.event_pb2 import (
    EMessageType,
    TCopierResponse,
    TEventMessage,
    TImageRequest,
    TVideoRequest,
    TVideoResponse,
    TBumpMessage,
    TMetaUpdate,
    TDeleteRequest,
    TDeleteResponse,
)
from extsearch.images.protos.imagemetadata_pb2 import TImageMetaData
from market.idx.datacamp.picrobot.proto.picture_zora_context_pb2 import TPictureZoraContext

from robot.rthub.yql.protos.queries_pb2 import TMarketItem, TImageExtMetaUpdate

from yt.wrapper import ypath_join
from yatest.common.network import PortManager


BASE_YT_DIR = '//tmp'


class ThreadingSimpleServer(
        SocketServer.ThreadingMixIn,
        BaseHTTPServer.HTTPServer
):
    pass


class YaDiskApiHttpServer(object):
    def __init__(self, host=None, port=None):
        self._host = host or 'localhost'
        self._port = port

    @property
    def host(self):
        return self._host

    @property
    def port(self):
        return self._port

    def start_server(self, request_data=None):

        logging.info('Starting ya disk api server on {}:{}'.format(self.host, self.port))

        class YaDiskHandler(BaseHTTPRequestHandler):
            def do_GET(self):
                logging.info('Ya disk server. GET request: {}'.format(self.path))
                response = {
                    'href': 'https://downloader.disk.yandex.ru/disk/a6addca0843f3da59c00733d2eed83991a79fff8fe193a7a6517bd1f443d0d5b/60e5d1c0/...',
                    'method': "GET",
                    'templated': False,
                }
                self.send_response(200, 'OK')
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(six.ensure_binary(json.dumps(response)))

        server = ThreadingSimpleServer((self.host, self.port), YaDiskHandler)
        mock_server_thread = Thread(target=server.serve_forever)
        mock_server_thread.setDaemon(True)
        mock_server_thread.start()

        logging.info('Ya disk server started')


@pytest.fixture(scope='module')
def yadisk_server():
    with PortManager() as pm:
        server = YaDiskApiHttpServer(port=pm.get_port())
        server.start_server()
        return server


def sharded_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled, shard_count):
    yt_cluster = standalone_yt_cluster.get_proxy_address()

    test_id = re.sub(r'[^\w\d]', '_', request.node.name)
    input_queue_path = ypath_join(BASE_YT_DIR, 'input_queue_' + test_id)
    output_queue_path = ypath_join(BASE_YT_DIR, 'output_queue_' + test_id)
    copier_queue_path = ypath_join(BASE_YT_DIR, 'copier_queue_' + test_id)
    copier_queue_namespace_path = ypath_join(BASE_YT_DIR, 'copier_queue_namespace_' + test_id)
    consuming_system_path = ypath_join(BASE_YT_DIR, 'test_consuming_system_' + test_id)
    state_table_path = ypath_join(BASE_YT_DIR, 'states/State_' + test_id)
    global_log = 'global_' + test_id + '.log'

    yt_client = standalone_yt_cluster.get_yt_client()

    input_yt_queue = create_yt_queue(yt_client, input_queue_path, shard_count)
    output_yt_queue = create_yt_queue(yt_client, output_queue_path, shard_count)
    copier_yt_queue = create_yt_queue(yt_client, copier_queue_path, shard_count)
    copier_queue_namespace_yt_queue = create_yt_queue(yt_client, copier_queue_namespace_path, shard_count)

    input_queue_consumer = "processor"
    execute_cli(["consumer", "create", input_yt_queue["path"], input_queue_consumer, "--ignore-in-trimming", "0"])

    create_simple_state_table(yt_client, state_table_path)
    return make_namedtuple("PicrobotTestStand", **locals())


def sharded_resharder_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled, shard_count):
    yt_cluster = standalone_yt_cluster.get_proxy_address()

    test_id = re.sub(r'[^\w\d]', '_', request.node.name)
    global_log = 'resharder_global_' + test_id + '.log'
    yt_log = 'resharder_yt_' + test_id + '.log'

    request_queue_path = ypath_join(BASE_YT_DIR, 'request_queue_' + test_id)
    video_request_queue_path = ypath_join(BASE_YT_DIR, 'video_request_queue_' + test_id)
    image_queue_path = ypath_join(BASE_YT_DIR, 'image_queue_' + test_id)
    event_queue_path = ypath_join(BASE_YT_DIR, 'event_queue_' + test_id)

    request_cs_path = ypath_join(BASE_YT_DIR, 'request_cs_' + test_id)
    video_request_cs_path = ypath_join(BASE_YT_DIR, 'video_request_cs_' + test_id)
    image_cs_path = ypath_join(BASE_YT_DIR, 'image_cs_' + test_id)
    event_cs_path = ypath_join(BASE_YT_DIR, 'event_cs_' + test_id)

    yt_client = standalone_yt_cluster.get_yt_client()

    request_yt_queue = create_yt_queue(yt_client, request_queue_path, shard_count)
    video_request_yt_queue = create_yt_queue(yt_client, video_request_queue_path, shard_count)
    image_yt_queue = create_yt_queue(yt_client, image_queue_path, shard_count)
    event_yt_queue = create_yt_queue(yt_client, event_queue_path, shard_count)

    resharder_queue_consumer = "resharder"
    execute_cli(["consumer", "create", request_yt_queue["path"], resharder_queue_consumer, "--ignore-in-trimming", "0"])
    execute_cli(["consumer", "create", video_request_yt_queue["path"], resharder_queue_consumer, "--ignore-in-trimming", "0"])
    execute_cli(["consumer", "create", image_yt_queue["path"], resharder_queue_consumer, "--ignore-in-trimming", "0"])
    execute_cli(["consumer", "create", event_yt_queue["path"], resharder_queue_consumer, "--ignore-in-trimming", "0"])

    return make_namedtuple("ResharderTestStand", **locals())


@pytest.fixture()
def stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    return sharded_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled, shard_count=1)


@pytest.fixture()
def stand16(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    return sharded_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled, shard_count=16)


@pytest.fixture()
def resharder_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    return sharded_resharder_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled, shard_count=1)


@pytest.fixture()
def resharder_stand16(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled):
    return sharded_resharder_stand(request, standalone_yt_cluster, standalone_yt_ready_env, port_manager, config_test_default_enabled, shard_count=16)


def make_stateful_config(stand, worker_minor_name, yadisk_server=None):
    with open(yatest.common.source_path("market/idx/datacamp/picrobot/processor/test_config.json.j2")) as f:
        port=9999
        if yadisk_server:
            port = yadisk_server.port
        conf_s = jinja2.Template(f.read()).render(
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(yatest.common.output_path(), stand.global_log),
            port=stand.port_manager.get_port(),
            input_queue_path=stand.input_queue_path,
            cs_path=stand.consuming_system_path,
            shard_count=stand.shard_count,
            state_path=stand.state_table_path,
            output_queue_path=stand.output_queue_path,
            queue_consumer=stand.input_queue_consumer,
            copier_queue_path=stand.copier_queue_path,
            copier_queue_namespace_path=stand.copier_queue_namespace_path,
            mds_host='localhost',
            avatars_hostname='avatars.mds.yandex.net',
            avatars_test_hostname='avatars.mdst.yandex.net',
            ya_disk_api='http://localhost:' + str(port) +'/',
        )
    return make_json_file(conf_s, name_template="processor_config_{json_hash}.json")


def make_resharder_config(stand, resharder_stand, worker_minor_name):
    with open(yatest.common.source_path("market/idx/datacamp/picrobot/resharder/test_config.json.j2")) as f:
        conf_s = jinja2.Template(f.read()).render(
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(yatest.common.output_path(), resharder_stand.global_log),
            yt_log=os.path.join(yatest.common.output_path(), resharder_stand.yt_log),
            port=resharder_stand.port_manager.get_port(),
            shard_count=stand.shard_count,
            output_queue=stand.input_queue_path,
            request_cs_path=resharder_stand.request_cs_path,
            video_request_cs_path=resharder_stand.video_request_cs_path,
            image_cs_path=resharder_stand.image_cs_path,
            event_cs_path=resharder_stand.event_cs_path,
            request_queue=resharder_stand.request_queue_path,
            video_request_queue=resharder_stand.video_request_queue_path,
            image_queue=resharder_stand.image_queue_path,
            event_queue=resharder_stand.event_queue_path,
        )
    return make_json_file(conf_s, name_template="resharder_config_{json_hash}.json")


class StatefulProcess(BulliedProcess):
    def __init__(self, config_path, env=None):
        super(StatefulProcess, self).__init__(
            launch_cmd=[yatest.common.binary_path("market/idx/datacamp/picrobot/processor/processor"), '-c', config_path],
            env=env,
        )


def launch(stand, data, yadisk_server=None, env=None):
    config = make_stateful_config(stand, "0", yadisk_server)

    full_data = {shard: data[shard] for shard in range(stand.shard_count)}

    launch_bullied_processes_reading_queue(
        [StatefulProcess(config, env)],
        stand.input_yt_queue,
        stand.input_queue_consumer,
        full_data,
        timeout=60
    )


class ResharderProcess(BulliedProcess):
    def __init__(self, config_path):
        super(ResharderProcess, self).__init__(
            launch_cmd=[yatest.common.binary_path("market/idx/datacamp/picrobot/resharder/resharder"), '-c', config_path]
        )


def launch_resharder(stand, resharder_stand, request_data, video_request_data, image_data, event_data):
    config = make_resharder_config(stand, resharder_stand, "0")

    queues = [
        resharder_stand.request_yt_queue,
        resharder_stand.video_request_yt_queue,
        resharder_stand.image_yt_queue,
        resharder_stand.event_yt_queue,
    ]

    offsets = []
    for queue, data in zip(queues, [request_data, video_request_data, image_data, event_data]):
        queue_offsets = []
        for i in range(queue["shards"]):
            queue_offsets.append(len(data.get(i, [])) - 1)
        offsets.append(queue_offsets)

    def check_func():
        for i in range(len(queues)):
            if not check_queue_is_read(
                queues[i],
                resharder_stand.resharder_queue_consumer,
                expected_offsets=offsets[i],
            ):
                return False
        return True

    launch_bullied_processes_reading_queue(
        [ResharderProcess(config)],
        data_or_check_func=check_func,
        timeout=60,
    )


def pack_messages(msgs):
    output = BytesIO()
    packer = Packer(output)

    for msg in msgs:
        packer.add_proto(msg)

    packer.flush()
    return output.getvalue()


def pack_message(msg):
    return pack_messages([msg])


def request_msg(url, namespace, offer):
    msg = TImageRequest()
    msg.Url = url
    msg.MdsNamespace = namespace
    msg.Offer.OfferId = offer
    return msg.SerializeToString()


def video_request_msg(url, namespace, offer):
    msg = TVideoRequest()
    msg.Url = url
    msg.Namespace = namespace
    msg.Offer.OfferId = offer
    return msg.SerializeToString()


def video_response_msg(url, namespace, offer, creativeId):
    msg = TVideoResponse()
    msg.Url = url
    msg.VideoInfo.Namespace = namespace
    msg.VideoInfo.CreativeId = creativeId
    msg.Offer.OfferId = offer
    return msg.SerializeToString()


def request_event(url, namespace, offer):
    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.IMAGE_REQUEST
    event.Body = request_msg(url, namespace, offer)
    event.Key = url

    return event


def video_request_event(url, namespace, offer):
    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.VIDEO_REQUEST
    event.Body = video_request_msg(url, namespace, offer)
    event.Key = url

    return event


def video_response_event(url, namespace, offer, creativeId):
    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.VIDEO_RESPONSE
    event.Body = video_response_msg(url, namespace, offer, creativeId)
    event.Key = url

    return event


def delete_msg(url, namespace, group_id, image_name):
    msg = TDeleteRequest()
    msg.Url = url
    msg.Namespace = namespace
    msg.GroupId = group_id
    msg.ImageName = image_name
    return msg.SerializeToString()


def delete_event(url, namespace, group_id, image_name, ts=None):
    event = TEventMessage()
    event.TimeStamp = ts or int(time.time())
    event.Type = EMessageType.DELETE_REQUEST
    event.Body = delete_msg(url, namespace, group_id, image_name)
    event.Key = url
    return event


def ext_meta_update_event(url, key, featureIdx, colorness=None, deleted=False, quant_local_descriptors=''):
    msg = TImageExtMetaUpdate()
    msg.Url = url

    meta = TImageMetaData()
    if colorness is not None:
        meta.ExtImageAttributes.Colorness = colorness
    if quant_local_descriptors != '':
        meta.QuantizedLocalDescriptors = quant_local_descriptors

    net = meta.NeuralNetOutputs[key]
    if deleted is not True:
        feature = net.NeuralNetOutputs.Features.add()
        feature.LayerIdx = featureIdx
    else:
        net.Deleted = True
    msg.ImageMetaData = meta.SerializeToString()

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.EXT_META_UPDATE
    event.Body = msg.SerializeToString()
    event.Key = url
    return event


def delete_response_event(url, namespace, group_id, image_name, isDeleted, ts=None):
    event = TEventMessage()
    event.TimeStamp = ts or int(time.time())
    event.Type = EMessageType.DELETE_RESPONSE
    event.Key = url
    msg = TDeleteResponse()
    msg.Url = url
    msg.Namespace = namespace
    msg.IsDeleted = isDeleted
    msg.Timestamp = ts or int(time.time())
    event.Body = msg.SerializeToString()
    return event


def generate_mds_json(namespace, name='pic123', group_id=456, skip_sizes=False):
    data = {
        "group-id": group_id,
        "imagename": name,
    }
    if not skip_sizes:
        data['sizes'] = {
            "orig": {
                "path": "/get-{}/{}/{}/orig".format(namespace, group_id, name),
                "width": 600,
                "height": 600
            },
            "big": {
                "path": "/get-{}/{}/{}/orig".format(namespace, group_id, name),
                "width": 300,
                "height": 300
            },
            "small": {
                "path": "/get-{}/{}/{}/orig".format(namespace, group_id, name),
                "width": 123,
                "height": 200
            },
            "XXL": {
                "path": "/get-{}/{}/{}/orig".format(namespace, group_id, name),
                "width": 899,
                "height": 1199
            }
        }
    else:
        data['sizes'] = {
            "orig": {
                "path": "/get-{}/{}/{}/orig".format(namespace, group_id, name),
                "width": 600,
                "height": 600
            },
        }
    return json.dumps(data)


def compress_data(data):
    out = BytesIO()
    with gzip.GzipFile(fileobj=out, mode="wb") as f:
        f.write(six.ensure_binary(data))
    return out.getvalue()


def image_msg(url, mds_json, short_ya_disk_url='', compress=False):
    msg = TMarketItem()
    msg.OriginalUrl = url
    msg.MdsJson = six.ensure_binary(mds_json)
    if short_ya_disk_url != '':
        ctx = TPictureZoraContext()
        ctx.ShortYaDiskUrl = short_ya_disk_url
        msg.ZoraCtx = ctx.SerializeToString()

    data = msg.SerializeToString()
    if compress:
        return compress_data(data)
    return data


def image_event(url, mds_json, http_code=200, last_access=int(time.time()), quant_local_descriptors=''):
    inner = TMarketItem()
    inner.OriginalUrl = url
    inner.MdsJson = six.ensure_binary(mds_json)
    inner.HttpCode = http_code
    inner.LastAccess = last_access
    if quant_local_descriptors != '':
        inner.QuantizedLocalDescriptors = quant_local_descriptors

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.MDS_RESULT
    event.Body = inner.SerializeToString()
    event.Key = url

    return event


def bump_event(url):
    inner = TBumpMessage()
    inner.Url = url

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.BUMP_MESSAGE
    event.Body = inner.SerializeToString()
    event.Key = url

    return event


def meta_update_event(url, namespace, group_id=None, name=None):
    inner = TMetaUpdate()
    inner.Url = url
    inner.MdsId.Namespace = namespace
    if group_id is not None and name is not None:
        inner.MdsId.GroupId = group_id
        inner.MdsId.ImageName = name

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.META_UPDATE
    event.Body = inner.SerializeToString()
    event.Key = url

    return event


def copier_response(url, mds_json):
    inner = TCopierResponse()
    inner.Url = url
    inner.MdsJson = six.ensure_binary(mds_json)

    event = TEventMessage()
    event.TimeStamp = int(time.time())
    event.Type = EMessageType.COPIER_RESPONSE
    event.Body = inner.SerializeToString()
    event.Key = url

    return event
