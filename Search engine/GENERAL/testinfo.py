SUCCESS = "SUCCESS"
WARNING = "WARNING"
FAILED = "FAILED"
ERROR = "ERROR"


class TestInfo(object):
    def __init__(self, name, message, output, status):
        self._name = name
        self._message = message
        self._output = output
        self._status = status

    @property
    def name(self):
        return self._name

    @property
    def message(self):
        return self._message

    @property
    def output(self):
        return self._output

    @property
    def status(self):
        return self._status

    def to_json(self):
        return {
            "name": self._name,
            "message": self._message,
            "output": self._output,
            "status": self._status
        }

    @staticmethod
    def from_json(json_):
        return TestInfo(json_["name"], json_["message"], json_["output"], json_["status"])
