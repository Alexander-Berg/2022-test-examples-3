import pytest

from crypta.lib.python.solomon.proto import alert_pb2
from crypta.spine.pushers.upload_solomon_alerts import lib


alert1_1 = alert_pb2.TAlert()
alert1_1.id = "alert_id_1"
alert1_1.projectId = "project_id_1"

alert1_2 = alert_pb2.TAlert()
alert1_2.id = "alert_id_2"
alert1_2.projectId = "project_id_1"

alert2_1 = alert_pb2.TAlert()
alert2_1.id = "alert_id_1"
alert2_1.projectId = "project_id_2"

alert3_1 = alert_pb2.TAlert()
alert3_1.id = "alert_id_1"
alert3_1.projectId = "project_id_3"


@pytest.mark.parametrize("alerts,result", [
    (
        [
            alert1_1
        ],
        {
            "project_id_1": {
                alert1_1.id: alert1_1
            }
        }
    ),
    (
        [
            alert1_1,
            alert1_2,
            alert2_1,
            alert3_1,
        ],
        {
            "project_id_1": {
                alert1_1.id: alert1_1,
                alert1_2.id: alert1_2,
            },
            "project_id_2": {
                alert2_1.id: alert2_1,
            },
            "project_id_3": {
                alert3_1.id: alert3_1,
            }
        }
    ),
])
def test_get_alerts_by_projects(alerts, result):
    assert result == lib.get_alerts_by_projects(alerts)


@pytest.mark.parametrize("alerts", [
    (
        [
            alert1_1,
            alert1_1
        ],
        [
            alert1_1,
            alert1_2,
            alert1_1
        ]
    )
])
@pytest.mark.xfail(raises=Exception)
def test_get_alerts_by_projects_duplicated(alerts, result):
    assert result == lib.get_alerts_by_projects(alerts)
