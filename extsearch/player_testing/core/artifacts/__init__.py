from os.path import join as pj
from shutil import rmtree
import os
import json
import logging


class Artifacts(object):
    def __init__(self, folder, fmt='png'):
        self.folder = folder
        self.cap_seq = 0
        self.vis_seq = 0
        self.fmt = fmt
        self.autoplay_capture = []
        self.content_capture = []
        self.video_capture = []
        self.last_capture = None
        self.visuals = []
        self._make_folder()

    def _make_folder(self):
        if not os.path.exists(self.folder):
            os.mkdir(self.folder)

    def wipe(self):
        try:
            rmtree(self.folder)
        except:
            pass

    def add_capture(self):
        self.cap_seq += 1
        self.last_capture = pj(self.folder, 'capt-{}.{}'.format(self.cap_seq - 1, self.fmt))
        return self.last_capture

    def add_autoplay_capture(self):
        self.autoplay_capture.append(self.add_capture())
        return self.autoplay_capture[-1]

    def add_content_capture(self):
        self.content_capture.append(self.add_capture())
        return self.content_capture[-1]

    def add_visual(self):
        self.vis_seq += 1
        self.visuals.append(pj(self.folder, 'vis-{}.{}'.format(self.vis_seq - 1, self.fmt)))
        return self.visuals[-1]

    def get_video_capture(self):
        self.video_capture.append(pj(self.folder, 'vcapt.mp4'))
        return self.video_capture[-1]

    def store_result(self, url, res, f1, f2):
        dstpath = pj(self.folder, 'result.json')
        json.dump({'url': url, 'playing': res, 'f1': f1, 'f2': f2}, open(dstpath, 'w'))

    def log(self, *args):
        logging.info('{}: {}'.format(self.folder, ' '.join(map(str, args))))


class ArtifactsFactory(object):
    def __init__(self, folder='artifacts', fmt='png', seq=0):
        self.folder = folder
        self.fmt = fmt
        self.seq = seq

    def make(self):
        self.seq += 1
        return Artifacts(pj(self.folder, 'url-{}'.format(self.seq - 1)), self.fmt)
