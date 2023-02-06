from market.sre.tools.rtc.nanny.models.service import Service as RTCService
from market.sre.tools.rtc.nanny.models.sandbox import SandboxResource


def create_service(instance_spec=None, resources=None, tickets=None):
    return RTCService.from_dict({
        "_id": 'some_test_service',
        "auth_attrs": {"_id": "000", "content": {}},
        "info_attrs": {
            "_id": "000",
            "content": {
                "tickets_integration": {
                    "service_release_rules": tickets
                }
            }
        },
        "runtime_attrs": {
            "_id": "000",
            "content": {
                "instance_spec": instance_spec,
                "resources": resources
            }
        },
        "current_state": {
            "_id": "000",
            "content": {
                "summary": {
                    "entered": 1587365593,
                    "value": "ONLINE"
                }
            }
        }
    })


def create_layer(resource_type, resource_id, task_id, task_type, url, id):
    return {
        'id': id,
        'url': url,
        'fetchableMeta': {'type': 'SANDBOX_RESOURCE', 'sandboxResource': {
            'resourceId': resource_id,
            'resourceType': resource_type,
            'taskId': task_id,
            'taskType': task_type,
        }}
    }


def create_sandbox_file(local_path, resource_type, resource_id, task_id, task_type):
    return {
        'local_path': local_path,
        'resource_type': resource_type,
        'resource_id': resource_id,
        'task_type': task_type,
        'task_id': task_id,
        'is_dynamic': False
    }


def create_sandbox_resource(resource_type, resource_id, task_id, task_type, url):
    return SandboxResource.from_dict({
        'type': resource_type, 'id': resource_id,
        'task': {'id': task_id, 'type': task_type},
        'skynet_id': url,
        'http': {},
    })


def create_static_file(local_path, content):
    return {'local_path': local_path, 'content': content, 'is_dynamic': False}


def create_ticket_integration(resource_type, task_type=None, description="desc", env_filter="stable", queue="MARKET"):
    return {
        "auto_commit_settings": {"enabled": False, "scheduling_priority": "NORMAL"},
        "responsibles": [],
        "ticket_priority": "NORMAL",
        "queue_id": queue,
        "sandbox_task_type": task_type,
        "sandbox_resource_type": resource_type,
        "desc": description,
        "filter_params": {"expression": 'sandbox_release.release_type in ("{}",)'.format(env_filter)},
    }
