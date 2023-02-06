import yatest


def get_env_type():
    options = ['bigml', 'corp']
    env = yatest.common.get_param('env')
    if env is None:
        raise RuntimeError('environment is not set; choose from ' + str(options))
    if env not in options:
        raise RuntimeError('unknown environment type')
    return env
