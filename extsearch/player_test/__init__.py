from display import Display
from browser import Browser
from input_device import InputDevice
from motion_detector import MotionDetector
from scrolling_detector import ScrollingDetector
from video_capture import VideoCapture
from clicker import Clicker
from crop import CropTool
from time import sleep, time
from artifacts import ArtifactsFactory
from http_magic import HttpMagic
from s3 import S3Client
from hashlib import md5
from job import add_autoplay, guess_browser
from util import eval_player_id
import logging
from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import TJobResult, EVideoFormat, EArtifactType


MIN_CONTENT_SIZE = 64 * 1024


def is_playing(context):
    has_video = False
    content_size = 0
    from_qproxy = False
    from_vh = False
    for content in context.Http.Contents:
        if content.ContentType.find('video') != -1 or content.VideoFormat != EVideoFormat.EVF_UNKNOWN:
            content_size += content.ContentSize
            vec = content.Url.split('/')
            if len(vec) >= 3 and vec[2].endswith('strm.yandex.net'):
                from_vh = True
        if content.Url.startswith('https://quasar-proxy.yandex.net') and content.HttpCode >= 200 and content.HttpCode < 300:
            from_qproxy = True
    has_video = content_size >= MIN_CONTENT_SIZE
    is_moving = context.Player.WorkingArea > 0.5
    return (has_video or is_moving or from_qproxy or from_vh) and not context.Player.IsPopup


def with_prof(method):
    def save_prof(self, *args, **kwargs):
        start = time()
        ret = method(self, *args, **kwargs)
        if self.env_type == 'local':
            prof = self.context.Profile.add()
            prof.Name = method.__name__
            prof.Time = int(time() - start)
        return ret
    return save_prof


