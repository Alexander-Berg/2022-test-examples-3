import os, sys, logging, pytest
import utils


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


class GlobalTestEnv():
    PYTHON_BIN = _env.python_bin
    WORK_DIR = _env.work_dir


# must be explicitly imported in every test module
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
