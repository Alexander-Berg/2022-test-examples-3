#!/usr/bin/env python2.7

import logging
import os

from signal import SIGTERM
from subprocess import Popen

class InstanceCtl(object):
    def __init__(self):
        self._worker_pid = None

    def _spawn(self):
        if self._worker_pid:
            return

        if 'MASTER_SCRIPT' not in os.environ:
            raise InstanceInternalError("could not locate the MASTER_SCRIPT! please review qloud 'Variables' section")

        for dirname in ["/ephemeral/clustermaster-var/master", "/ephemeral/clustermaster-var/worker"]:
            if not os.path.isdir(dirname):
                os.makedirs(dirname)

        Popen(['/Berkanavt/communism/bin/solver', '-p', '-l', '/var/tmp/solver.log']).wait()
        Popen(['/Berkanavt/clustermaster/bin/master', '-l', '/var/tmp/master.log', '-v', '/Berkanavt/clustermaster/var/master',
            '-P', '/var/tmp/master.pid', '-s', os.environ['MASTER_SCRIPT'], '-h', '80']).wait()
        Popen(['/Berkanavt/clustermaster/bin/worker', '-l', '/var/tmp/worker.log', '-P', '/var/tmp/worker.pid', '-v', '/Berkanavt/clustermaster/var/worker']).wait()

        with open('/var/tmp/worker.pid') as fd:
            self._worker_pid = int(fd.readline().rstrip())

    def _kill(self):
        if not self._worker_pid:
            return

        try:
            os.kill(self._worker_pid, SIGTERM)
        except OSError:
            pass
        self._worker_pid = None

    def start(self):
        self._spawn()

    def stop(self):
        self._kill()
