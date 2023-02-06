from hamcrest import assert_that
import logging
from market.idx.pylibrary.mindexer_core.banner.banner import ParallelYqlExecutor, YqlProcessor
from conftest import GENERATION
from datetime import datetime, timedelta


class RequestStub(object):
    def __init__(self, status, is_success):
        self._status = status
        self._is_success = is_success

    @property
    def status(self):
        return self._status

    @property
    def is_success(self):
        return self._is_success


class SuccessRequest(RequestStub):
    def __init__(self):
        RequestStub.__init__(self, 'COMPLETED', True)


class FailedRequest(RequestStub):
    def __init__(self):
        RequestStub.__init__(self, 'ERROR', False)

    @property
    def errors(self):
        return ['first error', 'second error']


class FakeYqlExecutor(object):
    def __init__(self, responses):
        self.requests = []
        self.requests_datetime = []
        self._responses = responses  # query -> [RequestStub]

    def yql_start_request(self, yql_tokenpath, query):
        self.requests.append(query)
        self.requests_datetime.append(datetime.utcnow())

        return self._responses[query].pop(0)


class FakeProcessor(YqlProcessor):
    def __init__(self, name, config, yql_dependency=None):
        YqlProcessor.__init__(self, name, config, GENERATION, yql_dependency)

    @property
    def _yql_statement(self):
        # Запрос реально выполнять не будем, поэтому допускается невалидный запрос.
        return 'INSERT INTO {}'.format(self.name)


def test_execteption_restart(config):
    log = logging.getLogger()

    # Setup
    TIMEOUT_SEC = 5
    b = FakeProcessor('b', config)
    a = FakeProcessor('a', config, [b])
    all_processors = [a, b]

    responses = {
        b._yql_statement: [FailedRequest(), SuccessRequest()],
        a._yql_statement: [SuccessRequest()]
    }

    yql_executor = FakeYqlExecutor(responses)
    for p in all_processors:
        p.override_yql_executor(yql_executor)

    # Run
    executor = ParallelYqlExecutor(config, all_processors, 2, log, timeout_sec=TIMEOUT_SEC)
    executor.run()

    # Assert
    assert_that(len(yql_executor.requests) == 3, 'Wrong number of requests made')
    assert_that(yql_executor.requests == [b._yql_statement, b._yql_statement, a._yql_statement], 'Wrong sequence of requests')
    assert_that(yql_executor.requests_datetime[1] - yql_executor.requests_datetime[0] >= timedelta(seconds=TIMEOUT_SEC), 'Wrong timeout between requests')
