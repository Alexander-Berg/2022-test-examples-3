# -*- coding: utf-8 -*-

from datetime import datetime
from contextlib import contextmanager
from tempfile import NamedTemporaryFile
from itertools import chain
import mock
import os
import py
import pytest
import six

from pyb.taskrunner import TaskInfo
from market.pylibrary.hammer import AsyncThread
from market.pylibrary.yatestwrap.yatestwrap import source_path


class DummyTaskRunner(object):
    '''
    Replacement for TaskRunner that doesn't spawn processes or threads.
    Better for predictability and debugging.
    '''
    def __init__(self, *args, **kwargs):
        self.tasks = {}

    def start(self, name, target, args):
        try:
            result = target(*args)
        except Exception as e:
            result = str(e)
        self.tasks[name] = result

    def kill(self, name):
        if name in self.tasks:
            del self.tasks[name]

    def get(self, name):
        if name not in self.tasks:
            return TaskInfo(name, TaskInfo.NOT_FOUND)
        return TaskInfo(name, TaskInfo.FINISHED, self.tasks[name])


class SyncThread(AsyncThread):
    '''
    Replacement for market.pylibrary.hammer.AsyncThread that run given function synchronously
    '''
    def __init__(self, async_func, func_args):
        self._result = None
        self._done = False
        decorated_func = AsyncThread.decorator(async_func)
        func_args.insert(0, self)
        decorated_func(*func_args)

    def _do_wait_result(self):
        if self._done:
            raise Exception('AsyncThread is one-shot object')
        self._done = True

    def wait_is_ready(self, seconds=1):
        return True


def idx_to_string(part_idx, zpad=False):
    if isinstance(part_idx, str):
        return part_idx
    if zpad:
        return '{:02}'.format(part_idx)
    return str(part_idx)


@pytest.yield_fixture(scope='class')
def dist_names_test1_test2():
    from mock import patch

    def dist_hardcoded(config, service_name):
        return ['test1', 'test2']

    with patch('pyb.plugin.marketsearch._get_dist_names', dist_hardcoded):
        yield


@pytest.yield_fixture(scope='class')
def no_zk_filter():
    from mock import patch

    def filter_none(config, genlist, service_name):
        return genlist

    with patch('pyb.plugin.marketsearch._filter_incomplete_generations', filter_none):
        yield


@pytest.yield_fixture(scope='class')
def backctld_app(tmpdir_factory):
    from pyb.core import App, Config
    root_dir = tmpdir_factory.getbasetemp()
    config = Config()
    config.log_file = None
    config.root_dir = str(root_dir.join('root').ensure(dir=True))
    config.pid_dir = str(root_dir.join('pids').ensure(dir=True))
    config.plugins_dir = str(root_dir.join('plugins').ensure(dir=True))
    config.torrent_client = '/bin/echo'

    class TestApp(App):
        def __init__(self, config):
            App.__init__(self, config)
            for context in list(self._contexts.values()):
                context.task_runner = DummyTaskRunner()

        def set_root_dir(self, root_dir):
            config_dir = py.path.local(source_path('market/backctld/tests/test_configs'))
            for config_file in config_dir.listdir():
                if not config_file.basename.endswith('.conf'):
                    continue
                dst_config_file = py.path.local(config.plugins_dir).join(config_file.basename)
                config_content = config_file.read()
                config_content = config_content.format(root_dir=root_dir)
                config_content = six.ensure_text(config_content)
                dst_config_file.write(config_content)
                plugin_name = config_file.basename[:-len('.conf')]  # remove .conf
                plugin_context = self._contexts[plugin_name]
                plugin_context.plugin_config_path = str(dst_config_file)
                plugin_context.config.read(str(dst_config_file))
            py.path.local(root_dir).join('locks').ensure(dir=True)
            py.path.local(root_dir).join('cache').ensure(dir=True)
            py.path.local(root_dir).join('tmp').ensure(dir=True)
            py.path.local(root_dir).join('search/index').ensure(dir=True)
    app = TestApp(config)
    yield app


class FsObject(object):
    def make(self, root_dir, full_path):
        raise NotImplementedError()


def symlink_to(path):
    class Symlink(FsObject):
        def __init__(self, to_path):
            self._to_path = to_path

        def make(self, root_dir, full_path):
            root_dir = py.path.local(root_dir)
            full_path = py.path.local(full_path)
            py.path.local(full_path.dirname).ensure(dir=True)
            full_path.mksymlinkto(root_dir / self._to_path)
    return Symlink(path)


def empty_dir():
    class EmptyDir(FsObject):
        def make(self, _, full_path):
            py.path.local(full_path).ensure(dir=True)
    return EmptyDir()


def file_with_data(data):
    class FileWithData(FsObject):
        def __init__(self, data):
            self._data = data

        def make(self, _, full_path):
            py.path.local(full_path).write(self._data, ensure=True)
    return FileWithData(data)


