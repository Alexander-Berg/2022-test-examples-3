from os import environ
from time import sleep
from os.path import join as pj, exists
from shutil import rmtree
import requests
import subprocess
import logging
from threading import Thread


class Chromium(object):
    TIMEOUT = 10

    def __init__(self, display, proxy_port, extension, work_dir):
        self.extension = extension
        self.proc = None
        self.url = None
        self.logthr = []
        self.user_data_dir = pj(work_dir, 'chrome')
        self.env = environ.copy()
        self.env['DISPLAY'] = display.id
        self.env['LANG'] = 'ru_RU.UTF-8'
        self.proxy_port = proxy_port
        self.clear_cookies()

    def clear_cookies(self):
        logging.info('cleaning browser cookies')
        if exists(self.user_data_dir):
            logging.info('removing {}'.format(self.user_data_dir))
            rmtree(self.user_data_dir)

    def _wait_for_proxy(self, timeout=60):
        proxy = 'http://localhost:{}'.format(self.proxy_port)
        logging.info('waiting for proxy {}'.format(proxy))
        for i in range(timeout):
            try:
                requests.get('{}/ping'.format(proxy))
            except:
                sleep(1)
                continue
            logging.info('proxy {} OK'.format(proxy))
            return
        raise Exception('proxy {} timeout'.format(proxy))

    def _log_writer(self, fd, suffix):
        logger = logging.getLogger('Chromium:{}'.format(suffix))
        logger.info('Thread started')
        while True:
            line = fd.readline()
            if not line:
                break
            line = line.strip()
            if line.find('resource_bundle.cc') != -1 or line.find('bus.cc') != -1:
                continue
            if line:
                logger.info(line)
        logger.info('Thread is going to terminate')

    def _add_logger(self, fd, suffix):
        thr = Thread(target=self._log_writer, args=(fd, suffix))
        thr.start()
        self.logthr.append(thr)

    def start(self, url=None, no_proxy=False):
        if self.proc is not None:
            if self.proc.poll() is None:
                return
            logging.error('chromium instance is dead')
        cmd = ['chromium-browser',
               '--load-extension={}'.format(self.extension),
               '--enable-logging=stderr',
               '--start-maximized',
               '--disable-notifications',
               '--lang=ru',
               '--user-data-dir={}'.format(self.user_data_dir)]
        if self.proxy_port and not no_proxy:
            self._wait_for_proxy()
            cmd.append('--proxy-server=localhost:{}'.format(self.proxy_port))
        if url:
            cmd.append(url)
        logging.info('starting {}'.format(' '.join(cmd)))
        self.proc = subprocess.Popen(cmd, env=self.env, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self._add_logger(self.proc.stderr, 'CERR')
        self._add_logger(self.proc.stdout, 'COUT')

    def stop(self):
        if self.proc is not None:
            self.proc.terminate()
            done = False
            for i in range(self.TIMEOUT):
                if self.proc.poll() is not None:
                    done = True
                    break
                sleep(1)
            if not done:
                self.proc.kill()
            self.proc = None

    def open(self, url):
        self.start()
        cmd = ['chromium-browser', '--app={}'.format(url), '--user-data-dir={}'.format(self.user_data_dir)]
        logging.info('running {}'.format(' '.join(cmd)))
        subprocess.check_call(cmd)
        self.url = url

    def close(self):
        if self.url is None:
            return
        cmd = ['xdotool', 'getactivewindow', 'windowkill']
        logging.info('running {}'.format(' '.join(cmd)))
        subprocess.check_call(cmd, env=self.env)
        self.url = None

    def is_alive(self):
        return self.proc is not None and self.proc.poll() is None
