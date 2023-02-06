import requests
import time
from subprocess import PIPE, Popen


class BaseDaemon(object):
    def __init__(self):
        self.popen = None

    def start(self):
        cmd = self.get_cmd()
        print cmd
        self.popen = Popen(cmd, stderr=PIPE, stdout=PIPE)
        while True:
            if self.popen.poll() is not None:
                self.show_output()
                raise BaseException('{} crashed on start with code {}'.format(self.name, self.popen.poll()))
            if self.check():
                break
            print 'wait {}'.format(self.name)
            time.sleep(1)
        print '{} started!'.format(self.name)

    def shutdown(self):
        resp = requests.get('http://localhost:{}/admin?action=shutdown'.format(self.port), timeout=20)
        if not resp.ok:
            raise BaseException('Failed shutdown of {}'.self.name)
        self.show_output()

    def check(self):
        try:
            resp = requests.get('http://localhost:{}/admin?action=ping'.format(self.port), timeout=20)
        except requests.ConnectionError:
            return False
        return resp.ok

    def __enter__(self):
        self.clear_logs()
        self.start()

    def __exit__(self, type, value, traceback):
        self.shutdown()

    def show_output(self):
        self.out, self.err = self.popen.communicate()
        print '{} stdout:\n{}'.format(self.name, self.out)
        print '{} stderr:\n{}'.format(self.name, self.err)