class PlayerTest(object):
    def __init__(self, config):
        self.config = config.player
        self.env_type = config.env
        self.artifacts_factory = ArtifactsFactory(folder=config.images_dir)
        self.display = Display(config.display)
        self.input_device = InputDevice(self.display)
        self.crop_tool = CropTool(config, self.display)
        self.browser = Browser(self.display, self.input_device, self.crop_tool, config.http.proxy_port)
        self.browser.install()
        self.clicker = Clicker(self.display, self.input_device, self.config.play_timeout)
        self.video_capture = VideoCapture(self.display, config.video_capture)
        self.http_magic = HttpMagic(config)
        self.s3 = S3Client(config.s3)
        self.win_id = None
        self.keep_artifacts = config.keep_artifacts
        self.context = None
        self.player_id = None

    @staticmethod
    def is_playing(moving_area):
        return moving_area > 0.5

    def _initialize(self, job):
        self.win_id = None
        self.context = TJobResult()
        self.context.Job.CopyFrom(job)
        self.context.Env = self.env_type
        self.player_id = eval_player_id(job.Url)
        self.input_device.reset()

    def _wait_for_content(self, logsvc, timeout):
        if not self.context.Job.Flags.Fast:
            sleep(timeout)
            return False
        start = time()
        while start + timeout > time():
            if logsvc.has_pretty_video():
                return True
            sleep(1)
        return False

    @with_prof
    def _open_url(self, logsvc):
        if self.browser.is_running():
            self.browser.close()
        if self.context.Job.Flags.BrowserAutodetect:
            guess_browser(self.context.Job)
        self.browser.open(self.context.Http.ResultUrl,
                          family=self.context.Job.Browser,
                          device=self.context.Job.Device,
                          cookies=self.context.Job.Scripts.Cookies)
        self.display.set_viewport(self.browser.get_viewport())
        self._wait_for_content(logsvc, self.config.load_timeout)

    @with_prof
    def _check_scrolling(self, artifacts):
        if self.context.Job.Flags.CheckScrolling:
            self.context.Player.IsScrolling = ScrollingDetector(self.display, self.input_device).is_scrolling(artifacts)

    @with_prof
    def _detect_autoplay(self, artifacts, logsvc):
        for i in range(self.config.autoplay_capture_count):
            self.display.capture_screen(artifacts.add_autoplay_capture())
            if self.is_playing(MotionDetector.calc_moving_area(artifacts.autoplay_capture)):
                break
            if self._wait_for_content(logsvc, self.config.capture_delay):
                return True
        self.context.Player.AutoplayArea = MotionDetector.calc_moving_area(artifacts.autoplay_capture, artifacts.add_visual())
        return self.is_playing(self.context.Player.AutoplayArea)

    @with_prof
    def _start_player(self, artifacts, logsvc):
        if not self.context.Job.Flags.DontClick:
            logging.info('start_player: using visual clicker')
            self.clicker.play(logsvc, artifacts, self.player_id, self.context.Http.ResultUrl)
        if self.context.Job.Scripts.Play:
            logging.info('start_player: using play script')
            script_result = self.browser.execute_script(self.context.Job.Scripts.Play, self.context.Job.Scripts.Timeout)
            if script_result:
                self.context.Log = script_result

    @with_prof
    def _detect_playing(self, artifacts, logsvc):
        if self.context.Job.VideoCapture.Duration > 0:
            self.video_capture.start(artifacts.get_video_capture(), duration=self.context.Job.VideoCapture.Duration)
        for i in range(self.config.content_capture_count):
            self.display.capture_screen(artifacts.add_content_capture())
            if self._wait_for_content(logsvc, self.config.capture_delay):
                break
        self.context.Player.WorkingArea = MotionDetector.calc_moving_area(artifacts.content_capture, artifacts.add_visual())

    def _precheck_popup(self):
        if self.context.Job.Flags.CheckPopup:
            self.win_id = self.display.get_active_window()

    def _check_popup(self):
        if self.win_id is not None:
            win_id = self.display.get_active_window()
            self.context.Player.IsPopup = win_id is not None and self.win_id != win_id

    def _save_artifact(self, path, atype=EArtifactType.EAT_IMAGE, content_type='image/png'):
        try:
            key = '{}/{}/{}/{}'.format(self.env_type, md5(self.context.Job.Url).hexdigest(), int(time()), path.split('/')[-1])
            url = self.s3.upload_file(path, key, content_type)
            aft = self.context.Artifacts.add()
            aft.Type = atype
            aft.Url = url
        except Exception as e:
            logging.error('unable to save artifact {}: {}'.format(path, e))

    @with_prof
    def _save_artifacts(self, logsvc, artifacts):
        if artifacts.autoplay_capture:
            self._save_artifact(artifacts.autoplay_capture[-1])
        if len(artifacts.visuals) >= 2:
            self._save_artifact(artifacts.visuals[1])
        if artifacts.content_capture:
            self._save_artifact(artifacts.content_capture[-1])
        if artifacts.video_capture:
            self._save_artifact(artifacts.video_capture[-1], EArtifactType.EAT_VIDEO, 'video/mp4')

    @with_prof
    def _do_check(self, artifacts, logsvc):
        self._open_url(logsvc)
        try:
            self._check_scrolling(artifacts)
            self._precheck_popup()
            if not self._detect_autoplay(artifacts, logsvc):
                self._start_player(artifacts, logsvc)
            self._detect_playing(artifacts, logsvc)
            self._check_popup()
            self.video_capture.wait()
        finally:
            self.browser.close()
            self.video_capture.stop()

    def _get_player_url(self, logsvc):
        if self.context.Job.Flags.HttpProbe:
            self.context.Http.MergeFrom(self.http_magic.player_url_features(self.context.Job.Url))
        elif self.context.Job.Flags.Fast or self.context.Job.Flags.Autoplay:
            self.context.Http.ResultUrl = add_autoplay(self.context.Job.Url, force=self.context.Job.Flags.Autoplay)
        else:
            self.context.Http.ResultUrl = self.context.Job.Url
        logsvc.url_update(self.context)
        return self.context.Http.ResultUrl

    @with_prof
    def check_player_url(self, job, logsvc):
        self._initialize(job)
        artifacts = self.artifacts_factory.make()
        logsvc.url_start(self.context)
        try:
            if self._get_player_url(logsvc):
                self._do_check(artifacts, logsvc)
                self._save_artifacts(logsvc, artifacts)
                self.context.Http.MergeFrom(logsvc.proxy_stat())
                logsvc.url_update(self.context)
        except Exception as e:
            logging.error('check_player_url: {}'.format(e))
            self.context.Error = str(e)
        finally:
            self.context.MergeFrom(logsvc.url_finish())
            self.context.IsPlaying = is_playing(self.context)
            if not self.keep_artifacts:
                artifacts.wipe()
        return self.context

    def start(self):
        self.display.start()
        self.browser.calibrate()

    def stop(self):
        self.display.stop()
