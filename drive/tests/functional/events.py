import json
import sys


class TestEvent:
    def __init__(self, source, message, type="notification"):
        self.source = source
        self.message = message
        self.type = type

    def to_dict(self):
        result = {
            "type": self.type,
            "source": self.source,
            "message": self.message,
        }
        return result

    def to_json(self):
        obj = self.to_dict()
        return json.dumps(obj)

    def report_json(self):
        sys.stdout.write(self.to_json())
        sys.stdout.write('\n')
