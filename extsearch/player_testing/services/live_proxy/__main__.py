from conf import Config
from util import init_root_logger, get_node_fqdn
from models.worker import YDBConnection, WorkerModel
import tornado.httpclient
import tornado.httpserver
import tornado.gen
import tornado.ioloop
import tornado.web
from tornado.escape import url_escape
from tornado.log import access_log
from time import time
from os.path import join as pj, basename
from random import randint
import logging
import argparse
import json
import gzip
from io import BytesIO
from extsearch.video.robot.crawling.player_testing.services.live_proxy.metrics import Metrics


class PlaylistMuxer(object):
    def __init__(self, stream_dir, hostname, splash_gap=0, splash_id=None):
        self.splash_gap = splash_gap
        self.hostname = hostname
        self.splash_id = splash_id
        self.playlist = pj(stream_dir, 'stream.m3u8')
        self.target_duration = 1
        self.firstseg = None
        self.length = 5

    def _parse_source(self):
        segments = []
        for line in open(self.playlist):
            line = line.strip()
            if not line:
                continue
            if not line.startswith('#') and line.endswith('.ts'):
                segments.append(line)
        if len(segments) < self.length:
            raise Exception('invalid source playlist length {}'.format(len(segments)))
        return segments

    def _parse_sequence(self, segname):
        prefix = 'stream-'
        suffix = '.ts'
        pos = segname.find(prefix)
        if pos == -1:
            raise Exception('invalid source segment name {}'.format(segname))
        return int(segname[pos + len(prefix):-len(suffix)])

    def _fill_headers(self, sequence, discontinuity):
        return [
            '#EXTM3U',
            '#EXT-X-VERSION:3',
            '#EXT-X-ALLOW-CACHE:NO',
            '#EXT-X-TARGETDURATION:{}'.format(self.target_duration),
            '#EXT-X-MEDIA-SEQUENCE:{}'.format(sequence),
            '#EXT-X-DISCONTINUITY-SEQUENCE:{}'.format(discontinuity)
        ]

    def get_playlist(self, sid):
        segments = self._parse_source()
        sequence = self._parse_sequence(segments[0])
        if self.firstseg is None:
            self.firstseg = sequence
        if self.splash_gap:
            if sequence - self.firstseg <= self.splash_gap:
                sequence = self.firstseg
            else:
                sequence -= self.splash_gap
            splash_len = max(0, self.length - (sequence - self.firstseg))
            discontinuity_seq = int(splash_len == 0)
        else:
            splash_len = 0
            discontinuity_seq = 0
        content = self._fill_headers(sequence, discontinuity_seq)
        for i in range(self.length - splash_len, self.length):
            content.append('#EXTINF:{},'.format(self.target_duration))
            content.append('splash{}.ts?ch={}&fqdn={}&vsid={}'.format(i, self.splash_id, self.hostname, sid))
        live_len = self.length - splash_len
        if splash_len and live_len:
            content.append('#EXT-X-DISCONTINUITY')
        for seg in segments[self.length - live_len:]:
            content.append('#EXTINF:{},'.format(self.target_duration))
            content.append('{}?fqdn={}&vsid={}'.format(seg, self.hostname, sid))
        logging.info('final playlist: {}'.format('\n'.join(content)))
        return '\n'.join(content)


