import typing

from search.martylib.protobuf_utils.repeated import replace_in_repeated
from search.martylib.core.logging_utils import configure_binlog
from search.martylib.test_utils import TestCase

from search.mon.tickenator_on_db.src.reducers.progress_steps.task_runners.base_create_task_runner import BaseCreateTaskStepRunner
from search.mon.warden.proto.structures import component_pb2, duty_pb2, owner_pb2
from search.mon.tickenator_on_db.proto.structures import manual_ticket_pb2


class TestAssigneeFollowersDuty(TestCase):

    @classmethod
    def setUpClass(cls):
        configure_binlog(
            'tickenator',
            loggers=('tickenator', 'martylib', 'zephyr'),
            stdout=True,
        )

    def _check_duty_assignee_followers(
        self,
        child_component: component_pb2.Component,
        parent_component: component_pb2.Component,
        correct_duty: typing.List[str],
        correct_assignee: str,
        correct_followers: typing.List[str],
        manual_ticket: typing.Optional[manual_ticket_pb2.ManualTicket] = None,
        functionality_duty: typing.Optional[typing.Iterable[duty_pb2.DutyRecord]] = None
    ):
        runner = BaseCreateTaskStepRunner()
        duty = runner._get_duty([parent_component, child_component], functionality_duty if functionality_duty else [], False)
        self.assertListEqual(duty, correct_duty)
        assignee, followers = runner._get_assignee_and_followers(parent_component, child_component, manual_ticket, duty)
        self.assertEqual(assignee, correct_assignee)
        self.assertListEqual(followers, correct_followers)

    def test_owners(self):
        child_component = component_pb2.Component(
            duty_list=component_pb2.DutyList(on_duty=[
                duty_pb2.OnDuty(login='a'), duty_pb2.OnDuty(login='b')
            ]),
            owner_list=[owner_pb2.Owner(login='a'), owner_pb2.Owner(login='c'), owner_pb2.Owner(login='d')]
        )
        parent_component = component_pb2.Component()

        replace_in_repeated(child_component.protocol_settings.tickenator_settings.incident_followers, ['b', 'e'])
        replace_in_repeated(parent_component.protocol_settings.tickenator_settings.incident_followers, ['f'])

        child_component.protocol_settings.assign_on_duty = True
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b'], 'a', ['b', 'e', 'a'])

        child_component.protocol_settings.add_owner_to_ticket_followers = True
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b'], 'a', ['b', 'c', 'd', 'e', 'a'])

        replace_in_repeated(child_component.protocol_settings.tickenator_settings.incident_followers, [])
        child_component.protocol_settings.assign_on_duty = False
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b'], 'a', ['c', 'd', 'f', 'a', 'b'])

        child_component = component_pb2.Component(
            duty_list=component_pb2.DutyList(on_duty=[
                duty_pb2.OnDuty(login='a'), duty_pb2.OnDuty(login='b'), duty_pb2.OnDuty(login='c')
            ])
        )
        parent_component = component_pb2.Component(
            owner_list=[owner_pb2.Owner(login='a'), owner_pb2.Owner(login='d'), owner_pb2.Owner(login='e')]
        )

        child_component.protocol_settings.assign_on_duty = True
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'a', ['b', 'c', 'a'])

        child_component.protocol_settings.add_owner_to_ticket_followers = True
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'a', ['b', 'c', 'a'])

        child_component.protocol_settings.add_owner_to_ticket_followers = False
        parent_component.protocol_settings.add_owner_to_ticket_followers = True
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'a', ['b', 'c', 'd', 'e', 'a'])

        child_component.protocol_settings.assign_on_duty = False
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'a', ['d', 'e', 'a', 'b', 'c'])

        manual_ticket = manual_ticket_pb2.ManualTicket()
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'a', ['d', 'e', 'a', 'b', 'c'], manual_ticket)

    def test_manual_ticket(self):
        child_component = component_pb2.Component(
            duty_list=component_pb2.DutyList(on_duty=[
                duty_pb2.OnDuty(login='a'), duty_pb2.OnDuty(login='b'), duty_pb2.OnDuty(login='c')
            ])
        )
        parent_component = component_pb2.Component(
            owner_list=[owner_pb2.Owner(login='a'), owner_pb2.Owner(login='d'), owner_pb2.Owner(login='e')]
        )
        manual_ticket = manual_ticket_pb2.ManualTicket(assignee='f')

        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'f', ['a', 'd', 'e', 'b', 'c'], manual_ticket)

        manual_ticket = manual_ticket_pb2.ManualTicket(assignee='f', followers=['j', 'h', 'i'])
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'f', ['j', 'h', 'i'], manual_ticket)

        manual_ticket = manual_ticket_pb2.ManualTicket(followers=['j', 'h', 'i'])
        self._check_duty_assignee_followers(child_component, parent_component, ['a', 'b', 'c'], 'a', ['j', 'h', 'i'], manual_ticket)
