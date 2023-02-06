import pytest
import yaml
import mock
import load.projects.tasklets.shooting.py_impl.tools as tools


@pytest.mark.parametrize('deploy_str, deploy_args', [
    (
        'yandex-tank-finder-production.tank-finder-production.man',
        'stage=yandex-tank-finder-production&unit=tank-finder-production&dc=man'
    ),
    (
        'yandex-tank-finder-production..man',
        'stage=yandex-tank-finder-production&unit=&dc=man'
    )])
def test_parse_deploy_string(deploy_str, deploy_args):
    assert tools._parse_deploy_string(deploy_str) == deploy_args


@pytest.mark.parametrize('deploy_str, err', [
    (
        '',
        'Wrong deploy parameters in target {}'
    ),
    (
        '.tank-finder-production.man',
        'Wrong deploy parameters in target {}'
    )])
def test_parse_deploy_string_exeption(deploy_str, err):
    with pytest.raises(Exception) as test_error:
        tools._parse_deploy_string(deploy_str)
    assert str(test_error.value) == err.format(deploy_str)


@pytest.mark.parametrize('deploy_dict, deploy_args', [
    (
        {'stage': 'yandex-tank-finder-production', 'deploy_unit': 'tank-finder-production', 'data_center': 'man'},
        'stage=yandex-tank-finder-production&unit=tank-finder-production&dc=man'
    ),
    (
        {'stage': 'yandex-tank-finder-production', 'data_center': 'man'},
        'stage=yandex-tank-finder-production&unit=&dc=man'
    )])
def test_parse_deploy_dict(deploy_dict, deploy_args):
    assert tools._parse_deploy_dict(deploy_dict) == deploy_args


@pytest.mark.parametrize('deploy_dict, err', [
    (
        {'deploy_unit': 'tank-finder-production', 'data_center': 'man'},
        'Deploy stage is not specified in target {}'
    )])
def test_parse_deploy_dict_exception(deploy_dict, err):
    with pytest.raises(Exception) as test_error:
        tools._parse_deploy_dict(deploy_dict)
    assert str(test_error.value) == err.format(deploy_dict)


def test_read_urlfile():
    assert tools.read_urlfile('https://proxy.sandbox.yandex-team.ru/1995696082') == bytes('Test complite\n', 'utf-8')


@pytest.mark.parametrize('url, err', [
    (
        'https://proxy.sandbox.yandex-team.ru/1945158277',
        'The link {} is incorrect or outdated'
    )])
def test_read_urlfile_exeption(url, err):
    with pytest.raises(Exception) as test_error:
        tools.read_urlfile(url)
    assert str(test_error.value) == err.format(url)


@pytest.mark.parametrize('ammo_attrs, attrs', [
    (
        'mama=anarhia papa=portvein',
        {'mama': 'anarhia', 'papa': 'portvein'}
    ),
    (
        'vosmiklasnitsa=',
        {'vosmiklasnitsa': ''}
    ),
    (
        'kamchatka',
        {}
    ),
    (
        666,
        {}
    ),
    (
        {'mama': 'anarhia', 'papa': 'portvein'},
        {}
    )])
def test_get_attrs(ammo_attrs, attrs):
    assert tools.get_attrs(ammo_attrs) == attrs


