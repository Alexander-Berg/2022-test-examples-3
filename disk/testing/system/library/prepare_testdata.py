# -*- coding: utf-8 -*-

import os
import sys
import logging
from urllib import quote_plus

import time
from urlparse import urljoin
import requests
from hashlib import md5, sha256

from ansible.module_utils.basic import *

logger = logging.getLogger(__name__)

CA_PATH = '/etc/ssl/certs/ca-certificates.crt'


def hashs_file(path):
    md5h = md5()
    sha256h = sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(4096), b""):
            md5h.update(chunk)
            sha256h.update(chunk)
        return md5h.hexdigest(), sha256h.hexdigest()


class MPFSClient(object):
    def __init__(self, address, uid):
        self.address = address
        self._base_url = 'http://%s/' % self.address
        self.uid = uid

    def make_request(self, path, params, method='get', raise_for_status=True):
        headers = {
            'Host': 'mpfs.disk.yandex.net',
        }
        url = urljoin(self._base_url, path)
        if method.lower() == 'get':
            f = requests.get
        elif method.lower() == 'post':
            f = requests.post
        else:
            raise NotImplementedError

        r = f(url, params=params, headers=headers)
        if raise_for_status:
            r.raise_for_status()
        return r

    def _make_params(self, params=None, need_uid=True):
        if not params:
            params = {}
        _params = {
            'uid': self.uid
        }
        if need_uid:
            params.update(_params)
        return params

    def list(self, path=None):
        params = {}
        if path:
            params['path'] = path
        r = self.make_request('/json/list/', self._make_params(params))
        return r.json()

    def info(self, path=None, meta=","):
        params = {
            'meta': meta,
        }
        if path:
            params['path'] = path
        r = self.make_request('/json/info/', self._make_params(params))
        return r.json()

    def mkdir(self, path):
        """
        405 - каталог уже существует
        409 - родительский каталог не найден
        503 - хранилище недоступно
        507 - хранилище переполнено
        """
        params = {
            'path': path,
        }
        r = self.make_request('/json/mkdir/', self._make_params(params), raise_for_status=False)
        if r.status_code in (405, 200):
            return True
        return False

    def rm(self, path):
        params = {
            'path': path
        }
        r = self.make_request('/json/rm/', self._make_params(params), raise_for_status=False)
        if r.status_code in (404, 200):
            return True
        return True

    def status(self, oid):
        params = {
            'oid': oid,
        }

        r = self.make_request('/json/status/', self._make_params(params))
        return r.json()

    def _upload(self, url, src):
        files = {
            'file': open(src, 'rb')
        }

        r = requests.post(url, files=files, verify=CA_PATH)
        r.raise_for_status()

    def store(self, src, path, upload=True, force=True, hashs=None):
        """
        store file from src for path at disk
        """

        if hashs:
            md5h, sha256h = hashs
        else:
            md5h, sha256h = hashs_file(src)

        params = {
            'path': path,
            'md5': md5h,
            'sha256': sha256h,
            'size': os.stat(src).st_size,
        }
        if force:
            params['force'] = 1

        r = self.make_request('/json/store/', self._make_params(params))
        payload = r.json()

        if payload.get('status') == 'hardlinked':
            return True

        if upload:
            self._upload(payload.get('upload_url'), src)

        oid = payload.get('oid')
        status = self.status(oid).get('status')
        while status in ['WAITING', 'EXECUTING']:
            time.sleep(1)
            status = self.status(oid).get('status')
        if status == 'DONE':
            return True
        else:
            return False

    def albums_create_from_folder(self, path, title=None, media_type='image,video'):
        params = {
            'path': path,
            'media_type': media_type,
            'title': title,
        }
        r = self.make_request('/json/albums_create_from_folder/', self._make_params(params))
        return r.json()

    def albums_remove(self, album_id):
        params = {
            'album_id': album_id,

        }
        r = self.make_request('/json/album_remove/', self._make_params(params))
        return r.json()

    def albums_list(self):
        r = self.make_request('/json/albums_list/', self._make_params())
        return r.json()

    def albums_create(self, title):
        params = {
            'title': title,
        }
        r = self.make_request('/json/albums_create/', self._make_params(params))
        return r.json()

    def set_public(self, path):
        params = {
            'path': path,
        }
        r = self.make_request('/json/set_public/', self._make_params(params))
        return r.json()


class Prepare(object):
    def __init__(self, host, uid, test_file, basename='load-test', num_files=100, num_albums=100):
        logger.info('init for {}, uid={}'.format(host, uid))
        self.client = MPFSClient(host, uid)
        self.basename = basename
        self.dst_dir = '/disk/%s' % self.basename
        self.src = test_file
        self.hashs = hashs_file(test_file)
        self.num_albums = num_albums
        self.num_files = num_files

    def prepare_dir(self):
        self.client.mkdir(self.dst_dir)
        r = self.client.set_public(self.dst_dir)
        h = quote_plus(r.get('hash'))
        self.public_hash = h

    def prepare_albums(self):
        curr = len(self.client.albums_list())
        if curr > self.num_albums:
            self.remove_albums()
        else:
            self.create_albums(self.num_albums - curr)

    def create_albums(self, num):
        for x in range(num):
            d = {
                'name': self.basename,
                'id': x,
            }
            album_name = '%(name)s-%(id)d' % d
            logger.info('create album {}'.format(album_name))
            self.client.albums_create(album_name)

    def remove_albums(self):
        a = self.client.albums_list()
        curr = len(a)
        to_deleted = curr - self.num_albums
        for album in a:
            self.client.albums_remove(album.get('id'))
            to_deleted -= 1
            if to_deleted <= 0:
                break

    def prepare_files(self):
        if len(self.client.list(self.dst_dir)) < self.num_files:
            self.create_files(self.num_files)

    def create_files(self, num):
        for x in range(num):
            d = {
                'dst': self.dst_dir,
                'name': self.basename,
                'id': x,
            }
            path = '%(dst)s/%(name)s-%(id)d.jpg' % d
            logger.info('store {}'.format(path))
            self.client.store(self.src, path, self.hashs)

    def remove_files(self):
        a = self.client.list()
        for f in a:
            if f['type'] == 'dir' and f['path'] == '/dir': continue  # system dir
            self.client.rm(f['id'])

    def prepare(self):
        self.prepare_dir()
        # self.prepare_albums()
        self.prepare_files()

    def finish(self):
        logger.info('public hash: "%s"', self.public_hash)
        assert len(self.client.albums_list()) == self.num_albums
        assert len(self.client.list()) == self.num_files


def main():
    module = AnsibleModule(
        argument_spec=dict(
            host=dict(type="str", default=None),
            uid=dict(type="str", default=None),
            image_path=dict(type="str", default=None),
        ),
        supports_check_mode=False
    )

    host = module.params.get("host")
    uid = module.params.get("uid")
    src = module.params.get("image_path")

    if all((host, uid)):
        p = Prepare(host, uid, src)
        p.prepare()
    else:
        module.fail_json(msg="No valid options were provided.",
                         result=False)

    result_args = dict(
        result=True,
        info={'public_hash': p.public_hash}
    )
    module.exit_json(**result_args)


if __name__ == '__main__':
    main()
