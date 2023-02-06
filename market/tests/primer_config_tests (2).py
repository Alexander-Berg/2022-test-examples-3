# coding: utf8


import logging


from schema.schema import SchemaError
import primer_config_schema


log = logging.getLogger(__name__)


def test_primer_schema(primer_config, utils):
    """ В конфиге должны быть только валидные параметры """
    config = primer_config['data']
    filename = primer_config['filename']

    # Проверяем в целом схему yaml-конфига
    try:
        data = primer_config_schema.config_schema.validate(config)
        validate_dict_schema(filename, data)
    except SchemaError:
        log.error('Error in config %s', primer_config['filename'])
        raise

    # Проверяем, что:
    # 1. В конфиге есть yp сервисы
    # 2. Для таких конфигов включен resolve: yes в секции params
    # 3. Для таких конфигов явно выставлен флаг resolve: no в параметрах yp сервиса
    if 'servers' in config and utils.use_yp_services(config['servers']):
        if not utils.resolve_enabled(config['params']):
            assert False, 'The key `resolve` in `params` section is disabled! Enable it!'

        if utils.resolve_realyp_enabled(config['servers']):
            assert False, 'Resovling for real yp service in `servers` section is enabled!\n' +\
                          'This is enabled even when key `resolve` is absent in params of yp service!' +\
                          'It is a bad construction for YP reals. Set `resolve: no` for yp service!'
        if not utils.dns_resolvers_setuped(config['servers']):
            assert False, '`dns_resolvers` settings didn\'t found in `servers` section for yp' +\
                          ' service. Set `dns_resolvers`: "yandex-ns" for example for YP service.'


def validate_dict_schema(filename, data):
    """ Валидация обязательных параметров в конфиге haproxy(CSADMIN-43904) """
    if filename.count("001-globals_and_defaults"):
        return
    values = dict(data).get("values", {})
    for name in ["service_name"]:
        tmp = dict()
        for section, params in values.items():
            param = params.get("globals", {})
            # если параметр пустой, то не записываем его в словарь
            if param is None:
                continue
            tmp[section] = param.get(name, None)
        # проверяем, что default есть, либо есть default@location
        if tmp.get("default", None) is None:
            missed = [section for section, value in tmp.items() if section != "default" and value is None]
            locations = [section for section in tmp.keys() if section != "default"]
            if len(missed) > 0:
                raise SchemaError("not found '{}' param in sections {}, default is empty, sections: {}, filename: {}".format(name, missed, tmp, filename))
            elif len(locations) > len(missed):
                return
            raise SchemaError("not found '{}' param in section 'default', sections: {}, filename: {}".format(name, tmp, filename))
