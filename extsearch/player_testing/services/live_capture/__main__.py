from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import socket
from conf import Config
from input_device import InputDevice
from util import init_root_logger
from os.path import join as pj
from os import environ
from time import time, sleep
from sys import exit
import argparse
import logging
import traceback
import threading
import pulse
from library.python import resource
from urlparse import urlparse
from urllib import unquote
from extsearch.video.robot.crawling.player_testing.services.live_capture.chromium import Chromium
from extsearch.video.robot.crawling.player_testing.services.live_capture.vlc_capture import VLCStreamCapture
from extsearch.video.robot.crawling.player_testing.services.live_capture.multidisplay import MultiDisplay
from extsearch.video.robot.crawling.player_testing.services.live_capture.thumbcache import ThumbnailCache


class AppContext(object):
    DEF_STREAM_URL = 'https://www.1tv.ru/live'
    PLAYLIST = 'stream.m3u8'
    COOKIE_CLEANUP_INTVL = 60 * 60 * 2

    def __init__(self, resource_dir, channels_conf, restreamed_conf):
        self.config = Config()
        self.streaming = False
        self.cooldown = False
        self.alive = None
        self.config.init_workdir()
        self.thumbcache = ThumbnailCache(self.config.work_dir, channels_conf, restreamed_conf)
        self.display = MultiDisplay(self.config, resource_dir)
        self.input_device = InputDevice(self.display.back)
        self.video_capture = VLCStreamCapture(self.display.front, self.config.stream_dir, self.PLAYLIST)
        self.browser = Chromium(self.display.back, None, pj(resource_dir, 'extension'), self.config.work_dir)
        self.display.start()
        self._fill_cookies()
        self.video_capture.start()
        self.browser.start()
        self.started = int(time())
        self.hide_cursor_ts = None

    def _fill_cookies(self, timeout=10, sync=True):
        self.browser.start(self.DEF_STREAM_URL)
        if sync:
            sleep(timeout)
            self.browser.stop()

    def _health_status(self):
        vlc_status = self.video_capture.is_alive()
        return vlc_status if vlc_status is not None else True

    def _terminate(self):
        thread = threading.Thread(target=httpd.shutdown)
        thread.daemon = True
        thread.start()

    def _try_hide_cursor(self):
        if self.hide_cursor_ts is None or self.hide_cursor_ts >= int(time()):
            return
        disp = self.display.front
        self.input_device.move_cursor(disp.width / 2, disp.height / 2).execute()
        self.hide_cursor_ts = None

    def do_http_get(self, path):
        parsed = urlparse(path)
        path = parsed.path
        query = {}
        for item in filter(lambda v: len(v) != 0, parsed.query.split('&')):
            key, value = item.split('=')
            query[key] = value.strip()
        if path == '/start':
            if self.streaming:
                raise Exception('capture is already running')
            if not self.browser.is_alive():
                self._terminate()
                raise Exception('browser is not running')
            channel_id = query.get('ch')
            logo = self.thumbcache.get(channel_id)
            if logo is not None:
                self.display.set_splash(logo)
            self.browser.open(unquote(query.get('url', self.DEF_STREAM_URL)))
            self.streaming = True
            self.cooldown = False
            self.started = int(time())
        elif path == '/cooldown':
            if self.streaming and self.started + self.COOKIE_CLEANUP_INTVL < time():
                self.cooldown = True
                self.display.set_splash()
                self.browser.stop()
                self.browser.clear_cookies()
                self._fill_cookies(timeout=0, sync=False)
        elif path == '/stop':
            if self.streaming:
                self.display.set_splash()
                self.browser.stop()
                self.browser.start()
                self.streaming = False
                self.cooldown = False
        elif path == '/ping':
            healthy = self._health_status()
            if not healthy:
                logging.error('health status check failed, exiting')
                self._terminate()
            else:
                self._try_hide_cursor()
            return 'id={}&streaming={}'.format(self.started, int(self.streaming))
        elif path == '/js/ping':
            return 'OK' if self.streaming else 'stop'
        elif path == '/js/stream':
            if not self.cooldown:
                self.display.set_mirror(self.display.back)
        elif path == '/js/idle':
            self.display.set_splash()
        elif path == '/js/fullscreen':
            self.input_device.press_key('Down').execute()
            self.input_device.move_cursor(1, 25).click().execute()
        elif path == '/js/click':
            x = int(float(query.get('x', 0)))
            y = int(float(query.get('y', 0)))
            self.input_device.move_cursor(x, y).click().execute()
            self.hide_cursor_ts = int(time()) + 3

    def destroy(self):
        logging.info('going to shutdown')
        self.browser.stop()
        self.video_capture.stop()
        self.display.stop()


def get_request_handler(context):
    class HTTPRequestHandler(BaseHTTPRequestHandler):
        def __init__(self, *args):
            BaseHTTPRequestHandler.__init__(self, *args)

        def do_GET(self):
            try:
                body = context.do_http_get(self.path)
                if body is None:
                    body = 'OK'
                self.send_response(200)
                self.send_header('Connection', 'close')
                self.send_header('Content-Length', str(len(body)))
                self.send_header('Content-Type', 'text/plain')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(body)
            except Exception as e:
                self.send_response(500)
                self.end_headers()
                self.wfile.write(str(e))
                logging.error(traceback.format_exc())
            except SystemExit:
                raise
    return HTTPRequestHandler


class HTTPServerV6(HTTPServer):
    address_family = socket.AF_INET6


if __name__ == '__main__':
    init_root_logger()
    environ['SNAIL_ENV'] = 'live'
    ap = argparse.ArgumentParser()
    ap.add_argument('--port', type=int, default=8000)
    ap.add_argument('--resource-dir', default='resources')
    ap.add_argument('--channels-conf', required=True)
    ap.add_argument('--restreamed-conf', required=True)
    args = ap.parse_args()
    logging.info('starting live capture service')
    pulse.init()
    context = AppContext(args.resource_dir, args.channels_conf, args.restreamed_conf)
    httpd = HTTPServerV6(('', args.port), get_request_handler(context))
    httpd.serve_forever()
    context.destroy()
    logging.info('live capture service successfully terminated')
