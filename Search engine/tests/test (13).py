import base64
import copy
import yatest.common as yatest
import yt_utils
from mr_utils import TableSpec
from mapreduce.yt.python.yt_stuff import yt_stuff
from yt.wrapper import yson

BINARY = yatest.binary_path('search/tools/request_sampler/request_sampler')
INPUT_TABLE_PATH = '//apphost-event-log'
TABLE_FILE = 'binary-event-log-framed.2'
INPUT_TABLE_PATH_2 = '//apphost-event-log-2'
TABLE_FILE_2 = 'binary-event-log-with-http-proxies.yson.text'
INPUT_TABLES = [
    TableSpec(TABLE_FILE, table_name=INPUT_TABLE_PATH, mapreduce_io_flags=['--format', 'yson'], sort_on_read=False),
    TableSpec(TABLE_FILE_2, table_name=INPUT_TABLE_PATH_2, mapreduce_io_flags=['--format', 'yson'], sort_on_read=False),
]
SOURCE_NAMES = ['ugcpub:BLACKBOX_UGC.UGCPUB_BACKEND']
OUTPUT_FORMATS = ['app-host-json', 'dolbilo', 'fuzzer', 'grpc-client-plan']
FORMATS_WITH_DIR_OUTPUT = ['fuzzer']
REQUEST_TYPES = ['both', 'error', 'success']
HOST_NAMES = ['vla1-3263']
MIN_TIMESTAMP = 1551912000000000
MAX_TIMESTAMP = 1556160000000000
MIN_PAGE = 2
MAX_PAGE = 4
CONVERTER_INPUT_FORMATS = {
    'UNPACK_HTTP_RUN_REQUEST.MUSIC_SCENARIO_RENDER': 'service_request',
    'MUSIC_SCENARIO_CONTINUE_PROXY': 'http_request',
}


def parse_sources(sources):
    suffixes = []
    for graph_source_pair_set in sources.split(','):
        graphs_and_sources = graph_source_pair_set.split(':')
        sources = graphs_and_sources[-1].split('.')
        graphs = []
        if len(graphs_and_sources) > 1:
            graphs = graphs_and_sources[0].split('.')
        for source in sources:
            for graph in graphs:
                suffixes.append('_%s_%s' % (graph, source))
            if not graphs:
                suffixes.append('_%s' % source)
    return suffixes


def get_args(
    yt_server,
    output_prefix=None,
    input_table=INPUT_TABLE_PATH,
    sources='BLACKBOX_UGC.UGCPUB_BACKEND',
    download_count=None,
    min_timestamp=None,
    max_timestamp=None,
    min_page=None,
    max_page=None,
    output_format=None,
    request_type=None,
    enable_internal=None,
    download_first_host=None,
    host=None,
    top_slowest=None,
    slower=None,
    uri_path_prefix=None,
    converter_input_format=None,
    frame_base_64_encoded=None,
    frame_column_name=None,
):
    args = ['--cluster', yt_server, '--input-table', input_table, '--source', sources]
    suffixes = parse_sources(sources)
    generated_output_prefix = ''

    if frame_base_64_encoded:
        args += ['--frame-base-64-encoded']
        generated_output_prefix += 'frame-base-64-encoded_'

    if frame_column_name:
        args += ['--frame-column-name', frame_column_name]
        generated_output_prefix += 'frame-column-name-{}'.format(frame_column_name)

    if download_count is not None:
        args += ['--download-count', str(download_count)]
        generated_output_prefix += 'download-count-{}_'.format(download_count)

    if min_timestamp is not None:
        args += ['--min-timestamp', str(min_timestamp)]
        generated_output_prefix += 'min-timestamp-{}_'.format(min_timestamp)

    if max_timestamp is not None:
        args += ['--max-timestamp', str(max_timestamp)]
        generated_output_prefix += 'max-timestamp-{}_'.format(max_timestamp)

    if min_page is not None:
        args += ['--min-page', str(min_page)]
        generated_output_prefix += 'min-page-{}_'.format(min_page)

    if max_page is not None:
        args += ['--max-page', str(max_page)]
        generated_output_prefix += 'max-page-{}_'.format(max_page)

    if output_format is not None:
        args += ['--output-format', output_format]
        generated_output_prefix += 'output-format-{}_'.format(output_format)

    if request_type is not None:
        args += ['--request-type', request_type]
        generated_output_prefix += 'request-type-{}_'.format(request_type)

    if enable_internal is not None and enable_internal != False:
        args += ['--enable-internal']
        generated_output_prefix += 'enable-internal_'

    if download_first_host is not None and download_first_host != False:
        args += ['--download-first-host']
        generated_output_prefix += 'download-first-host_'

    if host is not None:
        args += ['--host', host]
        generated_output_prefix += 'host-{}_'.format(host)

    if top_slowest is not None:
        args += ['--top-slowest']
        generated_output_prefix += 'top-slowest_'

    if slower is not None:
        args += ['--slower', slower]
        generated_output_prefix += 'slower-{}_'.format(slower)

    if uri_path_prefix is not None:
        args += ['--uri-path-prefix', uri_path_prefix]
        generated_output_prefix += 'uri-path-prefix-{}_'.format(uri_path_prefix)

    if converter_input_format is not None:
        args += ['--converter-input-format', converter_input_format]
        generated_output_prefix += 'conveter-input-format-{}_'.format(converter_input_format)

    if output_prefix is None:
        output_prefix = generated_output_prefix + 'requests'

    args += ['--file', output_prefix]

    return args, [output_prefix + suffix for suffix in suffixes]


