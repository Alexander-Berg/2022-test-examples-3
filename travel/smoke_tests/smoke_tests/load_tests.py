import logging
import socket
from importlib import import_module

from travel.rasp.smoke_tests.smoke_tests.checkers import UrlCheck
from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants


log = logging.getLogger(__name__)


def load_env(env_module, env_name, stableness, params):
    for attr_name, value in vars(env_module).items():
        if attr_name.lower() == env_name.lower():
            env = value()
            setattr(env_module, 'env', env)
            setattr(env, 'stableness', stableness)
            setattr(env, 'env_name', env_name.lower())

            for param_name, param_value in params.items():
                try:
                    default_param = getattr(env, param_name)
                except AttributeError:
                    raise Exception('No such env param: {}'.format(param_name))
                else:
                    converter = type(default_param)
                    if converter is int:
                        converter = float

                    param_value = converter(param_value)

                setattr(env, param_name, param_value)
            return env
    else:
        raise Exception(f'{env_name} was not found in {env_module}')


class UrlCheckParams(object):
    """
    Дополнительные параметры проверки
    """
    def __init__(self, check_params, get_check_params_fun=None):
        self.check_params = check_params
        self.get_url_params_fun = get_check_params_fun

    def make(self):
        """Заполнение параметров из json"""
        params = self.check_params.copy()
        if self.get_url_params_fun:
            params.update(self.get_url_params_fun())

        self.use_host = params.get('use_host', True)
        self.host = params.get('host')
        self.url_processor = params.get('url_processor')
        self.name = params.get('name')

        self.expected_code = params.get('code', 200)
        self.data = params.get('data')
        self.headers = params.get('headers') or {'X-Rasp-Smoke-Tests': socket.gethostname()}
        self.cookies = params.get('cookies')
        self.method = params.get('method') or ('POST' if self.data is not None else 'GET')

        self.timeout = params.get('timeout', 3)
        self.retries = params.get('retries', 3)
        self.retries_delay = params.get('retries_delay', 10)
        self.allow_redirects = params.get('allow_redirects', True)
        self.processes = params.get('processes', ())


class UrlCheckConfig(object):
    """
    Одно описание конфигурации проверки
    """
    def __init__(self, module_config, url_conf, host):
        params_dict = dict(module_config.get('params', {}))
        params_dict['host'] = host

        url_params, extra_urls = {}, []
        if isinstance(url_conf, list):
            if len(url_conf) == 3:
                url, url_params, extra_urls = url_conf
            else:
                url, url_params = url_conf
        else:
            url = url_conf

        self.url = self.get_url_fun = None
        if isinstance(url, str):
            self.url = url
        else:  # url задан функцией
            self.get_url_fun = url

        get_url_params_fun = None
        if isinstance(url_params, dict):
            self.stableness = url_params.pop('stableness', StablenessVariants.STABLE)
            params_dict.update(url_params)
        else:  # параметры заданы функцией
            get_url_params_fun = url_params
            self.stableness = StablenessVariants.STABLE

        self.full_url = None
        self.params = UrlCheckParams(params_dict, get_url_params_fun)
        self.extra_url_configs = [
            UrlCheckConfig(module_config, extra_url, host)
            for extra_url in extra_urls
        ]

    def make(self):
        """Формирование полного урла и параметров запроса"""
        self.params.make()

        from travel.rasp.smoke_tests.smoke_tests.config.utils import context

        url_part = self.url if self.url is not None else self.get_url_fun()
        url_part = url_part.format(**context)

        full_url = f'{self.params.host}/{url_part}' if self.params.use_host else url_part
        if self.params.url_processor:
            full_url = self.params.url_processor(full_url)
        self.full_url = full_url

    def get_description(self):
        return str(self.get_url_fun or self.url)


def prepare_url_check_config(check_config, module_env, url_conf, host):
    url_config = UrlCheckConfig(check_config, url_conf, host)
    if not url_config.stableness.is_runnable(module_env.env_name, module_env.stableness):
        url_config.make()
        log.info(f'Skipping {url_config.params.name or url_config.full_url}')
        return None

    return url_config


def load_url_checkers(module_path, check_config, module_env):
    hosts = check_config.get('hosts')
    if not hosts:
        host = check_config.get('host')
        if not host:
            raise Exception(f'No hosts specified for {module_path} {check_config}')
        hosts = [host]

    url_checks = []
    for url_conf in check_config['urls']:
        for host in hosts:
            url_config = prepare_url_check_config(check_config, module_env, url_conf, host)
            if url_config is not None:
                url_checks.append(UrlCheck(module_path, url_config))

    return url_checks


def load_tests_module(module_path, env_name, stableness, params):
    try:
        env_module = import_module(f'travel.rasp.smoke_tests.smoke_tests.config.{module_path}.env')
    except ImportError:
        pass
    else:
        load_env(env_module, env_name, stableness, params)

    module = import_module(f'travel.rasp.smoke_tests.smoke_tests.config.{module_path}.checks')
    checks = []
    for check_config in module.checks:
        checks.extend(load_url_checkers(module_path, check_config, module.env))

    return checks
