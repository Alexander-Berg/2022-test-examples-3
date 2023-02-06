#!/usr/bin/env python2.7

import threading
import time
import urllib2

class CmStatus(object):
    def __init__(self, host='localhost', port=80):
        self.__cm_url = 'http://%s:%d/targets_text' % (host, port)
        self.__thread = None
        self.__lock = threading.Lock()
        self.__is_stop = threading.Event()
        self.__status = ('NODATA', 202)
        self.__last_finished = None

    def update_status(self):
        try:
            targets = urllib2.urlopen(self.__cm_url).readlines()
        except:
            with self.__lock:
                self.__status = ('CRIT', 500)
                return
        failed_targets = [ ]
        last_finished = None
        for t in targets:
            fields = t.split(" ")
            if len(fields) > 2:
                if 'failed' in fields[2].split(","):
                    failed_targets.append(fields[1])
                if 'qloud-finish' == fields[1]:
                    last_finished = int(fields[4])
        with self.__lock:
            self.__status = ('OK', 200) if 0 == len(failed_targets) else (' '.join(failed_targets), 502)
            self.__last_finished = last_finished

    def update_status_handler(self):
        while not self.__is_stop.is_set():
            self.update_status()
            time.sleep(10)

    def status(self):
        with self.__lock:
            return self.__status, self.__last_finished

    def start(self):
        self.__thread = threading.Thread(target=self.update_status_handler)
        self.__thread.start()

    def stop(self):
        self.__is_stop.set()
        if self.__thread:
            self.__thread.join()
        self.__is_stop.clear()
