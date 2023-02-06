import json
import os
import tarfile

import extsearch.ymusic.scripts.reindex.parsexml as px

import yatest.common as yc


def test__small_export():
    work_dir = yc.work_path()
    export_archive = os.path.join(work_dir, 'export.tar.gz')
    with tarfile.open(export_archive) as export:
        export.extractall()

    out_dir = os.path.join(work_dir, 'out')
    if not os.path.exists(out_dir):
        os.mkdir(out_dir)
    process_export_file(os.path.join(work_dir, 'tracks.xml'), os.path.join(out_dir, 'tracks.parsed'))
    process_export_file(os.path.join(work_dir, 'albums.xml'), os.path.join(out_dir, 'albums.parsed'))
    process_export_file(os.path.join(work_dir, 'artists.xml'), os.path.join(out_dir, 'artists.parsed'))
    process_export_file(os.path.join(work_dir, 'playlists.xml'), os.path.join(out_dir, 'playlists.parsed'))
    process_export_file(os.path.join(work_dir, 'users.xml'), os.path.join(out_dir, 'users.parsed'))
    process_export_file(os.path.join(work_dir, 'genres.xml'), os.path.join(out_dir, 'genres.parsed'))
    process_export_file(os.path.join(work_dir, 'radio.xml'), os.path.join(out_dir, 'radio.parsed'))
    return yc.canonical_dir(out_dir)


def process_export_file(export_filename, out_filename):
    all_parsed = []
    parsers = px.build_parsers()
    with open(export_filename) as export_file:
        for line in export_file:
            parsed = px.parse_xml(line.strip(), parsers)
            assert len(parsed) == 1
            all_parsed.append(parsed[0])
    with open(out_filename, 'w') as out_file:
        json.dump(all_parsed, out_file, indent=4, sort_keys=True)
