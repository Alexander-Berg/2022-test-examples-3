# coding: utf-8

from mail.pypg.pypg.types.repr_compare import base_selected_repr_compare


def pytest_assertrepr_compare(config, op, left, right):
    verbose = bool(config.option.verbose)
    if op == '==':
        return base_selected_repr_compare(left, right, verbose)
