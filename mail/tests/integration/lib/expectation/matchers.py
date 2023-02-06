import json

from hamcrest.core.base_matcher import BaseMatcher


class TransformMatcherHelper(BaseMatcher):
    def __init__(self, matcher, transform):
        self.matcher = matcher
        self.transform = transform

    def _matches(self, item):
        data = self.transform(item)
        return self.matcher.matches(data)

    def describe_to(self, description):
        self.matcher.describe_to(description)


def as_json(match):
    return TransformMatcherHelper(match, transform=json.loads)


def transformed_by(transform, match):
    return TransformMatcherHelper(match, transform)
