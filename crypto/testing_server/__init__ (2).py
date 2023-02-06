import logging
import multiprocessing
import os
import shutil
import socket
import stat
import tempfile
import time

from Crypto.PublicKey import RSA
import paramiko
import six

from yatest.common import network


logger = logging.getLogger(__name__)
logging.getLogger("paramiko").setLevel(logging.WARNING)


class StubServer(paramiko.ServerInterface):
    def __init__(self, pub_key, auths):
        self.auths = auths
        self.pub_key = pub_key

    def check_auth_password(self, username, password):
        return paramiko.AUTH_SUCCESSFUL if (username, password) in self.auths else paramiko.AUTH_FAILED

    def check_auth_publickey(self, username, key):
        return paramiko.AUTH_SUCCESSFUL if key == self.pub_key else paramiko.AUTH_FAILED

    def check_channel_request(self, kind, chanid):
        return paramiko.OPEN_SUCCEEDED

    def get_allowed_auths(self, username):
        return "password,publickey"


class StubSFTPHandle(paramiko.SFTPHandle):
    def stat(self):
        try:
            return paramiko.SFTPAttributes.from_stat(os.fstat(self.readfile.fileno()))
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)

    def chattr(self, attr):
        # python doesn't have equivalents to fchown or fchmod, so we have to
        # use the stored filename
        try:
            paramiko.SFTPServer.set_file_attr(self.filename, attr)
            return paramiko.SFTP_OK
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)


class StubSFTPServer(paramiko.SFTPServerInterface):
    def __init__(self, *args, **kwargs):
        self.root_dir = kwargs.pop('root_dir', os.getcwd())

    def _realpath(self, path):
        return self.root_dir + self.canonicalize(path)

    def list_folder(self, path):
        path = self._realpath(path)
        try:
            out = []
            flist = os.listdir(path)
            for fname in flist:
                attr = paramiko.SFTPAttributes.from_stat(os.stat(os.path.join(path, fname)))
                attr.filename = fname
                out.append(attr)
            return out
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)

    def stat(self, path):
        path = self._realpath(path)
        try:
            return paramiko.SFTPAttributes.from_stat(os.stat(path))
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)

    def lstat(self, path):
        path = self._realpath(path)
        try:
            return paramiko.SFTPAttributes.from_stat(os.lstat(path))
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)

    def open(self, path, flags, attr):
        path = self._realpath(path)
        try:
            binary_flag = getattr(os, 'O_BINARY',  0)
            flags |= binary_flag
            mode = getattr(attr, 'st_mode', None)
            if mode is not None:
                fd = os.open(path, flags, mode)
            else:
                # os.open() defaults to 0777 which is
                # an odd default mode for files
                fd = os.open(path, flags, 0o666)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        if (flags & os.O_CREAT) and (attr is not None):
            attr._flags &= ~attr.FLAG_PERMISSIONS
            paramiko.SFTPServer.set_file_attr(path, attr)
        if flags & os.O_WRONLY:
            if flags & os.O_APPEND:
                fstr = 'ab'
            else:
                fstr = 'wb'
        elif flags & os.O_RDWR:
            if flags & os.O_APPEND:
                fstr = 'a+b'
            else:
                fstr = 'r+b'
        else:
            # O_RDONLY (== 0)
            fstr = 'rb'
        try:
            f = os.fdopen(fd, fstr)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        fobj = StubSFTPHandle(flags)
        fobj.filename = path
        fobj.readfile = f
        fobj.writefile = f
        return fobj

    def remove(self, path):
        path = self._realpath(path)
        try:
            os.remove(path)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        return paramiko.SFTP_OK

    def rename(self, oldpath, newpath):
        oldpath = self._realpath(oldpath)
        newpath = self._realpath(newpath)
        try:
            os.rename(oldpath, newpath)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        return paramiko.SFTP_OK

    def mkdir(self, path, attr):
        path = self._realpath(path)
        try:
            os.mkdir(path)
            if attr is not None:
                paramiko.SFTPServer.set_file_attr(path, attr)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        return paramiko.SFTP_OK

    def rmdir(self, path):
        path = self._realpath(path)
        try:
            os.rmdir(path)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        return paramiko.SFTP_OK

    def chattr(self, path, attr):
        path = self._realpath(path)
        try:
            paramiko.SFTPServer.set_file_attr(path, attr)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        return paramiko.SFTP_OK

    def symlink(self, target_path, path):
        path = self._realpath(path)
        if target_path.startswith("/"):
            # absolute symlink
            target_path = os.path.join(self.root_dir, target_path[1:])
            if target_path.startswith("//"):
                # bug in os.path.join
                target_path = target_path[1:]
        else:
            # compute relative to path
            abspath = os.path.join(os.path.dirname(path), target_path)
            if not abspath.startswith(self.root_dir):
                # this symlink isn't going to work anyway -- just break it immediately
                target_path = '<error>'
        try:
            os.symlink(target_path, path)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        return paramiko.SFTP_OK

    def readlink(self, path):
        path = self._realpath(path)
        try:
            symlink = os.readlink(path)
        except OSError as e:
            return paramiko.SFTPServer.convert_errno(e.errno)
        # if it's absolute, remove the root
        if os.path.isabs(symlink):
            if symlink.startswith(self.root_dir):
                symlink = symlink[len(self.root_dir):]
                if not symlink.startswith("/"):
                    symlink = '/' + symlink
            else:
                symlink = '<error>'
        return symlink


