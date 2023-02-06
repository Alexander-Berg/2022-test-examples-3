# -*- encoding: utf-8 -*-

"""Tests promo_indexer.do_build_and_upload_quick_yt_promo_details locally.
"""

import logging
import os
import md5
import base64
import pytz

import pytest
import yatest.common
from datetime import timedelta, datetime
from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.pylibrary.graphite.graphite import DummyGraphite

from market.pylibrary.s3.s3.stub.s3_bucket_emulation import StubS3Client, S3BucketEmulation

import market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer as promo_indexer


BUCKET_NAME = 'the_bucket'
MMAP_FAKE_SIZE = 10


def create_table(yt, table_name, schema):
    attributes = dict()
    if schema:
        attributes['schema'] = schema

    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=attributes
    )


def _create_blue_3p_promos_table(yt, data, table_name):
    create_table(yt, table_name,
                 schema=[dict(name='description', type='string'),
                         dict(name='end_date', type='uint64'),
                         dict(name='promo_id', type='string'),
                         dict(name='start_date', type='uint64'),
                         ])
    yt.write_table(table_name, data)


def _create_msku_table(yt, data, table_name):
    create_table(yt, table_name,
                 schema=[dict(name='msku', type='string'),
                         dict(name='old_price', type='uint64'),
                         dict(name='price', type='uint64'),
                         dict(name='promo_id', type='string'),
                         ])
    yt.write_table(table_name, data)


def stub_build_yt_promo_details_ok(config, output_dir, dynamic_mode, generation):
    """A promo_indexer._buils_yt_promo_details stub that
    writes an empty file to the output directory.
    """
    mmap_path = os.path.join(output_dir, 'yt_promo_details.mmap')
    with open(mmap_path, 'w') as test_mmap:
        test_mmap.write(os.urandom(MMAP_FAKE_SIZE))


def stub_build_yt_promo_details_bad(config, output_dir, dynamic_mode, generation):
    """A promo_indexer._buils_yt_promo_details stub that
    always raises an exception.
    """
    raise RuntimeError()


def stub_generate_promos_hash_from_filename(promos_filename):
    """Calculates fake hash from name of file with promos."""
    hash_of_current_promos = md5.new()
    hash_of_current_promos.update(promos_filename)
    return base64.b64encode(hash_of_current_promos.hexdigest())


def stub_generate_promos_hash_const(promos_filename):
    """Calculates fake constant hash."""
    return 'test_hash'


def stub_path_of_yt_promo_stat(generation, filename):
    """Returns path of yt promo statistics."""
    return source_path('market/idx/marketindexer/medium_tests/data/yt_promo_stats.pb')


def stub_path_of_yt_promo_stat_bad(generation, filename):
    """Returns path of bad formed yt promo statistics."""
    stat_path = 'yt_promo_stats.pb'
    with open(stat_path, 'w') as test_stats:
        test_stats.write('Wrong statistics file')
    return stat_path


def create_default_yt_client_for_promos(yt_stuff, config, len=1):
    """Creates default yt_client for testing promos.
    Some default data for promos is prepared."""
    promo_data = []
    msku_data = []
    for i in range(len):
        str_index = str(i)
        promo = {
            'description': 'TestBlue3P_' + str_index,
            'end_date': 1528060000,
            'promo_id': 'PromoId_' + str_index,
            'start_date': 1528059600,
        }
        promo_data.append(promo)

        msku = {
            'msku': 'TestMSKU_' + str_index,
            'price': (i + 1) * 100,
            'old_price': (i + 1) * 1000,
            'promo_id': 'PromoId_' + str_index,
        }
        msku_data.append(msku)

    yt_client = yt_stuff.get_yt_client()
    _create_blue_3p_promos_table(yt_client, promo_data, config.yt_blue_3p_promo_table_name)
    _create_msku_table(yt_client, msku_data, config.yt_msku_table_name)

    return yt_client


class StubConfig(object):
    """Stub config class, only needed because object() doesn't have __dict__.
    """
    pass


