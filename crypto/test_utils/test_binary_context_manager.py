import logging
import os

import yatest

from crypta.lib.python import time_utils


class TestBinaryContextManager(object):
    def __init__(self, app_name, frozen_time=None, env=None, check_exit_code=True):
        self.app_name = app_name
        self.frozen_time = str(frozen_time) if frozen_time is not None else None

        self.process = None
        self.logger = logging.getLogger("ya.test")
        self.check_exit_code = check_exit_code

        self.env = os.environ.copy()

        if self.frozen_time is not None:
            self.logger.info("Setting %s to %s", time_utils.CRYPTA_FROZEN_TIME_ENV, self.frozen_time)
            self.env[time_utils.CRYPTA_FROZEN_TIME_ENV] = self.frozen_time

        self.env.setdefault("YT_TOKEN", "_FAKE_YT_TOKEN_")
        self.env.setdefault("YDB_TOKEN", "_FAKE_YDB_TOKEN_")

        if env:
            self.env.update(env)

    def start(self):
        if self.process:
            raise Exception("{} already started".format(self.app_name))

        command_line = self._prepare_start()

        self.logger.info("Starting subprocess %s ...:\n%s", self.app_name, " ".join(command_line))
        self.process = yatest.common.execute(command_line, wait=False, env=self.env)
        self.logger.info("Done")

        try:
            self._wait_until_up()
        except Exception as e:
            self.logger.exception("Waiting until up failed, %s", e)
            self.stop()
            raise

    def stop(self):
        if self.process is None:
            self.logger.error("Can't stop %s since it hasn't started yet", self.app_name)
            return False

        self._on_exit()

        self.logger.info("Stopping %s ...", self.app_name)
        self.process.terminate()
        self.logger.info("Waiting for process to handle signal ...")

        try:
            self.process.wait(timeout=60, check_exit_code=self.check_exit_code)
        except yatest.common.ExecutionTimeoutError:
            self.logger.error("Process did not return in time, kill it")
            try:
                self.process.kill()
            except yatest.common.InvalidExecutionStateError:
                self.logger.error("Tried to kill stopped process")
                pass
            result = False
        except Exception as e:
            self.logger.exception("Unexpected exception, %s", e)
            raise
        else:
            self.logger.info("Process exited successfully ...")
            result = True

        self.process = None
        return result

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()

    def _prepare_start(self):
        raise NotImplementedError

    def _on_exit(self):
        return

    def _wait_until_up(self):
        return
