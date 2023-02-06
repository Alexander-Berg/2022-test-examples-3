import pytest

from edera import Stack


def test_stack_is_initially_empty():
    assert not Stack()


def test_pushing_items_increases_stack_size():
    stack = Stack()
    for i in range(1, 6):
        stack.push(i)
        assert len(stack) == i


def test_top_of_stack_is_always_last_added_item():
    stack = Stack()
    for i in range(1, 6):
        stack.push(i)
        assert stack.top == i


def test_stack_pops_items_in_correct_order():
    stack = Stack()
    for i in range(1, 6):
        stack.push(i)
    assert stack.pop() == 5
    for i in range(5, 10):
        stack.push(i)
    for i in range(9, 0, -1):
        assert stack.pop() == i
    assert not stack


def test_accessing_empty_stack_gives_assertion_error():
    stack = Stack()
    with pytest.raises(AssertionError):
        return stack.top
    with pytest.raises(AssertionError):
        stack.pop()
