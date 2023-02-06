from enum import Enum
from typing import Dict

from bot.aiobeholder import IncidentAction, FinishPermission
from bot.modules.protocols.models import Protocol

TIncidentKey = str


class IncidentLogAction(Enum):
    ACCEPT_DEVOPS = 'ACCEPT_DEVOPS'
    ACCEPT_MARTY = 'ACCEPT_MARTY'
    FINISHED = 'FINISHED'


class MockBeholderIncidents:
    def __init__(self, *args, **kwargs):
        self._log_incidents: Dict[TIncidentKey, IncidentLogAction] = dict()

    async def incident_action(self, context, proto: Protocol, action: IncidentAction):
        assert proto.incident_id

        for a, log in (
            (IncidentAction.DEVOPS_ACCEPT, IncidentLogAction.ACCEPT_DEVOPS),
            (IncidentAction.MARTY_ACCEPT, IncidentLogAction.ACCEPT_MARTY),
        ):
            if a == action:
                self._log_incidents[proto.incident_id] = log
                break

        return await proto.to_json(context)

    async def finish_incident(self, context, proto: Protocol) -> FinishPermission:
        assert proto.incident_id

        self._log_incidents[proto.incident_id] = IncidentLogAction.FINISHED
        return FinishPermission(True)

    def assert_marty_accepted_incident(self, incident_id: str):
        self._assert_incident(incident_id, IncidentLogAction.ACCEPT_MARTY)

    def assert_devops_accepted_incident(self, incident_id: str):
        self._assert_incident(incident_id, IncidentLogAction.ACCEPT_DEVOPS)

    def assert_finished_incident(self, incident_id: str):
        self._assert_incident(incident_id, IncidentLogAction.FINISHED)

    def _assert_incident(self, incident_id: str, action: IncidentLogAction):
        assert self._log_incidents.get(incident_id) == action
