# coding: utf-8


def ok_(expr, msg=None):
    """Shorthand for assert. Saves 3 whole characters!
    """
    if not expr:
        raise AssertionError(msg)
