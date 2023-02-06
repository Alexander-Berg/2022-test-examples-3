import collections

from search.morty.tests.utils.test_case import MortyTestCase

from search.morty.src.common.utils import dfs, topological_sort


class TestTimeLine(MortyTestCase):
    def test_dfs(self):
        deps = collections.defaultdict(list)
        deps['1'].extend(('2', '3'))
        deps['2'].append('3')
        deps['4'].append('5')

        used = set()
        order = []
        dfs('1', deps, used, order)

        assert used == {'1', '2', '3'}
        assert order == ['3', '2', '1']

    def test_topological_sort(self):
        deps = collections.defaultdict(list)
        deps['1'].extend(('2', '3'))
        deps['2'].append('3')
        deps['3'].append('5')
        deps['4'].append('5')
        deps['5'].append('6')

        assert topological_sort(deps) == ['4', '1', '2', '3', '5', '6']
    #
    # def test_required_locks(self):
    #     lock = resource_pb2.ResourceLock(
    #         resources=resource_pb2.ResourceList(
    #             objects=[
    #                 resource_pb2.Resource(
    #                     locations=[
    #                         abstract_pb2.Location.MAN,
    #                         abstract_pb2.Location.VLA,
    #                     ],
    #                 ),
    #                 resource_pb2.Resource(
    #                     verticals=['vertical'],
    #                 ),
    #             ],
    #         ),
    #     )
    #     expected = {
    #         ('vertical', '', '', ''),
    #         ('', abstract_pb2.Location.MAN, '', ''),
    #         ('', abstract_pb2.Location.VLA, '', ''),
    #     }
    #     assert required_locks(lock) == expected
    #
    #     lock = resource_pb2.ResourceLock(
    #         resources=resource_pb2.ResourceList(
    #             objects=[
    #                 resource_pb2.Resource(
    #                     verticals=['vertical'],
    #                     locations=[
    #                         abstract_pb2.Location.MAN,
    #                         abstract_pb2.Location.VLA,
    #                     ],
    #                 ),
    #             ],
    #         ),
    #     )
    #     only_used = {
    #         ('vertical', abstract_pb2.Location.MAN, '', ''),
    #         ('vertical', abstract_pb2.Location.VLA, '', ''),
    #     }
    #     assert required_locks(lock) == set(list(expected) + list(only_used))
    #     assert required_locks(lock, only_used=True) == only_used
