import getopt

import sys
from load_test import Params, Runner

opts, args = getopt.getopt(sys.argv[1:], '', [
    'show-ok', 'threads=', 'url=', 'stid=', 'stid-file=', 'host=', 'test_duration=',
    'disable_cache=', 'disable_dynamic_prefetch=',
    'ffmpeg_in_threads=', 'ffmpeg_out_threads=', 'ffmpeg_max_proc_count=', 'init_prefetch_size=', 'segment_duration='
])

params = Params()

for opt, arg in opts:
    if opt == '--show-ok':
        params.show_ok = True
    elif opt == '--threads':
        params.thread_count = int(arg)
    elif opt == '--test_duration':
        params.test_duration = int(arg)
    elif opt == '--host':
        params.host = arg
    elif opt == '--url':
        params.url_providers = [lambda: arg]
    elif opt == '--stid':
        params.set_stid(arg)
    elif opt == '--stid-file':
        params.set_stid_file(arg)
    elif opt == '--ffmpeg_in_threads':
        params.ffmpeg_in_threads = int(arg)
    elif opt == '--ffmpeg_out_threads':
        params.ffmpeg_out_threads = int(arg)
    elif opt == '--ffmpeg_max_proc_count':
        params.ffmpeg_max_proc_count = int(arg)
    elif opt == '--init_prefetch_size':
        params.init_prefetch_size = int(arg)
    elif opt == '--segment_duration':
        params.segment_duration = int(arg)
    elif opt == '--disable_cache':
        params.disable_cache = bool(arg)
    elif opt == '--disable_dynamic_prefetch':
        params.disable_dynamic_prefetch = bool(arg)


if params.url_providers is None:
    quit(1)

Runner(params).run()
