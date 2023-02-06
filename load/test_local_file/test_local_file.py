import yaml

from load.tools.yaml_injection import InjectionLoader


def test_local_file():
    with open('main.yml') as in_:
        data = yaml.load(in_, InjectionLoader)

    with open('expected.yml') as in_:
        expected_data = yaml.safe_load(in_)

    assert expected_data == data
