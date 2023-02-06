#!/usr/bin/python
# -*- coding: utf-8 -*-

from test_cbirdaemon_suite import CbirdaemonSuite

import extsearch.images.kernel.cbir.cbirdaemon.protos.similar_features_pb2 as similar_features_pb2
from google.protobuf.json_format import MessageToJson

import yatest.common
from yatest.common import network

import base64
import os
import re
import requests
import time
import sys
import json


SHARD_ID_REG = r"cbirdaemon-\d+-\d+-\d+"


def ProcessOutput(str):
    res = ""
    for line in str.split('\n'):
        line = line.strip()
        if '[INFO]' in line:
            line = line[line.find('['):line[:-1].rfind('(')]
        if 'AppHostServant, port ' in line:
            continue
        res += line + '\n'
    return res


def DecodeSimilar(value):
    res = {}
    version, data = value.split('@')
    res['version'] = version
    raw = base64.b64decode(data)
    features = similar_features_pb2.TSimilarFeaturesPB()
    features.ParseFromString(raw)
    res['data'] = json.loads(MessageToJson(features))
    return res


def DecodeItem(id, body):
    if id == 'similarnn':
        return DecodeSimilar(body)
    if id == 'image2query':
        return DecodeSimilar(body)
    if id == 'barcodes':
        return json.loads(body)
    if id == 'nnpreds':
        return json.loads(body)
    if id == 'auto_top_classes':
        return json.loads(body)
    if id == 'info':
        return body.split('x')
    if id == 'quant':
        return body
    return body


def DecodeDaemonResponse(resp):
    res = {}
    for line in resp.split('\n'):
        line = line.strip()
        if len(line) == 0:
            continue
        start = line.find('{')
        end = line.rfind('}')
        id = line[:start]
        body = line[start + 1:end]
        res[id] = DecodeItem(id, body)
    return res


class TestHttpCbirdaemon(CbirdaemonSuite):

    default_sigrequest = 'configurable_v2'

    default_req_info = {
        "ImageFeatures": [
            {
                "Name": "FeatV8"
            }
        ],
        "ImageCropFeatures": [
            {
                "Name": "FeatCropV8"
            }
        ],
        "FaceFeatures": [
            {
                "Name": "simfaces_ver4"
            }
        ],
        "Image2TextFeatures": [
            {
                "Name": "I2TVer10"
            }
        ],
        "CbirFeatures": {
            "Dscr": False,
            "Quant": True
        },
        "ImageInfo": {},
        "ImageClassification": {
            "Name": "Classification8"
        },
        "AutoClassification": {
            "Name": "AutoVer8"
        },
        "Barcode": {}
    }

    @classmethod
    def setup_class(cls):
        cls.skip_tests = False
        cls.test_env = "http"
        cls.pm = network.PortManager()
        cls.http_port = cls.pm.get_port()
        cbirdaemon_program = yatest.common.binary_path("extsearch/images/daemons/cbirdaemon2/cbirdaemon2")
        cbirdaemon_conf = yatest.common.work_path("./cbirdaemon.conf")

        data_path = yatest.common.work_path("./")
        shards_names = [d for d in os.listdir(data_path) if os.path.isdir(os.path.join(data_path, d)) and re.match(SHARD_ID_REG, d)]
        assert len(shards_names) == 1
        data_path = yatest.common.work_path(shards_names[0])
        command = [cbirdaemon_program, "--config %s" % cbirdaemon_conf, "--data-dir %s" % data_path, "--port %d" % cls.http_port]
        cls.cbirdaemon_proc = yatest.common.execute(command,
                                                    check_exit_code=False,
                                                    shell=True,
                                                    wait=False,
                                                    collect_cores=True,
                                                    check_sanitizer=True)
        assert cls.cbirdaemon_proc is not None
        sys.stderr.write("Starting cbirdaemon...\n")
        time.sleep(15)

    def send_request(cls, image_path, sigrequest=default_sigrequest, req_info=default_req_info):
        image = open(image_path).read()
        req = requests.post("http://localhost:%d" % cls.http_port,
                            files={u'upfile': ('somefilename', image)},
                            headers={'Sigrequest': sigrequest, 'RequestInfo': json.dumps(req_info)},
                            timeout=60)

        assert req.status_code == 200
        return {"raw": req.content, 'json': DecodeDaemonResponse(req.content)}

    def call_stop_cbirdaemon(cls):
        sys.stderr.write("Stopping...\n")
        req = requests.get("http://localhost:%d/admin?action=shutdown" % cls.http_port)
        assert req.status_code == 200
        cls.cbirdaemon_proc.wait(timeout=15)
        assert cls.cbirdaemon_proc.exit_code == 0
        out_file_path = yatest.common.output_path("cbirdaemon.out")

        with open(out_file_path, "w") as out_file:
            out_file.write("cbirdaemon stdout:\n")
            out_file.write(ProcessOutput(cls.cbirdaemon_proc.std_out))
            out_file.write("cbirdaemon stderr:\n")
            out_file.write(ProcessOutput(cls.cbirdaemon_proc.std_err))
        return yatest.common.canonical_file(out_file_path)

    @classmethod
    def get_diff_tool(cls):
        return yatest.common.binary_path("extsearch/images/daemons/cbirdaemon2/tests/diff_tool/diff_tool")

    @classmethod
    def teardown_class(cls):
        if cls.cbirdaemon_proc.running:
            cls.cbirdaemon_proc.kill()
            assert False
        cls.pm.release()
