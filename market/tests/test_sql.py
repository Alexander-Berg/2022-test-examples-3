# -*- coding: utf-8 -*-

from market.pylibrary.mindexerlib import sql
from market.pylibrary import database
import tempfile
import mock
import datetime
from collections import namedtuple
from sqlalchemy import select, func


def test_multiple_setup(reusable_mysql):
    """
    Проверяем, что второй вызов setup_mysql_super() не удаляет существующие таблицы
    """
    Config = namedtuple('Config', [
        'datasources',
        # параметры необходимые чтобы вызов create_generation_name() не упал
        'mitype',
        'diff_mode',
        'allow_half_mode',
        'allow_scaling',
        'force_build_half_mode_generation',
        'force_build_scale_mode_generation',
        'target',
    ])
    with tempfile.NamedTemporaryFile() as tmp:
        datasources_path = tmp.name
        reusable_mysql.write_datasources(datasources_path)
        config = Config(
            datasources=database.load_datasources_from_config(datasources_path),
            mitype='stratocaster',
            diff_mode=False,
            allow_scaling=False,
            allow_half_mode=False,
            force_build_half_mode_generation=False,
            force_build_scale_mode_generation=False,
            target='production',
        )
        sql.setup_mysql_super(config)
        connection = sql.make_connection_to_super(config)

        counter = select([func.count()]).select_from(sql.generations)

        def run_count():
            return connection.execute(counter).fetchone()[0]

        with connection.begin():
            assert run_count() == 0
            sql.create_generation_name(config, "0.0.0.0", am_i_master=False)
            assert run_count() == 1

            sql.setup_mysql_super(config)
            assert run_count() == 1


def test_no_half_mode_if_half_mode_already_exists(reusable_mysql):
    """
    Проверяем, что если сегодня в проде уже было создано половинчатое поколение, то следующее будет полным
    """
    Config = namedtuple('Config', [
        'allow_scaling',
        'allow_half_mode',
        'datasources',
        'is_testing',
        'is_production',
        'mitype',
        'force_build_half_mode_generation',
        'force_build_scale_mode_generation',
        'target',
    ])

    with tempfile.NamedTemporaryFile() as tmp:
        datasources_path = tmp.name
        reusable_mysql.write_datasources(datasources_path)
        config = Config(
            allow_half_mode=True,
            allow_scaling=False,
            is_testing=False,
            is_production=True,
            mitype='stratocaster',
            target='production',
            force_build_half_mode_generation=False,
            force_build_scale_mode_generation=False,
            datasources=database.load_datasources_from_config(datasources_path)
        )
        sql.setup_mysql_super(config)
        assert sql.check_half_mode(config, "20190808_0200", am_i_master=False)
        with mock.patch('market.pylibrary.mindexerlib.sql.get_id_name_from_database', new=lambda *args, **kwargs: datetime.datetime(year=2019, month=8, day=8, hour=2, minute=0)):
            gen = sql.create_generation_name(config, "0.0.0.0", am_i_master=False)
            assert gen == "20190808_0200"
            assert not sql.check_half_mode(config, "20190808_0230", am_i_master=False)


def test_no_scale_mode_if_scale_mode_already_exists(reusable_mysql):
    """
    Проверяем, что если сегодня в проде уже было создано масштабированное поколение, то следующее будет полным
    """
    Config = namedtuple('Config', [
        'allow_half_mode',
        'allow_scaling',
        'datasources',
        'is_testing',
        'is_production',
        'mitype',
        'force_build_half_mode_generation',
        'force_build_scale_mode_generation',
        'target',
    ])

    with tempfile.NamedTemporaryFile() as tmp:
        datasources_path = tmp.name
        reusable_mysql.write_datasources(datasources_path)
        config = Config(
            allow_half_mode=False,
            allow_scaling=True,
            is_testing=False,
            is_production=True,
            mitype='stratocaster',
            target='production',
            force_build_half_mode_generation=False,
            force_build_scale_mode_generation=False,
            datasources=database.load_datasources_from_config(datasources_path)
        )
        sql.setup_mysql_super(config)
        assert sql.check_scale_mode(config, "20210808_0300", am_i_master=False)
        with mock.patch('market.pylibrary.mindexerlib.sql.get_id_name_from_database', new=lambda *args, **kwargs: datetime.datetime(year=2021, month=8, day=8, hour=3, minute=30)):
            gen = sql.create_generation_name(config, "0.0.0.0", am_i_master=False)
            assert gen == "20210808_0330"
            assert not sql.check_scale_mode(config, "20210808_0330", am_i_master=False)


def test_scale_mode_on_extra_time(reusable_mysql):
    """
    Проверяем, что если сегодня в проде уже было создано масштабированное поколение,
    то в дополнительное время ещё одно будет создано
    """
    Config = namedtuple('Config', [
        'allow_half_mode',
        'allow_scaling',
        'datasources',
        'is_testing',
        'is_production',
        'mitype',
        'force_build_half_mode_generation',
        'force_build_scale_mode_generation',
        'target',
    ])

    with tempfile.NamedTemporaryFile() as tmp:
        datasources_path = tmp.name
        reusable_mysql.write_datasources(datasources_path)
        config = Config(
            allow_half_mode=False,
            allow_scaling=True,
            is_testing=False,
            is_production=True,
            mitype='stratocaster',
            target='production',
            force_build_half_mode_generation=False,
            force_build_scale_mode_generation=False,
            datasources=database.load_datasources_from_config(datasources_path)
        )
        sql.setup_mysql_super(config)
        assert sql.check_scale_mode(config, "20210808_0300", am_i_master=False)
        with mock.patch('market.pylibrary.mindexerlib.sql.get_id_name_from_database', new=lambda *args, **kwargs: datetime.datetime(year=2021, month=8, day=8, hour=19, minute=0)):
            gen = sql.create_generation_name(config, "0.0.0.0", am_i_master=False)
            assert gen == "20210808_1900"
            assert sql.check_scale_mode(config, "20210808_1900", am_i_master=False)
