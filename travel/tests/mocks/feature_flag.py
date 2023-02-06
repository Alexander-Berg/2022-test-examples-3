# -*- encoding: utf-8 -*-
from feature_flag_client import Context


class FeatureFlagClientMock:
    def __init__(self):
        self._flags = []

    def update_flags(self, flags):
        self._flags = flags

    def create_context(self):
        return Context(set(self._flags))
