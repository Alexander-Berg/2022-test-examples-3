from market.front.tools.service_updater.helpers import find_tickets_integration, \
    update_tickets_integration, update_or_create_tickets_integration, remove_ticket_integration
from .assert_idempotent import assert_idempotent
from .assert_json_equal import assert_json_equal
from .mocks import create_service, create_ticket_integration


def test_find_tickets_integration():
    first_rule = create_ticket_integration('first_resource')
    second_rule = create_ticket_integration('second_resource', 'second_task')

    service = create_service(tickets=[first_rule, second_rule])

    assert find_tickets_integration(service, lambda _, resource: resource == 'first_resource') == first_rule
    assert find_tickets_integration(service, lambda task, _: task == 'second_task') == second_rule


def test_update_tickets_integration():
    first_rule = create_ticket_integration('first_resource')
    second_rule = create_ticket_integration('second_resource', 'second_task')

    service = create_service(tickets=[first_rule, second_rule])

    assert_idempotent(service, lambda: update_tickets_integration(
        service, lambda _, resource: resource == 'first_resource',
        'first_task', 'first_resource', 'modified_desc', 'testing', True, 'NEW_QUEUE'
    ))

    assert_idempotent(service, lambda: update_tickets_integration(
        service, lambda task, _: task == 'second_task',
        'second_task', 'second_resource_upd', 'modified_desc2', 'testing2', True, 'NEW_QUEUE2'
    ))

    update_expected = {'update': {
        'tickets_integration': {
            "update": {
                'service_release_rules': {
                    '0': {"update": {
                        'auto_commit_settings': {
                            'insert': {'mark_as_disposable': True},
                            'update': {'enabled': True}}, 'filter_params': {
                            'update': {
                                'expression': 'sandbox_release.release_type in ("testing",)'}
                        },
                        'queue_id': 'NEW_QUEUE', 'sandbox_task_type': 'first_task',
                        'desc': 'modified_desc'}},
                    '1': {
                        "update": {
                            'filter_params': {
                                'update': {
                                    'expression': 'sandbox_release.release_type in ("testing2",)'}
                            },
                            'queue_id': 'NEW_QUEUE2',
                            'auto_commit_settings': {
                                'insert': {'mark_as_disposable': True},
                                'update': {'enabled': True}},
                            'sandbox_resource_type': 'second_resource_upd',
                            'desc': 'modified_desc2'}
                    }
                }
            }
        }
    }
    }
    assert_json_equal(update_expected, service.info_attrs.diff)


def test_update_or_create_tickets_integration():
    first_rule = create_ticket_integration('first_resource')
    second_rule = create_ticket_integration('second_resource', 'second_task')

    service = create_service(tickets=[first_rule, second_rule])

    assert_idempotent(service, lambda: update_or_create_tickets_integration(
        service, lambda _, resource: resource == 'first_resource',
        'first_task', 'first_resource', 'modified_desc', 'testing', True, 'NEW_QUEUE'
    ))

    assert_idempotent(service, lambda: update_or_create_tickets_integration(
        service, lambda _, resource: resource == 'third_resource',
        'third_task', 'third_resource', 'description 3', 'testing', True, 'QUEUE3'
    ))
    update_expected = {'update': {'tickets_integration': {
        'update': {'service_release_rules': {
            '0': {
                'update': {
                    'filter_params': {
                        'update': {'expression': 'sandbox_release.release_type in ("testing",)'}},
                    'sandbox_task_type': 'first_task', 'auto_commit_settings': {
                        'insert': {'mark_as_disposable': True},
                        'update': {'enabled': True}},
                    'desc': 'modified_desc'}},
            'insert': [(2, {
                'filter_params': {
                    'expression': 'sandbox_release.release_type in ("testing",)'},
                'sandbox_resource_type': 'third_resource', 'queue_id': 'MARKET', 'ticket_priority': 'NORMAL',
                'desc': 'description 3',
                'sandbox_task_type': 'third_task', 'auto_commit_settings': {
                    'mark_as_disposable': True,
                    'scheduling_priority': 'NORMAL',
                    'enabled': True},
                'responsibles': []})]}}}}}

    assert_json_equal(update_expected, service.info_attrs.diff)


def test_remove_ticket_integration():
    first_rule = create_ticket_integration('first_resource')
    second_rule = create_ticket_integration('second_resource', 'second_task')

    service = create_service(tickets=[first_rule, second_rule])

    assert_idempotent(service, lambda: remove_ticket_integration(
        service, lambda _, resource: resource == 'first_resource'
    ))
    update_expected = {'update': {'tickets_integration': {'update': {'service_release_rules': {'delete': [0]}}}}}

    assert_json_equal(update_expected, service.info_attrs.diff)
