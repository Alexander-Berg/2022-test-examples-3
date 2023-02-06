# coding=utf-8
import sys
import os
from Queue import Queue

import socket
import logging
import time

from contextlib import contextmanager
from collections import namedtuple

import sys
sys.path.append(os.getcwd())
from tasks import mpfs_fake_task


Sample = namedtuple(
        'Sample', 'marker,threads,overallRT,httpCode,netCode,sent,received,connect,send,latency,receive,accuracy')


@contextmanager
def measure(marker, queue):
    start_ms = time.time()

    resp_code = 200
    try:
        yield
    except Exception as e:
        print marker, e
        resp_code = 500

    response_time = int((time.time() - start_ms) * 1000)

    data_item = Sample(
            marker,         # tag
            1,              # threads
            0,              # overallRT
            resp_code,      # httpCode
            0,              # netCode
            0,              # sent
            0,              # received
            0,              # connect
            0,              # send
            response_time,  # latency
            0,              # receive
            0,              # accuracy
    )
    queue.put((int(time.time()), data_item), timeout=5)
    if resp_code != 0:
        raise RuntimeError


def shoot(missile, marker, results):
    try:
        with measure("markerOfRequest", results):
            mpfs_fake_task.apply_async((5,))
    except RuntimeError as e:
        print "Scenario %s failed with %s" % (marker, e)
    finally:
        pass