class SftpTestingServer(object):
    def __init__(self, auths=None):
        self.auths = auths or []
        self.host = 'localhost'
        self.host_key = None
        self.is_running = multiprocessing.Value('b', False)
        self.key_file = None
        self.pm = None
        self.port = None
        self.root_dir = None
        self.sftp_server_process = None
        self.stop_timeout = 5
        self.tmp_dir = None

    def _run_sftp_server(self):
        server_socket = socket.socket(socket.AF_INET6, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, True)
        server_socket.bind((self.host, self.port))
        server_socket.listen(self.port)

        while self.is_running.value:
            conn, addr = server_socket.accept()
            transport = paramiko.Transport(conn)
            transport.add_server_key(self.host_key)
            transport.set_subsystem_handler('sftp', paramiko.SFTPServer, StubSFTPServer, root_dir=self.root_dir)
            transport.start_server(server=StubServer(self.host_key, self.auths))

            channel = transport.accept()
            while transport.is_active():
                if not self.is_running.value:
                    channel.close()
                    transport.close()
                    server_socket.close()
                    return
                time.sleep(0.2)

    def gen_key_file(self, file_dir):
        key_file = os.path.join(file_dir, "test_rsa")
        with open(key_file, 'w') as content_file:
            os.chmod(key_file, stat.S_IRUSR | stat.S_IWUSR)
            content_file.write(six.ensure_str(RSA.generate(2048).exportKey('PEM')))
        return key_file

    def start(self):
        self.pm = network.PortManager()
        self.port = self.pm.get_port()
        self.tmp_dir = tempfile.mkdtemp()
        self.root_dir = os.path.join(self.tmp_dir, 'sftp_root_dir')
        os.mkdir(self.root_dir)
        self.key_file = self.gen_key_file(self.tmp_dir)
        self.host_key = paramiko.RSAKey.from_private_key_file(self.key_file)
        self.is_running.value = True
        self.sftp_server_process = multiprocessing.Process(target=self._run_sftp_server)
        self.sftp_server_process.start()
        logger.info("Start SFTP server at %s port", self.port)
        time.sleep(1)

    def stop(self):
        self.is_running.value = False
        self.sftp_server_process.join(self.stop_timeout)
        if self.sftp_server_process.is_alive():
            self.sftp_server_process.terminate()
            self.sftp_server_process.join()
            logger.info("Brute terminate SFTP server")
        else:
            logger.info("Stop SFTP server")
        self.pm.release()
        shutil.rmtree(self.tmp_dir)

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()
