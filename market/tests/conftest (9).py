import pytest
import yatest


@pytest.fixture(scope="module")
def fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path("market/front/tools/server_exps/tests/fixtures")
