import pytest


pytest_plugins = [
    'crypta.lib.python.nirvana.test_helpers.fixtures',
]


@pytest.fixture
def date():
    return '2021-10-10'


@pytest.fixture(
    params=[
        pytest.param(
            None,
            id='default_dir',
        ),
        pytest.param(
            '//home/custom',
            id='custom_dir',
        ),
    ],
)
def custom_output_dir(request):
    return request.param
