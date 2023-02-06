import re

import pytest

from crypta.lib.python.purgatory.validation import ValidationError
from crypta.lib.python.purgatory.pipeline import Pipeline


@pytest.mark.parametrize("src,dst,ref,exc_match", [
    ({"required": 1, "optional": 2}, None, {"1": "1", "2": "2"}, None),
    ({"required": 3}, {"preexisting": "value"}, {"1": "3", "2": None, "preexisting": "value"}, None),

    ({"required": "four", "optional": 5}, None, None,
     "Object is invalid: Invalid value. Validator: <<lambda>>, path: required, "),
    ({"optional": 6}, None, None,
     "Object is invalid: Invalid value. Validator: <'required' is not present>, path: required")
])
def test_pipeline(src, dst, ref, exc_match):
    pipeline = Pipeline()
    pipeline.add("required", "1", lambda x: isinstance(x, int), str)
    pipeline.add_optional("optional", "2", lambda x: isinstance(x, int), str)

    if ref:
        assert ref == pipeline.process(src, dst)
    else:
        with pytest.raises(ValidationError) as excinfo:
            pipeline.process(src, dst)
        excinfo.match(re.escape(exc_match))


@pytest.mark.parametrize("src,dst,ref,exc_match", [
    ({"required": {"path": 1}, "optional": {"path": 2}}, None, {"1": {"2": "1"}, "2": "2"}, None),
    ({"required": {"path": 3}}, {"preexisting": "value"}, {"1": {"2": "3"}, "2": None, "preexisting": "value"}, None),

    ({"required": {"path": "four"}, "optional": {"path": 4}}, None, None,
     "Object is invalid: Invalid value. Validator: <<lambda>>, path: required.path,"),
    ({"required": 4}, None, None,
     "Object is invalid: Invalid value. Validator: <'required' is not present or is not a dict>, path: required.path")
])
def test_pipeline_with_paths(src, dst, ref, exc_match):
    pipeline = Pipeline()
    pipeline.add(["required", "path"], ["1", "2"], lambda x: isinstance(x, int), str)
    pipeline.add_optional(["optional", "path"], "2", lambda x: isinstance(x, int), str)

    if ref:
        assert ref == pipeline.process(src, dst)
    else:
        with pytest.raises(ValidationError) as excinfo:
            pipeline.process(src, dst)
        excinfo.match(re.escape(exc_match))
