from collections import defaultdict


class Hist(object):
    def __init__(self, bounds):
        self.bounds = bounds
        self.bounds.sort()
        self.values = [0] * len(bounds)
        self.inf = 0

    def update(self, value):
        for i, bound in enumerate(self.bounds):
            if value < bound:
                self.values[i] += 1
                return
        self.inf += 1

    def get(self):
        return {'bounds': self.bounds, 'buckets': self.values, 'inf': self.inf}


class Metrics(object):
    def __init__(self):
        self.started = defaultdict(int)
        self.requests = defaultdict(int)
        self.errors = defaultdict(int)
        self.counters = defaultdict(int)
        self.bytes_sent = 0
        self.view_time = Hist([5 * 60, 15 * 60, 30 * 60, 45 * 60, 60 * 60])

    def on_streaming(self, channel):
        self.started[channel] += 1

    def on_http_request(self, resource):
        self.requests[resource] += 1

    def on_payload(self, size):
        self.bytes_sent += size

    def on_error(self, error):
        self.errors[error] += 1

    def on_view_time(self, vt):
        self.view_time.update(vt)

    def count(self, name):
        self.counters[name] += 1

    def get_metrics(self):
        metrics = []
        for key, value in self.started.items():
            metrics.append({'labels': {'channel': key}, 'type': 'COUNTER', 'value': value})
        for key, value in self.requests.items():
            metrics.append({'labels': {'path': key}, 'type': 'COUNTER', 'value': value})
        for key, value in self.errors.items():
            metrics.append({'labels': {'error': key}, 'type': 'COUNTER', 'value': value})
        for key, value in self.counters.items():
            metrics.append({'labels': {'counter': key}, 'type': 'COUNTER', 'value': value})
        metrics.append({'labels': {'counter': 'bytes_sent'}, 'type': 'COUNTER', 'value': self.bytes_sent})
        metrics.append({'labels': {'histogram': 'view_time'}, 'type': 'HIST', 'hist': self.view_time.get()})
        return metrics
