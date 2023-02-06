import luigi
import pytest

from crypta.lib.python import script_name
from crypta.lib.python.script_name.test import script


@pytest.mark.parametrize("skip_locations", [
    None,
    {"crypta/lib/python/script_name/test/script"},
], ids=["without_skip_locations", "with_skip_locations"])
def test_simple(skip_locations):
    return script.wrapped_detect_script_name(skip_locations=skip_locations)


def test_luigi_task():
    class MyTask(luigi.Task):
        def detect_script_name(self):
            return script_name.detect_script_name()

    return MyTask().detect_script_name()
