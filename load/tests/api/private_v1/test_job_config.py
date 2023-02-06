import pytest
from pytest import mark
from load.projects.cloud.loadtesting.db import DB
from load.projects.cloud.loadtesting.server.api.private_v1.job_config import JobConfig
import yandex.cloud.priv.loadtesting.v1.tank_job_pb2 as job_messages
import yandex.cloud.priv.loadtesting.v1.tank_job_service_pb2 as service_messages


@mark.usefixtures('patch_db_get_ammo')
@pytest.mark.parametrize('input_duration', [
    '{duration: 30s, type: step, from: 10, to:100, step: 5}',
    '{duration: 30s, type: step, from: 10, to: 100, step: 5}'
])
def test_spaces_in_schedule(input_duration):
    schedule = job_messages.Schedule(
        load_profile=[input_duration]
        )
    request = service_messages.CreateTankJobRequest(
        folder_id='1111',
        ammo_id='ammo_id',
        load_schedule=schedule,
        ammo_type=job_messages.AmmoType.URI,
        generator=job_messages.TankJob.Generator.PANDORA
    )
    db = DB()
    config = JobConfig()
    config.make_from_scratch(db, request)
    result_schedule = config.content['pandora']['config_content']['pools'][0]['rps'][0]
    assert not config.error
    assert result_schedule['to'] == 100


@mark.usefixtures('patch_db_get_ammo')
@pytest.mark.parametrize('ssl', [True, False])
def test_ssl(ssl):
    request = service_messages.CreateTankJobRequest(
        folder_id='1111',
        ammo_id='ammo_id',
        ammo_type=job_messages.AmmoType.URI,
        generator=job_messages.TankJob.Generator.PANDORA,
        ssl=ssl
    )
    db = DB()
    config = JobConfig()
    config.make_from_scratch(db, request)
    assert not config.error
    assert config.content['pandora']['config_content']['pools'][0]['gun']['ssl'] is ssl


@mark.usefixtures('patch_db_get_ammo')
@pytest.mark.parametrize('generator', [
    job_messages.TankJob.Generator.PANDORA,
    job_messages.TankJob.Generator.PHANTOM
])
def test_uris_and_headers(generator):
    uris = ['/1']
    headers = ['Host: myhost.py', 'Connection: Close']
    request = service_messages.CreateTankJobRequest(
        folder_id='1111',
        ammo_type=job_messages.AmmoType.URI,
        generator=generator,
        ammo_urls=uris,
        ammo_headers=headers
    )
    db = DB()
    config = JobConfig()
    config.make_from_scratch(db, request)
    assert not config.error
    if config.content.get('pandora'):
        assert config.content['pandora']['config_content']['pools'][0]['ammo']['uris'] == uris
        assert config.content['pandora']['config_content']['pools'][0]['ammo']['headers'] == headers
    elif config.content.get('phantom'):
        assert config.content['phantom']['uris'] == uris
        assert config.content['phantom']['headers'] == headers
    else:
        raise AssertionError('No generator specified in result config')


@mark.usefixtures('patch_db_get_ammo')
def test_autostop():
    autostop_content = [
        {
            'autostop_type': job_messages.Autostop.AutostopType.HTTP,
            'autostop_criteria': '404, 10%, 12s'
        },
        {
            'autostop_type': job_messages.Autostop.AutostopType.HTTP,
            'autostop_criteria': '503, 10%, 12s',
            'autostop_case': 'first'
        },
    ]
    request = service_messages.CreateTankJobRequest(
        folder_id='yc.loadtesting.service-folder',
        ammo_id='ammo_id',
        ammo_type=job_messages.AmmoType.URI,
        generator=job_messages.TankJob.Generator.PANDORA,
        autostops=autostop_content
    )
    db = DB()
    config = JobConfig()
    config.make_from_scratch(db, request)
    assert not config.error
    assert config.content['autostop']['autostop'] == [
        'http(404, 10%, 12s)',
        'http(503, 10%, 12s)'
    ]
