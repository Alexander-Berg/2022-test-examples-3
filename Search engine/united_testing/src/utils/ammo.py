import search.priemka.tools.united_testing.src.utils.lib as lib
import socket


def build_ammo(args):
    lib.mkdir_p([args['baseline_dir'], args['experiment_dir']])
    cmd = [
        args['save_from_yt_binary'],
        '--host', socket.gethostname(),
        '--port', str(args['app_host_port']),
        '--count', str(args['count']),
        '--shoot', lib.get_requests(args, 'baseline'),
        '--united-shoot', lib.get_requests(args, 'experiment'),
        '--use-custom-src-setup', args['use_custom_src_setup'],
        '--use-custom-noapache', args['use_custom_noapache'],
        '--report-base', args['custom_report_baseline'],
        '--report-exp', args['custom_report_experiment'],
        '--filter-type', args.get('request_filtration', '')
    ]

    if args['sources_list']:
        cmd.extend(['--sources-list', args['sources_list']])

    if args['add_cgi_baseline']:
        cmd.extend(['--add-cgi-base', args['add_cgi_baseline']])

    if args['add_cgi_experiment']:
        cmd.extend(['--add-cgi-exp', args['add_cgi_experiment']])

    if args['add_test_ids']:
        cmd.extend(['--add-test-ids', args['add_test_ids']])

    if args['search_result']:
        cmd.extend(['--search-result'])

    if args['token']:
        cmd.extend(['--token', args['token']])

    lib.verified_call(cmd, shell=False)
