import argparse
import logging
import os
import re
import requests
import shutil
import tempfile

import devtools.ya.yalibrary.upload.lib as uploader
import search.geo.tools.production.geometasearch_configs_builder.lib as confbuilder


class Writer(object):
    def __init__(self, fd, banned_sources):
        self._fd = fd
        self._banned_sources = set(banned_sources)
        self._reset()

    def _reset(self):
        self._source = None
        self._group = []

    def add(self, line):
        self._group.append(line)

    def set_source(self, source):
        self._source = source

    def flush(self):
        if self._source not in self._banned_sources:
            for line in self._group:
                self._fd.write(line)
        self._reset()


def _filter_sources(path, banned):
    with open(path) as fd:
        lines = fd.readlines()

    with open(path, 'w') as fd:
        wr = Writer(fd, banned)
        for line in lines:
            if re.match(r'\s*<(Search|Aux)Source>', line):
                wr.flush()

            m = re.match(r'\s*ServerDescr (?P<source>\w+)\s*$', line)
            if m is not None:
                wr.set_source(m.group('source'))
            wr.add(line)

            if re.match(r'\s*</(Search|Aux)Source>', line):
                wr.flush()
        wr.flush()


def _copy_and_patch_config(src, dst):
    for line in src:
        line = re.sub(r'^(\s*WizardTimeout) \d+$', r'\g<1> 800000', line)
        line = re.sub('=zstd1,', '=lz4,', line)
        line = re.sub('MaxAttempts=3,', 'MaxAttempts=5,', line)
        line = re.sub('localhost:8032', 'localhost:${ MiddlePort and MiddlePort or 8032 }', line)
        line = line.replace(
            'recommender_server_prod.deploy_unit', 'recommender_server_prestable.deploy_unit'
        )  # TODO(sobols): change in gencfg?
        dst.write(line)


def update_configs(dstdir):
    UPPER_PORT = 8031
    MIDDLE_PORT = 8032
    LOCATION = 'all'

    CONFIGS = [
        ('addrsupper', 'upper.cfg'),
        ('addrsmiddle', 'middle.cfg'),
    ]

    for itype, fn in CONFIGS:
        logging.info('building %s -> %s', itype, fn)

        config = os.path.join(dstdir, '{}.tmp'.format(fn))
        geohost = os.path.join(dstdir, 'geohosts.json') if itype == 'addrsupper' else None
        tags = confbuilder.make_tags(itype, UPPER_PORT, MIDDLE_PORT, LOCATION)
        confbuilder.build(config, geohost, tags, should_resolve=True, force_tvm=False)

        with open(config, 'r') as src:
            with open(os.path.join(dstdir, fn), 'w') as dst:
                _copy_and_patch_config(src, dst)

        os.remove(config)

    _filter_sources(os.path.join(dstdir, 'middle.cfg'), ['BigBrother', 'GeoPersonalScoreProfiles', 'GEO_FAST_SNIPPETS'])


def _make_its_request(dstdir, tags):
    data = requests.post('http://its.yandex-team.ru/v1/process/', json=tags).json()
    for k, v in data.items():
        logging.info('ITS: saving %s', k)
        with open(os.path.join(dstdir, k), 'wb') as f:
            f.write(v.encode('utf-8'))


def update_its(dstdir):
    controls = os.path.join(dstdir, 'controls')
    os.mkdir(controls)
    _make_its_request(controls, ['a_itype_addrsupper', 'a_ctype_test', 'a_prj_addrs', 'a_geo_sas'])
    _make_its_request(controls, ['a_itype_addrsmiddle', 'a_ctype_test', 'a_prj_addrs', 'a_geo_sas'])


def upload(dstdir):
    logging.info('uploading to Sandbox...')
    files = [os.path.join(dstdir, fn) for fn in os.listdir(dstdir)]

    uploader.fix_logging()
    rid = uploader.do(
        paths=files,
        paths_root=dstdir,
        resource_description='Geometasearch configs for local run',
        ttl='inf',
        resource_owner='GEOMETA-SEARCH',
        should_tar=True,
    )

    logging.info('resource id is %s', rid)
    logging.info('now you have to manually replace it in ya.make')


def main():
    logging.basicConfig(format='%(asctime)s  %(message)s', level=logging.INFO)
    parser = argparse.ArgumentParser()
    parser.add_argument('--keep-temps', action='store_true', help='Do not remove the temp directory finally')
    args = parser.parse_args()

    temp_dir = tempfile.mkdtemp()
    logging.info('temp directory: %s', temp_dir)
    try:
        update_configs(temp_dir)
        update_its(temp_dir)
        upload(temp_dir)
    finally:
        if not args.keep_temps:
            shutil.rmtree(temp_dir)
