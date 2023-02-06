import logging
import os
import shlex
import time

from sandbox import common
from sandbox import sdk2
from sandbox.sdk2.helpers import subprocess as sp

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


# YMBOT-359
class YmbotTestNucImageFt(sdk2.Task):
    # ARCADIA_YMBOT_SRC_DIR = 'market/robotics/ymbot'
    #
    # class Parameters(sdk2.Task.Parameters):
    #     arcadia_url = sdk2.parameters.ArcadiaUrl(
    #         "Arcadia repository url",
    #         default_value="arcadia-arc:/#trunk",
    #         required=True
    #     )

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
            self.Requirements.container_resource = resource_types.YmbotLxcNucContainer.find(
                state=ctr.State.READY,
                attrs={"released": "stable"}
            ).first().id

        return super(YmbotTestNucImageFt, self).on_create()

    def on_execute(self):
        systemd_unit = "ymbot-dataset-collector.service"
        processes_to_check = [
            "/ymbot/ros2/foxy/bin/ros2cli/bin/ros2 launch "
            "ymbot_obstacle_points dataset_collector_localization.launch.py",

            "/ymbot/ros2/foxy/bin/ros2cli/bin/ros2 launch ymbot_collision_checker velocity_limiter.launch.py",
            "/ymbot/ros2/foxy/bin/ros2cli/bin/ros2 launch ymbot_gamepad_node gamepad.launch.py",
            "/ymbot/ros2/foxy/bin/ros2cli/bin/ros2 launch drcontrol_convert node.launch.py",
            "/ymbot/ymbot/bin/drcontrol_convert/lib/drcontrol_convert/drcontrol_convert_node",
            "/ymbot/ymbot/bin/ymbot_gamepad_node/lib/ymbot_gamepad_node/gamepad_node",
            "/ymbot/ymbot/bin/ymbot_collision_checker/lib/ymbot_collision_checker/velocity_limiter_node",
            "/ymbot/ros2/foxy/bin/realsense2_camera/lib/realsense2_camera/realsense2_camera_node",
            "/ymbot/ros2/foxy/bin/hesai_lidar/lib/hesai_lidar/hesai_lidar_node",
        ]
        processes_errors = []

        with sdk2.helpers.ProgressMeter("Run systemd unit {}...".format(systemd_unit)):
            ev = None
            try:
                self.execute("systemctl stop {}".format(systemd_unit), "ymbot-unit-start")
                self.execute("journalctl --rotate", "ymbot-unit-start")
                self.execute("journalctl --vacuum-time=1s", "ymbot-unit-start")
                self.execute("ip a add 192.168.1.10/24 dev lo", "ymbot-unit-start")
                self.execute("systemctl start {}".format(systemd_unit), "ymbot-unit-start")
            except Exception as ex:
                ev = ex

            if ev:
                logging.info("Unit startup failed, will print status and journal for you")
                self.execute("systemctl status {}".format(systemd_unit), "ymbot-unit-start", ignore_failure=True)
                self.execute("journalctl -xe --unit={} -n10000 --no-pager".format(systemd_unit),
                             "ymbot-unit-start",
                             ignore_failure=True)

            # logging.info("Will now try to start it as a standalone application")
            # self.execute("/run_dataset_collector.sh", "ymbot-unit-start")

            if ev is not None:
                raise ev
        with sdk2.helpers.ProgressMeter("Sleep for 15 seconds..."):
            time.sleep(15)
        with sdk2.helpers.ProgressMeter("Save and check journal for the service..."):
            self.execute("journalctl -xe --unit={} -n10000 --no-pager".format(systemd_unit), "ymbot-unit-journal")
            self.execute("bash -c 'errs=$(journalctl -xe --unit=ymbot-dataset-collector.service -n10000 --no-pager "
                         "| grep \"\\[ERROR\\]\" | wc -l) && [[ $errs -gt 0 ]] && "
                         "echo \"Error: There are $errs errors in the journal, expected 0\" "
                         "|| echo \"Success\" | grep \"Error\" && exit 1 || exit 0'", "ymbot-unit-journal")
            logging.info("OK: Service journal doesn't contain error strings")
        with sdk2.helpers.ProgressMeter("Check running processes..."):
            for process in processes_to_check:
                if not self.execute(
                        "bash -c 'echo $(ps -A x | grep \"{0}\" | grep -v grep > /dev/null && echo Success "
                        "|| echo \"Error: There is no such process {0}\") | grep Error && exit 1 "
                        "|| exit 0'".format(process),
                        "ymbot-unit-check-processes", ignore_failure=True):
                    processes_errors.append(process)

            if processes_errors:
                self.execute("ps -A xww", "process-list", ignore_failure=True)
                raise RichTextTaskFailure("There are NO such processes expected to be running: "
                                          "\n\t{}\n".format("\n\t".join(processes_errors)),
                                          "Pls see a list of running processes in the log "
                                          "<b><a href='{}'>{}</a></b>".format(
                                              "/".join((self.log_resource.http_proxy, 'process-list.log')),
                                              'process-list.log'))

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
