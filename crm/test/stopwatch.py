import datetime


class Stopwatch:
    def __init__(self):
        self.start_dt = datetime.datetime.now()

    def start(self):
        self.start_dt = datetime.datetime.now()

    def elapsed(self):
        return int((datetime.datetime.now() - self.start_dt).microseconds / 1000)
