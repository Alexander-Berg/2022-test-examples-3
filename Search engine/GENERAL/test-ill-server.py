#!/skynet/python/bin/python

import sys
import re
import os
import select
import errno
import tempfile
import stat
import subprocess
import httplib
import threading
import fcntl
import signal
from ctypes import cdll

from api.copier import Copier
import api.kqueue
from library.sky.hosts import resolveHosts
from library.format import formatHosts
from kernel.util.errors import formatException
from library.sky.hostresolver.resolver import Resolver


def setPdeathSig():
    if os.uname()[0] != "Linux":
        return
    PR_SET_PDEATHSIG = 1
    libc = cdll.LoadLibrary('libc.so.6')
    libc.prctl(PR_SET_PDEATHSIG, signal.SIGKILL, 0, 0, 0)


class BasesearchTester(object):
    def __init__(self):
        self.freetorrents = [
            "rbtorrent:21776aa067cce792d0493fad5677b3f5cca4985d", # binary
            "rbtorrent:18b9ad40f1dd97d217f7e9bf2fe44cd03125cede", # config
            "rbtorrent:3588bbd3caa157d7420091c440ea7473d8a9429a", # plan
            "rbtorrent:54c82b2b41bf9b88e89ff8d393517a8730b9af14", # d-executor
            "rbtorrent:a8bf8c3ed0fa9cabf232daf03e8e15efb7ef055f", # d-dumper
            "rbtorrent:7f78c1e90a76b3a1d7ccf5871a22fdfe70b73f14", # shard
            "rbtorrent:2b868cae8c56eddeb701884f2ec01fae08015a4b", # models
            ]
        self.linuxtorrents = [
            "rbtorrent:c3822dfbbada051467e338a64fb408d36a6b3f67", # binary
            "rbtorrent:18b9ad40f1dd97d217f7e9bf2fe44cd03125cede", # config
            "rbtorrent:3588bbd3caa157d7420091c440ea7473d8a9429a", # plan
            "rbtorrent:0e3858d21f4787bbeb4681acadd9e95ffcb21972", # d-executor
            "rbtorrent:bd893f458282c244d64980a8745a8c35d5d68962", # d-dumper
            "rbtorrent:7f78c1e90a76b3a1d7ccf5871a22fdfe70b73f14", # shard
            "rbtorrent:2b868cae8c56eddeb701884f2ec01fae08015a4b", # models
            ]
        self.osUser = "agri"

    def getTorrentList(self):
        if os.uname()[0] == "FreeBSD":
            return self.freetorrents
        elif os.uname()[0] == "Linux":
            return self.linuxtorrents
        else:
            raise Exception("OS not supported")

    def getBasesearchName(self):
        if os.uname()[0] == "FreeBSD":
            return "./freebsd.web_basesearch"
        elif os.uname()[0] == "Linux":
            return "./linux.web_basesearch"
        else:
            raise Exception("OS not supported")

    def loadResources(self, tmpdir):
        for torrent in self.getTorrentList():
            Copier().handle(torrent).get(dest=tmpdir, user=True).wait()

    def __del__(self):
        if hasattr(self, "bhandle"):
            self.bhandle.kill()

    def __call__(self):
        tmpdir = tempfile.mkdtemp(dir="/db/vartmp/")
        print "Running in the temporary directory: " + tmpdir
        sys.stdout.flush()
        os.chmod(tmpdir, stat.S_IXOTH | stat.S_IXGRP | stat.S_IRWXU)

        self.loadResources(tmpdir)

        os.chdir(tmpdir)
        devnull = open("/dev/null", "w+")
        self.bhandle = subprocess.Popen(
            [self.getBasesearchName(), "-p", "8010", "-d", "basesearch.cfg",
             "-V", "LoadLog=/dev/null", "-V", "PassageLog=/dev/null",
             "-V", "IndexDir=basesearch_database",
             "-V", "MXNetFile=models.archive"],
            stdout=devnull, stdin=devnull, stderr=devnull, close_fds=True,
            preexec_fn=setPdeathSig)

        while True:
            try:
                conn = httplib.HTTPConnection("localhost", "8010")
                conn.request("GET", "/")
                print "basesearch has been launched"
                sys.stdout.flush()
                break
            except:
                if self.bhandle.poll() is not None:
                    print self.bhandle.poll()
                    sys.stdout.flush()
                    return

        for i in xrange(4):
            exechandle = subprocess.Popen(
                ["./d-executor", "-p", "RESOURCE", "-H", "localhost",
                 "-P", "8010", "-Q", "100000", "-m", "finger", "-s", "8",
                 "-o", "shotres-%d" % i],
                stdout=devnull, stdin=devnull, stderr=devnull, close_fds=True,
                preexec_fn=setPdeathSig)
            exechandle.wait()

        dumphandle = subprocess.Popen(
            ["./d-dumper", "-c", "-f", "shotres-3"],
            stdout=subprocess.PIPE, stdin=devnull, stderr=devnull,
            close_fds=True, preexec_fn=setPdeathSig)
        for line in dumphandle.stdout:
            if line.startswith("requests/sec"):
                print float(line.split()[1])
                sys.stdout.flush()
                break


def launcher(targetHost):
    sp = subprocess.Popen(
        ["ssh", targetHost, "/skynet/python/bin/python"],
        stdout=subprocess.PIPE,
        stdin=open(__file__, "r"),
        close_fds=True,
        preexec_fn=setPdeathSig)
    for line in sp.stdout:
        fcntl.flock(sys.stdout, fcntl.LOCK_EX)
        try:
            print targetHost, line[:-1]
            sys.stdout.flush()
        finally:
            fcntl.flock(sys.stdout, fcntl.LOCK_UN)


if __name__ == "__main__":
    if len(sys.argv) == 1 and sys.argv[0] == '':
        setPdeathSig()
        BasesearchTester()()
        exit(0)

    hosts_to_test = sys.argv[1:]
    for host in hosts_to_test:
        threading.Thread(target=launcher, args=(host, )).start()
