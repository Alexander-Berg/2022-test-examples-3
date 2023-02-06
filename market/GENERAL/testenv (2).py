# -*- coding: utf-8 -*-

import os.path
import shutil
import subprocess
import tempfile
import resource
import random
from datetime import datetime


def is_yatest():
    return os.environ.get('YA_TEST_RUNNER', None) is not None


class YaTestProcessRunner(object):
    def __init__(self, command, out, err, env, stdin):
        import yatest.common
        self.__exec_context = yatest.common.execute(command, stdin=stdin, stdout=out, stderr=err, env=env, wait=False)

    @property
    def pid(self):
        return self.__exec_context.process.pid

    @property
    def alive(self):
        return self.__exec_context.running

    @property
    def returncode(self):
        return self.poll()

    def wait(self):
        self.__exec_context.wait()

    def poll(self):
        return self.__exec_context.process.poll()

    def terminate(self):
        self.__exec_context.process.terminate()

    def communicate(self, input=None):
        return self.__exec_context.process.communicate(input)


class LiteProcessRunner(object):
    def __init__(self, command, out, err, env, stdin):
        def set_limits():
            resource.setrlimit(resource.RLIMIT_CORE, (resource.RLIM_INFINITY, resource.RLIM_INFINITY))

        self.__exec_context = subprocess.Popen(command, stdin=stdin, stdout=out, stderr=err, env=env, preexec_fn=set_limits)

    @property
    def pid(self):
        return self.__exec_context.pid

    @property
    def alive(self):
        return self.__exec_context.poll() is None

    @property
    def returncode(self):
        return self.poll()

    def wait(self):
        self.__exec_context.wait()

    def poll(self):
        return self.__exec_context.poll()

    def terminate(self):
        self.__exec_context.terminate()

    def communicate(self, input=None):
        self.__exec_context.communicate(input)


def create_process(command, out=None, err=None, env=None, stdin=None):
    if err is None:
        err = out

    if is_yatest():
        return YaTestProcessRunner(command, out, err, env, stdin)
    else:
        return LiteProcessRunner(command, out, err, env, stdin)


def run_external_tool(command, log_path, get_output=False):
    with open(log_path, 'a+') as log:
        log.write('Running command: {0}\n'.format(' '.join(command)))
        log.flush()
        if get_output:
            temp_dir = tempfile.mkdtemp()
        try:
            if get_output:
                out_file_path = os.path.join(temp_dir, 'out.txt')
                out_file = open(out_file_path, 'w')
            try:
                proc = create_process(command, out_file if get_output else log, err=log)
                proc.wait()
                if proc.returncode != 0:
                    raise RuntimeError(
                        '{command} failed. See {log} for details'.format(
                            command=' '.join(command), log=log_path))
            finally:
                if get_output:
                    out_file.close()
            if get_output:
                with open(out_file_path, 'r') as out_file:
                    out = out_file.read()
                    log.write(out)
                    return out
        finally:
            if get_output:
                shutil.rmtree(temp_dir, ignore_errors=True)


class YaTestPortManager(object):
    def __init__(self, *args, **kwargs):
        from yatest.common.network import PortManager
        self.pm = PortManager(*args, **kwargs)

    def get_port(self, port=None):
        return self.pm.get_port(port)

    def release(self):
        return self.pm.release()


# ytodo store on FS registry on busy ports
class LitePortManager(object):
    def __init__(self, *_):
        pass

    @staticmethod
    def get_port(port=None):
        return int(port) if port else random.randint(1024, 30000)

    @staticmethod
    def release():
        pass


class TestEnvironment:
    def __init__(self, paths):
        self._paths = paths
        self._common_log = open(os.path.join(self._paths.logs, "testenv.log"), "a+")
        self._portman = YaTestPortManager() if is_yatest() else LitePortManager()

    def cleanup(self):
        self._portman.release()
        self._common_log.close()


    def log(self, title, message):
        self._common_log.write("{} [{}] {}\n".format(datetime.now().isoformat(), title, message))
        self._common_log.flush()

    def get_port(self, port=None):
        return self._portman.get_port(port)


__testenv = [None]


def setup(paths):
    __testenv[0] = TestEnvironment(paths)


def cleanup():
    if __testenv[0] is not None:
        __testenv[0].cleanup()
        __testenv[0] = None


def log(name, message):
    __testenv[0].log(name, message)


def get_port(port=None):
    return __testenv[0].get_port(port)


def get_paths():
    return __testenv[0]._paths


def get_source_path(path):
    if is_yatest():
        import yatest.common
        return yatest.common.source_path(path)
    else:
        return os.path.join(__testenv[0]._paths.src_root, path)
