import json
import os.path
import tempfile

import pytest
import yatest.common

BASE_PATH = 'search/plutonium/tools/worker_config_generator/test/'
SIMPLE_TEMPLATE = 'simple_template.json.j2'
COMPLEX_OBJECT_TEMPLATE = 'complex_object_template.json.j2'
BIN_PATH = yatest.common.binary_path('search/plutonium/tools/worker_config_generator/worker_config_generator')


def get_path(file):
    return yatest.common.source_path(os.path.join(BASE_PATH, file))


def generate(template_name, var_dict: dict = None, yaml_file_name=None):
    vars = []
    if var_dict:
        for k, v in var_dict.items():
            vars.append('--var')
            vars.append(f'{k}={v}')

    yaml_path = None
    if yaml_file_name:
        yaml_path = get_path(yaml_file_name)

    command = [
        BIN_PATH,
        '--input', get_path(template_name)
    ]
    if yaml_path:
        command.append('--yaml')
        command.append(yaml_path)
    command += vars

    with tempfile.TemporaryFile() as f:
        yatest.common.execute(command, stdout=f)
        f.seek(0)
        return f.read()


def test_vars():
    result = generate(
        SIMPLE_TEMPLATE,
        var_dict={
            'foo': 'FooValue',
            'strVar': 'test',
            'bar': 'BarValue',
            'intVar': 451
        }
    )
    assert json.loads(result) == {'staticField': 42, 'FooValue': 'test', 'BarValue': 451}


def test_bad_value():
    with pytest.raises(yatest.common.process.ExecutionError):
        generate(SIMPLE_TEMPLATE, var_dict={'foo': 'Foo=Value'})


def test_simple_yaml():
    result = generate(SIMPLE_TEMPLATE, yaml_file_name='simple_config.yml')
    assert json.loads(result) == {'staticField': 42, 'FooValue': 'test', 'BarValue': 451}


def test_complex_yaml():
    result = generate(COMPLEX_OBJECT_TEMPLATE, yaml_file_name='complex_object_config.yml')
    assert json.loads(result) == {'int': 42, 'joinedList': [{'field': 'one'}, {'field': 'two'}], 'staticField': 42, 'str': 'strFieldValue'}
