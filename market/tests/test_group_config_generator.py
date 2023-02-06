# -*- coding: utf-8 -*-

import pytest
from hamcrest import (
    assert_that,
    has_item,
    equal_to,
    is_not,
    raises,
    calling,
    only_contains,
    has_length,
    instance_of,
    none
)

from async_publishing.group_config import (
    GroupConfigCreator,
    GroupConfigGenerateError,
    GroupConfig,
    AsyncPublishingMode,
    make_groups_config,
)

from async_publishing.generation_meta import GenerationMeta, PackageMeta


def _generate_cluster_hosts(id, dc, count):
    def name(index):
        return '{}-{}-{}'.format(dc, id, index)

    return {
        name(i): {
            "cluster": id,
            "datacenter": dc,
            "redundancy": 1,
            "dists": {},
            "name": name(i),
            "key": "key:{}".format(name(i)),
            "service": "marketsearch3"
        }
        for i in range(count)
    }


@pytest.fixture(scope='module')
def publisher_config():

    # Таккой синаксис нужен для обратной совместимости со 2м питоном.
    # Это можно изменить при окончатлеьном переходе на 3ий питон.
    test_group_1_iva_hosts = dict()
    test_group_1_iva_hosts.update(_generate_cluster_hosts(0, 'iva', 3))
    test_group_1_iva_hosts.update(_generate_cluster_hosts(1, 'iva', 3))
    test_group_1_iva_hosts.update(_generate_cluster_hosts(2, 'iva', 3))

    test_group_2_multdc_hosts = dict()
    test_group_2_multdc_hosts.update(_generate_cluster_hosts(0, 'iva', 5))
    test_group_2_multdc_hosts.update(_generate_cluster_hosts(1, 'iva', 3))
    test_group_2_multdc_hosts.update(_generate_cluster_hosts(2, 'sas', 5))
    test_group_2_multdc_hosts.update(_generate_cluster_hosts(3, 'sas', 3))
    test_group_2_multdc_hosts.update(_generate_cluster_hosts(4, 'sas', 5))
    test_group_2_multdc_hosts.update(_generate_cluster_hosts(5, 'sas', 3))

    return {
        "download_timeout": "6000",
        "restart_timeout": "300",
        "reload_timeout": "900",
        "dcgroups": {
            "test-group-1@iva": {
                "async_publishing_mode": "enabled",
                "simultaneous_restart": 1,
                "close_firewall_sleep": 10,
                "is_always_successful": False,
                "hosts": test_group_1_iva_hosts,
                "failures_threshold": 2,
                "simultaneous_dc_restart": 10000,
                "min_alive": {
                    'iva': 2
                }
            },
            "test-group-2@multdc": {
                "async_publishing_mode": "enabled",
                "simultaneous_restart": 1,
                "close_firewall_sleep": 10,
                "is_always_successful": False,
                "hosts": test_group_2_multdc_hosts,
                "failures_threshold": 3,
                "simultaneous_dc_restart": 10000,
                "min_alive": {
                    'iva': 2,
                    'sas': 3,
                }
            },
        }
    }


@pytest.fixture(scope='module')
def prod_publisher_config():
    prod_group_1_sas_hosts = dict()
    prod_group_1_sas_hosts.update(_generate_cluster_hosts(0, 'sas', 3))
    prod_group_1_sas_hosts.update(_generate_cluster_hosts(1, 'sas', 3))
    prod_group_1_sas_hosts.update(_generate_cluster_hosts(2, 'sas', 3))

    return {
        "download_timeout": "6000",
        "restart_timeout": "300",
        "reload_timeout": "900",
        "dcgroups": {
            "prod-group-1@sas": {
                "async_publishing_mode": "enabled",
                "simultaneous_restart": 1,
                "close_firewall_sleep": 10,
                "is_always_successful": False,
                "hosts": prod_group_1_sas_hosts,
                "failures_threshold": 2,
                "simultaneous_dc_restart": 10000,
                "min_alive": {
                    'sas': 2,
                }
            },
        }
    }


