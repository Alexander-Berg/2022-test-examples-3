# encoding: utf-8

import os
import yatest.common
from yatest.common import network
import requests

from travel.hotels.test_helpers.app import HttpApp
LABEL_KEY = 'testtesttesttest'
TOKEN_KEY = 'tokentokentoken1'
REDIR_ADD_INFO_KEY = 'okentokentokent1'


class LabelCodec(object):
    def __init__(self):
        self.app = HttpApp(network.PortManager().get_port())
        self.label_key_file = os.path.join(yatest.common.work_path(), 'codec_label_key.txt')
        with open(self.label_key_file, 'w') as f:
            f.write(LABEL_KEY)
        self.token_key_file = os.path.join(yatest.common.work_path(), 'codec_token_key.txt')
        with open(self.token_key_file, 'w') as f:
            f.write(TOKEN_KEY)
        self.exec_obj = None
        self.session = requests.session()

    def start(self):
        bin = yatest.common.binary_path('travel/hotels/tools/label_codec_http/label_codec_http')
        self.exec_obj = yatest.common.execute([bin,
                                               '-p', str(self.app.port),
                                               '-l', self.label_key_file,
                                               '-t', self.token_key_file],
                                              wait=False)
        self.app.wait_ready('encode_url')

    def stop(self):
        if self.exec_obj is not None:
            self.exec_obj.kill()

    def encode_url(self, url):
        return self.app.checked_get('encode_url', params={'value': url}).content

    def decode_url(self, url):
        return self.app.checked_get('decode_url', params={'value': url}).content

    def encode_label(self, label):
        return self.app.checked_get('encode_label', params={'value': label}).content

    def decode_label(self, label):
        return self.app.checked_get('decode_label', params={'value': label}).content

    def encode_token(self, t):
        return self.app.checked_get('encode_token', params={'value': t}).content

    def decode_token(self, t):
        return self.app.checked_get('decode_token', params={'value': t}).content
