import pytest
import logging
from schema.schema import SchemaError
from config_schema import get_schema
from conftest import config


log = logging.getLogger(__name__)


@pytest.mark.parametrize("filename,data", sorted(config.get_configs()))
def test_schema(filename, data, ip_service_map):

    config_schema = get_schema(ip_service_map)

    # assert False

    try:
        config_schema.validate(data)
    except SchemaError:
        log.error('Error in config %s', filename)
        raise

    # Дополнительные проверки логики...

    # Тестируем наличие необходимых флажков при выставлении alternative_backends флажка в секции context.
    for _, params in data['context'].items():

        if 'alternative_backends' in params:
            assert 'alt_headers' in params, \
                'Aternative backends is enabled, but alt_headers fields not setup. See README (https://nda.ya.ru/t/GFyI7bo04XhhRz) and Tune it!'
