# -*- coding: utf-8 -*-

# Path search/integration_tests/test_suits/crashed used in the sandbox task
# https://a.yandex-team.ru/arc/trunk/arcadia/sandbox/projects/websearch/SearchIntegrationTestsWithRetries/__init__.py?rev=r8526249#L114
# If you want move crashed tests please change path in the sandbox

import base64
import hashlib
import pytest
import requests
import struct
import time
import logging

import vcdiff_helpers

from util.const import SEARCH_TOUCH, SEARCHAPP, USER_AGENT_TOUCH
from util.params import get_beta_host, test_id
from util.yappy import do_request


class SdchDictionary:
    def __init__(self, path, content):
        self.path = path

        dict_headers, dict_body = content.split(b'\n\n', 1)
        self.headers = dict()
        for r in dict_headers.decode('ascii').split("\n"):
            k, v = (s.strip() for s in r.split(':'))
            self.headers[k.lower()] = v

        self.content = dict_body

        hasher = hashlib.sha256(content)
        d = hasher.digest()
        self.client_id = base64.urlsafe_b64encode(d[:6])
        self.server_id = base64.urlsafe_b64encode(d[6:12])


def get_dict(beta_url, sdch_host, handler, params):
    response = None
    for i in range(5):
        try:
            response = do_request(
                beta_url=beta_url,
                handler=handler,
                headers={
                    'Host': sdch_host,
                    'User-Agent': USER_AGENT_TOUCH,
                    'Accept-Encoding': 'sdch',
                    'X-Yandex-Https': '1',
                    'X-Yandex-Internal-Request': '1'
                },
                params=params
            )
            if response.status_code == 200:
                break
        except:
            pass
        time.sleep(3)

    response.raise_for_status()

    dict_path = response.headers['Get-Dictionary']
    url = 'https://{beta_url}{dict_path}'.format(beta_url=beta_url, dict_path=dict_path)
    for i in range(5):
        try:
            dict_content_response = requests.get(url, headers={'Host': sdch_host,
                                                               'User-Agent': USER_AGENT_TOUCH,
                                                               'Accept-Encoding': 'gzip',
                                                               'X-Yandex-Https': '1',
                                                               'X-Yandex-Internal-Request': '1'})
            if dict_content_response.status_code == 200:
                break
        except:
            pass
        time.sleep(3)

    dict_content_response.raise_for_status()
    assert 'max-age' in response.headers['Cache-Control']
    assert response.headers['Expires']

    return SdchDictionary(dict_path, dict_content_response.content)


def decode_sdch(encoded, sdch_dict):
    encoded_header = encoded[:8]
    if isinstance(encoded[8], int):
        assert encoded[8] == 0                          # Python 3.x
    else:
        assert struct.unpack("<B", encoded[8])[0] == 0  # Python 2.x
    assert encoded_header == sdch_dict.server_id

    return vcdiff_helpers.decode_vcdiff(sdch_dict.content, encoded[9:])


class TestSDCH():
    @pytest.mark.parametrize('handler', (SEARCHAPP, SEARCH_TOUCH))
    @pytest.mark.parametrize('sdch_host', ('yandex.ru', 'yandex.by', 'yandex.kz', 'yandex.com.tr'))
    def test_sdch(self, sdch_host, handler):
        params = list()
        testid = test_id()
        if testid:
            params.append(('test-id', testid))
        else:
            params.append(('no-tests', '1'))

        beta_url = get_beta_host()
        sdch_dict = get_dict(beta_url, sdch_host, handler, params)
        encoded_response = None

        for i in range(5):
            try:
                encoded_response = do_request(
                    beta_url=beta_url,
                    handler=handler,
                    headers={
                        'Avail-Dictionary': sdch_dict.client_id,
                        'Host': sdch_host,
                        'User-Agent': USER_AGENT_TOUCH,
                        'Accept-Encoding': 'sdch',
                        'X-Yandex-Https': '1',
                        'X-Yandex-Internal-Request': '1'
                    },
                    params=params
                )
                if encoded_response.status_code == 200:
                    break
            except:
                pass
            time.sleep(3)

        encoded_response.raise_for_status()
        assert encoded_response.headers['Content-Encoding'] == 'sdch'

        encoded_serp = encoded_response.content
        logging.debug("client_id: {} server_id: {} Content: {}".format(sdch_dict.client_id, sdch_dict.server_id, encoded_serp))
        # encoded_serp = b'F9zxq9R7\x00\xd6\xc3'

        # exception here
        decoded_serp = decode_sdch(encoded_serp, sdch_dict).decode('utf-8')

        # TODO(SEARCH-9206) add more sanity checks on decoded content
        assert decoded_serp  # check that result is not empty
