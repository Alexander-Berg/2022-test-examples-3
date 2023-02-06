from taas_lib.interfaces import IQueue

import Queue


class MockQueue(IQueue):
    def __init__(self):
        self.queue = Queue.Queue()

    def put(self, item):
        self.queue.put(item)

    def get(self):
        return self.queue.get()

    def size(self):
        return self.queue.qsize()


assert issubclass(MockQueue, IQueue)
assert isinstance(MockQueue(), IQueue)
