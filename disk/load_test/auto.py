import time
import os

from load_test import Params, Runner

def cons_params(host, segment_duration, prefetch_size, dynamic_prefetch, ff_out_threads, max_proc_count, concurrency, test_time):
    params = Params()
    params.host = host
    params.segment_duration = segment_duration
    params.init_prefetch_size = prefetch_size
    params.disable_dynamic_prefetch = not dynamic_prefetch
    params.ffmpeg_out_threads = ff_out_threads
    params.ffmpeg_max_proc_count = max_proc_count
    params.thread_count = concurrency
    params.test_duration = test_time
    params.set_stid_file('/Users/lemeh/Downloads/stids/stids_2017-06-07.txt')
    return params

LOG_DIR = '/Users/lemeh/Downloads/load/' + time.strftime('%Y-%m-%d %H-%M-%S', time.localtime())
os.mkdir(LOG_DIR)

HOST = 'https://streaming.qloud.disk.yandex.net'
SEG_DURATION = 5
PREFETCH_SIZES = [0, 1]
DYNAMIC_PREFETCH = False
FF_OUT_THREADS_LIST = [0, 2, 3]
MAX_PROC_COUNT = 8
CONCURRENCIES = [90, 180]
TEST_TIME = 180


def current_datetime_str():
    return time.strftime('%Y-%m-%d %H:%M:%S', time.localtime())


with open(LOG_DIR + '/main.log', 'w') as main_log_file:
    def log_short(value=''):
        print str(value)
        main_log_file.write(str(value) + "\n")

    for prefetch_size in PREFETCH_SIZES:
        for ff_out_threads in FF_OUT_THREADS_LIST:
            for concurrency in CONCURRENCIES:
                log_filename = '%ds-pf%d-dynpf_%s-fft%d-mp%d-c%d-t%dsec.log' % (SEG_DURATION, prefetch_size, str(DYNAMIC_PREFETCH), ff_out_threads, MAX_PROC_COUNT, concurrency, TEST_TIME)
                with open(LOG_DIR + '/' + log_filename, 'w') as log_file:
                    params = cons_params(HOST, SEG_DURATION, prefetch_size, DYNAMIC_PREFETCH, ff_out_threads, MAX_PROC_COUNT, concurrency, TEST_TIME)

                    def log(value=''):
                        print str(value)
                        log_file.write(str(value) + "\n")

                    params.log = log
                    params.log_short = log_short

                    start = current_datetime_str()
                    Runner(params).run()
                    log_short(start + ' - ' + current_datetime_str())

                    main_log_file.flush()
