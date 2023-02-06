from datetime import datetime
import json
import logging
import os
import re
import threading

from rta import net
from rta import shell
from rta import startrek


environment = {}


class Registry(object):

    def __init__(self, storage):
        self._storage = storage
        self._instances = list()
        self._lck_loading = threading.Lock()
        self._is_loaded = None

    def __iter__(self):
        return iter(self._instances)

    def add(self, instance):
        self._instances.append(instance)
        self.dump()

    def clean(self):
        dead_instances = [instance for instance in self._instances if instance._status == instance.Status.DEAD]
        if dead_instances:
            logging.info("Removing {} dead instances".format(len(dead_instances)))
        self.remove(dead_instances)
        configuration = environment["configuration"]
        binaries = configuration["report.binaries"]
        logs = configuration["report.logs"]
        used_tickets = set(instance._ticket for instance in self._instances)
        all_tickets = set(shell.list_folders(binaries)) | set(shell.list_folders(logs))
        unused_tickets = all_tickets - used_tickets
        for ticket in unused_tickets:
            shell.remove_folder(os.path.join(binaries, ticket))
            shell.remove_folder(os.path.join(logs, ticket))

    def dump(self):
        resume = [
            {"user": instance._user, "ticket": instance._ticket, "servant": instance._servant}
            for instance in self._instances
        ]
        with open(self._storage, "w") as stream:
            json.dump(resume, stream)

    def find_by_ticket(self, ticket):
        return next((instance for instance in self._instances if instance._ticket == ticket), None)

    def find_by_pid(self, pid):
        return next((instance for instance in self._instances if instance._process.pid == pid), None)

    def load(self):
        if not os.path.exists(self._storage):
            return
        threading.Thread(target=self._do_load).start()

    def _do_load(self):
        if not self._lck_loading.acquire(False):
            return
        try:
            if self._is_loaded == True:
                return
            self._is_loaded = False
            logging.info("Loading the registry from {}".format(self._storage))
            with open(self._storage, "r") as stream:
                resume = json.load(stream)
                to_add = []
                for instance_resume in resume:
                    user, ticket, servant = map(instance_resume.get, ["user", "ticket", "servant"])
                    instance = ReportInstance(user, ticket, servant)
                    to_add.append(instance)
                for instance in to_add:
                    self.add(instance)
            logging.info("Registry loaded")
        finally:
            self._is_loaded = True
            self._lck_loading.release()

    def shutdown_reports(self):
        pids = [i.properties["pid"] for i in self._instances]
        kill_processes(pids)
        self._instances = list()
        self._is_loaded = None

    def remove(self, instances):
        self._instances = [instance for instance in self._instances if instance not in set(instances)]
        self.dump()

    def update(self):
        for instance in self._instances:
            instance.update()

    @property
    def is_in_loading_state(self):
        return self._is_loaded == False


