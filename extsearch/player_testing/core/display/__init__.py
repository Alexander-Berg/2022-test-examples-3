from os import environ, unlink
from os.path import exists
import cv2 as cv
import subprocess
import tempfile
import logging
from time import sleep


class Viewport(object):
    def __init__(self, left, top, width, height):
        self.left = left
        self.top = top
        self.width = width
        self.height = height

    def __repr__(self):
        return '(left:{} top:{} width:{} height:{})'.format(self.left, self.top, self.width, self.height)


class Display(object):
    def __init__(self, config, disp_id=None):
        self.id = ':{}'.format(disp_id if disp_id is not None else config.id)
        self.width, self.height = config.width, config.height
        self.x11proc = None
        self.wmproc = None
        self.is_ext = config.is_ext
        self.viewport = Viewport(0, 0, self.width, self.height)
        logging.info('display {} width:{} height:{}'.format(self.id, self.width, self.height))

    def set_viewport(self, vp):
        if vp is None:
            self.viewport.left = 0
            self.viewport.top = 0
            self.viewport.width = self.width
            self.viewport.height = self.height
        else:
            if vp.left < 0 or vp.left + vp.width > self.width:
                raise Exception('invalid viewport horizontal settings {}'.format(vp))
            if vp.top < 0 or vp.top + vp.height > self.height:
                raise Exception('invalid viewport vertical settings {}'.format(vp))
            self.viewport.left = vp.left
            self.viewport.top = vp.top
            self.viewport.width = vp.width
            self.viewport.height = vp.height

    def start(self):
        environ['DISPLAY'] = self.id
        if not self.is_ext:
            self._startx()
            self.wmproc = subprocess.Popen(['openbox'])
            logging.info('display: Xvfb [{}]'.format(self.x11proc.pid))
        else:
            tmp_cap = tempfile.mktemp(suffix='.png')
            self.capture_screen(tmp_cap)
            img = cv.imread(tmp_cap)
            self.width = img.shape[1]
            self.height = img.shape[0]
            logging.info('display: detected {}x{} screen'.format(self.width, self.height))

    def _startx(self):
        lock_file = '/tmp/.X{}-lock'.format(self.id[1:])
        if exists(lock_file):
            logging.info('display: trying to remove Xvfb lock file {}'.format(lock_file))
            unlink(lock_file)
        self.x11proc = subprocess.Popen(['Xvfb', self.id, '-screen', '0', '{}x{}x24'.format(self.width, self.height)])
        started = False
        for i in range(10):
            sleep(1)
            if exists('/tmp/.X11-unix/X{}'.format(self.id[1:])):
                started = True
                break
        if not started:
            logging.error('display: X server start timeout')

    def stop(self):
        if not self.is_ext:
            if self.wmproc is not None:
                self.wmproc.terminate()
                self.wmproc.wait()
            self.x11proc.terminate()
            self.x11proc.wait()
        logging.info('display: {} done'.format(self.id))

    def capture_screen(self, dst_path):
        res = subprocess.check_call(['import', '-window', 'root', '-define', 'png:bit-depth=8', '-define', 'png:color-type=2', dst_path])
        if res != 0:
            raise Exception('screen_capture failed: {}'.format(res))
        crop = self.viewport
        if crop.width != self.width or crop.height != self.height:
            img = cv.imread(dst_path)
            unlink(dst_path)
            cv.imwrite(dst_path, img[crop.top:crop.top + crop.height, crop.left:crop.left + crop.width])

    def get_active_window(self):
        try:
            winid = subprocess.check_output(['xdotool', 'getactivewindow']).strip()
            return winid if len(winid) != 0 else None
        except:
            pass