class AppContext(object):
    PING_DELAY = 10
    LOCK_TIMEOUT = 10
    STREAM_TIMEOUT = 30
    COOLDOWN_TIME = 10

    def __init__(self, config, db, table, peer_port, streamer_port, balancer, channels, metrics, splash_conf, sharing_enable):
        self.channels = channels
        self.config = config
        self.peer_port = peer_port
        self.streamer_port = streamer_port
        self.localhost = get_node_fqdn()
        self.balancer = balancer
        self.http = tornado.httpclient.AsyncHTTPClient()
        self.ioloop = tornado.ioloop.IOLoop.current()
        self.last_chunk_ts = 0
        self.ready = False
        self.streaming = False
        self.streaming_channel = None
        self.streaming_start = None
        self.lock_ts = 0
        self.workers = WorkerModel(db, table)
        self.workers.offline_worker()
        self.metrics = metrics
        self.start_seq = None
        self.playlist_muxer = None
        self.splash = splash_conf
        self.sharing_enable = sharing_enable

    def _client_gone(self):
        return self.streaming and self.last_chunk_ts + self.STREAM_TIMEOUT < time()

    def _lock_timeout(self):
        return self.lock_ts != 0 and self.lock_ts + self.LOCK_TIMEOUT < time()

    async def _start_streaming(self, channel_id, splash_gap):
        if not self.streaming:
            if channel_id not in self.channels:
                self.metrics.on_error('invalid_channel')
                raise Exception('invalid channel {} requested'.format(channel_id))
            self.metrics.on_streaming(channel_id)
            try:
                item = self.channels[channel_id]
                await self.http.fetch('http://localhost:{}/start?url={}&ch={}'.format(self.streamer_port, url_escape(item['url']), channel_id))
            except:
                self.metrics.on_error('streaming_start')
                raise
            try:
                self.workers.busy_worker(stream_id=channel_id)
            except:
                self.metrics.on_error('set_busy_state')
                await self.http.fetch('http://localhost:{}/stop'.format(self.streamer_port), request_timeout=30)
                raise
            self.playlist_muxer = PlaylistMuxer(self.config.stream_dir, self.localhost, splash_gap=splash_gap, splash_id=channel_id)
            self.streaming = True
            self.streaming_channel = channel_id
            self.streaming_start = time()
            self.lock_ts = 0
            self.start_seq = None
            self.metrics.count('stream_start')

    async def _cooldown_streaming(self):
        if not self.streaming:
            return
        try:
            await self.http.fetch('http://localhost:{}/cooldown'.format(self.streamer_port))
        except Exception as e:
            logging.error('cooldown request failed', e)

    async def _stop_streaming(self, force=False):
        if self.streaming or force:
            try:
                await self.http.fetch('http://localhost:{}/stop'.format(self.streamer_port), request_timeout=30)
            except:
                self.metrics.on_error('streaming_stop')
            finally:
                self._calc_view_time()
                self.streaming = False
                self.streaming_name = None
                self.streaming_start = None
                self.metrics.count('stream_stop')

    def _check_streaming(self, channel_id):
        if self.streaming and self.streaming_channel != channel_id:
            self.metrics.on_error('channel_mismatch')
            raise Exception('streaming channel mismatch')

    def _calc_view_time(self):
        if self.streaming_start is None:
            return
        view_time = time() - self.streaming_start
        if view_time > 0 and view_time < 86400:
            self.metrics.on_view_time(view_time)

    async def on_playlist_request(self, channel_id, splash_gap):
        if not self.ready:
            self.metrics.on_error('no_playlist')
            raise Exception('proxy service is offline')
        self.metrics.on_http_request('playlist')
        if not self.streaming:
            await self._start_streaming(channel_id, splash_gap)
        else:
            self._check_streaming(channel_id)
        self.last_chunk_ts = time()

    def on_lock_request(self):
        self.metrics.on_http_request('lock')
        if self.lock_ts != 0:
            raise Exception('got second lock')
        self.lock_ts = time()
        logging.info('worker node temporary locked up to {} sec'.format(self.LOCK_TIMEOUT))

    def run(self, immediate=False):
        delay = self.PING_DELAY if not immediate else 0
        self.ioloop.add_timeout(time() + delay, self._heartbeat)

    def _parse_ping_response(self, text):
        streaming = False
        for item in text.split('&'):
            key, value = item.split('=')
            if key == 'streaming':
                streaming = bool(int(value))
        return streaming

    async def _heartbeat(self):
        streamer_failed = False
        try:
            response = await self.http.fetch('http://localhost:{}/ping'.format(self.streamer_port))
            if self.streaming != self._parse_ping_response(response.body.decode('ascii')):
                logging.info('streaming state not synchronized, going offline')
                self.metrics.on_error('state_not_sync')
                streamer_failed = True
        except Exception as e:
            logging.info('streamer service failed: {}'.format(e))
            self.metrics.on_error('ping_failed')
            streamer_failed = True
        try:
            if streamer_failed:
                await self._stop_streaming(force=True)
                self.workers.offline_worker()
                self.ready = False
            else:
                do_release = not self.ready
                if self._lock_timeout():
                    self.lock_ts = 0
                    do_release = True
                elif self._client_gone():
                    await self._cooldown_streaming()
                    await tornado.gen.sleep(self.COOLDOWN_TIME)
                    await self._stop_streaming()
                    do_release = True
                if do_release:
                    self.workers.release_worker()
                else:
                    self.workers.update_worker()
                self.ready = True
        except Exception as e:
            logging.error('heartbeat error: {}'.format(e))
            self.streaming = False
            self.ready = False
        finally:
            self.run()


