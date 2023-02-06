from threading import Thread
from os import _exit
from time import sleep
from utils import load_url

CHECK_LOCK_URL = "http://localhost:8081/check_lock?resource=xeno_tests"


class LockListener(Thread):
    running = True

    def run(self):
        while self.running:
            try:
                resp = load_url(CHECK_LOCK_URL).read()
            except Exception as e:
                print("check lock error: " + str(e))
                _exit(1)
            sleep(1)

    def stop(self):
        self.running = False
