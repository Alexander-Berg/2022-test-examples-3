#! /usr/bin/env python2.7
# -*- coding: utf8 -*-
import threading
import time
from datetime import datetime

import requests

TEST_URL = "http://localhost:25699/api/legacy/json/public_info?uid=0&increment_views=1&meta=drweb%2Cmediatype%2Cmimetype%2Cshort_url%2Csize%2Csizes%2Cfile_id%2Ccomment_ids%2Cviews_counter%2Cdownload_counter%2Cblockings%2Cpage_blocked_items_num&private_hash=Mqq0Pdfwhw%2B2BJdgvEH5qqPMFxJnCa2p%2BrtyjsC5zAa%2BsASUyhJNO9ay4GjmmI9hq%2FJ6bpmRyOJonT3VoXnDag%3D%3D"
HEADERS = {'Content-Type': 'application/json'}
lock = threading.Lock()
THREADS = 40


class RequestThread(threading.Thread):

    def __init__(self):
        threading.Thread.__init__(self)
        self.setDaemon(True)

    def run(self):
        while True:
            self.do_request()

    def do_request(self):
        try:
            response = requests.get(TEST_URL, headers=HEADERS)
            # print response.content
            if not response.ok:
                now = datetime.now()
                with open('results.log', 'a') as f:
                    f.write('%s : response code: %s, headers: %s\n response: %s \n\n' % (
                        now, response.status_code, response.headers, response.content))
                return None
            return response
        except Exception as e:
            now = datetime.now()
            with lock:
                with open('results.log', 'a') as f:
                    f.write('%s : Exception: %s\n\n' % (now, e.message))


def main():
    threads = []
    for t in range(THREADS):
        threads.append(RequestThread())
        threads[t].start()
        time.sleep(0.1)

    while True:
        time.sleep(1)


if __name__ == "__main__":
    main()
