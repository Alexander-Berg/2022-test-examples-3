# coding: utf8
import logging

from schema.schema import SchemaError
import primer_config_schema


log = logging.getLogger(__name__)


def test_primer_schema(primer_config):
    """ В конфиге должны быть только валидные параметры """
    config = primer_config['data']

    # Валидируем схему
    try:
        primer_config_schema.config_schema.validate(config)
    except SchemaError:
        log.error('Error in config %s', primer_config['filename'])
        raise
