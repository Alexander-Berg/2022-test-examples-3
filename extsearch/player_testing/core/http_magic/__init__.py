#!/usr/bin/env python
# coding: utf-8
import requests
from conf import SANDBOX_URL
from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import THttpFeatures
try:
    from urllib.parse import quote, urlparse
except ImportError:
    from urllib import quote
    from urlparse import urlparse


class PlayerUrlFeatures(object):

    def __init__(self, config, url):
        self.proxy = config.proxy
        self.timeout = config.timeout
        self.ca_cert = config.ca_cert
        self.props = THttpFeatures()
        player_url = PlayerUrlFeatures._sanitize_url(url)
        if not player_url:
            return
        if not self._check(u'https://{}'.format(player_url)):
            self._check(u'http://{}'.format(player_url))
        self._init_url(player_url)

    def _check(self, url):
        try:
            r = requests.get(url,
                             timeout=self.timeout,
                             proxies={'https': self.proxy,
                                      'http': self.proxy},
                             headers={'referer': SANDBOX_URL},
                             verify=self.ca_cert,
                             stream=True)
            if r.status_code >= 200 and r.status_code < 300:
                self.props.ProbeStatusOk = True
                if url.startswith('https'):
                    self.props.ProbeHttps = True
                ctype = r.headers['content-type']
                if ctype.startswith('text/html'):
                    self.props.ProbeHtml = True
                elif ctype.startswith('video'):
                    self.props.ProbeVideo = True
                self.props.ClearField('Error')
        except Exception as e:
            self.props.ProbeStatusOk = False
            self.props.Error = str(e)

    def _init_url(self, url):
        if not self.props.ProbeStatusOk:
            return
        source_url = None
        if self.props.ProbeHttps:
            source_url = u'https://{}'.format(url)
        elif self.props.ProbeVideo:
            source_url = u'http://{}'.format(url)
        if not source_url:
            return
        player_html = None
        if self.props.ProbeHtml:
            player_html = u'<iframe src="{}"></iframe>'.format(source_url)
        elif self.props.ProbeVideo:
            player_html = u'<video src="{}"/ controls>'.format(source_url)
        if player_html:
            self.props.ResultUrl = u'{}#html={}'.format(SANDBOX_URL, quote(player_html))

    @staticmethod
    def _sanitize_url(url):
        schemaless = None
        if url.startswith(u'https://'):
            schemaless = url[8:]
        elif url.startswith(u'http://'):
            schemaless = url[7:]
        elif url.startswith(u'//'):
            schemaless = url[2:]
        elif url.startswith(u'/'):
            return None
        else:
            schemaless = url
        item = urlparse(u'http://{}'.format(schemaless))
        if item.scheme != 'http' or not item.netloc:
            return None
        return schemaless


class HttpMagic(object):
    def __init__(self, config):
        self.ca_cert = config.casvc.cert_file
        self.proxy = 'http://localhost:{}'.format(config.http.proxy_port)
        self.timeout = config.http.timeout

    def player_url_features(self, url):
        return PlayerUrlFeatures(self, url).props
