import pytest
import yatest


@pytest.fixture(scope="module")
def fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path("market/front/tools/service_updater/scenarios/tests/fixtures")


@pytest.fixture(scope="module")
def lib_fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path("market/front/tools/service_updater/lib/tests/fixtures")


@pytest.fixture(scope="module")
def helpers_fixtures_dir():  # type: () -> unicode
    return yatest.common.source_path("market/front/tools/service_updater/helpers/tests/fixtures")