def make_old_generations(
    working_dir,
    generations,
    qpromos
):
    """Stubs out some old generations in both local FS and the S3 bucket
    so that quick promo function has something to delete.
    """
    quick_promo_dir_path = os.path.join(
        working_dir,
        promo_indexer.QUICK_PROMO_WD_NAME,
    )
    os.makedirs(quick_promo_dir_path)

    for generation in generations:
        os.mkdir(os.path.join(quick_promo_dir_path, generation))

    for generation in generations:
        dst_path = promo_indexer.make_s3_promo_details_name(generation)
        qpromos.s3_client.write(BUCKET_NAME, dst_path, '')

    newest_generation = max(generations)
    qpromos.meta.recent_generation = newest_generation
    qpromos.meta.last_modification_timestamp = newest_generation
    qpromos.upload_promos_meta_to_mds()


def working_dir():
    """A local directory that substitutes for /var/lib/yandex/indexer/market.
    """
    dir_path = yatest.common.test_output_path('working_dir')
    os.makedirs(dir_path)
    return dir_path


@pytest.fixture(scope="module")
def log():
    """A real logger that passes all requests to a null handler.
    """
    logger = logging.Logger('')
    logger.propagate = False
    logger.setLevel(logging.DEBUG)
    logger.addHandler(logging.NullHandler())
    return logger


@pytest.fixture()
def config():
    """Default config."""
    config = StubConfig()
    config.yt_proxy = 'test_yt_proxy'
    config.yt_tokenpath = 'test_yt_tokenpath'
    config.quick_promo_generations_to_keep = 3
    config.market_idx_bucket = BUCKET_NAME
    config.working_dir = working_dir()
    config.s3_bucket_emulation = S3BucketEmulation()
    config.yt_blue_3p_promo_table_name = 'fake/blue_3p'
    config.yt_msku_table_name = 'fake/msku'
    config.quick_promo_max_promos = 1
    config.quick_promo_max_msku = 1
    config.quick_promo_max_mmap_size = MMAP_FAKE_SIZE
    config.use_fast_promos_mode_in_qpromos = False
    return config


def test_cold_start(monkeypatch, config, log):
    """Tests that do_build_and_upload_quick_yt_promo_details
    can create all of its directories from scratch.
    """
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    qpromos = promo_indexer.QPromos(config, log)

    generation = '20180527_224000'
    qpromos.do_build_and_upload_quick_yt_promo_details(generation)

    # check that the generation was created in S3
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(generation))

    # check that S3's recent generation in promos meta now points to our generation
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promos_meta())
    qpromos.download_meta_from_mds()
    assert generation == qpromos.meta.recent_generation

    # check that the generation was created in working_dir
    assert os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            generation,
        )
    )


def test_deletes_old_generations(monkeypatch, config, log):
    """Tests that do_build_and_upload_quick_yt_promo_details
    deletes old generations.
    """
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    qpromos = promo_indexer.QPromos(config, log)

    monkeypatch.setattr(
        qpromos.yt_promo_stat,
        '_generate_quick_promos_file_path',
        stub_path_of_yt_promo_stat
    )

    # stuff the bucket with old generations
    oldest_generation = '20170105_050505'
    make_old_generations(
        config.working_dir,
        [
            '20170505_050505',
            '20170405_050505',
            '20170305_050505',
            '20170205_050505',
            oldest_generation,
        ],
        qpromos
    )
    generation = '20180527_224000'

    qpromos.do_build_and_upload_quick_yt_promo_details(generation)

    # check that the generation was created in S3
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(generation))

    # checks that the S3's recent generation in promos meta now points to our generation
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promos_meta())
    qpromos.download_meta_from_mds()
    assert generation == qpromos.meta.recent_generation

    # check that our generation was created in working_dir
    assert os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            generation,
        )
    )
    # check that the oldest generation was cleaned from the S3
    assert not config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(oldest_generation))

    # check that the oldest generation was cleaned from working_dir
    assert not os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            oldest_generation,
        )
    )


def test_bad(monkeypatch, config, log):
    """Tests that do_build_and_upload_quick_yt_promo_details
    fails when the underlying _build_yt_promo_details fails
    """
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_bad,
    )

    qpromos = promo_indexer.QPromos(config, log)

    generation = '20180527_224000'

    with pytest.raises(RuntimeError):
        qpromos.do_build_and_upload_quick_yt_promo_details(generation)

    # check that the generation was not created in S3
    assert not config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(generation))


