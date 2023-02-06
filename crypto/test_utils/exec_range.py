import time


class ExecRange(object):
    def __init__(self):
        self.start_ts = self.get_current_ts()
        self.finish_ts = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, tb):
        self.finish_ts = self.get_current_ts()
        return False

    def __contains__(self, item):
        return item >= self.start_ts and item <= self.finish_ts

    @staticmethod
    def get_current_ts():
        return int(time.time())
