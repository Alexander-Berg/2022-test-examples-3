import extsearch.images.tools.offline_diff.united_testing.src.utils.lib as lib


def build_backends_and_bundle(args):
    cmd = [
        '/home/fexion/arcadia/ya', 'tool', 'apphost', 'setup',
        '-n',
        '--local-arcadia-path', '/home/fexion/arcadia',
        '-y',
        '-p', '28813',
        '--install-path', '/home/fexion/arcadia/extsearch/images/tools/offline_diff/united_testing/bundle/',
        'arcadia',
        '--vertical', 'IMGS'
    ]
    lib.call(cmd, False)
