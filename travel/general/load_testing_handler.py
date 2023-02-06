from typing import Any, Dict, Optional, Sequence

from travel.rasp.deploy_notifier.notifier import Event
from travel.rasp.deploy_notifier.notifier.handler import BaseHandler
from travel.rasp.deploy_notifier.notifier.utils import LoggingBase
from travel.rasp.deploy_notifier.notifier.utils.api_clients import SandboxAPI


DEPLOYED_STATUS = 'DEPLOYED'


class LoadTestingHandler(BaseHandler, LoggingBase):
    def __init__(self, token: str, owner: str, sandbox_task_type: str,
                 additional_params: Optional[Sequence[Dict[str, Any]]]=None) -> None:
        super().__init__()
        self._api_client = SandboxAPI(token)
        self._sandbox_task_type = sandbox_task_type
        self._additional_params = additional_params if additional_params else []
        self._owner = owner

    async def handle_qloud(self, event: Event) -> None:
        pass

    async def handle_http(self, event: Event) -> None:
        self._logger.info(f"LoadTestingHandler.handle_http event: {event}")
        if event.data.get('status') != DEPLOYED_STATUS:
            return
        result = await self._api_client.create_task(
            self._sandbox_task_type,
            description='Running from deploy-notifier. Comment:\n' + event.data.get('comment', ''),
            owner=self._owner,
            custom_fields=self._additional_params + [{
                'name': 'applications_to_run',
                'value': [event.data['environmentId'].split('.')[1]]
            }],
        )
        await self._api_client.start_tasks([result['id']])

    async def start(self):
        pass
