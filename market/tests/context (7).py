# -*- coding: utf-8 -*-

import unittest


def setup():
    from market.pylibrary.mi_util.logger import configure_logging
    configure_logging()


_setup_done = None
if not _setup_done:
    _setup_done = True
    setup()


def main():
    unittest.main()
