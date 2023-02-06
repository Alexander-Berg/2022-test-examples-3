#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import jsonschema
import yatest
import pytest


def test_jconfig():
    jschema = json.load(open(yatest.common.source_path("extsearch/images/razladki/yt_razladki/tests/config_schema.json"), "r"))
    jconfig = json.load(open(yatest.common.source_path("extsearch/images/razladki/yt_razladki/config.json"), "r"))
    try:
        jsonschema.validate(jconfig, jschema)
    except jsonschema.ValidationError as e:
        pytest.fail("Unexpected ValidationError\n" + str(e))