def empty_file():
    return file_with_data('')


def make_filestubs(root_dir, path_to_data):
    '''
    Создает множество файлов/каталогов с предопределенными данными.
    path_to_data - dict вида:
    {
        "относительный к root_dir путь": инструкция как его получить
    }
    root_dir - корневая директория относительно которуй указанны
               пути к файлам
    '''
    for path, fs_object in list(path_to_data.items()):
        assert isinstance(fs_object, FsObject)
        target_path = py.path.local(root_dir).join(path)
        fs_object.make(root_dir, target_path)


def _make_search_part_stage_0(root_dir, part_idx, name):
    make_filestubs(root_dir, {
        'content': file_with_data(name),
        'feedid_offerid.sequence.binary': file_with_data(name),
        'market_qbids.generation': file_with_data(name + '00'),
        'bids.flags.report.binary': file_with_data(name + '00'),
        # base
        'indexinv': file_with_data(name),
        'indexkey': file_with_data(name),
    })


def _make_search_part_stage_1(root_dir, part_idx, name):
    make_filestubs(root_dir, {
        'indexfactorann': file_with_data(name),
    })


def _make_search_part(root_dir, part_idx, name):
    _make_search_part_stage_0(root_dir, part_idx, name)
    _make_search_part_stage_1(root_dir, part_idx, name)


def _make_report_data(root_dir, name):
    make_filestubs(root_dir, {
        'content': file_with_data(name),
        'backends/content': file_with_data(name),
    })


def make_full_generation(root_dir, name, completed=False, parts=None, split_search_part=False):
    '''
    Создет full загруженное поколение.
    '''
    root_dir = py.path.local(root_dir)
    parts = [0, 1] if parts is None else parts
    for part_idx in parts:
        if split_search_part:
            search_part_stage_0 = (
                root_dir / 'search/marketsearch' /
                name / 'search-part-base-{}'.format(part_idx) /
                'search-part-base-{}-{}'.format(part_idx, name)
            )
            _make_search_part_stage_0(search_part_stage_0, part_idx, name)
            search_part_stage_1 = (
                root_dir / 'search/marketsearch' /
                name / 'search-part-additions-{}'.format(part_idx) /
                'search-part-additions-{}-{}'.format(part_idx, name)
            )
            _make_search_part_stage_1(search_part_stage_1, part_idx, name)
        else:
            search_part = (
                root_dir / 'search/marketsearch' /
                name / 'search-part-{}'.format(part_idx) /
                'search-part-{}-{}'.format(part_idx, name)
            )
            _make_search_part(search_part, part_idx, name)
    report_data = root_dir / 'search/marketsearch' / name / 'search-report-data' / 'search-report-data-{}'.format(name)
    _make_report_data(report_data, name)
    make_filestubs(root_dir, {
        'search/marketsearch/' + name + '/search-stats/content': file_with_data(name),
    })
    if completed:
        search_part_dirs = ('search-part-{}'.format(part_idx) for part_idx in parts)
        if split_search_part:
            search_part_dirs = chain(
                ('search-part-base-{}'.format(part_idx) for part_idx in parts),
                ('search-part-additions-{}'.format(part_idx) for part_idx in parts),
            )
        for folder in chain(('search-stats', 'search-report-data'), search_part_dirs):
            with open(os.path.join(str(root_dir), 'search/marketsearch', name, '{}'.format(folder), 'completed'), 'w'):
                pass


def make_full_index(root_dir, name, parts=None):
    '''
    Создет full поколение разложенное под репорт.
    '''
    if parts is None:
        parts = [0, 1]
    root_dir = py.path.local(root_dir)
    part_paths = []
    for part_idx in parts:
        part = root_dir / 'search/index/part-{}'.format(part_idx)
        _make_search_part(part, part_idx, name)
        part_paths.append(part)
    report_data = root_dir / 'search/report-data'
    _make_report_data(report_data, name)
    make_filestubs(root_dir, {
        'search/marketsearch/current.generation': file_with_data(name),
        'search/index/mmap/content': file_with_data(name),
    })
    return part_paths


def make_qindex_mds(root_dir, name, parts=[0, 1], permanent=True, finished=True):
    gen_dir_path = 'search/qindex-mds/{}/{}'.format(
        'delta' if permanent else 'download',
        name,
    )
    part_paths = ['data/delta.shard-{}.pbuf.sn'.format(p) for p in parts]
    make_filestubs(root_dir / gen_dir_path, {
        p: file_with_data(name) for p in part_paths
    })
    if finished:
        make_filestubs(root_dir / gen_dir_path, {
            'finished': file_with_data('')
        })


