import lib
import os
import json


def diff_results_impl(args, name, suff, out_name, binary=None):
    name = name.split(':')
    if binary is None:
        binary = args['dummy_diff_binary'] if len(name) < 2 else name[1]
    name = name[0]
    cmd = binary.split()
    cmd.extend(['{}/{}{}'.format(args['baseline_dir'], name, suff), '{}/{}{}'.format(args['experiment_dir'], name, suff)])
    if args['sources_list']:
        cmd.extend(['--sources-list', args['sources_list']])
    with open('diff/{}'.format(out_name), 'w') as output_file:
        lib.call(cmd, ignore_rc=True, output=output_file)


def diff_report_results(args):
    diff_results_impl(args, 'report', '.responses', 'report', args['report_diff_binary'])


def diff_reqans_results(args):
    diff_results_impl(args, 'report', '.responses', 'reqans', args['reqans_diff_binary'])


def diff_profile_results(args):
    profile_diff_opts = args['profile_diff_opts']

    def process_log_line(log_line):
        js = json.loads(log_line)
        return '{}\t{}'.format(js['requestId'], log_line)

    def prepare_log_lines(f):
        log_lines = f.read().split('\n')
        log_lines.pop()
        log_lines = map(process_log_line, log_lines)
        return list(log_lines)

    with open(profile_diff_opts['profile_log_baseline'], 'r') as f:
        log_lines = prepare_log_lines(f)
        with open(args['baseline_dir'] + '/report.profile', 'w') as f_out:
            f_out.write('\n'.join(log_lines))

    with open(profile_diff_opts['profile_log_experiment'], 'r') as f:
        log_lines = prepare_log_lines(f)
        with open(args['experiment_dir'] + '/report.profile', 'w') as f_out:
            f_out.write('\n'.join(log_lines))

    diff_results_impl(args, 'report', '.profile', 'profile', profile_diff_opts['profile_diff_binary'])


def diff_results(args):
    if not os.path.exists("diff"):
        os.mkdir("diff")
    for graph in filter(None, args['compare_graphs'].split(',')):
        diff_results_impl(args, graph, '', graph.split(':')[0])
    for source in filter(None, args['compare_requests'].split(',')):
        diff_results_impl(args, source, '.Frequests', source.split(':')[0])
    for source in filter(None, args['compare_responses'].split(',')):
        diff_results_impl(args, source, '.Fresponses', source.split(':')[0])
    if args['report_diff_binary']:
        diff_report_results(args)
    if args['reqans_diff_binary']:
        diff_reqans_results(args)
    if 'profile_diff_opts' in args and args['profile_diff_opts']:
        diff_profile_results(args)
