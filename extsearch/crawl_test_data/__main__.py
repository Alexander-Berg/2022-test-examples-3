import logging
import six
import tarfile
import yt.wrapper as yt
import yt.yson as yson


logger = logging.getLogger(__name__)
GENERATORS_DATA_SIZE_FOR_TESTS = 1000


def crawl_gendocs_tests_data():
    production_data = {
        'tracks': '//home/muzsearch/ymusic/data/track.2',
        'albums': '//home/muzsearch/ymusic/data/album.2',
        'artists': '//home/muzsearch/ymusic/data/artist.2',
        'playlists': '//home/muzsearch/ymusic/data/playlist.2',
        'users': '//home/muzsearch/ymusic/data/user',
        'genres': '//home/muzsearch/ymusic/data/genre',
    }
    data_files = []
    for part_name, part_table in production_data.items():
        logger.debug('Downloading tests data for %s from %s', part_name, part_table)
        with yt.TempTable() as tmp:
            data_files.append(crawl_generator_tests_data_part(part_name, part_table, tmp))
    with tarfile.TarFile.gzopen('prepared_data.tar.gz', 'w') as tar:
        for data_file in data_files:
            tar.add(data_file)


def crawl_generator_tests_data_part(part_name, part_table, tmp_table):
    data_file = '{}.yson'.format(part_name)
    sampling_rate = 1.0 * GENERATORS_DATA_SIZE_FOR_TESTS / yt.row_count(part_table)
    sampling_rate = min(sampling_rate, 1.0)
    yt.run_map(
        noop_mapper,
        part_table, tmp_table,
        spec={'job_io': {'table_reader': {'sampling_rate': sampling_rate}}, 'force_transform': True},
    )
    with open(data_file, 'w') as f:
        for rec in yt.read_table(tmp_table):
            f.write(yson.dumps(rec) + '\n')
    return data_file


def noop_mapper(rec):
    yield rec


def parse_args():
    from argparse import ArgumentParser
    parser = ArgumentParser()
    subs = parser.add_subparsers(dest='for_')
    subs.add_parser('generators')
    return parser.parse_args()


def main():
    logging.basicConfig(level=logging.DEBUG)
    args = parse_args()

    if args.for_ == 'generators':
        crawl_gendocs_tests_data()
        print(
            'Now just run something lilkee '
            + 'ya upload prepared_data.tar.gz '
            + '--ttl inf -d "test data for extsearch/ymusic/scripts/reindex/real_data_tests"'
            + ' and update sandbox resource in tests ya.make'
        )
    else:
        raise ValueError('Unknown tests target: {}'.format(args.for_))


if __name__ == '__main__':
    main()
