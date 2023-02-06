import os
import pytest
from crypta.graph.v1.tests.libs.conftest import *  # noqa


@pytest.fixture(scope="module")
def crypta_env_soup(crypta_env):
    assert set(os.environ["STREAMING_LOGS"].split()) == set(
        ["mm", "access", "redir", "wl", "bar", "eal", "bs-rtb-log", "postback-log"]
    )

    # dependency on v1 only tasks should fail
    os.environ["LUIGI_FAIL_TAGS"] = "v1"
