import subprocess
import logging
from threading import Thread
from time import sleep
from os.path import join as pj
from os import environ
from display import Display


class FFPlay(object):
    TIMEOUT = 5

    def __init__(self, resource_dir, disp):
        self.resource_dir = resource_dir
        self.disp = disp
        self.proc = None
        self.logger = logging.getLogger('FFPlay')
        self.logthr = []

    def make_splash(self, logo=None):
        self.logger.info('switching to splash screen')
        vfilter = ['[in]scale=100:100[int]']
        if logo is None:
            vfilter.append('[int]negate,pad=1280:720:590:310[out]')
        else:
            vfilter.extend(['movie={},fps=8,scale=1280:720[logo]'.format(logo),
                            '[logo][int]overlay=(W - w)/2:(H - h)/2,pad=1280:720:(1280-iw)/2:(720-ih)/2[out]'])
        args = ['-loop', '0', pj(self.resource_dir, 'loading.gif'), '-vf', ';'.join(vfilter)]
        self._start(args)

    def make_mirror(self, src):
        self.logger.info('switching to mirror display {}'.format(src.id))
        args = ['-video_size', '{}x{}'.format(self.disp.width, self.disp.height), '-draw_mouse', '0', '-f', 'x11grab', src.id]
        self._start(args)

    def _log_writer(self, fd, suffix):
        logger = logging.getLogger('FFPlay:{}'.format(suffix))
        logger.info('Thread started')
        while True:
            line = fd.readline()
            if not line:
                break
            line = line.strip()
            if line:
                logger.info(line)
        logger.info('Thread is going to terminate')

    def _add_logger(self, fd, suffix):
        thr = Thread(target=self._log_writer, args=(fd, suffix))
        thr.start()
        self.logthr.append(thr)

    def _start(self, args):
        self.stop()
        cmd = ['ffplay', '-fs', '-loglevel', 'warning']
        cmd.extend(args)
        env = environ.copy()
        env['DISPLAY'] = self.disp.id
        self.proc = subprocess.Popen(cmd, env=env, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        self.logger.info('start: [{}] {}'.format(self.proc.pid, ' '.join(cmd)))
        self._add_logger(self.proc.stderr, 'CERR')
        self._add_logger(self.proc.stdout, 'COUT')

    def stop(self):
        if self.proc is None:
            return
        if self.proc.poll() is not None:
            self.proc = None
            self.logger.info('stop: [{}] is dead'.format(self.proc.pid))
            return
        self.logger.info('stop: terminating [{}]'.format(self.proc.pid))
        self.proc.terminate()
        done = False
        for i in range(self.TIMEOUT * 10):
            sleep(0.1)
            if self.proc.poll() is not None:
                done = True
                break
        if not done:
            self.logger.error('failed to terminate [{}], sending KILL'.format(self.proc.pid))
            self.proc.kill()
        self.proc = None


class MultiDisplay(object):
    def __init__(self, config, resource_dir):
        self.front = Display(config.display, config.display.id)
        self.back = Display(config.display, config.display.id + 1)
        self.player = FFPlay(resource_dir, self.front)

    def set_splash(self, logo=None):
        self.player.make_splash(logo)

    def set_mirror(self, disp):
        self.player.make_mirror(disp)

    def start(self):
        self.front.start()
        self.back.start()
        sleep(1)
        self.set_splash()
        sleep(1)

    def stop(self):
        self.player.stop()
        self.front.stop()
        self.back.stop()