def make_qpipe_mds(root_dir, entity, name, parts=None, permanent=True, finished=True):
    parts = [0, 1] if parts is None else parts
    gen_dir_path = 'search/qpipe/{}/{}/{}'.format(
        'delta' if permanent else 'download',
        entity,
        name,
    )
    part_paths = ['data/delta.shard-{}.pbuf.sn'.format(p) for p in parts]
    make_filestubs(root_dir / gen_dir_path, {
        p: file_with_data(name) for p in part_paths
    })
    if finished:
        make_filestubs(root_dir / gen_dir_path, {
            'finished': file_with_data('')
        })


def make_qbid_mds(root_dir, name, parts=[0], permanent=True, finished=True):
    gen_dir_path = 'search/qbid-mds/{}/{}'.format(
        'delta' if permanent else 'download',
        name,
    )
    part_paths = ['data/delta.shard-{}.pbuf.sn'.format(p) for p in parts]
    make_filestubs(root_dir / gen_dir_path, {
        p: file_with_data(name) for p in part_paths
    })
    if finished:
        make_filestubs(root_dir / gen_dir_path, {
            'finished': file_with_data('')
        })


class DummyDownloader(object):
    def __init__(self):
        pass

    def download(self, url, output_filename=None, timeout=60, throttle=True, verify_etag=True):
        return dummy_download_file(url, output_filename)


def dummy_download_file(url, filepath=None):
    source = url[len('http://'):]
    if not filepath:
        tmp = NamedTemporaryFile(delete=False)
        filepath = tmp.name
    filepath = py.path.local(filepath)
    if source.startswith('content?'):
        filepath.write(source.split('?', 1)[1])
    else:
        py.path.local(source_path(source)).copy(filepath)
    return str(filepath)


class BaseFixture(object):
    def __init__(self, root_dir, app):
        self.root_dir = root_dir
        self._app = app
        self.search_part_paths = []
        self.search_part_ids = []

    def run(self, command):
        raise NotImplementedError('run must be implemented is descendants')

    def add_full_generation(self, generation, parts=[0]):
        self.search_part_paths.extend(
            make_full_index(self.root_dir, generation, parts)
        )
        self.search_part_ids.extend(parts)

    @contextmanager
    def mocks(self):
        try:
            self._make_mocks()
            yield
        finally:
            self._destroy_mocks()

    def _make_mocks(self):
        self._check_call_patcher = mock.patch('market.pylibrary.mi_util.util.watching_check_call', return_value=0)
        self._system_call_patcher = mock.patch('market.pylibrary.mi_util.util.watching_system_ex', return_value=0)
        ts_base = datetime.utcfromtimestamp(0)
        ts_now = datetime(2017, 3, 5, 20, 30)  # match 20170305_173000 in MSK timezone
        now = int((ts_now - ts_base).total_seconds())
        self._now_patcher = mock.patch('market.pylibrary.mi_util.util.now', return_value=now)
        self._async_thread_patcher = mock.patch('market.pylibrary.hammer.AsyncThread', SyncThread)
        self.check_call = self._check_call_patcher.start()
        self.system_call = self._system_call_patcher.start()
        self.now_call = self._now_patcher.start()
        self._async_thread_patcher.start()

    def _destroy_mocks(self):
        self._check_call_patcher.stop()
        self._system_call_patcher.stop()
        self._now_patcher.stop()
        self._async_thread_patcher.stop()


class UpdateFixture(BaseFixture):
    def __init__(self, *args, **kwargs):
        super(UpdateFixture, self).__init__(*args, **kwargs)
        self.update_result = None
        self.check_result = None

    def _make_mocks(self):
        super(UpdateFixture, self)._make_mocks()
        self._downloader_patcher = mock.patch(
            'market.pylibrary.mds_downloader.mds_downloader.global_downloader', DummyDownloader())
        self._qpipe_async_thread_patcher = mock.patch(
            'market.pylibrary.hammer.AsyncThread',
            SyncThread
        )
        self._chown_patcher = mock.patch('os.chown')
        self._getpwnam_patcher = mock.patch('pwd.getpwnam')
        self._getgrnam_patcher = mock.patch('grp.getgrnam')
        self._downloader_patcher.start()
        self._qpipe_async_thread_patcher.start()
        self._chown_patcher.start()
        self._getpwnam_patcher.start()
        self._getgrnam_patcher.start()

    def _destroy_mocks(self):
        super(UpdateFixture, self)._destroy_mocks()
        self._downloader_patcher.stop()
        self._qpipe_async_thread_patcher.stop()
        self._chown_patcher.stop()
        self._getpwnam_patcher.stop()
        self._getgrnam_patcher.stop()


def makedirs(dirpath):
    if not os.path.isdir(dirpath):
        os.makedirs(dirpath)


def symlink(src, dst):
    if os.path.lexists(dst):
        return
    src = os.path.realpath(src)
    dst = os.path.realpath(dst)
    dstdir = os.path.dirname(dst)
    src = os.path.relpath(src, dstdir)
    makedirs(dstdir)
    os.symlink(src, dst)
