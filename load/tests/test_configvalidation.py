import pytest
import yaml

from yatest import common

from load.projects.validator.src.api import get_validation_result, convert_ini


@pytest.mark.parametrize('yaml_config, expected', [
    (common.source_path('load/projects/validator/tests/good_1.yaml'), {}),
    (common.source_path('load/projects/validator/tests/good_2.yaml'), {})
])
def test_validate_valid_yaml(yaml_config, expected):
    with open(yaml_config, 'r') as fd:
        config = yaml.load(fd.read(), Loader=yaml.FullLoader)
    result = get_validation_result(config)
    assert result['errors'] == expected


@pytest.mark.xfail(reason='YANDEXTANK-438')
@pytest.mark.parametrize('ini_config, expected', [
    (common.source_path('load/projects/validator/tests/good_1.ini'), {}),
    (common.source_path('load/projects/validator/tests/good_2.ini'), {})
])
def test_validate_valid_ini(ini_config, expected):
    with open(ini_config, 'r') as fd:
        config = fd.read()
    result = get_validation_result(convert_ini(config))
    assert result['errors'] == expected


@pytest.mark.parametrize('yaml_config, expected', [
    (common.source_path('load/projects/validator/tests/bad_1.yaml'), {'phantom': {'address': ['required field']}}),
])
def test_validate_bad_yaml(yaml_config, expected):
    with open(yaml_config, 'r') as fd:
        config = yaml.load(fd.read(), Loader=yaml.FullLoader)
    result = get_validation_result(config)
    assert result['errors'] == expected
