import pytest


pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.siberia.bin.make_id_to_crypta_id.lib.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture
def matching_types():
    return ["puid"]


@pytest.fixture
def crypta_id_user_data_cypress_path():
    return "//crypta_id_user_data"
