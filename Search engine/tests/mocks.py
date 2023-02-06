import copy
import json

from datetime import datetime
from pathlib2 import Path

from search.geo.tools.production.downloader.sky import SkynetError

SKY_PREFIX = 'faketorrent:'


def sky_share(paths):
    return SKY_PREFIX + json.dumps(paths).encode('utf-8').encode('hex')


class FakeSkynetFetcher(object):
    def __init__(self):
        # If set to true, fetcher creates files full of zero bytes
        self.corrupt_data = False

    def fetch(self, rbtorrent, dest_dir):
        if not rbtorrent.startswith(SKY_PREFIX):
            raise SkynetError('bad torrent')
        torrent = rbtorrent[len(SKY_PREFIX):]

        dest = Path(dest_dir)

        fs = json.loads(torrent.decode('hex').decode('utf-8'))
        for path, content in fs.iteritems():
            path = dest / path
            path.parent.mkdir(parents=True, exist_ok=True)
            if not self.corrupt_data:
                path.write_text(content)
            else:
                path.write_bytes(b'\x00' * len(content))


class FakeSandboxChecker(object):
    def __init__(self):
        self._resources = []
        self._counter = 0

    def _is_accepted(self, resource, kwargs):
        resource_type = kwargs.get('resource_type')
        if resource_type is not None:
            if resource['type'] != resource_type:
                return False

        resource_id = kwargs.get('resource_id')
        if resource_id is not None:
            if resource['id'] != resource_id:
                return False

        for k, v in kwargs.get('attrs', {}).iteritems():
            if resource.get('attributes', {}).get(k) != str(v):
                return False

        return True

    def add_resource(self, resource):
        resource = copy.deepcopy(resource)

        self._counter += 1
        resource['id'] = self._counter

        resource['time'] = {
            'created': datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')
        }

        self._resources.append(resource)

    def get_last_resource(self, **kwargs):
        for resource in reversed(self._resources):
            if self._is_accepted(resource, kwargs):
                return resource
