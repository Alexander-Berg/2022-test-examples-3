from __future__ import print_function

import datetime as dt
import functools
import os
import pytest
import tarfile
import yt.yson as yson

import yatest.common as yc

import extsearch.ymusic.scripts.reindex.gendocs as gd
import extsearch.ymusic.scripts.test_utils.verbose_value as vv

import logging


logging.basicConfig(level=logging.DEBUG)


@pytest.fixture(scope='session')
def work_dir():
    yield yc.work_path()


@pytest.fixture(scope='session')
def generators_data(work_dir):
    export_archive = os.path.join(work_dir, 'prepared_data.tar.gz')
    with tarfile.open(export_archive) as export:
        export.extractall()
    yield work_dir


def source_path(path_in_arc):
    try:
        return yc.source_path(path_in_arc)
    except AttributeError:
        # running from pycharm
        return os.path.join(os.environ['ARC_DIR'], path_in_arc)


@pytest.fixture(scope='session')
def versions_file():
    yield source_path('extsearch/ymusic/scripts/reindex/conf/versions.txt')


@pytest.fixture(scope='session')
def descriptors_file():
    yield source_path('extsearch/ymusic/indexer/data/descriptors.json')


@pytest.fixture(scope='session')
def boosts_file():
    yield source_path('extsearch/ymusic/indexer/data/boosts.json')


@pytest.fixture(scope='session')
def genres_file(work_dir):
    genres_file = os.path.join(work_dir, 'genres.txt')
    yield genres_file


@pytest.fixture(scope='session')
def generators_kwargs(versions_file, descriptors_file, genres_file, boosts_file):
    yield {
        'versions_file': versions_file,
        'descriptors_file': descriptors_file,
        'genres_file': genres_file,
        'boosts_file': boosts_file,
        'local': True,
        'timestamp': dt.datetime.fromtimestamp(1609448401),  # 2022.01.01 something. timestamp for timezone safety
    }


def test__generate_tracks(generators_data, generators_kwargs):
    input_file_ = os.path.join(generators_data, 'tracks.yson')
    output_file_ = os.path.join(generators_data, 'tracks.yson.out')
    return generate_docs_with_joined_generator(gd.JoinAndGenerateTrack, generators_kwargs, input_file_, output_file_)


def test__generate_albums(generators_data, generators_kwargs):
    input_file_ = os.path.join(generators_data, 'albums.yson')
    output_file_ = os.path.join(generators_data, 'albums.yson.out')
    return generate_docs_with_joined_generator(gd.JoinAndGenerateAlbum, generators_kwargs, input_file_, output_file_)


def test__generate_artists(generators_data, generators_kwargs):
    input_file_ = os.path.join(generators_data, 'artists.yson')
    output_file_ = os.path.join(generators_data, 'artists.yson.out')
    return generate_docs_with_joined_generator(gd.JoinAndGenerateArtist, generators_kwargs, input_file_, output_file_)


def test__generate_playlists(generators_data, generators_kwargs):
    input_file_ = os.path.join(generators_data, 'playlists.yson')
    output_file_ = os.path.join(generators_data, 'playlists.yson.out')
    return generate_docs_with_joined_generator(gd.JoinAndGeneratePlaylist, generators_kwargs, input_file_, output_file_)


def generate_docs_with_joined_generator(generator_cls, generators_kwargs, input_file_, output_file_):
    generator = generator_cls('', '', **generators_kwargs)
    generator.init_job()
    out = gd.LinkIndexerOutput()
    generator_func = functools.partial(generator.generator.generate, out=out)
    generate_docs_per_line(generator_func, input_file_, output_file_)
    return yc.canonical_file(output_file_)


def test__generate_users(generators_data, generators_kwargs):
    input_file_ = os.path.join(generators_data, 'users.yson')
    output_file_ = os.path.join(generators_data, 'users.yson.out')
    generator = gd.GenerateUsers('', '', **generators_kwargs)

    generate_docs(generator.map_generate_user, input_file_, output_file_)

    return yc.canonical_file(output_file_)


def test__generate_genres(generators_data, generators_kwargs):
    input_file_ = os.path.join(generators_data, 'genres.yson')
    output_file_ = os.path.join(generators_data, 'genres.yson.out')
    generator = gd.GenerateGenres('', '', **generators_kwargs)

    generate_docs(generator.map_generate_genre, input_file_, output_file_)

    return yc.canonical_file(output_file_)


def generate_docs_per_line(generator_func, input_file_, output_file_):
    gen = _gen(generator_func, input_file_, batch=False)
    save_output(gen, output_file_)


def generate_docs(generator_func, input_file_, output_file_):
    gen = _gen(generator_func, input_file_, batch=True)
    save_output(gen, output_file_)


def _gen(generator_func, input_file_, batch=False):
    input_ = input_gen(input_file_)
    if batch:
        input_ = [input_]
    return (
        verbose(rec)
        for line in input_
        for rec in generator_func(line)
    )


def input_gen(input_file_):
    with open(input_file_, 'rb') as f:
        for line in f:
            yson_ = yson.loads(line.strip())

            binary_fields = {}
            for binary_field in ['service_ann_data', 'ann_data']:
                field_value = yson_.pop(binary_field, None)
                if field_value is not None:
                    binary_fields[binary_field] = field_value

            json_ = yson.convert.yson_to_json(yson_)
            for field, value in binary_fields.items():
                json_[field] = value

            yield json_


def verbose(rec):
    rec['value'] = vv.verbose_value(rec['value'])
    return rec


def save_output(output, output_file_):
    with open(output_file_, 'wb') as f:
        yson.dump(list(output), f, yson_format='pretty', sort_keys=True, indent=4)
