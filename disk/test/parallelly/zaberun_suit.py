# -*- coding: utf-8 -*-
import cjson
from datetime import date

import pytest
import urllib2
import urlparse

from test.base import (
    DiskTestCase,
    time_machine,
)

import mpfs.core.services.zaberun_service as zaberun

from mpfs.metastorage.mongo.binary import Binary

with open('fixtures/json/file.json') as fix_file:
    fixtures = cjson.decode(fix_file.read())
    
UID  = fixtures["uid"]
MID  = fixtures["file_value"]["meta"]["file_mid"]
NAME = fixtures["file_value"]["name"]
HASH = fixtures["hash"]
FOLDER = fixtures["key_dir_parent"]


class TestZaberun(DiskTestCase):
    def _generate_public(self):
        z = zaberun.Zaberun()
        url = z.generate_public_url(MID, NAME, hash=HASH)
        parsed_url = urlparse.urlparse(url)
        assert parsed_url.netloc.endswith(z.tlds['default'])
        hsh = urlparse.parse_qs(parsed_url.query)['hash'][0]
        self.assertEqual(hsh, HASH)
        result = urllib2.urlopen(url).read()
        self.assertNotEqual(result, None)

    def _generate_eternal(self):
        z = zaberun.Zaberun()
        url = z.generate_file_url('0', MID, NAME, eternal=True)
        parsed_url = urlparse.urlparse(url)
        assert parsed_url.netloc.endswith(z.tlds['default'])
        result = urllib2.urlopen(url).read()
        self.assertNotEqual(result, None)

    def test_10_generate_private(self):
        zaberun.USE_TOKEN_V2 = False
        z = zaberun.Zaberun()
        url = z.generate_file_url(UID, MID, NAME)
        parsed_url = urlparse.urlparse(url)
        assert parsed_url.netloc.endswith(z.tlds['default'])
        self.assertRaises(urllib2.HTTPError, urllib2.urlopen, url)
        try:
            urllib2.urlopen(url)
        except Exception, e:
            self.assertEqual(e.code, 403)
       
    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_20_generate_public(self):
        zaberun.USE_TOKEN_V2 = False
        self._generate_public()
        
    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_30_generate_eternal(self):
        zaberun.USE_TOKEN_V2 = False
        self._generate_eternal()
 
    def test_40_generate_private_folder(self):
        zaberun.USE_TOKEN_V2 = False
        z = zaberun.Zaberun()
        url = z.generate_folder_url(UID, FOLDER, 'test')
        parsed_url = urlparse.urlparse(url)
        assert parsed_url.netloc.endswith(z.tlds['default'])
        self.assertRaises(urllib2.HTTPError, urllib2.urlopen, url)
        try:
            urllib2.urlopen(url)
        except Exception, e:
            self.assertEqual(e.code, 403)
 
    def test_50_generate_public_token_v2(self):
        zaberun.USE_TOKEN_V2 = True
        self._generate_public()

    def test_60_generate_eternal_token_v2(self):
        zaberun.USE_TOKEN_V2 = True
        self._generate_eternal()

    def test_generate_preview_url(self):
        z = zaberun.Zaberun()
        preview_url = z.generate_preview_url(UID, MID, NAME)
        parsed_url = urlparse.urlparse(preview_url)
        assert parsed_url.netloc.endswith(z.tlds['preview'])
        query_dict = urlparse.parse_qs(parsed_url.query)
        filename = query_dict["filename"][0]
        assert filename == NAME


class TestGetZaberunUrls(DiskTestCase):
    file_path = '/disk/file_1.jpg'
    file_name = 'file_1.jpg'

    def test_get_file_url(self):
        self.upload_file(self.uid, self.file_path)
        req_result = self.json_ok('info', {'uid': self.uid, 'path': self.file_path, 'meta': ''})
        correct_file_url = req_result['meta']['file_url']
        stid = req_result['meta']['file_mid']

        optional_params = {
            'content_type': 'application/x-www-form-urlencoded',
            'media_type': 'image',
            'fsize': req_result['meta']['size'],
            'md5': req_result['meta']['md5'],
            'hid': req_result['meta']['hid'],
        }

        params = {
            'url_type': 'file',
            'uid': self.uid,
            'stid': stid,
            'file_name': self.file_name,
        }

        params.update(optional_params)
        file_url = self.json_ok('generate_zaberun_url', params)['zaberun_url']
        assert file_url == correct_file_url

    def test_get_preview_url(self):
        cur_time = date.today()

        self.upload_file(self.uid, self.file_path)
        with time_machine(cur_time):
            req_result = self.json_ok('info', {'uid': self.uid, 'path': self.file_path, 'meta': ''})
        correct_file_url = req_result['meta']['preview']
        stid = req_result['meta']['pmid']

        optional_params = {
            'inline': 1,
            'eternal': 1,
            'content_type': 'image/jpeg',
            'size': 'S',
            'crop': 0,
        }

        params = {
            'url_type': 'preview',
            'uid': self.uid,
            'stid': stid,
            'file_name': self.file_name,
        }

        params.update(optional_params)
        with time_machine(cur_time):
            file_url = self.json_ok('generate_zaberun_url', params)['zaberun_url']

        assert file_url == correct_file_url

    def test_get_public_file_url(self):
        self.upload_file(self.uid, self.file_path)
        hash = self.json_ok('set_public', {'uid': self.uid, 'path': self.file_path})['hash']
        req_result = self.json_ok('public_info', {'private_hash': hash, 'meta': ''})

        correct_file_url = req_result['resource']['meta']['file_url']
        stid = req_result['resource']['meta']['file_mid']

        optional_params = {
            'content_type': 'application/x-www-form-urlencoded',
            'media_type': 'image',
            'fsize': req_result['resource']['meta']['size'],
            'md5': req_result['resource']['meta']['md5'],
            'hid': Binary(str(req_result['resource']['meta']['hid']), subtype=2)
        }

        params = {
            'url_type': 'file',
            'stid': stid,
            'file_name': self.file_name,
        }

        params.update(optional_params)
        file_url = self.json_ok('generate_zaberun_url', params)['zaberun_url']

        assert file_url == correct_file_url

    def test_get_public_preview_url(self):
        self.upload_file(self.uid, self.file_path)
        hash = self.json_ok('set_public', {'uid': self.uid, 'path': self.file_path})['hash']
        req_result = self.json_ok('public_info', {'private_hash': hash, 'meta': ''})

        correct_file_url = req_result['resource']['meta']['preview']
        stid = req_result['resource']['meta']['pmid']

        optional_params = {
            'inline': 'true',
            'eternal': '',
            'content_type': 'image/jpeg',
            'size': 'S',
            'crop': '0',
        }

        params = {
            'url_type': 'preview',
            'stid': stid,
            'file_name': self.file_name,
        }

        params.update(optional_params)
        file_url = self.json_ok('generate_zaberun_url', params)['zaberun_url']
        assert file_url == correct_file_url

    def test_incorrect_url_type(self):
        params = {
            'url_type': 'fake',
            'stid': '111',
            'file_name': self.file_name,
        }
        self.json_error('generate_zaberun_url', params, code=221)
