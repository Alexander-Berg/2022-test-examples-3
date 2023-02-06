from os.path import join as pj
from os import path, mkdir, environ
from shutil import rmtree
import json

SANDBOX_PREFIX = u'https://yastatic.net/video-player'
SANDBOX_URL = u'{}/0xef2b21c/pages-common/default/default.html'.format(SANDBOX_PREFIX)


class ConfigItem(object):
    def __init__(self, **kwargs):
        for attr, value in iter(kwargs.items()):
            object.__setattr__(self, attr, value)


class Config(object):
    ENV = ['prod', 'dev', 'viewer', 'local', 'live']

    SQS_INPUT = {
        'prod':     {'video_player_testing_metric': 2,
                     'video_player_testing_crawl': 1},
        'dev':      {'snail_dev_input': 1},
        'viewer':   {'video_player_testing_viewer': 1}
    }
    SQS_OUTPUT = {
        'prod': 'video_player_testing_output',
        'dev': 'video_player_testing_output'
    }

    def __init__(self):
        base_port = int(environ.get('SNAIL_BASE_PORT', 8080))
        display = int(environ.get('SNAIL_DISPLAY', 42))
        self.env = environ.get('SNAIL_ENV', 'prod')
        if self.env not in self.ENV:
            raise Exception('invalid env {}'.format(self.env))
        self.work_dir = environ.get('SNAIL_WORK', '/work')
        self.images_dir = pj(self.work_dir, 'images')
        self.stream_dir = pj(self.work_dir, 'stream')
        self.display = ConfigItem(
            id=display,
            is_ext=False,
            width=1280 if self.env == 'live' else 1024,
            height=720 if self.env == 'live' else 768,
            is_main=True
        )
        self.http = ConfigItem(
            proxy_port=base_port,
            timeout=15
        )
        self.player = ConfigItem(
            load_timeout=10,
            play_timeout=20,
            capture_delay=3,
            autoplay_capture_count=3,
            content_capture_count=5
        )
        self.queuesvc = ConfigItem(
            hb_file=pj(self.work_dir, 'heartbeat'),
            hb_timeout=180
        )
        self.casvc = ConfigItem(
            server_port=base_port + 1,
            cache_dir=pj(self.work_dir, 'cert_cache'),
            cert_file=pj(self.work_dir, 'ca_cert.pem'),
        )
        self.logsvc = ConfigItem(
            server_port=base_port + 2,
            cache_size=25,
            preview=ConfigItem(
                width=320,
                height=240
            )
        )
        self.yt_writer = ConfigItem(
            proxy='arnold',
            output_prefix='//home/videoindex/deletes/snail/portions',
            flush_row_count=25,
            flush_timeout=5 * 60
        )
        self.yt_merge = ConfigItem(
            max_portions=2000,
            result_ttl=86400 * 7,
            prev_state='//home/videoindex/deletes/snail/jobs.prev',
            new_state='//home/videoindex/deletes/snail/jobs.new'
        )
        sqs_input_conf = self.SQS_INPUT.get(self.env)
        sqs_input_override = environ.get('SNAIL_INPUT_QUEUE')
        if sqs_input_override is not None:
            sqs_input_conf = {sqs_input_override: 1}
        self.sqs = ConfigItem(
            input_queues=sqs_input_conf,
            output_queue=self.SQS_OUTPUT.get(self.env),
            endpoint_url='http://sqs.yandex.net:8771',
            visibility_timeout=180,
            account='robotvideo',
            max_receive_count=3
        )
        s3_auth = json.loads(environ.get('SNAIL_S3_AUTH', '{}'))
        self.s3 = ConfigItem(
            bucket='snail-dev',
            endpoint_url='https://s3.mds.yandex.net',
            access_key_id=s3_auth.get('AccessKeyId'),
            secret_access_key=s3_auth.get('AccessSecretKey')
        )
        self.video_capture = ConfigItem(
            ffmpeg_scale_param='-vf scale=480:360',
            ffmpeg_vcodec_param='-c:v libx264 -preset ultrafast -profile:v high -crf 25 -maxrate 500k -bufsize 1M -coder 1 -pix_fmt yuv420p -movflags +faststart -g 30 -bf 2',
            ffmpeg_acodec_param='-c:a aac -strict -2'
        )
        self.keep_artifacts = self.env == 'local'

    def init_workdir(self):
        if not path.exists(self.work_dir):
            mkdir(self.work_dir)
        if path.exists(self.images_dir):
            rmtree(self.images_dir)
        if path.exists(self.stream_dir):
            rmtree(self.stream_dir)
        mkdir(self.images_dir)
        mkdir(self.stream_dir)
