# -*- coding: utf-8 -*-

import context
from market.pylibrary.mindexerlib import repeater


import unittest


class TestResistance(unittest.TestCase):
    def test_resistance(self):
        class MyStrangeException(Exception):
            def __str__(self):
                return self.__class__.__name__

        @repeater.resist((0, 0, 0), MyStrangeException, 'Развалилось')
        def my_strange_function(state, max_state):
            state.counter += 1
            if state.counter > max_state:
                return
            raise MyStrangeException()

        class State(object):
            def __init__(self):
                self.counter = 0

            def reset(self):
                self.__init__()

        state = State()
        my_strange_function(state, 0)
        self.assertEqual(1, state.counter)

        state.reset()
        my_strange_function(state, 3)
        self.assertEqual(4, state.counter)

        state.reset()
        self.assertRaises(MyStrangeException, my_strange_function, state, 99)


if '__main__' == __name__:
    context.main()