class ReportInstance(object):

    class Status:

        DEAD = 0
        INACTIVE = 1
        ACTIVE = 2

        LABELS = {
            DEAD: "dead",
            INACTIVE: "inactive",
            ACTIVE: "active",
        }

    def __init__(self, user, ticket, servant):
        logging.info("Create a servant `{}` within {}".format(servant, ticket))
        self._user = user
        self._ticket = ticket
        self._servant = servant
        configuration = environment["configuration"]
        self._binary_path = os.path.join(configuration["report.binaries"], ticket)
        shell.make_folder(self._binary_path)
        self._binary = os.path.join(self._binary_path, servant)
        copy_ticket_binary(ticket, self._binary)
        self._log_path = os.path.join(configuration["report.logs"], ticket)
        shell.make_folder(self._log_path)
        reserved_ports = set(report._port for report in environment["registry"])
        self._port = net.find_free_port(reserved_ports)
        self._command = [self._binary, "-d", self._configure()]
        self._process = shell.detach(self._command)
        self._timestamp = datetime.now()
        self._status = self.Status.INACTIVE
        logging.info("Servant created")

    def _configure(self):
        if self._servant in ["market-offline-report"]:
            return self._patch_static_configuration()
        return self._generate_configuration()

    def _generate_configuration(self):
        configuration = environment["configuration"]
        report_configuration = os.path.join(self._binary_path, "{}.cfg".format(self._servant))
        generator_log = os.path.join(self._log_path, "market-report-configs.log")
        generator = [
            configuration["report.configuration.generator"],
            "--log-file", generator_log,
            "--set-prop", "REPORT_TYPE={}".format(self._servant),
            "--default-props", configuration["report.configuration.generator.default"],
            "--set-prop", "HAVE_LOCAL_FIXTARIFF=True",
            "--set-prop", "SERVER_PORT={}".format(self._port),
            "--set-prop", "SEARCH_PID_PATH={}".format(self._binary_path),
            "--set-prop", "SEARCH_LOG_PATH={}".format(self._log_path),
            configuration["report.configuration.generator.template"],
            report_configuration,
        ]
        shell.run(generator)
        return report_configuration

    def _patch_static_configuration(self):
        configuration = environment["configuration"]
        report_configuration = os.path.join(self._binary_path, "{}.cfg".format(self._servant))
        static_configuration = configuration["report.configuration.static"].format(servant=self._servant)
        patcher = [
            "sed",
            "-e", r"s/\(17057\)/{}/g".format(self._port),
            "-e", r"s#/var/log/search#{}#g".format(self._log_path),
            "-e", r"/LoadLog/i\    ServerLog {0}/{1}-server.log".format(self._log_path, self._servant),
            "-e", r"/PidName/i\    PidDir {}".format(self._binary_path),
            static_configuration,
        ]
        shell.run_into_file(patcher, report_configuration)
        return report_configuration

    def kill(self):
        logging.info("Killing process #{}".format(self._process.pid))
        self._process.kill()

    @property
    def properties(self):
        return {
            "user": self._user,
            "ticket": self._ticket,
            "servant": self._servant,
            "port": self._port,
            "status": self.Status.LABELS[self._status],
            "timestamp": self._timestamp.strftime("%Y-%m-%d %H:%M:%S"),
            "command": shell.render(self._command),
            "pid": self._process.pid,
            "logs": self._log_path,
        }

    def terminate(self):
        logging.info("Terminating process #{}".format(self._process.pid))
        self._process.terminate()

    def update(self):
        if self._process.poll() is not None:
            logging.info("Process #{} died".format(self._process.pid))
            self._process.communicate()
            self._status = self.Status.DEAD
        elif net.check_port(self._port):
            self._status = self.Status.INACTIVE
        else:
            self._status = self.Status.ACTIVE


def clean():
    environment["registry"].clean()


def copy_ticket_binary(ticket, target):
    key = environment["configuration"]["rta.ssh-key"]
    ticket_binary = get_ticket_binary(ticket)
    try:
        shell.scopy(key, ticket_binary + "/report_bin", target)
    except RuntimeError:
        shell.scopy(key, ticket_binary, target)


def get_ticket_binary(ticket):
    st = environment["st.client"]
    try:
        issue = st.issues[ticket]
    except Exception as error:
        raise RuntimeError("Failed to fetch ticket {0}: {1}".format(ticket, error))
    try:
        comments = [comment.text.encode("utf-8") for comment in issue.comments.get_all()]
    except Exception as error:
        raise RuntimeError("Failed to read comments to {0}: {1}".format(ticket, error.message))
    reference_format = environment["configuration"]["st.reference-format"]
    matches = [re.match(reference_format, comment, re.DOTALL) for comment in comments]
    last_match = next((match for match in reversed(matches) if match is not None), None)
    if last_match is None:
        raise RuntimeError("No comments to {0} match `{1}`".format(ticket, reference_format))
    return last_match.group(1)


def kill_process(pid):
    kill_processes([pid])


def kill_processes(pids):
    instances = []
    for pid in pids:
        instance = environment["registry"].find_by_pid(pid)
        if instance is None:
            raise RuntimeError("Unknown PID: {}".format(pid))
        instances.append(instance)
    for instance in instances:
        instance.kill()


def launch_ticket_binary(user, ticket):
    instance = environment["registry"].find_by_ticket(ticket)
    if instance is not None:
        raise RuntimeError("Already running")
    for servant in ["market-parallel-report"]:
        instance = ReportInstance(user, ticket, servant)
        environment["registry"].add(instance)


def setup_environment(configuration):
    environment.update({
        "configuration": configuration,
        "st.client": startrek.connect(*map(configuration.get, ["st.user-agent", "st.base-url", "st.token"])),
        "registry": Registry(configuration["rta.registry"]),
    })
    environment["registry"].load()


def terminate_process(pid):
    terminate_processes([pid])


def terminate_processes(pids):
    instances = []
    for pid in pids:
        instance = environment["registry"].find_by_pid(pid)
        if instance is None:
            raise RuntimeError("Unknown PID: {}".format(pid))
        instances.append(instance)
    for instance in instances:
        instance.terminate()