def convert_table(src_table_file, dst_table_file, converter):
    with open(src_table_file, 'rb') as inp:
        with open(dst_table_file, 'wb') as out:
            parsed_rows = yson.loads(b'[' + inp.read() + b']')
            for row in parsed_rows:
                out.write(yson.dumps(converter(row)) + b';\n')


def split_into_files_and_dirs(args, output):
    output_format = None
    for i, option in enumerate(args):
        if option == '--output-format':
            output_format = args[i + 1]
    output_dirs = []
    output_files = []
    if output_format in FORMATS_WITH_DIR_OUTPUT:
        output_dirs = output
    else:
        output_files = output

    return output_files, output_dirs


def run_test(yt_stuff, args, output, input_tables=INPUT_TABLES):
    output_files, output_dirs = split_into_files_and_dirs(args, output)

    return yt_utils.yt_test(
        BINARY,
        args=args,
        data_path=yatest.work_path(),
        input_tables=input_tables,
        output_files=output_files,
        output_dirs=output_dirs,
        yt_stuff=yt_stuff,
    )


def test_source_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_sources(sources):
        args, output_files = get_args(yt_server, sources=sources)

        return run_test(yt_stuff, args, output_files)

    return [test_for_sources(sources) for sources in ['BLACKBOX_UGC', 'WEB', 'BLENDER']]


def test_frame_base64_encoded(yt_stuff):
    yt_server = yt_stuff.get_server()
    current_table_path = '{}-encoded'.format(INPUT_TABLE_PATH)
    current_table_file_path = yatest.output_path('{}.encoded'.format(TABLE_FILE))
    args, output_files = get_args(
        yt_server,
        sources='BLACKBOX_UGC',
        input_table=current_table_path,
        frame_base_64_encoded=True,
        output_format='app-host-json',
    )

    def encode_frame_column(row):
        row = copy.deepcopy(row)
        row['frame'] = base64.b64encode(row['frame'])
        return row

    convert_table(TABLE_FILE, current_table_file_path, encode_frame_column)

    current_tables = [
        TableSpec(
            current_table_file_path,
            table_name=current_table_path,
            mapreduce_io_flags=['--format', 'yson'],
            sort_on_read=False,
        ),
    ]

    return run_test(yt_stuff, args, output_files, input_tables=current_tables)


def test_frame_column_name(yt_stuff):
    yt_server = yt_stuff.get_server()
    another_frame_name = 'another_frame_name'
    current_table_path = '{}-{}'.format(INPUT_TABLE_PATH, another_frame_name)
    current_table_file_path = yatest.output_path('{}.{}'.format(TABLE_FILE, another_frame_name))

    args, output_files = get_args(
        yt_server,
        sources='BLACKBOX_UGC',
        input_table=current_table_path,
        frame_column_name=another_frame_name,
        output_format='app-host-json',
    )

    def change_frame_name(row):
        row = copy.deepcopy(row)
        row[another_frame_name] = row['frame']
        del row['frame']
        return row

    convert_table(TABLE_FILE, current_table_file_path, change_frame_name)

    current_tables = [
        TableSpec(
            current_table_file_path,
            table_name=current_table_path,
            mapreduce_io_flags=['--format', 'yson'],
            sort_on_read=False,
        ),
    ]

    return run_test(yt_stuff, args, output_files, input_tables=current_tables)


