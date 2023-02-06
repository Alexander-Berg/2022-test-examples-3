import yatest.common
import os


def test_example():
    out = yatest.common.output_path('out')
    os.mkdir(out)

    cmd = [
        yatest.common.binary_path('contrib/tools/protoc/protoc'),
        '--proto_path={}'.format(yatest.common.source_path('')),  # arcadia root
        '--proto_path={}'.format(yatest.common.source_path('contrib/libs/protobuf/src')),
        '--plugin=protoc-gen-broc={}'.format(yatest.common.binary_path('extsearch/geo/kernel/broto/compiler/broc')),
        '--broc_out={}'.format(out),
        'extsearch/geo/kernel/broto/tests/proto/example.proto',
    ]

    yatest.common.execute(cmd)

    subdir = os.path.join(out, 'extsearch/geo/kernel/broto/tests/proto')
    result = []
    for ext in ('h', 'cpp'):
        # We add the .txt extension to canonical files stored in arcadia repo
        # so they do not look like real C++ source files and are skipped during global refactorings.
        src_filename = 'example.pbro.{}'.format(ext)
        dst_filename = 'example.pbro.{}.txt'.format(ext)

        os.rename(os.path.join(subdir, src_filename), os.path.join(subdir, dst_filename))
        result.append(yatest.common.canonical_file(os.path.join(subdir, dst_filename), local=True))

    return result
