import os
import yatest.common as yc
import subprocess


def run(stdout, options=[], dump_file='dump.json', config=''):
    binary = yc.binary_path('search/daemons/begemot/default/begemot')
    outdir = yc.output_path('configs')
    if not os.path.exists(outdir):
        os.mkdir(outdir)
    output = os.path.join(outdir, stdout)

    args = [
        binary,
        '--dump-config', os.path.join(outdir, dump_file)
    ]
    args.extend(options)
    if config:
        args.extend(['--cfg', config])

    with open(output, 'w') as output_file:
        subprocess.call(args, stdout=output_file)

    return outdir


def gen_config_file(name):
    content = """EventLogSyncPageCacheBackendBufferSize: 93
Threads: 54
AppHostThreads: 54
Port2: 93
AdditionalThreads: 93
AllowLazyReads: true
LogRequestsProbability: 0.278
GrpcPort: 93
ResultType: "dogs"
LockMemory: LOCK_MEMORY_YES
Port: 93
EventLogUseSyncPageCacheBackend: true
CacheSize: 93
"""
    config = yc.output_path(name)
    with open(config, 'w') as config_file:
        config_file.write(content)
    return config


def test_options():
    options_set = [
        ['--jobs', '27', '--network-threads', '27', '--port2', '42', '--sync-log-max-pending-size', '42', '--no-precharge', '--port', '42', '--log-requests', '__FILE__',
            '--log-probability', '0.314', '--max-blockcodec-size', '42', '--additional-cgi', 'cats', '--mlock', 'yes', '--print-rulegraph',
            '--print-bgschema', '--realtime', '__DIRECTORY__'],
        ['--jobs', '27', '--network-threads', '27', '--test-jobs', '42', '--print-bgschema', '--port2', '42', '--result-type', 'cats', '--data', '__DIRECTORY__', '--additional-cgi', 'cats',
            '--log-requests', '__FILE__', '--no-precharge', '--fresh', '__DIRECTORY__', '--cache_threshold', '42', '--no-limit-blockcodec-size',
            '--apphost-max-queue-size', '42', '--mlock', 'yes', '--grpc', '42', '--test', '--print-rules-files', '--print-rulegraph'],
        ['--use-sync-log', '--test-apphost-log', '--sync-log-buffer-size', '42'],
        ['--jobs', '27', '--network-threads', '27', '--port2', '42', '--allow-lazy-reads', '--grpc', '42', '--log-requests', '__FILE__', '--cache_threshold', '42', '--no-limit-blockcodec-size',
            '--realtime', '__DIRECTORY__', '--test-apphost-log', '--sync-log-buffer-size', '42', '--mlock', 'yes', '--test-jobs', '42', '--admin-threads', '42',
            '--print-rulegraph'],
        ['--jobs', '27', '--result-type', 'cats', '--log-cgi', '__FILE__', '--no-precharge', '--log-requests', '__FILE__', '--port', '42', '--apphost-max-queue-size', '42',
            '--cache_size', '42', '--test-jobs', '42', '--realtime', '__DIRECTORY__', '--sync-log-max-pending-size', '42', '--sync-log-buffer-size', '42',
            '--mlock', 'yes', '--port2', '42', '--test-cgi', '--test', '--use-sync-log', '--additional-jobs', '42', '--additional-cgi', 'cats',
            '--log-probability', '0.314', '--log', '__FILE__', '--no-limit-blockcodec-size', '--fresh', '__DIRECTORY__'],
        ['--jobs', '27', '--network-threads', '27', '--use-sync-log', '--print-bgschema', '--log-cgi', '__FILE__', '--no-precharge', '--data', '__DIRECTORY__', '--additional-jobs', '42',
            '--log-probability', '0.314', '--cache_threshold', '42', '--no-limit-blockcodec-size', '--apphost-max-queue-size', '42',
            '--admin-threads', '42', '--fresh', '__DIRECTORY__', '--test-jobs', '42', '--print-rulegraph'],
        ['--network-threads', '27', '--allow-lazy-reads', '--result-type', 'cats', '--log-cgi', '__FILE__', '--port', '42', '--log-requests', '__FILE__', '--test-apphost-log',
            '--test-jobs', '42', '--max-blockcodec-size', '42', '--sync-log-buffer-size', '42', '--grpc', '42', '--mlock', 'yes',
            '--port2', '42', '--test-cgi', '--admin-threads', '42', '--log-probability', '0.314', '--log', '__FILE__', '--no-limit-blockcodec-size',
            '--fresh', '__DIRECTORY__', '--print-rulegraph'],
        ['--use-sync-log', '--print-bgschema', '--data', '__DIRECTORY__', '--sync-log-max-pending-size', '42', '--additional-cgi', 'cats',
            '--log-requests', '__FILE__', '--additional-jobs', '42', '--log-probability', '0.314', '--log', '__FILE__', '--max-blockcodec-size', '42',
            '--test-apphost-log', '--mlock', 'yes', '--cache_size', '42', '--test-jobs', '42', '--print-rulegraph'],
        []
    ]

    test_dir = yc.output_path('test_dir')
    os.mkdir(test_dir)
    test_file = f'{test_dir}/test_file'
    for options in options_set:
        for i in range(len(options)):
            if options[i] == '__DIRECTORY__':
                options[i] = test_dir
            if options[i] == '__FILE__':
                options[i] = test_file
    config = gen_config_file('begemot.cfg')

    for i, options in enumerate(options_set):
        if '--jobs' in options and '--network-threads' in options:
            out = run(options=options, dump_file=f'options.{i + 1}.json', stdout=f'options.{i + 1}.cfg')
        out = run(options=options, dump_file=f'options.config.{i + 1}.json', stdout=f'options.config.{i + 1}.cfg', config=config)

    return yc.canonical_dir(out)
