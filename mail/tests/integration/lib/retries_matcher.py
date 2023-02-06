import time
from datetime import timedelta
from hamcrest.core.base_matcher import BaseMatcher


class RetriesMatcher(BaseMatcher):
    def __init__(self, sleep_times, matcher):
        self.sleep_times = sleep_times
        self.matcher = matcher
        self._last_result = None

    def _matches(self, item):
        if not callable(item):
            raise RuntimeError('expect callable item, got %r', item)
        for try_sleep in self.sleep_times:
            self._last_result = item()
            if self.matcher.matches(self._last_result):
                return True
            time.sleep(try_sleep)
        return False

    def describe_mismatch(self, item, mismatch_description):
        mismatch_description.append_text('last result of ') \
                            .append_description_of(item) \
                            .append_text(' ')
        self.matcher.describe_mismatch(self._last_result, mismatch_description)

    def describe_to(self, description):
        retries_td = timedelta(seconds=sum(self.sleep_times))
        description.append_description_of(self.matcher) \
                   .append_text(' [') \
                   .append_text('after %d retries ' % len(self.sleep_times)) \
                   .append_text('with total duration %s' % str(retries_td)) \
                   .append_text(']')