def test_config_generator_group_count(publisher_config):
    group_configs = GroupConfigCreator(1).generate(publisher_config)

    assert_that(group_configs, has_length(2))


@pytest.mark.parametrize("group,dc,min_alive_pr,expected", [
    ('test-group-1@iva', 'iva', 0, 0),
    ('test-group-2@multdc', 'iva', 0, 0),
    ('test-group-2@multdc', 'sas', 0, 0),
    ('test-group-1@iva', 'iva', 0.5, 2),
    ('test-group-2@multdc', 'sas', 0.5, 2),
    ('test-group-1@iva', 'iva', 1, 3),
    ('test-group-2@multdc', 'sas', 1, 4),
])
def test_config_generator_min_alive(publisher_config, group, dc, min_alive_pr, expected):
    """
    Проверка логики вычисления податацентрового вычисления min_alive
    """
    group_configs = GroupConfigCreator(min_alive_pr).generate(publisher_config)

    group = group_configs[group]
    assert_that(type(group.min_alive), dict)
    assert_that(group.min_alive[dc], equal_to(expected))


@pytest.mark.parametrize("group,dc,expected", [
    ('test-group-1@iva', 'iva', 2),
    ('test-group-2@multdc', 'iva', 2),
    ('test-group-2@multdc', 'sas', 3),
])
def test_min_alive_default_value(publisher_config, group, dc, expected):
    """
    Проверяем, что по умолчанию значение min_alive берется из конфига паблишера
    """
    group_configs = GroupConfigCreator().generate(publisher_config)

    group = group_configs[group]
    assert_that(group.min_alive[dc], equal_to(expected))


@pytest.mark.parametrize("filtered_group, expected_group", [
    (['test-group-1@iva'], ['test-group-1@iva']),
    (['test-group-1@iva', 'test-group-2@multdc'], ['test-group-1@iva', 'test-group-2@multdc']),
    ([], ['test-group-1@iva', 'test-group-2@multdc']),
])
def test_group_filter(publisher_config, filtered_group, expected_group):
    """
    Проверяем, что выставив фильтр по группе, в итоговом конфиге останутся только группы из списка
    """
    group_configs = GroupConfigCreator(1).generate(publisher_config, group_filter=filtered_group)

    assert_that(list(group_configs.keys()), equal_to(expected_group))


@pytest.mark.parametrize("filter_dc, dc, group", [
    ('iva', 'iva', 'test-group-1@iva'),
    ('sas', 'sas', 'test-group-2@multdc'),
])
def test_filter_by_dc_positive(publisher_config, filter_dc, dc, group):
    """
    Позитивный тест за фильтруцию по дц.
    Проверяем что в конечном конфиге будут хосты из кластера по которому фильтрация
    """
    group_configs = GroupConfigCreator(1).generate(publisher_config, dc_filter=[filter_dc])
    group = group_configs[group]

    assert_that([host.datacenter for host in group.hosts], has_item(dc))


@pytest.mark.parametrize("filter_dc, dc, group", [
    ('iva', 'sas', 'test-group-1@iva'),
    ('sas', 'iva', 'test-group-2@multdc'),
    ('iva', 'sas', 'test-group-2@multdc'),
])
def test_filter_by_dc_negative(publisher_config, filter_dc, dc, group):
    """
    Негативный тест для фильтрацию по дц. Убаждаемся, что хосты дц не из списка не попадают в итоговый конфиг
    """
    group_configs = GroupConfigCreator(1).generate(publisher_config, dc_filter=[filter_dc])
    group = group_configs[group]

    assert_that([host.datacenter for host in group.hosts], is_not(has_item(dc)))


