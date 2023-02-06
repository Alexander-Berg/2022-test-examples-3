#!/usr/bin/env python

import argparse
import json
import os
import subprocess
import sys
import tempfile

ARCADIA_DIR = os.path.join(os.path.dirname(__file__), "../../../../../")
HAMSTERWHEEL_BIN = os.path.join(ARCADIA_DIR, "tools", "snipmake", "download_contexts", "hamsterwheel", "hamsterwheel")
HAMSTERWHEEL_PARAMS = ['-j', '1', '-p', '4', '-f', 'images']

def load_filters(filter_path):
    with open(filter_path) as file:
        return json.loads(file.read())

def invoke_hamsterwheel(request_file, output_file, params):
    if not os.path.exists(HAMSTERWHEEL_BIN):
        raise IOError(HAMSTERWHEEL + " doesn't exist")
    if not os.path.exists(request_file):
        raise IOError(request_file + " doesn't exist")
    args = [HAMSTERWHEEL_BIN, '-i', request_file] + HAMSTERWHEEL_PARAMS
    if params:
        args.extend(['-e', params])

    print(' '.join(args))

    with open(output_file, 'a') as f:
        try:
            proc = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
            out, err = proc.communicate()
            f.write(out)
            print err,
        except OSError as e:
            if e.errno == 2:
                print("ERROR: {0} is not found.".format(HAMSTERWHEEL_BIN))
                sys.exit(1)
            raise
    print(80 * '*')

def main(request_file, filters, ext_params, output_file):
    if os.path.exists(output_file):
        os.remove(output_file)
    invoke_hamsterwheel(request_file, output_file, ext_params)
    for filter in filters:
        for value in filters[filter]:
            param = ext_params + "&" + filter + "=" + value
            invoke_hamsterwheel(request_file, output_file, param)

if __name__ == "__main__":
    arg_parser = argparse.ArgumentParser(description='download snippet contexts with hamsterwheel for queries with differnent filters')
    arg_parser.add_argument(
        '-q', '--queries',
        dest='queries',
        required=True,
        type=str,
        help='file with queries in hamsterwheel format: reqid=id\tuser-region=region\tquery=q\tdom-region=dom',
    )
    arg_parser.add_argument(
        '-f', '--filters',
        dest='filters',
        required=True,
        type=str,
        help='file with list of filters in json format',
    )
    arg_parser.add_argument(
        '-p', '--params',
        dest='params',
        required=False,
        default="",
        type=str,
        help='extendent query params',
    )
    arg_parser.add_argument(
        '-o', '--output',
        dest='output',
        required=True,
        type=str,
        help='output file name',
    )

    params = arg_parser.parse_args()

    main(params.queries, load_filters(params.filters), params.params, params.output)
