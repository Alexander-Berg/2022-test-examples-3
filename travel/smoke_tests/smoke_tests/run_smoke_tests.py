import logging

from travel.rasp.smoke_tests.smoke_tests.load_tests import load_tests_module


log = logging.getLogger(__name__)


def _parse_env_params(env_params):
    if env_params:
        return dict(param_kv.split('=') for param_kv in env_params.split(';'))


class SmokeTestsFailed(Exception):
    pass


def run(config_module, env_name, stableness, envparams=None):
    log.info(f'Run smoke test. Config: {config_module}. Environment: {env_name}. Stableness: {stableness}')

    params_dict = _parse_env_params(envparams) or {}
    url_checks = load_tests_module(config_module, env_name, stableness, params_dict)

    errors_count = 0
    for url_check in url_checks:
        try:
            url_check()
        except Exception:
            errors_count += 1

    if errors_count == 0:
        log.info('All smoke tests passed successfully')
    else:
        raise SmokeTestsFailed(f'{errors_count} errors in smoke tests')
