import json
import time
from typing import List, Callable

from hamcrest.core.base_matcher import BaseMatcher


class SuccessUnistatResponse(BaseMatcher):
    def __init__(self, matcher: BaseMatcher):
        self.matcher = matcher

    def _matches(self, item) -> bool:
        if item.status_code != 200:
            return False

        return self.matcher.matches({k: v for k, v in json.loads(item.text)})

    def describe_to(self, description):
        description.append_text('unistat response with status_code == 200 and json array in text ')
        self.matcher.describe_to(description)


def success_unistat_response(matcher):
    return SuccessUnistatResponse(matcher)


class FirstWithAction(BaseMatcher):
    def __init__(self, action: str, matcher: BaseMatcher):
        self.matcher = matcher
        self.action = action

    def _matches(self, item: List) -> bool:
        data = list(filter(lambda x: 'action' in x and x['action'] == self.action, item))

        if len(data) != 1:
            return False

        return self.matcher.matches(data[0])

    def describe_to(self, description):
        description.append_text(f'first element with action "{self.action}" ')
        self.matcher.describe_to(description)


def first_with_action(action, matcher):
    return FirstWithAction(action, matcher)


class WaitWith(BaseMatcher):
    def __init__(self, times: int, matcher: BaseMatcher):
        self.times = times
        self.matcher = matcher
        self.last_data = None

    def _matches(self, item: Callable[[], object]) -> bool:
        if not callable(item):
            raise AssertionError('item must be callable')

        for _ in range(self.times):
            self.last_data = item()
            if self.matcher.matches(item=self.last_data):
                return True
            time.sleep(1)

        return False

    def describe_to(self, description):
        description.append_text(f' trying "{self.times}" times data {self.last_data} ')
        self.matcher.describe_to(description)


def wait_with(matcher: BaseMatcher, times: int = 15):
    return WaitWith(matcher=matcher, times=times)


class SuccessLaunch(BaseMatcher):
    def _matches(self, item) -> bool:
        return 'launched' in item.text and item.status_code == 200

    def describe_to(self, description):
        description.append_text('must be success launch')


def success_launch():
    return SuccessLaunch()


class FailedLaunch(BaseMatcher):
    def __init__(self, code: int):
        self.code = code

    def _matches(self, item) -> bool:
        return item.status_code == self.code

    def describe_to(self, description):
        description.append_text('must be failed launch')


def failed_launch(code: int):
    return FailedLaunch(code=code)


class ValueOfField(BaseMatcher):
    def __init__(self, key: str, matcher: BaseMatcher):
        self.matcher = matcher
        self.key = key

    def _matches(self, data) -> bool:
        return self.key in data and self.matcher.matches(data[self.key])

    def describe_to(self, description):
        description.append_text(f'field "{self.key}" ')
        self.matcher.describe_to(description)


def value_of_field(key, matcher):
    return ValueOfField(key, matcher)


class MissingField(BaseMatcher):
    def __init__(self, key: str):
        self.key = key

    def _matches(self, data) -> bool:
        return self.key not in data

    def describe_to(self, description):
        description.append_text(f'field "{self.key}" is missing')


def missing_field(key):
    return MissingField(key)


class AllSuitesArePassed(BaseMatcher):
    def _matches(self, data) -> bool:
        try:
            return int(data['failed']) == 0
        except:
            return False

    def describe_to(self, description):
        description.append_text('all test suites must be passed')


def all_suites_are_passed():
    return AllSuitesArePassed()
