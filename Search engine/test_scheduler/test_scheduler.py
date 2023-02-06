import uuid

from search.martylib.db_utils import session_scope, to_model
from search.martylib.core.date_utils import now, mock_now

from search.morty.proto.structures import rule_pb2, event_pb2, common_pb2, process_pb2, resource_pb2
from search.morty.sqla.morty import model

from search.morty.src.model.process.utils import get_default_process, get_default_subprocess
from search.morty.src.scheduler.scheduler import ProcessScheduler
from search.morty.src.scheduler.allocator import Allocator

from search.morty.tests.utils.test_case import MortyTestCase


class TestProcessScheduler(MortyTestCase):
    def test_run_once(self):
        scheduler = ProcessScheduler()
        now_ = int(now().timestamp())

        with session_scope() as session:
            event = event_pb2.Event(process=get_default_process())
            process = event.process
            process.subprocesses.extend((get_default_subprocess(), ))

            process.params.CopyFrom(
                process_pb2.ProcessParams(
                    priority=10,
                    created_at=now_,
                ),
            )
            process.subprocesses[0].lock.resources.objects.extend((
                resource_pb2.Resource(
                    verticals=('vertical', ),
                ),
            ))
            process.subprocesses[0].lock.duration = 300
            session.merge(to_model(event))

        scheduler.run_once()
        with session_scope() as session:
            process = session.query(model.Process).first().to_protobuf()
            assert process.lock.start > now_
            assert process.subprocesses[0].lock.start > now_

        with session_scope() as session:
            session.query(model.Process).update({'flags': common_pb2.ManipulationFlags(cancelled=True).SerializeToString()})
            session.commit()

            scheduler.run_once()
            assert session.query(model.Process).filter(model.Process.unusable.is_(True)).count() == 1

    def test_allocate_process(self):
        scheduler = ProcessScheduler()
        process = get_default_process()
        process.subprocesses.extend((get_default_subprocess(), ))
        process.subprocesses[0].lock.resources.objects.extend((
            resource_pb2.Resource(
                verticals=('vertical',),
            ),
        ))
        process.subprocesses[0].lock.duration = 300

        allocator = Allocator([])
        scheduled = scheduler.allocate_process(allocator, process)
        assert scheduled[process].start >= allocator.now

        # test cancelled
        allocator = Allocator([])
        process.flags.cancelled = True
        scheduled = scheduler.allocate_process(allocator, process)
        assert scheduled is None

    def test_allocate_group(self):
        scheduler = ProcessScheduler()
        processes = []
        for i in range(3):
            processes.append(get_default_process())
            processes[-1].subprocesses.extend((get_default_subprocess(),))
            processes[-1].subprocesses[0].lock.resources.objects.extend((
                resource_pb2.Resource(
                    verticals=(str(i), ),
                ),
            ))
            processes[-1].subprocesses[0].lock.duration = 300

        scheduler.allocate_group(processes, [])
        assert len({pr.lock.start for pr in processes}) == 1
        for pr in processes:
            assert pr.lock.start > 0

        # test cancelled
        processes[0].flags.cancelled = True
        scheduler.allocate_group(processes, [])
        assert len({pr.lock.start for pr in processes}) == 2
        assert processes[0].lock.start == 0
        assert processes[0].unusable is True

        # test order
        rule = rule_pb2.AppliedRule(
            id='test',
            type=rule_pb2.Rule.Type.ORDER,
            order=rule_pb2.OrderedPredicates(
                nodes=[
                    rule_pb2.OrderedPredicates.Node(id='2', required=['1']),
                ],
            ),
            processes=[
                process_pb2.Process(
                    id=processes[1].id,
                ),
                process_pb2.Process(
                    id=processes[2].id,
                ),
            ],
        )
        processes[1].params.rule_aliases['test'] = '1'
        processes[2].params.rule_aliases['test'] = '2'

        scheduler.allocate_group(processes, [rule])
        assert len({pr.lock.start for pr in processes}) == 3

    def test_status_update(self):
        with session_scope() as session:
            scheduler = ProcessScheduler()

            event = event_pb2.Event(process=get_default_process(), id=str(uuid.uuid4()))
            process = event.process

            process.state.execution_starts = True
            process.start_at = int(now().timestamp())
            process.flags.paused_until = sleep_until = process.start_at + 3 * 3600
            session.merge(to_model(event))

            scheduler.run_once()

            event: event_pb2.Event = session.query(model.Event).filter(model.Event.id == event.id).one().to_protobuf()
            assert event.status == event_pb2.Event.Status.PAUSED

            with mock_now(sleep_until + 10):
                scheduler.run_once()

            event: event_pb2.Event = session.query(model.Event).filter(model.Event.id == event.id).one().to_protobuf()
            assert event.status == event_pb2.Event.Status.IN_PROGRESS