CONFIGS = [
    (
        '''
        pandora:
            enabled: false
            package: yandextank.plugins.Pandora
            pandora_cmd: /usr/local/bin/pandora
        phantom:
            enabled: true
            address:
                stage: Yandextank_manual-testing
                deploy_unit: target
                data_center: myt
                port: 80
            ammofile: sandbox.AMMO_FILE
            ammo_type: uripost
            ammo_attrs: 'ammo_format=uri ammo_label=tasklet'
            load_profile: {load_type: rps, schedule: 'line(1,2,60s)'}
            multi:
              - address: deploy:taskarbiter-loadtesting.antimalware.sas:20051
                ammofile: sandbox.AMMO_FILE
                ammo_type: uripost
              - address: nanny:overload_front_production:12674
                ammofile: https://proxy.sandbox.yandex-team.ru/2568633433
                ammo_attrs: 'ammo_format=uri ammo_label=tasklet'
        uploader:
            enabled: true
            package: yandextank.plugins.DataUploader
            meta:
                use_tank:
                    data_center: vla
                    deploy_unit: main-deploy-unit
                    port: 8083
                    stage: antimalware-tank
            operator: lunapark
            task: LOAD-666
        metaconf:
            enabled: true
            package: yandextank.plugins.MetaConf
            firestarter:
                tank: nanny:production_yandex.net
                target: deploy:test-stage.target
                target_port: 9876
        ''',
        '''
        pandora:
            enabled: false
            package: yandextank.plugins.Pandora
            pandora_cmd: /usr/local/bin/pandora
        phantom:
            enabled: true
            address: deploy.host.yandex.net:80
            ammofile: https://storage-int.mds.yandex.net/test/123
            ammo_type: uripost
            load_profile: {load_type: rps, schedule: 'line(1,2,60s)'}
            multi:
              - address: deploy.host.yandex.net:80
                ammofile: https://storage-int.mds.yandex.net/test/123
                ammo_type: uripost
              - address: nanny.host.yandex.net:443
                ammofile: https://proxy.sandbox.yandex-team.ru/2568633433
        uploader:
            enabled: true
            package: yandextank.plugins.DataUploader
            meta:
                use_tank: deploy.host.yandex.net:80
            operator: lunapark
            task: LOAD-666
        metaconf:
            enabled: true
            package: yandextank.plugins.MetaConf
            firestarter:
                tank: nanny:production_yandex.net
                target: deploy:test-stage.target
                target_port: 9876
        '''
    ),
    (
        '''
        pandora:
            enabled: true
            package: yandextank.plugins.Pandora
            pandora_cmd: https://proxy.sandbox.yandex-team.ru/2753379247
            config_content:
                log:
                    level: info
                monitoring:
                    expvar:
                        enabled: true
                        port: 1234
                pools:
                  - id: GetTask
                    ammo:
                        source:
                            path: ./ammo.json
                            type: file
                        type: taskarbiter-ammo-json
                    gun:
                        target: deploy:taskarbiter-loadtesting.antimalware.sas:20051
                        testcase: unbalanced-host-groups-only
                        type: taskarbiter-gun
                    result:
                        destination: ./phout.log
                        type: phout
                    rps:
                      - duration: 5m
                        ops: 3000
                        type: const
                    startup:
                        times: 5000
                        type: once
                  - id: Pool2
                    gun:
                        target: deploy:taskarbiter-loadtesting.antimalware.sas:20051
                        testcase: balanced-host-groups-only
                        type: taskarbiter-gun
                  - id: Pool3
                    gun:
                        target: nanny:maps_core_front_production
                        testcase: nobalanced-host-groups
                        type: taskarbiter-gun
            resources:
              - dst: ./ammo.json
                src: sandbox.AMMO_FILE
                ammo_attrs: 'ammo_format=uri ammo_label=tasklet'
              - dst: ./telegraf.xml
                src: https://proxy.sandbox.yandex-team.ru/2568633433
                ammo_attrs: 'ammo_format=xml ammo_label=telegraf'
        phantom:
            enabled: false
            address: deploy:target.yandex.myt:80
            ammofile: https://storage-int.mds.yandex.net/test/123
            ammo_type: uripost
            load_profile: {load_type: rps, schedule: 'line(1,2,60s)'}
        uploader:
            enabled: true
            package: yandextank.plugins.DataUploader
            meta:
                use_tank: nanny:production_yandex_net
            operator: lunapark
            task: LOAD-666
        ''',
        '''
        pandora:
            enabled: true
            package: yandextank.plugins.Pandora
            pandora_cmd: https://proxy.sandbox.yandex-team.ru/2753379247
            config_content:
                log:
                    level: info
                monitoring:
                    expvar:
                        enabled: true
                        port: 1234
                pools:
                  - id: GetTask
                    ammo:
                        source:
                            path: ./ammo.json
                            type: file
                        type: taskarbiter-ammo-json
                    gun:
                        target: deploy.host.yandex.net:80
                        testcase: unbalanced-host-groups-only
                        type: taskarbiter-gun
                    result:
                        destination: ./phout.log
                        type: phout
                    rps:
                      - duration: 5m
                        ops: 3000
                        type: const
                    startup:
                        times: 5000
                        type: once
                  - id: Pool2
                    gun:
                        target: deploy.host.yandex.net:80
                        testcase: balanced-host-groups-only
                        type: taskarbiter-gun
                  - id: Pool3
                    gun:
                        target: nanny.host.yandex.net:443
                        testcase: nobalanced-host-groups
                        type: taskarbiter-gun
            resources:
              - dst: ./ammo.json
                src: https://storage-int.mds.yandex.net/test/123
              - dst: ./telegraf.xml
                src: https://proxy.sandbox.yandex-team.ru/2568633433
        phantom:
            enabled: false
            address: deploy:target.yandex.myt:80
            ammofile: https://storage-int.mds.yandex.net/test/123
            ammo_type: uripost
            load_profile: {load_type: rps, schedule: 'line(1,2,60s)'}
        uploader:
            enabled: true
            package: yandextank.plugins.DataUploader
            meta:
                use_tank: nanny.host.yandex.net:443
            operator: lunapark
            task: LOAD-666
        '''
    ),
    (
        '''
        pandora:
            enabled: true
            package: yandextank.plugins.Pandora
            pandora_cmd: https://proxy.sandbox.yandex-team.ru/2753379247
            config_content:
                log:
                   level: info
                monitoring:
                    expvar:
                        enabled: true
                        port: 1234
                pools:
                  - id: GetTask
                    ammo:
                        source:
                            path: ./ammo.json
                            type: file
                        type: taskarbiter-ammo-json
                    gun:
                        target:
                            stage: taskarbiter-loadtesting
                            deploy_unit: antimalware
                            data_center: sas
                            port: 20051
                        testcase: unbalanced-host-groups-only
                        type: taskarbiter-gun
                    result:
                        destination: ./phout.log
                        type: phout
                    rps:
                      - duration: 5m
                        ops: 3000
                        type: const
                    startup:
                        times: 5000
                        type: once
                  - id: Pool2
                    gun:
                        target: nanny:overload_front_production:12674
                        testcase: balanced-host-groups-only
                        type: taskarbiter-gun
            resources:
              - dst: ./ammo.json
                src: sandbox.AMMO_FILE
                ammo_attrs: 'ammo_format=uri ammo_label=tasklet'
        phantom:
            enabled: true
            address:
                stage: Yandextank_manual-testing
                deploy_unit: target
                data_center: myt
                port: 80
            ammofile: sandbox.AMMO_FILE
            ammo_type: uripost
            ammo_attrs: 'ammo_format=uri ammo_label=tasklet'
            load_profile:
                load_type: rps
                schedule: 'line(1,2,60s)'
        uploader:
            enabled: true
            package: yandextank.plugins.DataUploader
            operator: lunapark
            task: LOAD-666
        ''',
        '''
        pandora:
            enabled: true
            package: yandextank.plugins.Pandora
            pandora_cmd: https://proxy.sandbox.yandex-team.ru/2753379247
            config_content:
                log:
                    level: info
                monitoring:
                    expvar:
                        enabled: true
                        port: 1234
                pools:
                  - id: GetTask
                    ammo:
                        source:
                            path: ./ammo.json
                            type: file
                        type: taskarbiter-ammo-json
                    gun:
                        target: deploy.host.yandex.net:80
                        testcase: unbalanced-host-groups-only
                        type: taskarbiter-gun
                    result:
                        destination: ./phout.log
                        type: phout
                    rps:
                      - duration: 5m
                        ops: 3000
                        type: const
                    startup:
                        times: 5000
                        type: once
                  - id: Pool2
                    gun:
                        target: nanny.host.yandex.net:443
                        testcase: balanced-host-groups-only
                        type: taskarbiter-gun
            resources:
              - dst: ./ammo.json
                src: https://storage-int.mds.yandex.net/test/123
        phantom:
            enabled: true
            address: deploy.host.yandex.net:80
            ammofile: https://storage-int.mds.yandex.net/test/123
            ammo_type: uripost
            load_profile:
                load_type: rps
                schedule: 'line(1,2,60s)'
        uploader:
            enabled: true
            package: yandextank.plugins.DataUploader
            operator: lunapark
            task: LOAD-666
        metaconf:
            enabled: true
            package: yandextank.plugins.MetaConf
            firestarter:
                tank: common
        '''
    )
]


