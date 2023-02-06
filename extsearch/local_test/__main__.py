from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJob
from google.protobuf.json_format import MessageToJson
from conf import Config
from job import verify_job
from player_test import PlayerTest
from log_aggregator import LogAggregatorClient
from time import sleep
from sys import exit
import subprocess
import argparse
import requests
import logging
import os


class ServicesContext(object):
    def __init__(self, bindir, config):
        self.config = config
        self.proxy = subprocess.Popen([
            os.path.join(bindir, 'cpproxy'),
            '-p', str(config.http.proxy_port),
            '-c', str(config.casvc.server_port),
            '-l', str(config.logsvc.server_port)
        ]) if config.http.proxy_port else None
        self.casvc = subprocess.Popen([
            os.path.join(bindir, 'cert_gen')
        ])
        self.logsvc = subprocess.Popen([
            os.path.join(bindir, 'log_aggregator')
        ])
        logging.info('services started')

    def wait_ready(self):
        svc = [self.config.http.proxy_port, self.config.casvc.server_port, self.config.logsvc.server_port]
        for i in range(10):
            try:
                for port in svc:
                    if not port:
                        continue
                    requests.get('http://[::1]:{}/ping'.format(port)).raise_for_status()
                    logging.info('HTTP port {} OK'.format(port))
                return
            except Exception as e:
                print e
                sleep(1)
        raise Exception('services not ready')

    def stop(self):
        if self.proxy:
            self.proxy.kill()
        self.casvc.kill()
        self.logsvc.kill()
        logging.info('services terminated')


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    ap = argparse.ArgumentParser()
    ap.add_argument('--url', default='https://www.youtube.com/embed/XDaWK39MwkQ')
    ap.add_argument('--work-dir', default='/work')
    ap.add_argument('--bin-dir', default='/bin')
    ap.add_argument('--base-port', type=int, default=8100)
    ap.add_argument('--browser', default='chromium')
    ap.add_argument('--device', default='desktop')
    ap.add_argument('--play-script', help='e.g. document.querySelector("iframe").contentWindow.postMessage({method: "play", "*")')
    ap.add_argument('--play-script-file', type=argparse.FileType())
    ap.add_argument('--cookie-file', type=argparse.FileType())
    ap.add_argument('--fast-flag', action='store_true')
    ap.add_argument('--script-timeout', type=int, default=0)
    args = ap.parse_args()
    os.environ['SNAIL_ENV'] = 'local'
    os.environ['SNAIL_DOCKER'] = '1'
    os.environ['SNAIL_BASE_PORT'] = str(args.base_port)
    os.environ['SNAIL_WORK'] = args.work_dir
    os.environ['SNAIL_DISPLAY'] = '43'
    config = Config()
    config.init_workdir()
    services = ServicesContext(args.bin_dir, config)
    retcode = 0
    try:
        services.wait_ready()
        checker = PlayerTest(config)
        logsvc = LogAggregatorClient(config.logsvc, config.http.proxy_port)
        checker.start()
        job_item = TJob()
        job_item.Url = args.url
        job_item.Id = 'local_job'
        if args.fast_flag:
            job_item.Flags.Fast = True
        job_item.Browser = args.browser
        job_item.Device = args.device
        if args.play_script or args.play_script_file:
            script = args.play_script if args.play_script else args.play_script_file.read()
            job_item.Flags.DontClick = True
            job_item.Scripts.Play = script
            job_item.Scripts.Timeout = args.script_timeout
            if args.cookie_file:
                job_item.Scripts.Cookies = args.cookie_file.read().strip()
        verify_job(job_item)
        result = checker.check_player_url(job_item, logsvc)
        checker.stop()
        logging.info('test result: {}'.format(MessageToJson(result)))
        assert result.Finished != 0, 'job processing failed'
        logging.info('job processing time: {} sec'.format(result.Finished - result.Started))
    except Exception as e:
        logging.error('player check failed: {}'.format(str(e)))
        retcode = 1
    finally:
        services.stop()
    exit(retcode)
