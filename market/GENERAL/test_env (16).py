from .tracelog_parser import parse_tracelog


class TraceTestEnv(object):
    def __init__(self, trace_path):
        self.__trace_path = trace_path

    @property
    def offer_trace_log(self):
        return parse_tracelog(self.__trace_path)
