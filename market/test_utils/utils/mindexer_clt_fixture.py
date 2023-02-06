# -*- coding: utf-8 -*-

import os
import errno
import six.moves.configparser as ConfigParser
import copy
import logging

import pytest
import yatest
import market.pylibrary.database as database
from sqlalchemy.sql import func

from market.pylibrary.mindexerlib.sql import (
    generations,
    blue_generations,
    HOSTNAME,
)
from market.idx.marketindexer.miconfig import MiConfig


logger = logging.getLogger()


def _makedirs(path, exist_ok=True):
    """os.makedirs wrapper
    - don't cause an error if path already exists
    """
    try:
        os.makedirs(path)
    except OSError as e:
        if e.errno != errno.EEXIST or not exist_ok:
            raise


def _symlink(src, dst):
    """os.symlink wrapper
    - create dst directories if they aren't exits
    - replace symlink if it already exits
    """
    try:
        if os.path.exists(dst):
            os.remove(dst)
        _makedirs(os.path.dirname(dst))
        os.symlink(src, dst)
    except OSError as e:
        if e.errno != errno.EEXIST:
            raise


class MIndexerCltFixture(object):
    def __init__(self):
        self.binary_path = yatest.common.binary_path('market/idx/marketindexer/bin/mindexer_clt/mindexer_clt')
        self.env = {
            'IC_CONFIG_PATH': yatest.common.source_path('market/idx/miconfigs/etc/feature/common.ini'),
            'IL_CONFIG_PATH': yatest.common.source_path('market/idx/miconfigs/etc/feature/common.ini'),
            'DS_CONFIG_PATH': yatest.common.source_path('market/idx/marketindexer/tests/datasources.conf'),
            'ZK_CONFIG_PATH': yatest.common.source_path('market/idx/marketindexer/tests/zookeeper.conf'),
            'ENV_TYPE': 'development',  # для определенности
            'MI_TYPE': 'stratocaster',
        }
        self.config = None
        self._local_config_overrides = {
            ('general', 'envtype'): self.env['ENV_TYPE'],
            ('general', 'mitype'): self.env['MI_TYPE'],
        }
        self._zk_prefix = ''

    @property
    def prefix_dir(self):
        return self.env.get('MI_PREFIX_DIR')

    @prefix_dir.setter
    def prefix_dir(self, dir):
        dir = str(dir)
        bin_dir = os.path.join(dir, 'bin')
        os.makedirs(bin_dir)
        self.env.update({
            'MI_PREFIX_DIR': dir,
            'PATH': bin_dir,
        })

    @property
    def zk_prefix(self):
        return self._zk_prefix

    @zk_prefix.setter
    def zk_prefix(self, zk_prefix):
        self._zk_prefix = zk_prefix
        if self._zk_prefix:
            self._local_config_overrides.update({
                ('zookeeper', 'master_dir'): '{}/mimaster'.format(self._zk_prefix),
                ('zookeeper', 'blue_master_dir'): '{}/mimaster_blue'.format(self._zk_prefix),
                ('publish.async', 'root_prefix'): '{}/publisher'.format(self._zk_prefix),
            })

    @property
    def env_type(self):
        return self.env.get('ENV_TYPE')

    @env_type.setter
    def env_type(self, value):
        self.env.update({
            'ENV_TYPE': value
        })
        env_file = os.path.join(self.prefix_dir, 'etc/yandex/environment.type')
        _makedirs(os.path.dirname(env_file))
        with open(env_file, 'w') as f:
            f.write(value)
        self._local_config_overrides.update({
            ('general', 'envtype'): value,
        })
        self._init_configs()

    @property
    def datasources_path(self):
        return self.env.get('DS_CONFIG_PATH')

    @datasources_path.setter
    def datasources_path(self, path):
        self.env.update({
            'DS_CONFIG_PATH': path
        })

    @property
    def mitype(self):
        return self.env.get('MI_TYPE')

    @mitype.setter
    def mitype(self, mitype):
        self.env.update({
            'MI_TYPE': mitype
        })
        self._local_config_overrides.update({
            ('general', 'mitype'): mitype,
        })

    @property
    def local_ini_path(self):
        return os.path.join(self.prefix_dir, 'etc/yandex/marketindexer/local.ini')

    def init(self):
        assert self.prefix_dir, 'you need to set_prefix_dir first'
        self._init_zk_config()
        # зовется после _init_zk_config но перед make_local_config
        # если найден работающий локальный zookeeper то
        # прописывает в локальный конфиг строку подключения к нему
        self.make_local_config({})
        self._init_configs()
        self._link_binaries()
        self._link_data_files()
        self._make_preinstall_dirs()

    def path(self, path):
        assert self.prefix_dir, 'you need to set_prefix_dir first'
        return os.path.join(self.prefix_dir, path)

    def _init_configs(self):
        """Инициализируем конифиги БОИ"""
        self.config = MiConfig(
            paths=[self.env['IC_CONFIG_PATH'], self.env['IL_CONFIG_PATH']],
            dspath=self.datasources_path,
            prefix_dir=self.prefix_dir,
            envtype=self.env['ENV_TYPE'],
        )

    def _init_zk_config(self):
        """Детектим запущен ли сейчас локальные zookeeper и если он работает, то используем его"""
        # Рецепт ZK не запущен продолжаем исплользовать стабовый конфиг
        if 'RECIPE_ZOOKEEPER_PORT' not in os.environ:
            return
        zk_conf_tmpl_path = yatest.common.source_path('market/idx/marketindexer/test_utils/data/zookeeper.conf.tmpl')
        with open(zk_conf_tmpl_path) as zk_conf_tmpl_fd:
            zk_conf_tmpl = zk_conf_tmpl_fd.read()
        local_zk_conf_path = os.path.join(self.prefix_dir, 'etc/yandex/market-datasources/zookeeper.conf')
        _makedirs(os.path.dirname(local_zk_conf_path))
        with open(local_zk_conf_path, 'w') as local_zk_conf_fd:
            local_zk_conf_fd.write(zk_conf_tmpl.format(port=os.getenv('RECIPE_ZOOKEEPER_PORT'), zk_prefix=self._zk_prefix))
        self.env.update({
            'ZK_CONFIG_PATH': local_zk_conf_path
        })
        self._local_config_overrides.update({
            ('zookeeper', 'connection'): 'localhost:{}'.format(os.getenv('RECIPE_ZOOKEEPER_PORT'))
        })

    def _make_preinstall_dirs(self):
        """Создаем директории которые mindexer_clt ожидает видеть уже созданными
        Обычно их создание происходит при установке пакета в postinstall
        """
        for config in (self.config,):
            _makedirs(config.lock_dir)
            _makedirs(config.run_dir)
            _makedirs(config.command_status_dir)

    def _link_data_files(self):
        """Создаем симлинки на некоторые файлы которые используюся
        NB: Все файлы на которые создаются ссылки должны быть перечисленны в секции DATA в dependencies.inc
        """
        assert self.config, 'must be called after _init_configs'
        data_files = {
            'market/idx/miconfigs/etc/master/conf-available/development.stratocaster.ini':  self.config.master_config_path,
            'market/idx/generation/packages/yandex-market-offers/scripts/PlatformVersionStats.xslt': self.config.feedlog_platform_version_stats_xslt,
        }
        for arcadia_path, destination in data_files.items():
            _symlink(yatest.common.source_path(arcadia_path), destination)

        reductor_conf_files = {
            'market/idx/miconfigs/etc/reductor/conf-available/stub.ini':  self.config.proto_config_path,
            'market/idx/miconfigs/etc/reductor/updater/conf-available/testing.ini':  self.config.updater_proto_config_path,
        }
        for arcadia_path, destination in reductor_conf_files.items():
            _symlink(yatest.common.build_path(arcadia_path), destination)

    def _link_binaries(self):
        """Создаем симлинки на собранные бинарники из аркадии
        NB: Все файлы на которые создаются ссылки должны быть перечисленны в секции DEPENDS в dependencies.inc
        """
        assert self.config, 'must be called after _init_configs'
        binaries = {
            'market/idx/generation/feedlog-merger/feedlog-merger': self.config.feedlog_merger,
            'market/idx/generation/feedlog-stats/feedlog-stats': self.config.feedlog_stats,
            'market/reductor/configure/configure': self.config.configure_reductor_bin,
            'contrib/tools/xsltproc/xsltproc': os.path.join(self.prefix_dir, 'bin/xsltproc'),
        }
        for arcadia_path, destination in binaries.items():
            _symlink(
                yatest.common.binary_path(arcadia_path),
                destination
            )

    def make_local_config(self, local_config_overrides=None):
        assert self.prefix_dir, 'you need to set_prefix_dir first'

        config = ConfigParser.ConfigParser()
        config_overrides = copy.deepcopy(self._local_config_overrides)
        if local_config_overrides:
            config_overrides.update(local_config_overrides)
        for (section, option), value in config_overrides.items():
            if not config.has_section(section) and section.lower() != 'default':
                config.add_section(section)
            config.set(section, option, value)
        _makedirs(os.path.dirname(self.local_ini_path))
        with open(self.local_ini_path, 'w') as local_ini:
            config.write(local_ini)
        self.env.update({
            'IL_CONFIG_PATH': self.local_ini_path,
        })
        self._init_configs()

    def update_local_config(self, local_config_overrides=None):
        assert self.prefix_dir, 'you need to set_prefix_dir first'

        if not os.path.exists(self.local_ini_path):
            self.make_local_config(local_config_overrides)
            return
        config = ConfigParser.ConfigParser()
        config.read(self.local_ini_path)
        for (section, option), value in local_config_overrides.items():
            if not config.has_section(section) and section.lower() != 'default':
                config.add_section(section)
            config.set(section, option, value)
        with open(self.local_ini_path, 'w') as local_ini:
            config.write(local_ini)

    def execute(self, *args, **kwargs):
        cmd_args = [self.binary_path]
        cmd_args.extend(args)
        env = copy.deepcopy(self.env)
        if kwargs.pop('blue', False):
            env['MASSINDEXER_BLUE'] = 'true'
        return yatest.common.execute(cmd_args, env=env, **kwargs)

    def add_generation_to_super(self, generation_name, blue=False, half=False, **kwargs):
        """Создает стаб поколения в супер базе

        :param generation_name: имя поколения, например 20180101_0101
        :param blue: является ли поколение синим поколением
        :param kwargs: переопределить поля, которые будут записаны в супербазу
        """
        config = self.config
        table = generations
        if blue:
            table = blue_generations
        connection = database.connect(**config.datasources['super'])
        with connection.begin():
            generation_values = {
                'hostname': HOSTNAME,
                'mi_ver': '2018.4.80',
                'mitype': config.mitype,
                'name': generation_name,
                'production': 0,
                'sc_version': 'SYNC UC',
                'state': 'released',
                'target': config.target,
                'release_date': func.now(),
                'released': 1,
                'ok': 1,
            }
            if blue:
                generation_values.update({
                    'type': 'blue'
                })
            if half:
                generation_values.update({
                    'half_mode': True
                })
            generation_values.update(**kwargs)
            connection.execute(table.insert().values(**generation_values))


