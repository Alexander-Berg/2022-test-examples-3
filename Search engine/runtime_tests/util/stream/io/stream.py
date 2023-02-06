# -*- coding: utf-8 -*-
from abc import ABCMeta, abstractmethod
import os
import struct
import socket
import select
import errno
import time
import StringIO
from runtime_tests.util.resource import AbstractResource


class StreamException(AssertionError):
    def __init__(self, data, *args, **kwargs):
        super(StreamException, self).__init__(*args, **kwargs)
        self.data = data


class StreamRecvException(StreamException):
    pass


class EndOfStream(StreamRecvException):
    pass


class StreamTimeout(StreamRecvException):
    pass


class StreamRst(StreamRecvException):
    pass


class NonEmptyStream(StreamException):
    pass


class Stream(AbstractResource):
    __metaclass__ = ABCMeta

    def __init__(self):
        super(Stream, self).__init__()
        self.__data = list()

    close = AbstractResource.finish

    @abstractmethod
    def _finish(self):
        raise NotImplementedError()

    def clean(self):
        self.__data = list()

    @property
    def data(self):
        if len(self.__data) != 1:
            self.__data = [''.join(self.__data)]
        return self.__data[0]

    def recv(self, size=-1):
        try:
            result = self._recv(size)
            self.__data.append(result)
            # noinspection PyChainedComparisons
            if size > 0 and len(result) != size:
                raise EndOfStream(''.join(result))
            else:
                return result
        except StreamRecvException, exc:
            self.__data.append(exc.data)
            raise

    @abstractmethod
    def _recv(self, size=-1):
        raise NotImplementedError()

    def recv_quiet(self, size=-1):
        try:
            return self.recv(size)
        except StreamRecvException, exc:
            return exc.data

    @abstractmethod
    def has_data(self):
        raise NotImplementedError()


class FileStream(Stream):

    def __init__(self, file_obj):
        super(FileStream, self).__init__()
        self.__file = file_obj

    def _recv(self, size=-1):
        return self.__file.read(size)

    def has_data(self):
        return self.__file.tell() < os.fstat(self.__file.fileno()).st_size

    def _finish(self):
        self.__file.close()


class StringStream(Stream):

    def __init__(self, s):
        super(StringStream, self).__init__()
        self.__string = StringIO.StringIO(s)

    def _recv(self, size=-1):
        return self.__string.read(size)

    def has_data(self):
        return self.__string.tell() < self.__string.len

    def send(self, data):
        self.__string.write(data)

    def _finish(self):
        pass

    def __str__(self):
        return self.__string.getvalue()


class _SocketStream(Stream):
    CHECK_TIMEOUT = 0.5
    BUF_SIZE = 8192
    MILLISECONDS = 1000
    TIMEOUT = 20

    def __init__(self, sock):
        super(_SocketStream, self).__init__()
        self._socket = sock
        self.__poll = select.poll()
        self.__timeout = self._socket.gettimeout()
        self.__poll.register(self._socket.fileno(), select.POLLIN | select.POLLHUP)

    def fileno(self):
        return self._socket.fileno()

    def set_timeout(self, timeout):
        self._socket.settimeout(timeout)

    def restore_timeout(self):
        self._socket.settimeout(self.__timeout)

    def shutdown(self, how):
        self._socket.shutdown(how)

    def _finish(self):
        self._socket.close()

    def is_closed(self, timeout=None):
        if timeout is None:
            timeout = self.CHECK_TIMEOUT
        fds = self.__poll.poll(timeout * self.MILLISECONDS)
        if fds:
            try:
                data = self._socket.recv(self.BUF_SIZE, socket.MSG_PEEK)
                if data:
                    raise NonEmptyStream(data, 'Socket not empty')
                else:
                    return True
            except socket.error, err:
                if err.errno == errno.ECONNRESET:
                    return True
                else:
                    raise
        else:
            return False

    def has_data(self):
        fds = self.__poll.poll(self.CHECK_TIMEOUT * self.MILLISECONDS)
        if fds:
            data = self._socket.recv(self.BUF_SIZE, socket.MSG_PEEK)
            return data != ''
        else:
            return False

    def poll(self, timeout=None):
        fds = self.__poll.poll(timeout * self.MILLISECONDS)
        return fds != []

    def send(self, data):
        self._socket.sendall(data)

    def _recv(self, size=-1):
        result = list()
        try:
            if size < 0:
                while True:
                    data = self._socket.recv(self.BUF_SIZE)
                    if not data:
                        break
                    result.append(data)
            else:
                data = '1'
                while size > 0 and data:
                    data = self._socket.recv(min(size, self.BUF_SIZE))
                    size -= len(data)
                    result.append(data)
        except socket.timeout, exc:
            raise StreamTimeout(''.join(result), exc.message)
        except socket.error, err:
            if err.errno == errno.ECONNRESET:
                raise StreamRst(''.join(result), err.message)
            else:
                raise
        return ''.join(result)


class SocketStream(_SocketStream):

    @classmethod
    def from_address(cls, host, port, timeout=None):
        if timeout is None:
            timeout = cls.TIMEOUT
        sock = socket.create_connection((host, port), timeout=timeout)
        return SocketStream(sock)

    def set_recv_buffer_size(self, buffer_size):
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, buffer_size)

    def set_send_buffer_size(self, buffer_size):
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_SNDBUF, buffer_size)

    @property
    def sock_ip(self):
        return self._socket.getsockname()[0]

    @property
    def sock_port(self):
        return self._socket.getsockname()[1]

    def send_rst(self):
        self._socket.setsockopt(socket.SOL_SOCKET, socket.SO_LINGER, struct.pack('ii', 1, 0))
        self.close()
        time.sleep(0.5)
