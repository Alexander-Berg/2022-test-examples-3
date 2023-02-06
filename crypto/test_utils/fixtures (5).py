import pytest

from crypta.lib.python.tvm.test_utils.tvm_api_recipe import TvmApiRecipe


@pytest.fixture(scope="session")
def tvm_api():
    with TvmApiRecipe() as recipe:
        yield recipe
