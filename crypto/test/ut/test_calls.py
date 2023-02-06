from cStringIO import StringIO
import socket
import sys

from push_client_check import call_juggler_queue_event


class Capturing(list):
    def __enter__(self):
        self._stdout = sys.stdout
        sys.stdout = self._stringio = StringIO()
        return self

    def __exit__(self, *args):
        self.extend(self._stringio.getvalue().splitlines())
        del self._stringio    # free up some memory
        sys.stdout = self._stdout


def test_juggler_queue_event_dry_run():
    with Capturing() as output:
        call_juggler_queue_event('/path/to/conf.yaml', 0, 'description', True)

    test = 'sudo juggler_queue_event --host={} -n push_conf -s 0 -d description'.format(socket.getfqdn())
    assert output[0] == test