class LockHandler(tornado.web.RequestHandler):
    def initialize(self, appctx):
        self.appctx = appctx

    def get(self, *args, **kwargs):
        self.appctx.on_lock_request()


class PlaylistHandler(tornado.web.RequestHandler):
    def initialize(self, appctx):
        self.appctx = appctx
        self.localhost = appctx.localhost
        self.http = tornado.httpclient.AsyncHTTPClient()
        self.gzip = False

    def _get_playlist(self, sid):
        return bytes(self.appctx.playlist_muxer.get_playlist(sid), 'ascii')

    def _write_content(self, payload):
        if self.gzip:
            self.set_header('Content-Encoding', 'gzip')
            buf = BytesIO()
            handle = gzip.GzipFile(fileobj=buf, mode='w')
            handle.write(payload)
            handle.close()
            buf.seek(0)
            self.write(buf.getvalue())
        else:
            self.write(payload)

    async def get(self, *args, **kwargs):
        self.gzip = self.request.headers.get('Accept-Encoding', '').find('gzip') != -1
        self.set_header('Content-Type', 'application/x-mpegurl')
        self.set_header('Cache-Control', 'no-cache')
        fqdn = self.get_query_argument('fqdn', default=self.localhost)
        channel_id = self.get_query_argument('ch', default='1tv')
        sid = self.get_query_argument('vsid', default='')
        splash_gap = int(self.get_query_argument('splash', default=self.appctx.splash['gap']))
        content = None
        if fqdn == self.localhost:
            await self.appctx.on_playlist_request(channel_id, splash_gap)
            self._write_content(self._get_playlist(sid))
        else:
            response = await self.http.fetch('http://{}:{}/stream.m3u8?ch={}&splash={}&vsid={}'.format(fqdn, self.appctx.peer_port, channel_id, splash_gap, sid))
            self._write_content(response.body)


class ChunkHandler(tornado.web.RequestHandler):
    CONTENT_TYPE = 'video/MP2T'
    BUFFERING = 1024 * 1024
    SID_UPDATE_INTVL = 10

    def initialize(self, appctx):
        self.stream_dir = appctx.config.stream_dir
        self.splash_dir = appctx.splash['data_dir']
        self.peer_port = appctx.peer_port
        self.localhost = appctx.localhost
        self.http = tornado.httpclient.AsyncHTTPClient()
        self.metrics = appctx.metrics
        self.logger = logging.getLogger('ChunkHandler')
        self.workers = appctx.workers

    async def get(self, *args, **kwargs):
        self.metrics.on_http_request('chunk')
        fqdn = self.get_query_argument('fqdn', default=self.localhost)
        sid = self.get_query_argument('vsid', default='')
        http_path = basename(self.request.path)
        if fqdn != self.localhost:
            extra_cgi = 'ch={}&'.format(self.get_query_argument('ch')) if http_path.startswith('splash') else ''
            response = await self.http.fetch('http://{}:{}/{}?{}vsid={}'.format(fqdn, self.peer_port, http_path, extra_cgi, sid))
            self.set_header('Content-Type', self.CONTENT_TYPE)
            self.write(response.body)
            return
        try:
            if http_path.startswith('stream'):
                chunk_path = pj(self.stream_dir, http_path)
            elif http_path.startswith('splash'):
                chunk_path = pj(self.splash_dir, self.get_query_argument('ch'), http_path)
            payload = open(chunk_path, 'rb', self.BUFFERING).read()
            self.metrics.on_payload(len(payload))
            self.set_header('Content-Type', self.CONTENT_TYPE)
            self.finish(payload)
            if randint(0, self.SID_UPDATE_INTVL - 1) == 0:
                self.workers.update_sid(sid)
        except IOError as e:
            self.logger.error(e)
            self.set_status(404, 'Not found')
            self.finish()


