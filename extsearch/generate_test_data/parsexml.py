import os
import shutil
import subprocess
import tarfile
import yt.wrapper as yt


TEST_EXPORT_SIZE = 1000
_entity_types = {
    'tracks',
    'albums',
    'artists',
    'playlists',
    'users',
    'genres',
    'radio',
}


def _fake_map(rec):
    yield rec


class ParsexmlTestData(object):

    def __init__(self, args):
        self.args = args

    @staticmethod
    def fill_args(args_parser):
        pass

    def generate_test_data(self):
        workdir = os.path.join(os.getcwd(), 'parsexml-export')
        if not os.path.exists(workdir):
            os.mkdir(workdir)
        test_export_files = []
        for type_ in _entity_types:
            with yt.TempTable() as tmp:
                export_table = f'//home/muzsearch/ymusic/data/search-export.{type_}.xml'
                yt.run_map(
                    _fake_map,
                    export_table,
                    tmp,
                    job_io={'table_reader': self.determine_table_reader_config(export_table)}
                )
                export_file = os.path.join(workdir, f'{type_}.xml')
                with open(export_file, 'w') as f:
                    for rec in yt.read_table(tmp):
                        f.write(rec['value'].replace('\n', '') + '\n')
                test_export_files.append(export_file)
        export_archive = os.path.join(workdir, 'export.tar.gz')
        with tarfile.open(export_archive, 'w:gz') as archive:
            for test_export_file in test_export_files:
                archive.add(test_export_file, arcname=os.path.basename(test_export_file))
        self.upload_archive(export_archive)
        shutil.rmtree(workdir)

    def upload_archive(self, archive):
        return subprocess.call(['ya', 'upload', archive, '--ttl', '300', '--sandbox'])

    def determine_table_reader_config(self, export_table):
        row_count = yt.row_count(export_table)
        rate = min(1.0 * TEST_EXPORT_SIZE / row_count, 1.0)
        return {
            'sampling_rate': rate,
        }
