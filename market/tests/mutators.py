from market.sre.tools.rtc.nanny.models.service import Service as RTCService  # noqa
from market.front.tools.service_updater.helpers import update_or_create_file_resource


def mutate_idempotent(rtc_service):  # type: (RTCService) -> None
    update_or_create_file_resource(rtc_service, 'experiments.json', 'updated')


def mutate_none(rtc_service):
    pass


def mutate_innacurate(rtc_service):  # type: (RTCService) -> None
    rtc_service.runtime_attrs.resources.static_files.append({
        'is_dynamic': False,
        'local_path': 'new path',
        'content': 'new content'
    })