def test_quick_promos_generation_for_modified(monkeypatch, config, log, yt_stuff):
    """Tests that modified promos since last complete generation
    are processed by quick pipeline"""
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    qpromos = promo_indexer.QPromos(config, log)
    yt_client = create_default_yt_client_for_promos(yt_stuff, config)

    # upload old generation
    timezone = pytz.timezone('Europe/Moscow')
    generation = (datetime.now(tz=timezone) - timedelta(hours=1)).strftime('%Y%m%d_%H%M%S')
    qpromos.do_build_and_upload_quick_yt_promo_details(generation)

    assert qpromos.check_on_fresh_promos(yt_client)


def test_no_quick_promos_generation_for_not_modified(monkeypatch, config, log, yt_stuff):
    """Tests that not modified promos since last complete generation
    are not processed by quick pipeline"""
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    qpromos = promo_indexer.QPromos(config, log)
    yt_client = create_default_yt_client_for_promos(yt_stuff, config)

    # upload new generation
    timezone = pytz.timezone('Europe/Moscow')
    # PROMOHOTLINE-538 - timedelta(hours=1) --> timedelta(minutes=1) (временно)
    generation = (datetime.now(tz=timezone) + timedelta(minutes=1)).strftime('%Y%m%d_%H%M%S')
    qpromos.do_build_and_upload_quick_yt_promo_details(generation)
    assert not qpromos.check_on_fresh_promos(yt_client)


def test_fresh_and_updated_promos_uploaded(monkeypatch, config, log):
    """Tests fresh promos, which were changed by loyalty, uploaded to mds and meta updated."""
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.get_promos_hash',
        stub_generate_promos_hash_from_filename,
    )

    qpromos_old = promo_indexer.QPromos(config, log)

    # upload old generation
    now = datetime.utcnow()
    old_generation = (now - timedelta(hours=5)).strftime('%Y%m%d_%H%M%S')
    qpromos_old.do_build_and_upload_quick_yt_promo_details(old_generation)

    # check that the old generation was created in S3
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(old_generation))

    # check that S3's contains meta with correct values
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promos_meta())
    qpromos_old.download_meta_from_mds()
    assert old_generation == qpromos_old.meta.recent_generation
    assert old_generation == qpromos_old.meta.last_modification_timestamp

    # check that the old generation was created in working_dir
    assert os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            old_generation,
        )
    )

    qpromos_new = promo_indexer.QPromos(config, log)
    # fresh promos
    new_generation = now.strftime('%Y%m%d_%H%M%S')
    qpromos_new.do_build_and_upload_quick_yt_promo_details(new_generation)

    # check that the nre generation was created in S3
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(new_generation))

    # check that S3's contains updated meta
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promos_meta())
    qpromos_new.download_meta_from_mds()
    assert new_generation == qpromos_new.meta.recent_generation
    assert new_generation == qpromos_new.meta.last_modification_timestamp

    # check that the new generation was created in working_dir
    assert os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            new_generation,
        )
    )


def test_fresh_but_not_updated_promos_not_uploaded(monkeypatch, config, log):
    """Tests fresh promos, which weren't changed by loyalty, not uploaded to mds, but meta updated."""
    # PROMOHOTLINE-538 - тест временно отключен
    '''
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.get_promos_hash',
        stub_generate_promos_hash_const,
    )

    qpromos_old = promo_indexer.QPromos(config, log)

    # upload old generation
    moscow_timezone = pytz.timezone('Europe/Moscow')
    now = datetime.now(tz=moscow_timezone)
    old_generation = (now - timedelta(minutes=20)).strftime('%Y%m%d_%H%M%S')
    qpromos_old.do_build_and_upload_quick_yt_promo_details(old_generation)

    # check that the old generation was created in S3
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(old_generation))

    # check that S3's contains meta with correct values
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promos_meta())
    qpromos_old.download_meta_from_mds()
    assert old_generation == qpromos_old.meta.recent_generation
    assert old_generation == qpromos_old.meta.last_modification_timestamp

    # check that the generation was created in working_dir
    assert os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            old_generation,
        )
    )

    qpromos_new = promo_indexer.QPromos(config, log)

    # fresh promos, but not updated by loyalty.
    # Update happened less than one hour after recent_generation (interval from now-20 to now+30),
    # so we do not upload new file to mds
    not_yet_expired_time = datetime.strptime(qpromos_new.meta.recent_generation, '%Y%m%d_%H%M%S') + timedelta(minutes=30)
    new_generation = not_yet_expired_time.strftime('%Y%m%d_%H%M%S')
    with pytest.raises(promo_indexer.QPromosNoDifferenceError) as e:
        qpromos_new.do_build_and_upload_quick_yt_promo_details(new_generation)

    assert str(e.value) == 'No difference between uploaded to mds promos and promos from loyalty'

    # check that the new generation was not created in S3
    assert not config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(new_generation))

    # check that S3's contains updated meta
    assert config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promos_meta())
    qpromos_new.download_meta_from_mds()
    assert old_generation == qpromos_new.meta.recent_generation
    assert new_generation == qpromos_new.meta.last_modification_timestamp

    # check that the new generation was not created in working_dir
    assert not os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            new_generation,
        )
    )
    '''


