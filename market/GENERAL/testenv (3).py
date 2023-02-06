import os, sys, logging, pytest
import utils


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self._python_bin = yatest.common.python_path()
        self._work_dir = yatest.common.work_path()
        self._binary_path = yatest.common.binary_path
        self._source_path = yatest.common.source_path

    def binary_path(self, path):
        return self._binary_path(path)

    def source_path(self, path):
        return self._source_path(path)

    @property
    def work_dir(self):
        return self._work_dir

    @property
    def python_bin(self):
        return self._python_bin


class __SimpleEnv:  # for "pytest ..."  - doesn't work for now because of yt_local
    def __init__(self):
        this_file_dir = os.path.dirname(os.path.abspath(__file__))
        self._arcadia_root = os.path.realpath(os.path.join(os.path.expanduser(this_file_dir), '../../../../'))

    def binary_path(self, path):
        return os.path.join(self._arcadia_root, path)

    def source_path(self, path):
        return os.path.join(self._arcadia_root, path)

    @property
    def work_dir(self):
        return os.getcwd()

    @property
    def python_bin(self):
        return sys.executable


_env = __YaTestEnv() if (os.environ.get('YA_TEST_RUNNER', None) is not None) else __SimpleEnv()


class GlobalTestEnv:
    PYTHON_BIN = _env.python_bin
    WORK_DIR = _env.work_dir
    FORECASTER_OFFLINE_BIN = _env.binary_path("market/forecaster/tools/offline_forecaster/forecaster-offline")
    RESOURCE_PRODUCER_BIN = _env.binary_path("market/forecaster/tools/resource_producer/resource_producer")
    FORECASTER_DOWNLOADER_BIN = _env.binary_path("market/forecaster/tools/downloader/forecaster-downloader")


@pytest.fixture(scope="module")
def testenv(request):
    module_work_dir = os.path.join(GlobalTestEnv.WORK_DIR, request.module.__name__)
    utils.force_create_directory(module_work_dir)

    logging.info("Set work dir '{}'".format(module_work_dir))

    class ModuleTestEnv(GlobalTestEnv):
        WORK_DIR = module_work_dir
        MODULE_NAME = request.module.__name__

    wd = os.getcwd()
    os.chdir(ModuleTestEnv.WORK_DIR)
    yield ModuleTestEnv

    logging.info("Restore work dir '{}'".format(wd))
    os.chdir(wd)
