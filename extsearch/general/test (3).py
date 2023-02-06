import os

import yatest.common

binary = "extsearch/images/tools/imageloader/imageloader"


def do_test_images(path, mime):
    out_path = yatest.common.output_path(mime + ".out")
    with open(out_path, "w") as out:
        image_files = os.listdir(path)

        for image_file in sorted(image_files):
            result = yatest.common.execute([yatest.common.binary_path(binary), "--path", os.path.join(path, image_file), "--dump-mat-crc", "--crop"])
            print >> out, image_file, ":", result.std_out
    return yatest.common.canonical_file(out_path)


def test_all():
    test_files_suite = os.path.join(os.getcwd(), 'mime_test_files')

    format_dirs = os.listdir(test_files_suite)

    results = {}
    for format_dir in sorted(format_dirs):
        results[format_dir] = do_test_images(os.path.join(test_files_suite, format_dir), format_dir)

    return results
