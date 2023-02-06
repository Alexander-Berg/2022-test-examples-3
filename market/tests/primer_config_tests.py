# coding: utf8
import logging


from schema.schema import SchemaError
import primer_config_schema
import primer_config_unique

log = logging.getLogger(__name__)


def test_primer_schema(primer_config):
    """ В конфиге должны быть только валидные параметры """
    config = primer_config['data']

    # Проверяем в целом схему yaml-конфига
    try:
        primer_config_schema.config_schema.validate(config)
    except SchemaError:
        log.error('Error in config %s', primer_config['filename'])
        raise


def test_unique_params(primer_config):
    config = primer_config['data']
    filename = primer_config['filename']
    try:
        primer_config_unique.uniq.validate(config)
        log.info("config {} passed".format(filename))
    except ValueError as err:
        log.error("{} config {}".format(err, filename))
        raise ValueError("{} config {}".format(err, filename))


def test_validate_params(primer_config):
    config = primer_config['data']
    rps_safe_per_host = config['kraken']['rps_safe_per_host']
    rps_min_per_host = config['kraken']['rps_min_per_host']
    if rps_safe_per_host < rps_min_per_host:
        log.error("rps_safe_per_host smaller rps_min_per_host: {} < {}. fix it".format(rps_safe_per_host, rps_min_per_host))
        raise ValueError("rps_safe_per_host smaller rps_min: {} < {}. fix it".format(rps_safe_per_host, rps_min_per_host))
    sla_exts = [i.replace("sla_", "") for i in config["kraken"] if i.startswith("sla_ext")]
    for sla_param in sla_exts:
        selector_name = "{}_selector".format(sla_param)
        templater_name = "{}_template".format(sla_param)
        if not (selector_name in config["target"]):
            raise ValueError("not found selector '{}' in 'target' section".format(selector_name))
        if not (templater_name in config["solomon"] or templater_name in config["graphite"]):
            raise ValueError("not found template '{}' in 'solomon'/'graphite' section".format(templater_name))