@pytest.fixture()
def mindexer_clt(tmpdir, request):
    """Инициализируем окружение для mindexer_clt
    В том числе создаем базы в mysql

    NB: что бы эта фикстура работала с mysql надо:
    1. в файле с тестами заимпортить reusable_mysql и mysql (это важно)
    2. в тесте использовать фикстуру reusable_mysql (иначе не будет очистки по окончанию теста)


    Пример теста:
    from market.idx.marketindexer.test_utils.utils.mindexer_clt_fixture import mindexer_clt  # noqa
    from market.pylibrary.mindexerlib.test_utils.mysql import reusable_mysql, mysql  # noqa

    def test_something(mindexer_clt, reusable_mysql):
        assert ...


    Пример промежуточной фикстуры:
    from market.idx.marketindexer.test_utils.utils.mindexer_clt_fixture import mindexer_clt  # noqa
    from market.pylibrary.mindexerlib.test_utils.mysql import reusable_mysql, mysql  # noqa

    @pytest.fixture()  # noqa
    def mindexer_clt_with_generations(mindexer_clt, reusable_mysql):
        mindexer_clt.add_generation_to_super('20180101_0101')
        mindexer_clt.add_generation_to_super('20180101_0100', blue=True)
        return mindexer_clt

    def test_something(mindexer_clt_with_generations):
        assert ...
    """
    from _pytest.fixtures import FixtureLookupError
    fixture = MIndexerCltFixture()
    fixture.prefix_dir = tmpdir
    use_mysql = False
    try:
        logger.debug('Initializing mysql fixture')
        mysql = request.getfixturevalue("mysql")
        datasources_path = str(tmpdir / 'etc/yandex/marketindexer/datasources.conf')
        _makedirs(os.path.dirname(datasources_path))
        mysql.write_datasources(datasources_path)
        fixture.datasources_path = datasources_path
        use_mysql = True
    except FixtureLookupError:
        # если mysql имортировать не удалось, то живем без настоящей супербазы
        pass

    try:
        logger.debug('Initializing reusable_zk fixture')
        zk = request.getfixturevalue("reusable_zk")
        fixture.zk_prefix = zk.chroot
        logger.debug('zk_chroot: %s', zk.chroot)
    except FixtureLookupError:
        # если mysql имортировать не удалось, то живем без настоящей супербазы
        pass

    fixture.init()

    if use_mysql:
        # создаем супербазу
        fixture.execute('setup')
    return fixture
