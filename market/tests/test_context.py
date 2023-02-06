import context
import fill_context
from helpers import prepare_service


def test_fill_context(tmp_dir, service_yaml_dict):
    prepare_service(tmp_dir, service_yaml_dict)
    fill_context.fill_context()
    assert context.service_name == 'market-java-application-template'
    assert context.root_package == 'ru.yandex.market.template'