class StartHandler(tornado.web.RequestHandler):
    HTML_BODY = '''
<?doctype html>
<html>
  <body>
    <video id="videojs-player-video">
      <source src="{url}" type="application/x-mpegurl"/>
    </video>
  </body>
</html>'''
    SID_TIMEOUT = 30

    def initialize(self, appctx):
        self.appctx = appctx
        self.workers = appctx.workers
        self.localhost = appctx.localhost
        self.peer_port = appctx.peer_port
        self.streamer_port = appctx.streamer_port
        self.balancer = appctx.balancer
        self.http = tornado.httpclient.AsyncHTTPClient()
        self.metrics = appctx.metrics
        self.sharing_enable = appctx.sharing_enable

    def _try_reuse_worker(self, sid):
        worker = None
        if not sid:
            return worker
        try:
            worker = self.workers.find_by_sid(sid, self.SID_TIMEOUT)
        except:
            self.metrics.on_error('restart_failed')
            return worker
        if worker is not None:
            self.metrics.on_error('restart_success')
        return worker

    async def _alloc_or_reuse_worker(self, sid):
        worker = self._try_reuse_worker(sid)
        if worker is not None:
            return worker
        try:
            worker = self.workers.alloc_worker()
        except:
            self.metrics.on_error('worker_alloc')
            return None
        try:
            self.workers.insert_sid(sid, worker)
            if worker == self.localhost:
                self.appctx.on_lock_request()
            else:
                lock_url = 'http://{}:{}/lock?vsid={}'.format(worker, self.peer_port, sid)
                logging.info('locking worker at {}'.format(lock_url))
                await self.http.fetch(lock_url)
        except Exception as e:
            logging.error('got error response: {}'.format(e))
            self.workers.release_worker(worker)
            self.metrics.on_error('worker_lock')
            return None
        return worker

    def _get_shared_worker(self, channel_id, sid):
        worker = None
        try:
            worker = self.workers.find_worker(channel_id)
            self.workers.insert_sid(sid, worker)
        except:
            logging.error('no shared worker for stream {}'.format(channel_id))
        return worker

    async def get(self, *args, **kwargs):
        self.metrics.on_http_request('start')
        channel_id = self.get_query_argument('ch', default='1tv')
        force_shared = bool(self.get_query_argument('shared', False))
        sid = self.get_query_argument('vsid', default='')
        worker = await self._alloc_or_reuse_worker(sid)
        if worker is None or force_shared:
            shared = None
            if self.sharing_enable and not self.appctx.channels[channel_id].get('sharing_disable', False):
                shared = self._get_shared_worker(channel_id, sid)
            else:
                logging.info('stream sharing is disabled')
            if worker is None and shared is None:
                self.metrics.on_error('denial_of_service')
                self.set_status(503, 'Service Unavailable')
                self.finish()
                return
            worker = shared
        splash_gap = int(self.get_query_argument('splash', self.appctx.splash['gap']))
        playlist_url = '{}/stream.m3u8?fqdn={}&ch={}&splash={}&vsid={}'.format(self.balancer, worker, channel_id, splash_gap, sid)
        self.write(self.HTML_BODY.format(url=playlist_url))


class StatHandler(tornado.web.RequestHandler):
    def initialize(self, appctx):
        self.appctx = appctx

    def get(self, *args, **kwargs):
        self.set_header('Content-Type', 'application/json')
        self.write(json.dumps({'metrics': self.appctx.metrics.get_metrics()}))


if __name__ == "__main__":
    init_root_logger()
    logging.basicConfig(level=logging.INFO)
    tornado.httpclient.AsyncHTTPClient.configure("tornado.curl_httpclient.CurlAsyncHTTPClient")
    ap = argparse.ArgumentParser()
    ap.add_argument('--config', required=True, type=argparse.FileType('r'))
    args = ap.parse_args()
    snail_conf = Config()
    app_conf = json.load(args.config)
    db = YDBConnection(app_conf['ydb'])
    http = app_conf['http']
    metrics = Metrics()
    appctx = AppContext(snail_conf,
                        db,
                        app_conf['ydb']['table'],
                        http['bind_port'],
                        http['streamer_port'],
                        http['balancer_url'],
                        app_conf['channels'],
                        metrics,
                        app_conf['splash'],
                        app_conf.get('sharing_enable', False))
    settings = {
        'appctx': appctx
    }
    app = tornado.web.Application([
        (r'/stream.m3u8', PlaylistHandler, settings),
        (r'/(stream\-|splash)[0-9]+.ts', ChunkHandler, settings),
        (r'/start', StartHandler, settings),
        (r'/lock', LockHandler, settings)
    ], compress_response=True)
    server = tornado.httpserver.HTTPServer(app, no_keep_alive=False)
    server.listen(http['bind_port'])
    stat = tornado.web.Application([
        (r'/stat', StatHandler, settings)
    ])
    stat.listen(http['stat_port'])
    appctx.run(immediate=True)
    tornado.ioloop.IOLoop.current().start()
