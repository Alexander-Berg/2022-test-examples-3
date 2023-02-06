# coding: utf-8


import yaml
import pytest


from schema.schema import SchemaError
from market.sre.tools.dpreparer.lib.schemes.schemes import MANIFEST_SCHEMA
from market.sre.tools.dpreparer.lib.schemes.examples import MANIFEST_EXAMPLE_TEXT
from market.sre.tools.dpreparer.lib.schemes.examples import _MANIFEST_EXAMPLE_TEXT_WRONG


def test_validate_blank_manifest():
    assert MANIFEST_SCHEMA.validate(yaml.safe_load(MANIFEST_EXAMPLE_TEXT))


def test_exception_blank_manifest():
    with pytest.raises(SchemaError):
        MANIFEST_SCHEMA.validate(yaml.safe_load(_MANIFEST_EXAMPLE_TEXT_WRONG))
