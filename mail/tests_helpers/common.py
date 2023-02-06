import logging
import os
import subprocess

import retrying
import yatest.common

log = logging.getLogger(__name__)
work_dir = yatest.common.work_path()


@retrying.retry(
    retry_on_result=bool,
    wait_exponential_multiplier=1000, wait_exponential_max=10000,
)
def wait_docker_container(container_name):
    cmd = ['docker', 'inspect', '-f', '\'{{.State.Running}}\'', container_name]
    res = subprocess.call(cmd)
    return res != 0


def destroy_docker_container(container_name):
    try:
        cmd = ['docker', 'stop', container_name]
        yatest.common.execute(cmd, shell=False, wait=True, cwd=work_dir)
    except Exception as e:
        log.error('Docker stop error: %s', e)
    try:
        cmd = ['docker', 'rm', container_name]
        yatest.common.execute(cmd, shell=False, wait=True, cwd=work_dir)
    except Exception as e:
        log.error('Docker rm error: %s', e)


def start_daemon(arc_bin_path, arc_conf_path):
    template_identifier_bin = yatest.common.binary_path(arc_bin_path)
    config = yatest.common.source_path(arc_conf_path)
    cmd = [template_identifier_bin, '--config', config]
    return yatest.common.execute(
        cmd, shell=False, check_sanitizer=True, wait=False, cwd=work_dir
    )


def shutdown_daemon(proc_name):
    return os.system('pkill -9 {proc_name}'.format(**locals()))
