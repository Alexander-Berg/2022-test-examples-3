import logging
import os
import shlex

from sandbox import common
from sandbox import sdk2
from sandbox.sdk2.helpers import subprocess as sp

import sandbox.common.types.resource as ctr

from sandbox.projects.common.arcadia import sdk as arcadia_sdk

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


# YMBOT-301
class YmbotRunTests(sdk2.Task):

    ARCADIA_YMBOT_SRC_DIR = 'market/robotics/ymbot'

    class Parameters(sdk2.Task.Parameters):
        arcadia_url = sdk2.parameters.ArcadiaUrl(
            "Arcadia repository url",
            default_value="arcadia-arc:/#trunk",
            required=True
        )

    __exec_logger = {}

    @property
    def target_dir(self):
        return str(self.path() / "ymbot")

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

        return super(YmbotRunTests, self).on_create()

    def on_execute(self):
        ev = None

        with sdk2.helpers.ProgressMeter("Mounting Arcadia url {}...".format(self.Parameters.arcadia_url)):
            with self.checkout() as path:
                logging.info("Mounted Arcadia source dir: %s", path)

                src_dir = path + self.ARCADIA_YMBOT_SRC_DIR

                self.execute("ls -l " + src_dir, 'checkout')
                self.execute("du -hd5 " + src_dir, 'checkout')

                with sdk2.helpers.ProgressMeter("Building YMBot..."):
                    logging.info(self.build(src_dir))
                with sdk2.helpers.ProgressMeter("Testing YMBot..."):
                    try:
                        ret = self.test(src_dir)
                    except Exception as ex:
                        ev = ex
                    logging.info(ret if ev is None else "Tests failed")
                with sdk2.helpers.ProgressMeter("Print short test results..."):
                    logging.info(self.print_short_test_results())
                with sdk2.helpers.ProgressMeter("Print full test results..."):
                    logging.info(self.print_full_test_results())

        if ev is not None:
            raise ev

    def checkout(self):
        self.execute("id", 'checkout')
        self.execute("ls -l", 'checkout')

        return arcadia_sdk.mount_arc_path(
            self.Parameters.arcadia_url,
            use_arc_instead_of_aapi=True,
            fetch_all=False,
            minimize_mount_path=False,
        )

    def build(self, path):
        os.chdir(path)
        self.execute("bash -c '. /ymbot/ros2/foxy/bin/setup.bash && "
                     "colcon build --build-base {} "
                     "--install-base {} --test-result-base {}'".format(self.target_dir + '/build',
                                                                       self.target_dir + '/bin',
                                                                       self.target_dir + '/tests'),
                     'build')
        return "'colcon build' executed -> success"

    def test(self, path):
        os.chdir(path)
        self.execute("bash -c '. /ymbot/ros2/foxy/bin/setup.bash && . {1}/local_setup.bash && "
                     "colcon test --build-base {0} --install-base {1} --test-result-base {2} && "
                     "colcon test-result --test-result-base {2}'".format(self.target_dir + '/build',
                                                                         self.target_dir + '/bin',
                                                                         self.target_dir + '/tests'),
                     'test')
        return "'colcon test' executed -> success"

    def print_short_test_results(self):
        self.execute("colcon test-result --all --test-result-base " + self.target_dir + '/tests',
                     'test-results-short', ignore_failure=True)
        return "'colcon test-result' executed"

    def print_full_test_results(self):
        self.execute("colcon test-result --all --verbose --test-result-base  " + self.target_dir + '/tests',
                     'test-results-full', ignore_failure=True)
        return "'colcon test-result --verbose' executed"

    def execute(self, command_line, logger_name, add_custom_env_secrets=False, ignore_failure=False):
        """
        Take text of shell command.
        """

        args = shlex.split(command_line)
        my_env = os.environ.copy()
        my_env["TMPDIR"] = "/var/tmp"

        if add_custom_env_secrets:
            my_env.update(self._extract_custom_env_secrets())

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
