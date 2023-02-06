from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJob
from conf import SANDBOX_PREFIX, SANDBOX_URL
from util import eval_player_id
from xml.dom import minidom
import json
import logging

try:
    from urllib.parse import quote, unquote, urlparse, urlunparse
except ImportError:
    from urllib import quote, unquote
    from urlparse import urlparse, urlunparse

GUESS_BROWSER_BY_PLAYER_ID_RULES = {
    'ivi': 'firefox',
    'ivi_trailer': 'firefox',
    'showjet': 'firefox',
    'mailru_new': 'firefox',
    'moretv': 'firefox',
    'ntv': 'firefox',
    'tiktok': 'firefox',
}

GUESS_BROWSER_BY_HOST_RULES = {
    'www.ntv.ru': 'firefox',
    'odysseus.more.tv': 'firefox',
    'odysseus.ctc.ru': 'firefox',
    'www.tiktok.com': 'firefox',
    'video.mail.ru': 'firefox',
    'my.mail.ru': 'firefox',
    'm.spankbang.com': 'firefox',
    'spankbang.com': 'firefox',
    'static.spankbang.com': 'firefox',
    'ru.spankbang.com': 'firefox',
}

VALID_BROWSER = ['chromium', 'chromedriver', 'firefox']

VALID_DEVICE = ['desktop', 'iphone', 'android']

AUTOPLAY_HOSTS = ['www.youtube.com', 'ok.ru', 'vk.com', 'my.mail.ru', '24v.tv', 'www.ntv.ru']


def parse_iframe_src(url):
    data = urlparse(url)
    if not data.fragment:
        raise ValueError("data.fragment is None")
    player_code = None
    for item in data.fragment.split('&'):
        if item.startswith('html='):
            player_code = unquote(item[5:])
            break
    if not player_code:
        raise ValueError("player_code is None")
    dom = minidom.parseString(player_code)
    node = dom.childNodes[0]
    if node.nodeName != 'iframe':
        raise ValueError("iframe not found")
    return node


def get_iframe_src_host(url):
    return urlparse(parse_iframe_src(url).getAttribute('src')).netloc


def guess_browser(job):
    result = 'chromium'

    # host rules have more priority than player_id rules, so they are checked at last
    try:
        # apply player_id rules
        player_id = eval_player_id(job.Url)
        if player_id in GUESS_BROWSER_BY_PLAYER_ID_RULES:
            result = GUESS_BROWSER_BY_PLAYER_ID_RULES[player_id]

        # apply host rules
        if job.Url.startswith(SANDBOX_PREFIX):
            host = get_iframe_src_host(job.Url)
            if host in GUESS_BROWSER_BY_HOST_RULES:
                result = GUESS_BROWSER_BY_HOST_RULES[host]
    except Exception as e:
        logging.error('guess_browser failed on url {} with error: {}'.format(job.Url, e))

    job.Browser = result


def verify_browser(job):
    if job.Browser in VALID_BROWSER:
        return
    guess_browser(job)


def verify_device(job):
    if job.Device not in VALID_DEVICE:
        job.Device = VALID_DEVICE[0]


def verify_job(job):
    if not job.Url:
        raise Exception('verify_job: empty URL')
    if not job.Id:
        raise Exception('verify_job: empty job ID')
    verify_browser(job)
    verify_device(job)


def from_string(buf):
    job = TJob()
    job.ParseFromString(buf)
    verify_job(job)
    return job


def add_autoplay(url, force=False):
    if not url.startswith(SANDBOX_PREFIX):
        return url
    try:
        node = parse_iframe_src(url)
        src = urlparse(node.getAttribute('src'))
        if not force and src.netloc not in AUTOPLAY_HOSTS:
            return url
        params = filter(lambda param: not param.startswith('autoplay'), src.query.split('&')) if src.query else []
        params.append('autoplay=1')
        query = '&'.join(params)
        node.setAttribute('src', urlunparse((src.scheme, src.netloc, src.path, src.params, query, src.fragment)))
        node.setAttribute('allow', 'autoplay; fullscreen')
        return '{}#html={}'.format(SANDBOX_URL, quote(node.toxml()))
    except Exception as e:
        logging.error('add_autoplay: {}'.format(e))
        return url
