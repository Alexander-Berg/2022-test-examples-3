from os.path import join as pj
from conf import SANDBOX_URL
import cv2 as cv
from display import Viewport
import numpy as np
import logging
try:
    from urllib.parse import quote
except ImportError:
    from urllib import quote


class CropTool(object):
    def __init__(self, config, display):
        self.url = '{}#html={}'.format(SANDBOX_URL, quote('<div style="background-color:#008000;width:100%;height:100%"></div>'))
        self.capt = pj(config.work_dir, 'calibrate.png')
        self.display = display
        self.pad = 5

    def get_url(self):
        return self.url

    def calc_viewport(self):
        self.display.set_viewport(None)
        self.display.capture_screen(self.capt)
        img = cv.imread(self.capt)
        height, width, _ = img.shape
        pivot_x, pivot_y = 0, 0
        green = np.array([0, 0x80, 0])
        step = 10
        while pivot_x < width:
            if (img[pivot_y, pivot_x] == green).all():
                break
            pivot_x += step
            pivot_y = int(float(pivot_x) / width * height)
        if pivot_x >= width:
            logging.info('failed to find green viewport')
            return None
        left, right = 0, 0
        for x in range(0, pivot_x):
            if not (img[pivot_y, x] == green).all():
                left += 1
        for x in range(pivot_x, width):
            if not (img[pivot_y, x] == green).all():
                right += 1
        top, bottom = 0, 0
        for y in range(0, pivot_y):
            if not (img[y, pivot_x] == green).all():
                top += 1
        for y in range(pivot_y, height):
            if not (img[y, pivot_x] == green).all():
                bottom += 1
        return Viewport(left + self.pad,
                        top + self.pad,
                        width - (left + right + self.pad),
                        height - (top + bottom + self.pad))
