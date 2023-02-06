import itertools
import json
from random import Random


class RtDuidMessageGenerator(object):
    counter = itertools.count(1)
    sources = ["port", "serp", "unknown", "tls", "ertelecom"]

    def __init__(self, producer):
        self.producer = producer
        self.id_generator = Random(42)
        self.next_source = itertools.cycle(self.sources)

    def next_id(self):
        return self.id_generator.randint(1, 1000)

    def write(self, n):
        msgs = [
            {
                "yuid": "{:02}".format(self.next_id()),
                "domain_cookie": "{:02}".format(self.next_id()),
                "timestamp": 1500000000,
                "source": self.next_source.next(),
            }
            for _ in range(n)
        ]
        result = self.producer.write(self.counter.next(), "\n".join(json.dumps(msg) for msg in msgs)).result(timeout=10)
        assert result.HasField("ack")
        return msgs
