# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
    is_not,
    has_entries
)

from market.idx.publication.publisher_daemon.backend.reload.reload_logic import State
from market.idx.publication.publisher_daemon.backend.reload.fsm import (
    FSM,
    FSMError,
    BaseState
)


def nothing():
    pass


def test_create_empty_fsm():
    fsm = FSM()

    assert_that(fsm.current_state_name, equal_to(''))


def test_state_action():
    def one():
        return 1

    state = State('one', one)

    assert_that(state.execute(1), equal_to(1))


def test_state_name():
    name = 'somename'
    state = State(name, None)

    assert_that(state.name, equal_to(name))


def test_add_good_transition_by_state():
    state_from = State("from", nothing)
    state_to = State('to', nothing)

    fsm = FSM()

    fsm.add_state(state_from)\
       .add_state(state_to)

    assert_that(
        calling(fsm.add_transition).with_args(state_from, state_to),
        is_not(raises(FSMError))
    )


def test_add_good_transition_by_name():

    fsm = FSM()\
        .add_state(State("from", nothing))\
        .add_state(State('to', nothing))

    assert_that(
        calling(fsm.add_transition).with_args('event', 'from', 'to'),
        is_not(raises(FSMError))
    )


def test_imposible_transition():
    fsm = FSM().add_state(State('from', nothing))

    assert_that(
        calling(fsm.add_transition).with_args('event', 'from', 'to'),
        raises(FSMError)
    )


def test_imposible_transition_2():
    fsm = FSM().add_state(State('to', nothing))

    assert_that(
        calling(fsm.add_transition).with_args('event', 'from', 'to'),
        raises(FSMError)
    )


def test_start():
    fsm = FSM().add_state(State('begin', nothing))

    fsm.start('begin')

    assert_that(fsm.current_state_name, equal_to('begin'))


def test_bad_start():
    fsm = FSM().add_state(State('begin', nothing))

    assert_that(
        calling(fsm.start).with_args('unexisting'),
        raises(FSMError)
    )


def test_stop():
    fsm = FSM().add_state(State('begin', nothing))

    fsm.start('begin')
    fsm.stop()

    assert_that(fsm.current_state_name, equal_to(''))


def test_process():
    fsm = FSM()
    fsm.add_state(State('one', nothing))\
       .add_state(State('two', nothing))\
       .add_state(State('three', nothing))\
       .add_transition('one-two', 'one', 'two')\
       .add_transition('two-three', 'two', 'three')\
       .start('one')

    fsm.process('one-two')
    fsm.process('two-three')

    assert_that(fsm.current_state_name, equal_to('three'))


def test_context():
    class ContextState(BaseState):
        def __init__(self, name):
            super(ContextState, self).__init__(name)

        def execute(self, ctx):
            ctx[self.name] = ctx.get(self.name, 0) + 1
            return ctx

    fsm = FSM()
    fsm.add_state(ContextState('one'))\
       .add_state(ContextState('two'))\
       .add_state(ContextState('three'))\
       .add_transition('one-two', 'one', 'two')\
       .add_transition('two-three', 'two', 'three')\
       .add_transition('three-one', 'three', 'one')\
       .add_transition('one-one', 'one', 'one')\
       .start('one')

    fsm.process()
    fsm.process('one-two')
    fsm.process('two-three')
    fsm.process()
    fsm.process('three-one')
    fsm.process('one-one')

    assert_that(fsm.context, has_entries({'one': 3, 'two': 1, 'three': 2}))