@pytest.mark.parametrize("filter_dcs, expected_groups", [
    (['iva'], ['test-group-1@iva', 'test-group-2@multdc']),
    (['sas'], ['test-group-2@multdc']),
    (['sas', 'iva'], ['test-group-1@iva', 'test-group-2@multdc']),
    ([], ['test-group-1@iva', 'test-group-2@multdc']),
])
def test_group_contains_filter_dc(publisher_config, filter_dcs, expected_groups):
    """
    Проверяем, что в случае если пои фильтрации по дц в группе не осталось ни одного хоста,
    то группы не пападает в конфиг
    """
    group_configs = GroupConfigCreator(1).generate(publisher_config, dc_filter=filter_dcs)

    assert_that(list(group_configs.keys()), equal_to(expected_groups))


def test_filter_by_cluster(publisher_config):
    """
    Проверяем, что фильтрации по номеру кластера оставляет хосты только этого кластера
    """
    group_name = 'test-group-2@multdc'
    cluster_id = '1'
    group_configs = GroupConfigCreator(1).generate(
        publisher_config,
        group_filter=[group_name],
        cluster_filter=[cluster_id]
    )

    group = group_configs[group_name]
    assert_that([host.cluster_id for host in group.hosts], only_contains(cluster_id))


def test_empty_filter_by_cluster(publisher_config):
    """
    Проверяем, что если списко фильтрации по кластеру пустой, но этот фильтр не учитывается
    """
    group_name = 'test-group-2@multdc'
    group_configs = GroupConfigCreator(1).generate(
        publisher_config,
        group_filter=[group_name],
        cluster_filter=[]
    )

    group = group_configs[group_name]
    assert_that(group.hosts, has_length(24))


def test_filter_cluster_without_group(publisher_config):
    """
    Проверяем, что в случае если фильтр кластера задан, но не задана группа, то это приводит к исключению
    """

    def call():
        gen = GroupConfigCreator(1)
        gen.generate(publisher_config, cluster_filter=[1, 2])

    assert_that(calling(call), raises(GroupConfigGenerateError))


def test_all_filter(publisher_config):
    """
    Тест на работу всхе фильтров одновременно
    """
    group_name = 'test-group-1@iva'
    dc_name = 'iva'
    cluster_id = '0'
    generation = '20180723_1805'

    group_configs = GroupConfigCreator(0.5).generate(
        publisher_config,
        group_filter=[group_name],
        dc_filter=[dc_name],
        cluster_filter=[cluster_id],
        full_generation_meta=generation
    )

    expected = GroupConfig(simultaneous_restart=1,
                           failures_threshold=2,
                           hosts=(
                               GroupConfig.Host(fqdn='iva-0-0', cluster_id=0, datacenter='iva', port=9002),
                               GroupConfig.Host(fqdn='iva-0-1', cluster_id=0, datacenter='iva', port=9002),
                               GroupConfig.Host(fqdn='iva-0-2', cluster_id=0, datacenter='iva', port=9002),
                           ),
                           reload_timeout="900",
                           async_publishing=AsyncPublishingMode.enabled,
                           min_alive={
                               'iva': 1
                           },
                           full_generation_meta=generation
                           )

    assert_that(group_configs[group_name], expected)


@pytest.mark.parametrize("group, expected", [
    ('test-group-1@iva', 2),
    ('test-group-2@multdc', 3)
])
def test_failures_threshold_default(publisher_config, group, expected):
    """
    Проверяем что по умолчанию  failures_threshold копируется из изначального конфига
    """
    group_configs = GroupConfigCreator(1).generate(publisher_config)

    assert_that(group_configs[group].failures_threshold, equal_to(expected))


@pytest.mark.parametrize("pr, group, expected", [
    (1, 'test-group-1@iva', 3),
    (1, 'test-group-2@multdc', 6),
    (0.75, 'test-group-1@iva', 2),
    # (0.75, 'test-group-2@multdc', 5),  Флапает на 2ом и 3ем питоне из-за особенностей округления
    (0.5, 'test-group-1@iva', 2),
    (0.5, 'test-group-2@multdc', 3),
    (0, 'test-group-1@iva', 0),
    (0, 'test-group-2@multdc', 0),
])
def test_failures_threshold(publisher_config, pr, group, expected):
    """
    Проверяем логику работы генератора  по вычислению failures_threshold
    по проценту от бщего количества кластеров в группе
    """
    group_configs = GroupConfigCreator(1, failures_threshold_pr=pr).generate(publisher_config)

    assert_that(group_configs[group].failures_threshold, equal_to(expected))


