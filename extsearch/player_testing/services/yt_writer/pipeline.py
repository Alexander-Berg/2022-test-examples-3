from extsearch.video.robot.crawling.player_testing.protos.job_pb2 import EArtifactType
import logging
from time import time


class BSContentPipeline(object):
    def __init__(self, client):
        self.table = '//home/videoindex/bs_data/content'
        self.client = client
        self.rows = []

    def push(self, msg, raw_msg):
        content_url = None
        for item in msg.Artifacts:
            if item.Type == EArtifactType.EAT_VIDEO:
                content_url = item.Url
        if not msg.IsPlaying:
            content_url = None
        self.rows.append({
            'url': msg.Job.Url,
            'add_time': int(time()),
            'content_url': content_url,
            'vdp': not bool(msg.IsPlaying),
            'snail_state': raw_msg
        })

    def commit(self):
        if not self.rows:
            return
        try:
            self.client.insert_rows(self.table, self.rows)
        finally:
            self.rows = []


class PipelineHandler(object):
    def __init__(self, client):
        self.handlers = {
            'bs_content': BSContentPipeline(client)
        }

    def push(self, pipeline, msg, raw_msg):
        if pipeline not in self.handlers:
            logging.info('unknown pipeline: {}'.format(pipeline))
            return
        try:
            self.handlers[pipeline].push(msg, raw_msg)
        except Exception as e:
            logging.info('failed to push msg to {} pipeline: {}'.format(pipeline, e))

    def commit(self):
        for handler in self.handlers.values():
            handler.commit()
