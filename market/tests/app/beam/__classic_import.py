import os
import sys

if 'ARCADIA_SOURCE_ROOT' not in os.environ:
    cur_dir = os.path.dirname(os.path.abspath(__file__))
    while cur_dir != '/':
        if os.path.exists(os.path.join(cur_dir, '.arcadia.root')):
            os.environ['ARCADIA_SOURCE_ROOT'] = cur_dir
            break
        cur_dir = os.path.dirname(cur_dir)
    else:
        raise RuntimeError('Cannot find arcadia root')

if not getattr(sys, "is_standalone_binary", False):
    import imp
    imp.load_source(
        'classic_import',
        os.path.join(os.environ['ARCADIA_SOURCE_ROOT'], 'market/pylibrary/lite/classic_import.py')
    )
