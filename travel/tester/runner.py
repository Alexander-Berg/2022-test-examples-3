import os
import sys


def load_env():
    PROJECT_PATH = os.path.abspath(os.path.dirname(__file__))

    activate_this = os.path.join(PROJECT_PATH, '../virtualenv/bin/activate_this.py')
    execfile(activate_this, dict(__file__=activate_this))


def run(config, args=None):
    os.environ['DJANGO_SETTINGS_MODULE'] = config.get('settings', 'tests_settings')

    load_env()

    import pytest

    if args is None:
        args = sys.argv[1:]

    from travel.avia.stat_admin.tester import initializer
    from travel.avia.stat_admin.tester.plugins import transaction

    plugins = config.get('default_plugins', [initializer, transaction])

    if config.get('plugins'):
        if callable(config['plugins']):
            plugins.extend(config['plugins']())
        else:
            plugins.extend(config['plugins'])

    pytest.main(args, plugins)