def test_download_count_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_download_count(rows, sources):
        args, output_files = get_args(yt_server, sources=sources, download_count=rows)

        return run_test(yt_stuff, args, output_files)

    return [test_for_download_count(50, sources) for sources in SOURCE_NAMES]


def test_timestamp_bound_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_timestamp_bound(min_timestamp=None, max_timestamp=None):
        args, output_files = get_args(yt_server, min_timestamp=min_timestamp, max_timestamp=max_timestamp)

        return run_test(yt_stuff, args, output_files)

    result = []
    result += test_for_timestamp_bound(min_timestamp=MIN_TIMESTAMP)
    result += test_for_timestamp_bound(max_timestamp=MAX_TIMESTAMP)
    result += test_for_timestamp_bound(min_timestamp=MIN_TIMESTAMP, max_timestamp=MAX_TIMESTAMP)
    return result


def test_page_bound_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_page_bound(min_page=None, max_page=None):
        args, output_files = get_args(yt_server, min_page=min_page, max_page=max_page)

        return run_test(yt_stuff, args, output_files)

    result = []
    result += test_for_page_bound(min_page=MIN_PAGE)
    result += test_for_page_bound(max_page=MAX_PAGE)
    result += test_for_page_bound(min_page=MIN_PAGE, max_page=MAX_PAGE)
    return result


def test_output_format_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_output_format(output_format):
        args, output_files = get_args(yt_server, output_format=output_format)

        return run_test(yt_stuff, args, output_files)

    return [test_for_output_format(output_format) for output_format in OUTPUT_FORMATS]


def test_request_type_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_request_type(request_type):
        args, output_files = get_args(yt_server, sources='BLENDER', request_type=request_type)

        return run_test(yt_stuff, args, output_files)

    return [test_for_request_type(request_type) for request_type in REQUEST_TYPES]


def test_enable_internal_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_enable_internal_for_request_type(request_type):
        args, output_files = get_args(yt_server, request_type=request_type, enable_internal=True)

        return run_test(yt_stuff, args, output_files)

    return [test_enable_internal_for_request_type(request_type) for request_type in REQUEST_TYPES]


def test_download_first_host_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    args, output_files = get_args(yt_server, download_first_host=True)

    return run_test(yt_stuff, args, output_files)


def test_host_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_host(host):
        args, output_files = get_args(yt_server, host=host)

        return run_test(yt_stuff, args, output_files)

    return [test_for_host(host) for host in HOST_NAMES]


def test_top_slowest_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    args, output_files = get_args(yt_server, download_count=50, top_slowest=True)

    return run_test(yt_stuff, args, output_files)


def test_slower_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_slower(slower, request_type):
        args, output_files = get_args(yt_server, slower=slower, request_type=request_type)

        return run_test(yt_stuff, args, output_files)

    return [test_for_slower('400ms', request_type) for request_type in REQUEST_TYPES]


def test_uri_path_prefix_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_uri_path_prefix(uri_path_prefix, sources, request_type):
        args, output_files = get_args(yt_server, sources=sources, uri_path_prefix=uri_path_prefix)

        return run_test(yt_stuff, args, output_files)

    result = []
    result += [test_for_uri_path_prefix('iznanka', 'BLENDER', request_type) for request_type in REQUEST_TYPES]
    result += test_for_uri_path_prefix('search', 'WEB', None)
    return result


def test_converter_input_format_option(yt_stuff):
    yt_server = yt_stuff.get_server()

    def test_for_converter_input_format(source, converter_input_format):
        args, output_files = get_args(
            yt_server,
            input_table=INPUT_TABLE_PATH_2,
            output_format='app-host-json',
            converter_input_format=converter_input_format,
            sources=source,
        )

        return run_test(yt_stuff, args, output_files)

    return [
        test_for_converter_input_format(source, converter_input_format)
        for source, converter_input_format in CONVERTER_INPUT_FORMATS.items()
    ]
