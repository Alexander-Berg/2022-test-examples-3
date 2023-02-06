import asyncore
import threading


class LocalSmtpServer(object):
    def __init__(self, server):
        self._smtp = server
        self._thread = threading.Thread(target=asyncore.loop, kwargs={'timeout': 1})
        self.local_address = server.addr

    def start(self):
        self._thread.start()

    def stop(self):
        self._smtp.close()
        self._thread.join()

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, *args):
        self.stop()