@pytest.mark.parametrize('start_config, finish_config', CONFIGS)
@mock.patch('load.projects.tasklets.shooting.py_impl.tools.get_resource_url', return_value='https://storage-int.mds.yandex.net/test/123')
@mock.patch('load.projects.tasklets.shooting.py_impl.tools.get_deploy_target', return_value='deploy.host.yandex.net:80')
@mock.patch('load.projects.tasklets.shooting.py_impl.tools.get_nanny_target', return_value='nanny.host.yandex.net:443')
def test_prepare_config(get_nanny_target, get_deploy_target, get_resource_url, start_config, finish_config):

    prepared_config = tools.prepare_config(yaml.safe_load(start_config))
    assert prepared_config == yaml.safe_load(finish_config)


@pytest.mark.parametrize('start_config, option, finish_config', [
    (
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}},
        'uploader.task=LOAD-777',
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'LOAD-777', 'operator': 'user'}}
    ),
    (
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}},
        'phantom.multi.0.address=Rio-de-Janeiro',
        {'phantom': {'multi': [{'address': 'Rio-de-Janeiro'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}}
    ),
    (
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}},
        'phantom.load_profile.0=const(4,4)',
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['const(4,4)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}}
    )])
def test_apply_option(start_config, option, finish_config):
    assert tools.apply_option(start_config, option) == finish_config


@pytest.mark.parametrize('config, option, error', [
    (
        [{'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}}],
        'uploader.task=LOAD-777',
        'uploader is not suitable as a list index it must be an int'
    ),
    (
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}},
        'phantom.multi.0.0.address=Rio-de-Janeiro',
        'Double nested list is not supported in tank configuration'
    ),
    (
        {'phantom': {'multi': [{'address': 'test'}], 'load_profile': ['line(1,2,3)']}, 'uploader': {'task': 'TEST-111', 'operator': 'user'}},
        'phantom.load_profile.3=const(4,4)',
        'The index of the updated element is beyond the scope of the possible'
    ),
    (
        123,
        'uploader.operator=lunapark',
        'Configuration 123 has unsupported format'
    )])
def test_apply_option_raise_error(config, option, error):
    with pytest.raises(tools.ShootingError) as tse:
        tools.apply_option(config, option)
    assert tse.type is tools.ShootingError
    assert tse.value.txt == error
