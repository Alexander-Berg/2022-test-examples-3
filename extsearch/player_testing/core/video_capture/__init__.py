import subprocess
import logging
from time import time, sleep


class VideoCapture(object):
    def __init__(self, display, config):
        self.display = display
        self.vcodec_param = config.ffmpeg_vcodec_param.strip().split(' ')
        self.acodec_param = config.ffmpeg_acodec_param.strip().split(' ')
        self.scale_param = config.ffmpeg_scale_param.strip().split(' ')
        self.output_fmt = 'mp4'
        self.proc = None
        self.end_time = None

    def start(self, dst_path, duration=None):
        assert self.proc is None, 'running ffmpeg process found'
        video_size = '{}x{}'.format(self.display.width, self.display.height)
        cmd = ['ffmpeg',
               '-thread_queue_size', '512',
               '-f', 'x11grab',
               '-video_size', video_size,
               '-i', self.display.id]
        if self.acodec_param:
            cmd.extend(['-f', 'pulse', '-i', 'default'])
        cmd.extend(self.vcodec_param)
        cmd.extend(self.acodec_param)
        if self.scale_param:
            cmd.extend(self.scale_param)
        if duration:
            cmd.extend(['-t', str(duration)])
        cmd.extend(['-f', self.output_fmt, dst_path])
        self.proc = subprocess.Popen(cmd)
        self.end_time = int(time()) + duration

    def wait(self):
        now = int(time())
        if self.end_time is not None and self.end_time > now:
            logging.info('waiting for {}sec to complete content capture'.format(self.end_time - now))
            for i in range(self.end_time - now):
                sleep(1)

    def stop(self):
        if self.proc is not None:
            self.proc.terminate()
            self.proc.wait()
            self.proc = None
