# coding: utf-8

import contextlib
import mock
import os
import time

from ConfigParser import SafeConfigParser
from datetime import datetime, timedelta
from tempfile import NamedTemporaryFile
from unittest import TestCase

from market.pylibrary import common
from market.idx.pylibrary.versioned_data import VersionedDirectory

from market.idx.marketindexer.mi.plugins.bids import BidsPlugin


class BidsEnv(object):

    __slots__ = ('config_paths', 'temp_dirpath')

    def __init__(self, **kwargs):
        for attr_name, attr_val in kwargs.items():
            setattr(self, attr_name, attr_val)


def _build_config_dict(temp_dirpath, **kwargs):

    def build_path(*args):
        return os.path.join(temp_dirpath, *args)

    def build_dir(*args):
        path = os.path.join(temp_dirpath, *args)
        os.makedirs(path)
        return path

    return {
        ('bids', 'bids_dir'): kwargs.get('bids_dir') or build_path('bids'),
        ('bids', 'lock_dir'): kwargs.get('lock_dir', build_dir('locks')),
        ('bids', 'int keep_last_n_snapshots'): kwargs.get('keep_last_n_snapshots') or 3,
        ('bids', 'int keep_last_n_datastore_versions'): kwargs.get('keep_last_n_datastore_versions') or 3,
        ('bids', 'mbi_credentials_path'): kwargs.get('mbi_credentials_path') or build_path('creds'),
        ('bids', 'metafile_creator_bin'): kwargs.get('metafile_creator_bin') or build_path('bin'),
        ('bids', 'model_bids_url'): kwargs.get('model_bids_url') or 'https://test.com/path',
        ('bids', 'offer_bids_url'): kwargs.get('offer_bids_url') or 'https://test.com/path',
        ('bids', 'snapshot_concatenator_bin'): kwargs.get('snapshot_concatenator_bin') or build_path('bin'),

    }


