# -*- coding: utf-8 -*-
import ctypes

from Queue import Empty, Full
from collections import deque
import multiprocessing as m


class _InnerQueue(deque):
    def qsize(self):
        return len(self)

    def empty(self):
        return len(self) == 0

    def full(self):
        return len(self) == self.maxlen

    def put(self, obj, *_):
        if self.full():
            raise Full()
        self.append(obj)

    def get(self, *_):
        if self.empty():
            raise Empty()
        return self.popleft()


class Queue(object):
    def __init__(self, timeout, maxsize=0):
        super(Queue, self).__init__()
        self.__timeout = timeout
        self.__sync_queue = m.Queue(maxsize=maxsize)
        self.__is_closed = False
        if maxsize <= 0:
            maxlen = None
        else:
            maxlen = maxsize
        self.__buffer_queue = _InnerQueue(maxlen=maxlen)

    def qsize(self):
        return self.__queue.qsize()

    def empty(self):
        return self.__queue.empty()

    def full(self):
        return self.__queue.full()

    def put(self, obj, block=True, timeout=None):
        if timeout is None:
            timeout = self.__timeout
        return self.__queue.put(obj, block, timeout)

    def get(self, block=True, timeout=None):
        if timeout is None:
            timeout = self.__timeout
        return self.__queue.get(block, timeout)

    def close(self):
        if not self.__is_closed:
            self.__sync_queue.close()
            self.__is_closed = True

    def finish(self):
        if not self.__is_closed:
            while not self.__sync_queue.empty():
                obj = self.__sync_queue.get(block=True, timeout=self.__timeout)
                self.__buffer_queue.append(obj)
            self.close()

    @property
    def __queue(self):
        if not self.__is_closed:
            return self.__sync_queue
        else:
            return self.__buffer_queue


class Counter(object):
    def __init__(self, default=0):
        super(Counter, self).__init__()
        self.__default = default
        self.__counter = m.Value(ctypes.c_int, default)

    def inc(self):
        with self.__counter.get_lock():
            result = self.__counter.value
            self.__counter.value += 1
            return result

    @property
    def value(self):
        return self.__counter.value

    def reset(self):
        self.__counter.value = self.__default
