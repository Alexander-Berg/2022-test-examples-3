# -*- encoding: utf-8 -*-
import json

from tests.utils import dump_flag


def test_dump_flag():
    flags = []
    assert {'flags': flags, 'abFlags': []} == json.loads(dump_flag(flags))

    flags = ['TEST1']
    assert {'flags': flags, 'abFlags': []} == json.loads(dump_flag(flags))

    flags = ['TEST1', 'TEST2']
    assert {'flags': flags, 'abFlags': []} == json.loads(dump_flag(flags))
