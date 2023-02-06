import pytest
from pytest_bdd import (
    given,
    parsers,
)
from yatest.common import source_path
from tests_common.pytest_bdd import context as ctx


def get_path(resource):
    path = source_path(resource).split("/")
    path.pop()
    return "/".join(path)


@pytest.fixture(scope="session", autouse=True)
def context():
    return ctx


@pytest.fixture(scope="function", autouse=True)
def step_setup(request, context):
    context.request = request


@pytest.fixture
def pytestbdd_strict_gherkin():
    return False


def pytest_bdd_before_scenario(request, feature, scenario):
    ctx.set_example_params(scenario)


def pytest_bdd_after_step(request, feature, scenario, step, step_func, step_func_args):
    ctx.clear_step()


@given(parsers.string(u'nothing'))
def given_nothing(context):
    pass
