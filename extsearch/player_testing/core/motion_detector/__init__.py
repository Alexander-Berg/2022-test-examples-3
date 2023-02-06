import cv2 as cv


class MotionDetector(object):
    @staticmethod
    def calc_moving_area(frames, vis_out=None):
        result = 0.0
        prev = None
        delta = None
        for frame in frames:
            img = cv.imread(frame)
            gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
            cur = cv.GaussianBlur(gray, (5, 5), 0)
            if prev is not None:
                cur_delta = cv.absdiff(prev, cur)
                if delta is not None:
                    delta = cv.bitwise_or(delta, cur_delta)
                else:
                    delta = cur_delta
            prev = cur
        if delta is not None:
            thresh, mask = cv.threshold(delta, 10, 255, cv.THRESH_BINARY)
            _, contours, hierarchy = cv.findContours(mask, cv.RETR_EXTERNAL, cv.CHAIN_APPROX_SIMPLE)
            moving_area = 0
            max_bbox = None
            max_bbox_area = 0
            for cnt in contours:
                moving_area += cv.contourArea(cnt)
                x, y, w, h = cv.boundingRect(cnt)
                if w * h > max_bbox_area:
                    max_bbox_area = w * h
                    max_bbox = (x, y, w, h)
            if vis_out is not None:
                vis = cv.imread(frames[-1])
                cv.drawContours(vis, contours , -1, (0, 255, 0), 2)
                if max_bbox is not None:
                    x, y, w, h = max_bbox
                    cv.rectangle(vis, (x, y), (x + w, y + h), (0, 0, 255), 2)
                cv.imwrite(vis_out, vis)
            result = float(max(moving_area, max_bbox_area)) / (prev.shape[0]*prev.shape[1])
        return result
