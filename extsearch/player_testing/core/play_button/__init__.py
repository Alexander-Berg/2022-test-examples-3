import cv2 as cv
import numpy as np
import math


def distance(p1, p2):
    return np.linalg.norm(p1 - p2)


class ButtonFeatures(object):
    def __init__(self, contour, screen_width, screen_height):
        m = cv.moments(contour)
        self.x = int(m['m10']/m['m00'])
        self.y = int(m['m01']/m['m00'])
        self.center = np.array([self.x, self.y])
        self.weight = cv.contourArea(contour)
        self.d1 = distance(self.center, np.array([screen_width / 2, screen_height / 2]))
        self.d2 = distance(self.center, np.array([0, screen_height]))


class PlayButton(object):
    IMG_CONTRAST = 20
    IMG_BLUR = (3, 3)
    ARC_LENGTH_DIFF = 0.05
    EDGE_LENGTH_DIFF = 0.2
    MIN_EDGE_PX = 5
    MIN_BUTTON_DISTANCE_PX = 100
    LEFT_EDGE_MAX_SLOPE_DEG = 5
    COLOR_GREEN = (0, 255, 0)
    COLOR_RED = (0, 0, 255)

    @staticmethod
    def is_button_shaped(contour):
        if contour.shape[0] != 3:
            return False
        if not PlayButton.is_equilateral(contour):
            return False
        # sort vertices by x
        vertices = contour.reshape(3, 2).tolist()
        vertices.sort(key=lambda p: p[0])
        v0, v1 = vertices[0:2]
        if v0[1] == v1[1]:
            return False
        slope = 180 * np.arctan(float(v1[0] - v0[0]) / abs(v1[1] - v0[1])) / math.pi
        # left edge should be vertical
        if slope >= PlayButton.LEFT_EDGE_MAX_SLOPE_DEG:
            return False
        return True

    @staticmethod
    def is_equilateral(trig):
        edges = [
            distance(trig[0], trig[1]),
            distance(trig[1], trig[2]),
            distance(trig[2], trig[0])
        ]
        m = np.mean(edges)
        if m < PlayButton.MIN_EDGE_PX:
            return False
        epsilon = PlayButton.EDGE_LENGTH_DIFF * m
        for e in edges:
            if np.abs(e - m) > epsilon:
                return False
        return True

    @staticmethod
    def prune(candidates):
        if len(candidates) <= 1:
            return candidates
        pruned = []
        # order by center distance
        candidates.sort(key=lambda btn: btn.d1)
        pruned.append(candidates[0])
        # order by left bottom corner distance
        candidates.sort(key=lambda btn: btn.d2)
        if distance(pruned[0].center, candidates[0].center) >= PlayButton.MIN_BUTTON_DISTANCE_PX:
            pruned.append(candidates[0])
        return pruned

    @staticmethod
    def lookup(screencap, vis=None):
        img = cv.imread(screencap)
        height, width, _ = img.shape
        contrast = PlayButton.IMG_CONTRAST
        alpha_c = 131*(contrast + 127)/(127*(131-contrast))
        gamma_c = 127*(1-alpha_c)
        contrast = cv.addWeighted(img, alpha_c, img, 0, gamma_c)
        gray = cv.cvtColor(contrast, cv.COLOR_BGR2GRAY)
        blurred = cv.GaussianBlur(gray, PlayButton.IMG_BLUR, 0)
        thresh = cv.adaptiveThreshold(blurred, 255, cv.ADAPTIVE_THRESH_GAUSSIAN_C, cv.THRESH_BINARY, 11, 2)
        _, contours, hierarchy = cv.findContours(thresh, cv.RETR_TREE, cv.CHAIN_APPROX_SIMPLE)
        candidates = []
        features = []
        for cnt in contours:
            epsilon = PlayButton.ARC_LENGTH_DIFF * cv.arcLength(cnt, True)
            approx = cv.approxPolyDP(cnt, epsilon, True)
            if PlayButton.is_button_shaped(approx):
                candidates.append(approx)
                features.append(ButtonFeatures(approx, width, height))
        pruned = PlayButton.prune(features)
        if vis is not None:
            cv.drawContours(img, candidates, -1, PlayButton.COLOR_GREEN, 2)
            for button in pruned:
                cv.circle(img, (button.x, button.y), 3, PlayButton.COLOR_RED, -1)
            cv.imwrite(vis, img)
        return pruned


if __name__ == '__main__':
    from sys import argv
    PlayButton.lookup(argv[1], argv[2])