def test_qpromos_meet_size_limits(monkeypatch, config, log, yt_stuff):
    """Tests that size limits are met."""
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    qpromos = promo_indexer.QPromos(config, log)
    yt_client = create_default_yt_client_for_promos(yt_stuff, config)

    assert qpromos.check_qpromos_size_constraints(yt_client)


def test_qpromos_exceed_size_limits(monkeypatch, config, log, yt_stuff):
    """Tests that size limits are exceeded."""
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    qpromos = promo_indexer.QPromos(config, log)
    yt_client = create_default_yt_client_for_promos(yt_stuff, config, 2)

    assert not qpromos.check_qpromos_size_constraints(yt_client)


def test_exceed_mmap_file_size_limits(monkeypatch, config, log):
    """Tests that mmap-file size limits are exceeded."""
    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer.S3Client',
        StubS3Client,
    )

    monkeypatch.setattr(
        'market.idx.pylibrary.mindexer_core.promo_indexer.promo_indexer._build_yt_promo_details',
        stub_build_yt_promo_details_ok,
    )

    config.quick_promo_max_mmap_size = MMAP_FAKE_SIZE - 1
    qpromos = promo_indexer.QPromos(config, log)

    # upload generation
    timezone = pytz.timezone('Europe/Moscow')
    generation = datetime.now(tz=timezone).strftime('%Y%m%d_%H%M%S')
    with pytest.raises(Exception) as e:
        qpromos.do_build_and_upload_quick_yt_promo_details(generation)

    assert str(e.value) == 'Constraint on mmap size for quick promos pipeline is exceeded'
    # check that the generation was not created in S3
    assert not config.s3_bucket_emulation.has_path(BUCKET_NAME, promo_indexer.make_s3_promo_details_name(generation))

    # check that the generation was not created in working_dir
    assert not os.path.exists(
        os.path.join(
            config.working_dir,
            promo_indexer.QUICK_PROMO_WD_NAME,
            generation,
        )
    )


def test_promo_statistics_red_correclty(monkeypatch, config, log):
    """Tests that promo statistics have correct values."""
    fake_generation = '20180907_165722'
    yt_promo_stat_expected = {
        'number_of_promos': 3,
        'number_of_msku': 3,
        'generation_time': 123,
        'mmap_size': 904,
    }

    yt_promo_stat = promo_indexer.YtPromosStat(config, log, qpromos=True, graphite_cls=DummyGraphite)
    monkeypatch.setattr(
        yt_promo_stat,
        '_generate_quick_promos_file_path',
        stub_path_of_yt_promo_stat
    )

    yt_promo_stat_real = yt_promo_stat.read_yt_promo_stats(fake_generation)
    assert yt_promo_stat_expected == yt_promo_stat_real


def test_promo_statistics_send_to_graphite(monkeypatch, config, log):
    """Tests that promo statistics are sent to Graphite."""
    fake_generation = '20180907_165722'

    yt_promo_stat = promo_indexer.YtPromosStat(config, log, qpromos=True, graphite_cls=DummyGraphite)
    monkeypatch.setattr(
        yt_promo_stat,
        '_generate_quick_promos_file_path',
        stub_path_of_yt_promo_stat
    )

    yt_promo_stat.send_promos_stat_to_graphite(fake_generation)


def test_promo_bad_statistics(monkeypatch, config, log):
    """Tests that there is no fail on bad formed statistics."""
    fake_generation = '20180907_165722'

    yt_promo_stat = promo_indexer.YtPromosStat(config, log, qpromos=True, graphite_cls=DummyGraphite)
    monkeypatch.setattr(
        yt_promo_stat,
        '_generate_quick_promos_file_path',
        stub_path_of_yt_promo_stat_bad
    )

    yt_promo_stat.send_promos_stat_to_graphite(fake_generation)
