#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

from auto_perf_test import *


def exec_command(command):
    print 'Executing "{}"'.format(command)
    return subprocess.Popen(command, shell=True)


def main():
    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('--artifacts-dir', default=DEFAULT_ARTIFACTS_DIR)
    arg_parser.add_argument('--session', metavar='GUID', required=True)
    arg_parser.add_argument('--flame-graph-path', default=DEFAULT_FLAME_GRAPH_PATH)
    arg_parser.add_argument('--output-prefix', default='')
    args = arg_parser.parse_args()

    artifacts_dir = expand_path(args.artifacts_dir)
    flame_graph_path = expand_path(args.flame_graph_path)

    root_path = os.path.join(artifacts_dir, args.session)
    test_ids = [int(name) for name in os.listdir(root_path) if is_int(name)]
    test_ids.sort()

    perf_stack_paths = list()
    for test_id in test_ids:
        test_path = os.path.join(root_path, str(test_id))
        for top_dir, _, files in os.walk(test_path):
            if PERF_FOLDED_STACKS in files:
                perf_stack_paths.append(os.path.join(top_dir, PERF_FOLDED_STACKS))

    if perf_stack_paths:
        print 'Found perf stacks:'
    else:
        print 'No perf stacks found'
    for path in perf_stack_paths:
        print path
    if len(perf_stack_paths) != 2:
        raise Exception('Expected to find exactly two stacks')

    difffolded_pl = os.path.join(flame_graph_path, 'difffolded.pl')
    flamegraph_pl = os.path.join(flame_graph_path, 'flamegraph.pl')

    def get_output_path(name):
        if args.output_prefix:
            prefixed_name = args.output_prefix + '_' + name
        else:
            prefixed_name = name
        return os.path.join(root_path, prefixed_name)

    processes = list()
    processes.append(exec_command(
        '{flamegraph_pl} < {input} > {output}'.format(
            flamegraph_pl=flamegraph_pl,
            input=perf_stack_paths[0],
            output=get_output_path('before.svg'))))
    processes.append(exec_command(
        '{flamegraph_pl} < {input} > {output}'.format(
            flamegraph_pl=flamegraph_pl,
            input=perf_stack_paths[1],
            output=get_output_path('after.svg'))))
    processes.append(exec_command(
        '{difffolded_pl} {input1} {input2} | {flamegraph_pl} > {output}'.format(
            difffolded_pl=difffolded_pl,
            input1=perf_stack_paths[0],
            input2=perf_stack_paths[1],
            flamegraph_pl=flamegraph_pl,
            output=get_output_path('diff.svg'))))
    processes.append(exec_command(
        '{difffolded_pl} -n {input1} {input2} | {flamegraph_pl} > {output}'.format(
            difffolded_pl=difffolded_pl,
            input1=perf_stack_paths[0],
            input2=perf_stack_paths[1],
            flamegraph_pl=flamegraph_pl,
            output=get_output_path('diff_normalized.svg'))))
    processes.append(exec_command(
        '{difffolded_pl} {input1} {input2} | {flamegraph_pl} --negate > {output}'.format(
            difffolded_pl=difffolded_pl,
            input1=perf_stack_paths[1],
            input2=perf_stack_paths[0],
            flamegraph_pl=flamegraph_pl,
            output=get_output_path('diff_inverted.svg'))))
    processes.append(exec_command(
        '{difffolded_pl} -n {input1} {input2} | {flamegraph_pl} --negate > {output}'.format(
            difffolded_pl=difffolded_pl,
            input1=perf_stack_paths[1],
            input2=perf_stack_paths[0],
            flamegraph_pl=flamegraph_pl,
            output=get_output_path('diff_normalized_inverted.svg'))))

    for process in processes:
        process.wait()


if __name__ == "__main__":
    main()
