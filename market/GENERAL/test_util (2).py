from __future__ import print_function
import pprint
import sys


def stderr_print(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


def debug_print(*args):
    frame = sys._getframe(1)
    for arg in args:
        stderr_print('{}: {}'.format(arg, pprint.pformat(eval(arg, frame.f_globals, frame.f_locals))))
