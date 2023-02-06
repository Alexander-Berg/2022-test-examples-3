import logging
import os
import shlex
from distutils import dir_util

from sandbox import common
from sandbox import sdk2
from sandbox.sdk2.helpers import subprocess as sp
from sandbox.sdk2 import service_resources

import sandbox.common.types.resource as ctr

from sandbox.projects.market.robotics import resource_types


class RichTextTaskFailure(common.errors.TaskFailure):
    def __init__(self, message, rich_addition):
        super(RichTextTaskFailure, self).__init__(message)
        self.rich_addition = rich_addition
        self.message = message

    def __str__(self):
        return "Error was occurred: {}. Info: {}".format(self.message, self.rich_addition)

    def get_task_info(self):
        return self.rich_addition


# YMBOT-300
class YmbotRos2ImageRunTests(sdk2.Task):

    class Requirements(sdk2.Requirements):
        privileged = True

    __exec_logger = {}

    def exec_logger_get(self, name):
        if name not in self.__exec_logger:
            exec_logger = logging.getLogger(name)
            map(exec_logger.removeHandler, exec_logger.handlers[:])
            handler = logging.FileHandler(str(self.log_path(name + '.log')))
            handler.setFormatter(logging.Formatter("%(asctime)s\t%(message)s"))
            exec_logger.addHandler(handler)
            exec_logger.propagate = False
            self.__exec_logger[name] = exec_logger

        return self.__exec_logger[name]

    def on_create(self):
        if not self.Requirements.container_resource:
            self.Requirements.container_resource = resource_types.YmbotLxcRos2Container.find(
                state=ctr.State.READY,
                attrs={"released": "stable"}
            ).first().id

        return super(YmbotRos2ImageRunTests, self).on_create()

    def on_execute(self):
        ev = None

        with sdk2.helpers.ProgressMeter("Run ROS2 test command..."):
            try:
                ret = self.test_ros2()
            except Exception as ex:
                ev = ex
            logging.info(ret if ev is None else "Tests failed")
        with sdk2.helpers.ProgressMeter("Print short test results..."):
            logging.info(self.print_short_test_results())
        with sdk2.helpers.ProgressMeter("Print full test results..."):
            logging.info(self.print_full_test_results())
        with sdk2.helpers.ProgressMeter("Save logs resources..."):
            logging.info(self.save_logs())

        if ev is not None:
            raise ev

    def test_ros2(self):
        os.chdir("/arc/arcadia/market/robotics")
        self.execute("id", 'test')
        self.execute("ls -l", 'test')
        self.execute("ps aux", 'test')
        self.execute("bash -c 'colcon --log-base /ymbot/ros2/foxy/logs test --executor sequential "
                     "--base-paths libs/common libs/main libs/common tools/ament tools/osrf tools/ros tools/ros2 "
                     "--build-base /ymbot/ros2/foxy/build "
                     "--install-base /ymbot/ros2/foxy/bin --test-result-base /ymbot/ros2/foxy/tests && "
                     "colcon test-result --test-result-base /ymbot/ros2/foxy/tests'"
                     , 'test',
                     {'ROS_DISTRO': 'foxy'})
        return "'colcon test' executed"

    def print_short_test_results(self):
        self.execute("colcon --log-base /ymbot/ros2/foxy/logs test-result --all "
                     "--test-result-base /ymbot/ros2/foxy/tests", 'test-results-short',
                     ignore_failure=True)
        return "'colcon test-result' executed"

    def print_full_test_results(self):
        self.execute("colcon --log-base /ymbot/ros2/foxy/logs test-result --all "
                     "--verbose --test-result-base /ymbot/ros2/foxy/tests", 'test-results-full',
                     ignore_failure=True)
        return "'colcon test-result --verbose' executed"

    def save_logs(self):
        resource = service_resources.TaskLogs(self, "YMBot Tests Full Logs", "colcon_logs")
        data = sdk2.ResourceData(resource)
        data.path.mkdir(0o755, parents=True, exist_ok=True)
        resource_dir = str(data.path)
        dir_util.copy_tree("/ymbot/ros2/foxy/logs", resource_dir)
        data.ready()

    def execute(self, command_line, logger_name, env_addition=None, add_custom_env_secrets=False, ignore_failure=False):
        """
        Take text of shell command.
        """

        args = shlex.split(command_line)
        my_env = os.environ.copy()
        my_env["TMPDIR"] = "/var/tmp"

        if add_custom_env_secrets:
            my_env.update(self._extract_custom_env_secrets())

        if env_addition is not None:
            my_env.update(env_addition)

        self.exec_logger_get(logger_name).info("\t>>> EXECUTING COMMAND: %s", command_line)
        with sdk2.helpers.ProcessLog(self, logger=self.exec_logger_get(logger_name), set_action=False) as pl:
            try:
                return_code = sp.Popen(args, stdout=pl.stdout, stderr=sp.STDOUT, env=my_env).wait()
                if return_code == 0:
                    return True
                if not ignore_failure:
                    raise RichTextTaskFailure(
                        "Command {!r} failed, see the log below.".format(command_line),
                        "Shell commands output written to <b><a href='{}'>{}</a></b>".format(
                            "/".join((self.log_resource.http_proxy, logger_name + '.log')),
                            logger_name + '.log'
                        )
                    )
            except Exception:
                logging.exception("SUBPROCESS ERROR")
                raise

    def _extract_custom_env_secrets(self):
        logging.info("Extracting secrets to environment variables")
        acceptable_secrets = {}
        for name, secret in self.Parameters.custom_env_secrets.items():
            secret = sdk2.yav.Secret.__decode__(secret)
            if secret.default_key is not None:
                acceptable_secrets[name] = str(secret.value())
            else:
                logging.warning("Can't extract %s without default_key", name)
        return acceptable_secrets
