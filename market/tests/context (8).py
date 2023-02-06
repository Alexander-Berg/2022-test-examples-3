# -*- coding: utf-8 -*-

# import sys
# import os
import unittest
# sys.path.insert(0, os.path.dirname(os.path.abspath(__name__)))


def setup():
    from market.pylibrary.mindexerlib import configure_logging
    configure_logging()


_setup_done = None
if not _setup_done:
    _setup_done = True
    setup()


def main():
    unittest.main()
