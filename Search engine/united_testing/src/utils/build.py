import search.priemka.tools.united_testing.src.utils.lib as lib
import json
import os
import shutil


def build_bundle(args, mode, backends_path):
    output_dir = lib.get_graphs_dir(args, mode)
    lib.mkdir_p(output_dir)

    # Cleaning old graphs
    for item in os.listdir(output_dir):
        item_path = os.path.join(output_dir, item)
        if os.path.isdir(item_path):
            shutil.rmtree(item_path)

    # Building bundle
    cmd = [
        args['graph_generator_binary'], '-vv',
        'bundle',
        '-g', args['{}_graphs'.format(mode)],
        '-o', output_dir,
        'WEB',
    ]
    lib.call(cmd, False)

    with open(backends_path, 'r') as inp:
        with open(os.path.join(output_dir, 'backends.json'), 'w') as out:
            backends = json.load(inp)
            backends = backends['MAN_WEB_APP_HOST_HAMSTER']
            json.dump(backends, out)
    os.mkdir(os.path.join(output_dir, 'patched_graphs'))


def build_backends_and_bundle(args):
    backends_path = os.path.abspath('_backends.WEB.json')
    if args['backends'] or not os.path.exists(backends_path):
        cmd = [
            args['graph_generator_binary'], '-vv',
            'backends',
            '-R',
            '-P',
            '-g', args['conf'],
            '-o', os.path.abspath('.'),
            'WEB',
        ]
        lib.call(cmd, False)

    for mode in ['baseline', 'experiment']:
        build_bundle(args, mode, backends_path)
