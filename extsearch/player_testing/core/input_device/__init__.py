import subprocess
import logging
from time import sleep


class InputDevice(object):
    def __init__(self, display):
        self.display = display
        self.action = []

    def move_cursor(self, x, y):
        self.action.extend(['mousemove', '--sync', x, y])
        return self

    def scroll_down(self, step=1):
        self.action.extend(['click', '5'] * step)
        return self

    def click(self):
        self.action.extend(['getactivewindow', 'click', 1])
        return self

    def press_key(self, key):
        self.action.extend(['key', key])
        return self

    def execute(self):
        if not self.action:
            return self
        cmd = ['xdotool']
        cmd.extend(map(str, self.action))
        res = subprocess.check_call(cmd)
        if res != 0:
            raise Exception('input [{}] failed: {}'.format(' '.join(cmd), res))
        self.action = []
        sleep(0.1)
        logging.info(' '.join(cmd))
        return self

    def reset(self):
        self.action = []
