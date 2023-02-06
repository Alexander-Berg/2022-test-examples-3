import subprocess
import logging
from os import environ
from os.path import exists, getmtime, join as pj
from time import sleep, time
from threading import Thread


class VLCStreamCapture(object):
    SEG_PATTERN = 'stream-#####.ts'
    VENC_OPT = 'x264{aud,profile=high,preset=fast,keyint=30,ref=1,qpmin=1,qpmax=100,qcomp=0.0,ratetol=0.1,vbv-maxrate=3000,vbv-bufsize=1000}'
    CODECS_CONF = 'fps=30,vcodec=h264,venc=%s,acodec=mp3,ab=128,channels=2,samplerate=44100' % (VENC_OPT)
    MUXER_CONF = 'access=livehttp{seglen=1,delsegs=true,numsegs=5,index=%s,index-url=%s},mux=ts{use-key-frames},dst=%s'
    STREAM_TIMEOUT = 5

    def __init__(self, display, stream_dir, playlist):
        self.stream_dir = stream_dir
        self.playlist = pj(stream_dir, playlist)
        self.disp_id = display.id
        self.proc = None
        muxer_conf = self.MUXER_CONF % (playlist, self.SEG_PATTERN, self.SEG_PATTERN)
        self.cmd = ['cvlc',
                    'screen://',
                    '--screen-fps=30.0',
                    '--input-slave=pulse://auto_null.monitor',
                    '--sout',
                    '#transcode{%s}:std{%s}' % (self.CODECS_CONF, muxer_conf)]
        self.logthr = []
        self.started = 0

    def _log_writer(self, fd, suffix):
        logger = logging.getLogger('VLCStreamCapture:{}'.format(suffix))
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

    def start(self):
        env = environ.copy()
        env['DISPLAY'] = self.disp_id
        self.proc = subprocess.Popen(self.cmd, cwd=self.stream_dir, env=env, stderr=subprocess.PIPE, stdout=subprocess.PIPE)
        self._add_logger(self.proc.stderr, 'ERR')
        self._add_logger(self.proc.stdout, 'OUT')
        self.started = int(time())
        sleep(3)

    def stop(self):
        if not self.proc:
            return
        self.proc.terminate()
        self.proc.wait()
        self.started = 0

    def is_alive(self):
        if not self.proc or not self.started:
            return None
        if self.proc.poll() is not None:
            return False
        now = time()
        if self.started + self.STREAM_TIMEOUT >= now:
            return True
        if not exists(self.playlist) or getmtime(self.playlist) + self.STREAM_TIMEOUT < now:
            logging.info('output HLS stream timeout detected')
            return False
        return True
