# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    apply_processors, GeosearchState, GeosearchResult
)
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import (
    create_testing_input_search_context, create_testing_point_list
)


class TestParseGeosearchContext(TestCase):
    """
    Тесты на последовательную обработку PоintList'ов списком "процессоров".
    """
    def test_apply_processors_without_errors(self):
        """
        Во время обработки не возникло ошибок.
        """
        input_context = create_testing_input_search_context()
        point_from = create_settlement()
        point_to = create_settlement()

        def process1(state):
            return GeosearchState(state.input_context, create_testing_point_list(), create_testing_point_list())

        def process2(state):
            return state

        def process3(state):
            return GeosearchState(state.input_context,
                                  create_testing_point_list(point_from),
                                  create_testing_point_list(point_to))

        result_state = apply_processors(GeosearchState(input_context), [process1, process2, process3])
        result = GeosearchResult(result_state, input_context)

        assert result.input_context == input_context
        assert result.point_from == point_from
        assert result.point_to == point_to
        assert result.errors == []

    def test_parse_geosearch_context_with_errors(self):
        """
        Во время обработки на одном из промежуточных шагов возникла ошибка.
        """
        input_context = create_testing_input_search_context()
        point_from = create_settlement()
        point_to = create_settlement()

        def process1(state):
            return GeosearchState(state.input_context, create_testing_point_list(), create_testing_point_list())

        def process2(state):
            return state

        def process3(state):
            return GeosearchState(state.input_context, errors='errors')

        def process4(state):
            return GeosearchState(state.input_context,
                                  create_testing_point_list(point_from),
                                  create_testing_point_list(point_to))

        result_state = apply_processors(GeosearchState(input_context), [process1, process2, process3, process4])
        result = GeosearchResult(result_state, input_context)

        assert result.input_context == input_context
        assert result.point_from is None
        assert result.point_to is None
        assert result.errors == 'errors'

    def test_stop_processing(self):
        input_context = create_testing_input_search_context()

        def process1(state):
            return GeosearchState(state.input_context, create_testing_point_list(), create_testing_point_list())

        def process2(state):
            new_state = GeosearchState(state.input_context, create_testing_point_list(), create_testing_point_list())
            new_state.errors = [{'some_key': 'some_error'}]
            return new_state

        def process3(state):
            assert False

        apply_processors(GeosearchState(input_context), [process1, process2, process3])
