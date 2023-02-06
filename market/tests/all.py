#!/usr/bin/env python

import os
import __classic_import
from market.pylibrary.lite.run import run
if __name__ == '__main__':
    this_dir = os.path.dirname(os.path.abspath(__file__))
    this_file = os.path.basename(os.path.abspath(__file__))
    run(this_dir, this_file, 'test_')
