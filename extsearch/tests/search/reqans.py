from collections import namedtuple

ReqansRecord = namedtuple('ReqansRecord', ['request', 'results'])


def _split_reqans_line(s):
    result = {}
    for kv in s.rstrip('\n').split('@@'):
        k, v = kv.split('=', 1)
        result[k] = v
    return result


def parse_reqans(fd):
    while True:
        line = fd.readline()
        if not line:
            break
        assert line == 'REQANS-START\n'
        request = _split_reqans_line(fd.readline())
        results = []
        while True:
            line = fd.readline()
            if line == 'REQANS-END\n':
                break
            results.append(_split_reqans_line(line))
        yield ReqansRecord(request, results)
