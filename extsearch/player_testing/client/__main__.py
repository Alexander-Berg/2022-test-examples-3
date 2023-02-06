from conf import Config
from sqs import SQSClient
from job import verify_job
from util import init_root_logger
from time import time, sleep
import yt.wrapper as yt
import argparse
import logging
import json
import sys
from base64 import b64decode
from hashlib import md5
from os import environ
from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJob, TJobResult
from google.protobuf.json_format import MessageToJson


def make_job_id(args):
    if args.job_id is not None and len(args.job_id) > 0:
        return args.job_id
    else:
        return '{}{}'.format('' if args.job_id_prefix is None else args.job_id_prefix, md5(str(time())).hexdigest())


def get_params(args, params):
    max_retry = args.max_retry
    http_magic = args.http_magic
    browser = args.browser
    device = args.device
    play_script = None
    vec = params.strip().split(',')
    for item in vec:
        kv = item.split('=')
        if len(kv) == 2:
            key, value = kv
            try:
                if key == 'max_retry':
                    max_retry = int(value)
                elif key == 'http_magic':
                    http_magic = bool(value)
                elif key == 'browser':
                    browser = value
                elif key == 'device':
                    device = value
                elif key == 'jsapi.play':
                    play_script = 'document.querySelector("iframe").contentWindow.postMessage({method:"play"},"*")'
                elif key == 'playscript':
                    play_script = b64decode(value)
            except Exception:
                continue
    if args.play_script_file:
        play_script = open(args.play_script_file).read()
    return max_retry, http_magic, browser, device, play_script


def apply_profile(job, profile):
    if profile == 'default':
        job.Flags.CheckPopup = True
        job.Flags.CheckScrolling = True
        job.Crawl.RetryCount = 3
    elif profile == 'vdp':
        job.Flags.Fast = True
        job.Crawl.RetryCount = 3
    elif profile == 'bs_content':
        job.OutputPipeline = 'bs_content'
        job.VideoCapture.Duration = 360


def push_task(config, args):
    if args.dry_run:
        logging.warn('working in dry run mode!')
    sqs = SQSClient(config.sqs)
    queue = sqs.get_queue(args.source)
    job_id = make_job_id(args)
    maxinfly = args.max_infly
    if args.rpm and maxinfly is None:
        maxinfly = 3 * args.rpm
    sent = 0
    minibatch = 0
    ts = time()
    for line in args.input:
        row = line.strip().split('\t')
        if not row:
            continue
        url = row[0].strip()
        if not url:
            continue
        params = row[1] if len(row) > 1 else ''
        max_retry, http_magic, browser, device, play_script = get_params(args, params)
        try:
            job_item = TJob()
            job_item.Url = url
            job_item.Id = job_id
            apply_profile(job_item, args.profile)
            job_item.Browser = browser
            job_item.Device = device
            job_item.Crawl.RetryCount = max_retry
            if http_magic:
                job_item.Flags.HttpProbe = True
            if play_script is not None:
                job_item.Scripts.Play = play_script
                job_item.Scripts.Timeout = args.script_timeout
                if args.cookie_file:
                    job_item.Scripts.Cookies = open(args.cookie_file).read().strip()
            if not args.disable_browser_autodetect:
                job_item.Flags.BrowserAutodetect = True
            if args.dont_click:
                job_item.Flags.DontClick = True
            verify_job(job_item)
            if args.dry_run:
                logging.info('item {}: {}'.format(sent, MessageToJson(job_item)))
                continue
            queue.push(job_item.SerializeToString())
        except Exception as e:
            logging.error('push: {}'.format(e))
            continue
        sent += 1
        minibatch += 1
        if args.rpm and minibatch >= args.rpm:
            delay = int(ts + 60 - time())
            if delay > 0:
                logging.info('{} sent, sleeping {}s'.format(sent, delay))
                sleep(delay)
            ts = time()
            minibatch = 0
        if maxinfly:
            while queue.size() >= maxinfly:
                logging.info('queue is full, sleeping')
                sleep(30)

    args.output_json.write(json.dumps({'job_id': job_id, 'job_size': sent, 'started': int(time())}))


def wait_task(config, args):
    job = json.load(args.job_json)
    if job['job_size'] <= 0:
        args.output_json.write(json.dumps(job))
        return
    updated = None
    recall = 0.0
    results = 0
    client = yt.YtClient(proxy=config.yt_writer.proxy, token=environ.get('YT_TOKEN'))
    while True:
        now = int(time())
        results = 0
        for row in client.read_table(yt.TablePath(config.yt_merge.prev_state, columns=['started'], exact_key=job['job_id'])):
            updated = max(updated, row['started'])
            results += 1
        update_min = (now - updated) / 60 if updated is not None else -1
        recall = min(1.0, float(results) / job['job_size'])
        logging.info("job [%s] %d/%d/%d%% up %d min ago", job['job_id'], results, job['job_size'], 100*recall, update_min)
        if job['started'] + args.wait_timeout < now or (updated is not None and updated + args.job_timeout < now):
            break
        sleep(600)
    if recall < args.recall:
        logging.error('job [%s] wait timeout', job['job_id'])
        sys.exit(-1)
    job['output_size'] = results
    job['updated'] = updated
    job['recall'] = recall
    args.output_json.write(json.dumps(job))


