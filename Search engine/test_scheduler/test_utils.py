import uuid

from search.morty.tests.utils.test_case import MortyTestCase

from search.morty.proto.structures import rule_pb2, process_pb2

from search.morty.src.scheduler.utils import processes_ordered_graph, processes_iterator, subprocesses_iterator


class TestSchedulerUtils(MortyTestCase):
    def test_processes_iterator(self):
        processes = [
            process_pb2.Process(
                id=str(uuid.uuid4()),
                params=process_pb2.ProcessParams(
                    priority=20,
                ),
            ),
            process_pb2.Process(
                id=str(uuid.uuid4()),
                params=process_pb2.ProcessParams(
                    priority=10,
                ),
                start_at=0,
            ),
            process_pb2.Process(
                id=str(uuid.uuid4()),
                params=process_pb2.ProcessParams(
                    priority=10,
                ),
                start_at=10,
            ),
        ]

        for i, (pr, _) in enumerate(processes_iterator(processes, [])):
            assert pr.id == processes[i].id

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
                    id=processes[0].id,
                ),
            ],
        )
        processes[0].params.rule_aliases['test'] = '2'

        for i, (pr, _) in enumerate(processes_iterator(processes, [rule])):
            assert pr.id == processes[i + 1].id

        rule.processes.extend((process_pb2.Process(id=processes[2].id), ))
        processes[2].params.rule_aliases['test'] = '1'

        for i, (pr, _) in enumerate(processes_iterator(processes, [rule])):
            assert pr.id == processes[(i + 1) % 3].id

    def test_processes_ordered_graph(self):
        rule = rule_pb2.AppliedRule(
            id='test',
            type=rule_pb2.Rule.Type.ORDER,
            order=rule_pb2.OrderedPredicates(
                nodes=[
                    rule_pb2.OrderedPredicates.Node(id='2', required=['1']),
                    rule_pb2.OrderedPredicates.Node(id='3', required=['2']),
                    rule_pb2.OrderedPredicates.Node(id='4', required=['2']),
                    rule_pb2.OrderedPredicates.Node(id='5', required=['2']),
                    rule_pb2.OrderedPredicates.Node(id='6', required=['3', '4'], gap=100),
                ],
            ),
        )

        events = {
            str(i): process_pb2.Process(
                id=str(uuid.uuid4()),
                params=process_pb2.ProcessParams(
                    rule_aliases={rule.id: str(i)}
                )
            )
            for i in range(1, 7)
        }

        rule.processes.extend((events['4'], ))
        graph = processes_ordered_graph(events.values(), [rule])
        assert graph[events['4'].id] == {None: 0}

        rule.processes.extend((events['6'], ))
        graph = processes_ordered_graph(events.values(), [rule])
        assert graph[events['4'].id] == {None: 0}
        assert graph[events['6'].id] == {None: 100, events['4'].id: 100}

        rule.processes.extend((events['1'], ))
        graph = processes_ordered_graph(events.values(), [rule])
        assert graph[events['4'].id] == {None: 0}
        assert graph[events['6'].id] == {None: 100, events['4'].id: 100}
        assert graph[events['1'].id] == {}

        rule.processes.extend((events['5'], ))
        graph = processes_ordered_graph(events.values(), [rule])
        assert graph[events['4'].id] == {None: 0}
        assert graph[events['6'].id] == {None: 100, events['4'].id: 100}
        assert graph[events['1'].id] == {}
        assert graph[events['5'].id] == {None: 0}

        rule.processes.extend((events['2'], ))
        graph = processes_ordered_graph(events.values(), [rule])
        assert graph[events['4'].id] == {events['2'].id: 0}
        assert graph[events['6'].id] == {None: 100, events['4'].id: 100}
        assert graph[events['1'].id] == {}
        assert graph[events['5'].id] == {events['2'].id: 0}
        assert graph[events['2'].id] == {events['1'].id: 0}

        rule.processes.extend((events['3'], ))
        graph = processes_ordered_graph(events.values(), [rule])
        assert graph[events['4'].id] == {events['2'].id: 0}
        assert graph[events['6'].id] == {events['3'].id: 100, events['4'].id: 100}
        assert graph[events['1'].id] == {}
        assert graph[events['5'].id] == {events['2'].id: 0}
        assert graph[events['2'].id] == {events['1'].id: 0}
        assert graph[events['3'].id] == {events['2'].id: 0}

    def test_subprocesses_iterator(self):
        process = process_pb2.Process(
            subprocesses=[
                process_pb2.SubProcess(
                    id='1',
                ),
                process_pb2.SubProcess(
                    id='2',
                    required={'1': 0},
                ),
                process_pb2.SubProcess(
                    id='3',
                    required={'1': 0, '2': 0},
                ),
                process_pb2.SubProcess(
                    id='4',
                ),
                process_pb2.SubProcess(
                    id='5',
                    required={'4': 0, '3': 0},
                ),
                process_pb2.SubProcess(
                    id='6',
                    required={'5': 0},
                ),
            ],
        )

        assert list(idx for idx, _ in subprocesses_iterator(process)) == ['4', '1', '2', '3', '5', '6']
        assert list(deps for _, deps in subprocesses_iterator(process)) == [['5'], ['2', '3'], ['3'], ['5'], ['6'], []]
