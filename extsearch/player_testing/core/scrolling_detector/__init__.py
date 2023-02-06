import cv2 as cv
import numpy as np
from time import sleep
import logging


class ScrollingDetector(object):
    def __init__(self, display, input_device):
        self.display = display
        self.input_device = input_device
        self.step = 3
        self.max_shift = 0.05

    def is_scrolling(self, artifacts):
        self.display.capture_screen(artifacts.add_capture())
        im1 = cv.imread(artifacts.last_capture, cv.IMREAD_GRAYSCALE)
        self.input_device.move_cursor(self.display.width / 2, self.display.height / 2).scroll_down(self.step).execute()
        sleep(1)
        self.display.capture_screen(artifacts.add_capture())
        im2 = cv.imread(artifacts.last_capture, cv.IMREAD_GRAYSCALE)
        win = cv.createHanningWindow((im1.shape[1], im1.shape[0]), cv.CV_32F)
        point, _ = cv.phaseCorrelate(np.float32(im1), np.float32(im2), win)
        logging.info('shift abs {} relative {}'.format(point[1], float(point[1]) / self.display.height))
        return abs(float(point[1])) / self.display.height >= self.max_shift
