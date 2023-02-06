import multiprocessing
import os
import shutil
import signal
import six
import sys
import tempfile
import time

from yatest.common import network


class FtpTestingServer(object):
    def __init__(self, auths):
        self.pm = network.PortManager()
        self.port = None
        self.root_dir = None
        self.auths = auths
        self.ftp_server_process = None

    def _run_ftp_server(self):
        from twisted.internet import reactor
        from twisted.protocols import ftp as ftp_protocol
        from twisted.cred import (
            checkers,
            portal as twisted_portal
        )

        checker = checkers.InMemoryUsernamePasswordDatabaseDontUse()
        for user, password in self.auths:
            checker.addUser(user, password)
            user_home_dir = os.path.join(self.root_dir, user)
            if not os.path.exists(user_home_dir):
                os.makedirs(user_home_dir)

        realm = ftp_protocol.FTPRealm(self.root_dir, self.root_dir)
        portal = twisted_portal.Portal(realm, [checker])
        ftp = ftp_protocol.FTPFactory(portal)
        reactor.listenTCP(self.port, ftp)

        reactor.run(True)

    def start(self):
        self.root_dir = tempfile.mkdtemp()
        self.port = self.pm.get_port()
        self.ftp_server_process = multiprocessing.Process(target=self._run_ftp_server)
        self.ftp_server_process.start()
        time.sleep(1)

    def stop(self):
        self.ftp_server_process.terminate()
        self.ftp_server_process.join(timeout=5)

        if self.ftp_server_process.is_alive():
            six.print_("FTP process did not terminate, sending SIGKILL to {}".format(self.ftp_server_process.pid), file=sys.stderr)
            os.kill(self.ftp_server_process.pid, signal.SIGKILL)

        self.pm.release()
        shutil.rmtree(self.root_dir)

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()