class TestBidsPlugin(TestCase):

    @classmethod
    def setUpClass(cls):
        cls._old_value = os.environ.get('DISABLE_SKIP_IF')
        os.environ['DISABLE_SKIP_IF'] = '1'

    @classmethod
    def tearDownClass(cls):
        if cls._old_value is not None:
            os.environ['DISABLE_SKIP_IF'] = cls._old_value
        else:
            del os.environ['DISABLE_SKIP_IF']

    def test_update_snapshot(self):
        with mock.patch('market.idx.marketindexer.mi.plugins.bids.MbiService') as MbiService:

            def download_stub(url, dst_path, *args, **kwargs):
                _create_file(dst_path, content='snapshot stub')
                return int(round(time.time()))

            instance = MbiService.return_value
            instance.download = download_stub

            with mock.patch('market.idx.marketindexer.marketindexer.bids.resource.util') as UtilMod:

                def create_meta(cmd_list, *args, **kwargs):
                    meta_filepath = cmd_list[cmd_list.index('--ometa') + 1]
                    _create_file(meta_filepath, content='snapshot meta stub')
                    return 0

                UtilMod.watching_check_call = create_meta

                with _build_env(env_dict={'MI_TYPE': 'strat', 'ENV_TYPE': 'dev'}) as env:
                    plugin = BidsPlugin(config_paths=env.config_paths)

                    version = plugin.update_snapshot(
                        data_type='offer',
                        with_quick_bids=True,
                        with_slow_bids=False,
                        threshold=None,
                        with_merge=False,
                        with_yt_upload=False,
                    )

                    self.assertIsNotNone(version)

                    snapshot_path = os.path.join(version.path, 'snapshot.pbuf.sn')
                    meta_path = os.path.join(version.path, 'snapshot.meta')
                    self.assertTrue(os.path.exists(snapshot_path))
                    self.assertTrue(os.path.exists(meta_path))

                    with open(snapshot_path) as sn_fobj:
                        self.assertEqual(sn_fobj.read(), 'snapshot stub')
                    with open(meta_path) as meta_fobj:
                        self.assertEqual(meta_fobj.read(), 'snapshot meta stub')

    def test_copy_snapshot(self):
        with _build_env(env_dict={'MI_TYPE': 'strat', 'ENV_TYPE': 'dev'}) as env:
            dest_dirpath = os.path.join(env.temp_dirpath, 'copy')

            plugin = BidsPlugin(config_paths=env.config_paths)
            work_dir = os.path.join(plugin.config.bids_dir, 'snapshots', 'offer', 'quick')
            vdir = VersionedDirectory(work_dir)

            for index in range(3):
                v = _create_version(vdir, datetime.now() - timedelta(hours=index))
                _create_file(os.path.join(v.path, 'data'), content='test data %s' % index)
            vdir.update_recent()

            version = plugin.copy_snapshot(
                data_type='offer',
                with_quick_bids=True,
                with_slow_bids=False,
                dest_dirpath=dest_dirpath,
            )

            data_path = os.path.join(version.path, 'data')
            dest_path = os.path.join(dest_dirpath, 'data')
            self.assertTrue(os.path.exists(data_path))
            self.assertTrue(os.path.exists(dest_path))

            with open(data_path) as src_fobj, open(dest_path) as dst_fobj:
                self.assertEqual(src_fobj.read(), dst_fobj.read())

    def test_update_datastore(self):
        with _build_env(env_dict={'MI_TYPE': 'strat', 'ENV_TYPE': 'dev'}) as env:
            src_dirpath = os.path.join(env.temp_dirpath, 'source')
            _create_file(os.path.join(src_dirpath, 'data0'), content='test data 0')
            _create_file(os.path.join(src_dirpath, 'dir1', 'data1'), content='test data 1')

            plugin = BidsPlugin(config_paths=env.config_paths)
            work_dir = os.path.join(plugin.config.bids_dir, 'datastore', 'test')
            vdir = VersionedDirectory(work_dir)

            datetime_mod = 'market.idx.pylibrary.versioned_data.versioned_directory.datetime'
            for index in range(5):
                with mock.patch(datetime_mod) as MockDateTime:
                    date = datetime.now() + (-1)**index * timedelta(hours=index)
                    MockDateTime.utcnow.return_value = date
                    MockDateTime.now.return_value = date
                    plugin.update_datastore(
                        'test', time.time() + (-1)**index * 3600 * index,
                        inputs=[
                            os.path.join(src_dirpath, 'data0'),
                            os.path.join(src_dirpath, 'dir1', 'data1')
                        ],
                        outputs=['data0', os.path.join('dir123', 'data1')],
                    )
            self.assertIsNotNone(vdir.recent)
            self.assertEqual(len(list(vdir.versions)), 3)

            src_path = os.path.join(src_dirpath, 'data0')
            dst_path = os.path.join(vdir.recent.path, 'data0')
            self.assertTrue(os.path.exists(src_path))
            self.assertTrue(os.path.exists(dst_path))
            with open(src_path) as src_fobj, open(dst_path) as dst_fobj:
                self.assertEqual(src_fobj.read(), dst_fobj.read())

            src_path = os.path.join(src_dirpath, 'dir1', 'data1')
            dst_path = os.path.join(vdir.recent.path, 'dir123', 'data1')
            self.assertTrue(os.path.exists(src_path))
            self.assertTrue(os.path.exists(dst_path))
            with open(src_path) as src_fobj, open(dst_path) as dst_fobj:
                self.assertEqual(src_fobj.read(), dst_fobj.read())


@contextlib.contextmanager
def _build_env(config_dict_builder=_build_config_dict, env_dict=None):

    def write_config(config_dict, file_obj):
        cfg = SafeConfigParser()

        for (section, opt_name), opt_val in config_dict.iteritems():
            if section not in cfg.sections():
                cfg.add_section(section)
            cfg.set(section, opt_name, str(opt_val))

        cfg.write(file_obj)
        file_obj.flush()

    with common.temp.make_dir() as temp_dirpath:

        with NamedTemporaryFile(mode='w+') as cfg_fobj:
            write_config(config_dict_builder(temp_dirpath), cfg_fobj)

            with common.context.updated_mapping(os.environ, env_dict or {}):
                yield BidsEnv(
                    config_paths=[cfg_fobj.name],
                    temp_dirpath=temp_dirpath,
                )


def _create_file(filepath, content=None):
    dir_path = os.path.dirname(filepath)
    if (not os.path.exists(dir_path)):
        os.makedirs(dir_path)

    with open(filepath, 'w') as fobj:
        if content is not None:
            fobj.write(content)


def _create_version(vdir, date):
    datetime_mod = 'market.idx.pylibrary.versioned_data.versioned_directory.datetime'
    with mock.patch(datetime_mod) as MockDateTime:
        MockDateTime.utcnow.return_value = date
        MockDateTime.now.return_value = date
        return common.context.close(vdir.create_version())