class OutputItem(object):
    def __init__(self, config, row):
        self.url = row['url']
        self.env_type = config.env
        self.overlap = 1
        self.data = self._get_props(row)

    def _get_error(self, res):
        if res.Error and not res.IsPlaying:
            return res.Error
        elif res.Http.Error:
            return res.Http.Error
        elif res.Env != self.env_type:
            return 'client environment {} != {}'.format(self.env_type, res.Env)

    def _get_props(self, row):
        res = TJobResult()
        res.ParseFromString(row['data'])
        err = self._get_error(res)
        if err:
            logging.error('skipping row: {}'.format(err))
            return None
        data = {}
        data['is_playing'] = res.IsPlaying
        data['host'] = res.Host
        data['started'] = res.Started
        data['img_url'] = res.Artifacts[-1].Url if res.Artifacts else ''
        player = res.Player
        data['is_popup'] = player.IsPopup
        data['is_scrolling'] = player.IsScrolling
        data['play_area1'] = player.AutoplayArea
        data['play_area2'] = player.WorkingArea
        data['has_known_player'] = len(player.Known) > 0
        data['players'] = [known_player.PlayerId for known_player in player.Known]
        http = res.Http
        data['is_valid'] = http.ProbeStatusOk
        data['is_https'] = http.ProbeHttps
        data['is_html'] = http.ProbeHtml
        data['is_video'] = http.ProbeVideo
        data['user_agent'] = http.UserAgent
        data['content_url'] = http.Contents[-1].Url if http.Contents else ''
        data['raw'] = res

        return data

    def update(self, row):
        self.overlap += 1
        data = self._get_props(row)
        if not data:
            return
        if not self.data:
            self.data = data
        else:
            for col, value in self.data.iteritems():
                self.data[col] = max(value, data.get(col))
        self.data['raw'].IsPlaying = self.data['is_playing']

    def as_list(self):
        return [self.url, 'OK' if self.data['is_playing'] else 'BAD', self.overlap]

    def as_dict(self):
        res = self.data.copy()
        res['url'] = self.url
        res['overlap'] = self.overlap
        res['status'] = 'OK' if self.data['is_playing'] else 'BAD'
        del res['raw']
        return res

    def valid(self):
        return self.data is not None


def get_task(config, args):
    job = json.load(args.job_json)
    results = {}
    client = yt.YtClient(proxy=config.yt_writer.proxy, token=environ.get('YT_TOKEN'))
    for row in client.read_table(yt.TablePath(config.yt_merge.prev_state, exact_key=job['job_id'])):
        url = row['url']
        if url not in results:
            results[url] = OutputItem(config, row)
        else:
            results[url].update(row)
    if args.json:
        json.dump([item.as_dict() for item in results.itervalues() if item.valid()], args.output, indent=1)
    else:
        for item in results.itervalues():
            if not item.valid():
                continue
            if args.json_raw:
                print >>args.output, json.dumps(json.loads(MessageToJson(item.data['raw'])))
            else:
                print >>args.output, '\t'.join(map(str, item.as_list()))


if __name__ == '__main__':
    init_root_logger()
    config = Config()
    ap = argparse.ArgumentParser()
    sp = ap.add_subparsers(dest='cmd')
    push_sp = sp.add_parser('push')
    push_sp.add_argument('--input', type=argparse.FileType('r'), required=True)
    push_sp.add_argument('--max-retry', type=int, default=3)
    push_sp.add_argument('--job-id')
    push_sp.add_argument('--job-id-prefix')
    push_sp.add_argument('--source', default='video_player_testing_crawl')
    push_sp.add_argument('--rpm', type=int, default=10)
    push_sp.add_argument('--max-infly', type=int)
    push_sp.add_argument('--http-magic', action='store_true')
    push_sp.add_argument('--output-json', type=argparse.FileType('w'), required=True)
    push_sp.add_argument('--browser', type=str, default='auto')
    push_sp.add_argument('--device', type=str, default='desktop')
    push_sp.add_argument('--profile', default='default')
    push_sp.add_argument('--dry-run', action='store_true', default=False)
    push_sp.add_argument('--disable-browser-autodetect', action='store_true', default=False)
    push_sp.add_argument('--play-script-file')
    push_sp.add_argument('--cookie-file')
    push_sp.add_argument('--script-timeout', type=int, default=0)
    push_sp.add_argument('--dont-click', action='store_true', default=False)
    wait_sp = sp.add_parser('wait')
    wait_sp.add_argument('--job-json', type=argparse.FileType('r'), required=True)
    wait_sp.add_argument('--job-timeout', type=int, default=3600, help='job update timeout, sec')
    wait_sp.add_argument('--wait-timeout', type=int, default=86400, help='total wait timeout, sec')
    wait_sp.add_argument('--recall', type=float, default=0.95, help='job recall value required')
    wait_sp.add_argument('--output-json', type=argparse.FileType('w'), required=True)
    get_sp = sp.add_parser('get')
    get_sp.add_argument('--job-json', type=argparse.FileType('r'), required=True)
    get_sp.add_argument('--output', required=True, type=argparse.FileType('w'))
    get_sp.add_argument('--json', action='store_true')
    get_sp.add_argument('--json-raw', action='store_true')
    args = ap.parse_args()
    if args.cmd == 'push':
        push_task(config, args)
    elif args.cmd == 'wait':
        wait_task(config, args)
    elif args.cmd == 'get':
        get_task(config, args)
    else:
        raise Exception('unknown cmd {}'.format(args.cmd))
