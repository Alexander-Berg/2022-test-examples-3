import re
import json
import logging
import requests
from os.path import join as pj


def add_scheme(url):
    return url if url.startswith('http') else 'http:{}'.format(url)


def load_json(path):
    raw = open(path).read()
    return json.loads(re.sub(r'/\*[^*]+\*/', '', raw))


class ThumbnailCache(object):
    def __init__(self, work_dir, channels_conf, restreamed_conf):
        self.logger = logging.getLogger('ThumbnailCache')
        self.logger.info('loading thumbnail cache')
        self.cache = dict()
        channels = load_json(channels_conf)['channels']
        thumbs = load_json(restreamed_conf)
        for channel_id, props in iter(channels.items()):
            content_id = props.get('content_id')
            if not content_id:
                continue
            if content_id not in thumbs:
                self.logger.info('no content info for {}'.format(channel_id))
                continue
            thumb = add_scheme(thumbs[content_id]['thumbnail'])
            try:
                self.logger.info('Downloading {} as {} thumbnail'.format(thumb, channel_id))
                resp = requests.get(thumb)
                resp.raise_for_status()
                dst_file = pj(work_dir, '{}.png'.format(channel_id))
                with open(dst_file, 'wb') as outfd:
                    outfd.write(resp.content)
                self.cache[channel_id] = dst_file
            except:
                self.logger.error('Failed to fetch {}'.format(thumb))

    def get(self, channel_id):
        return self.cache.get(channel_id)
