# -*- coding: utf-8 -*-
import pytest

from rtcc.core.session import Session


@pytest.mark.long
def test_request_simple(tmpdir):
    name_a = "{}/A".format(tmpdir.strpath)
    name_b = "{}/B".format(tmpdir.strpath)
    session_a = Session(name_a)
    session_b = Session(name_b)
    from rtcc.core.generator import Generator
    from rtcc.core.generator import DiffGenerator
    Generator('sfront', session_a).generate("template")
    Generator('sfront', session_b).generate("template")
    diff = DiffGenerator('sfront', session_a, session_b).generate()
    assert diff