def test_generator_result(publisher_config):
    """
    Проверяем что генератов хранит словарь из  GroupConfig
    """
    group_configs = GroupConfigCreator().generate(publisher_config)

    assert_that(group_configs.values(), only_contains(instance_of(GroupConfig)))


@pytest.mark.parametrize("mode", [
    AsyncPublishingMode.dry_run,
    AsyncPublishingMode.enabled,
    AsyncPublishingMode.disabled
])
def test_generator_mode(publisher_config, mode):
    """
    Проверяем выставление mode
    """
    group_configs = GroupConfigCreator().generate(publisher_config, mode=mode)

    assert_that([group.async_publishing for group in group_configs.values()], only_contains(equal_to(mode)))


def test_generator_no_generation(publisher_config):
    """
    Проверяем выставление generation
    """
    group_configs = GroupConfigCreator().generate(publisher_config)

    assert_that([group.full_generation for group in group_configs.values()], only_contains(none()))


def test_generator_with_generation(publisher_config):
    """
    Если поколение не указано то это поле содежрижт None
    """
    generation = GenerationMeta('20180723_1755', torrent_server_host='mi01ht.market.yandex.net')
    group_configs = GroupConfigCreator().generate(publisher_config, full_generation_meta=generation)

    assert_that([group.full_generation for group in group_configs.values()], only_contains(equal_to(generation)))


def test_generator_no_packages(publisher_config):
    """
    Если пакеты не указаны то поле packages None
    """
    group_configs = GroupConfigCreator().generate(publisher_config)

    assert_that([group.packages for group in group_configs.values()], only_contains(none()))


@pytest.mark.parametrize("packages", [
    {'report': '2018.3.85.0',  'dssm': '0.3673241', 'formulas': '0.3831296'},
    {'report': '2018.3.86.0'},
])
def test_generator_with_packages(publisher_config, packages):
    """
    Проверям запись в поле packages
    """
    environment = 'testing'

    meta = {environment: [PackageMeta(version=version, name=name) for (name, version) in packages.items()]}

    group_configs = GroupConfigCreator().generate(
        publisher_config,
        packages_meta=meta
    )
    assert_that([group.packages for group in group_configs.values()], only_contains(equal_to(meta)))


def test_make_groups_configs(publisher_config):
    """
    Проверяем работу make_groups_config в простом варианте
    """

    group_name = 'test-group-2@multdc'
    group_configs = make_groups_config(
        groups=[group_name],
        cluster_id=None,
        datacenter=None,
        failures_threshold=0,
        generation='20200225_2120',
        min_alive=1,
        mode='enabled',
        package=None,
        publisher_config=publisher_config
    )

    group = group_configs[group_name]
    assert_that(group.hosts, has_length(24))


@pytest.mark.parametrize("packages", [
    [('report', '2018.3.85.0'),  ('dssm', '0.3673241'), ('formulas', '0.3831296')],
    [('report', '2018.3.86.0')],
])
def test_make_groups_configs_with_packages(prod_publisher_config, packages):
    """
    Проверяем работу make_groups_config с пакетами
    """

    group_name = 'prod-group-1@sas'
    group_configs = make_groups_config(
        groups=[group_name],
        cluster_id=None,
        datacenter=None,
        failures_threshold=0,
        generation='20200225_2120',
        min_alive=1,
        mode='enabled',
        package=packages,
        publisher_config=prod_publisher_config,
        environment='production'
    )

    meta = {env: [PackageMeta(version=version, name=name) for (name, version) in packages] for env in ['production', 'prestable']}
    assert_that(group_configs[group_name].packages, equal_to(meta))
