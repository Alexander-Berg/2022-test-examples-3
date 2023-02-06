import pytest

from load.projects.cloud.loadtesting.db.tables import AgentVersionTable, TankTable, STATUS_COMMENTS, AgentVersionStatus
from load.projects.cloud.loadtesting.server.api.private_v1.tank import get_ui_agent_version
from yandex.cloud.priv.loadtesting.v1 import tank_instance_pb2

VERSION_STATUS = tank_instance_pb2.AgentVersion.VersionStatus


@pytest.mark.parametrize(('case_description', 'db_tank', 'db_version', 'ui_version'), [
    ('UNSET (None)',
     TankTable(),
     None,
     tank_instance_pb2.AgentVersion(
         status=VERSION_STATUS.UNSET,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.UNSET.value]
     ),
     ),
    ('UNSET ("")',
     TankTable(agent_version=''),
     None,
     tank_instance_pb2.AgentVersion(
         status=VERSION_STATUS.UNSET,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.UNSET.value]
     ),
     ),
    ('UNKNOWN',
     TankTable(agent_version='111'),
     None,
     tank_instance_pb2.AgentVersion(
         status=VERSION_STATUS.UNKNOWN,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.UNKNOWN.value]
     ),
     ),
    ('OUTDATED',
     TankTable(agent_version='111'),
     AgentVersionTable(image_id='111', status='OUTDATED'),
     tank_instance_pb2.AgentVersion(
         id='111',
         status=VERSION_STATUS.OUTDATED,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.OUTDATED.value]
     )
     ),
    ('DEPRECATED',
     TankTable(agent_version='111'),
     AgentVersionTable(image_id='111', status='DEPRECATED'),
     tank_instance_pb2.AgentVersion(
         id='111',
         status=VERSION_STATUS.DEPRECATED,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.DEPRECATED.value]
     )
     ),
    ('ACTUAL common',
     TankTable(agent_version='111'),
     AgentVersionTable(image_id='111', status='ACTUAL'),
     tank_instance_pb2.AgentVersion(
         id='111',
         status=VERSION_STATUS.ACTUAL,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.ACTUAL.value]
     )
     ),
    ('ACTUAL target',
     TankTable(agent_version='111'),
     AgentVersionTable(image_id='111', status='TARGET'),
     tank_instance_pb2.AgentVersion(
         id='111',
         status=VERSION_STATUS.ACTUAL,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.ACTUAL.value]
     )
     ),
    ('TESTING',
     TankTable(agent_version='111'),
     AgentVersionTable(image_id='111', status='TESTING'),
     tank_instance_pb2.AgentVersion(
         id='111',
         status=VERSION_STATUS.TESTING,
         status_comment=STATUS_COMMENTS['ru'][AgentVersionStatus.TESTING.value]
     )
     ),
])
def test_ui_version(case_description, db_tank, db_version, ui_version):
    assert ui_version == get_ui_agent_version(db_tank, db_version), case_description


def test_ui_version_mismatch_version():
    with pytest.raises(AssertionError):
        get_ui_agent_version(TankTable(agent_version='111'), AgentVersionTable(image_id='112'))


def test_ui_version_mismatch_status():
    with pytest.raises(AssertionError):
        get_ui_agent_version(TankTable(agent_version='111'), AgentVersionTable(image_id='111', status='StAtUs'))
