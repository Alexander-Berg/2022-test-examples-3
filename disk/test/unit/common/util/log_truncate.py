#!/usr/bin/python
# -*- coding: utf-8 -*-

from unittest import TestCase
from logging import makeLogRecord

from mpfs.common.util.logger import TSKVSafeLimitedFormatter


class LogTruncatorTestCase(TestCase):

    @staticmethod
    def format(message, message_max_len, tail_offset):
        fmt = TSKVSafeLimitedFormatter(message_max_len)
        fmt.TAIL_OFFSET = tail_offset
        return fmt.format(makeLogRecord({'msg': message}))

    def test_truncate(self):
        trunc = '...<TRUNCATED %d>...'

        assert self.format('', 100, 10) == ''
        assert self.format('x'*100, 100, 10) == 'x'*100
        assert self.format('x'*101, 100, 10) == 'x'*90 + trunc % 1 + 'x'*10
        assert self.format('x'*20000 + 'y'*25000, 25000, 1000) == 'x'*20000 + 'y'*4000 + trunc % 20000 + 'y'*1000
