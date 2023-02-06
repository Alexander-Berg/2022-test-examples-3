# coding: utf-8


import pytest
import yatest.common

from market.sre.tools.dpreparer.lib.sections.certificate import CertificateSection


@pytest.fixture(scope='module')
def fxt_manifest():
    return yatest.common.source_path('market/sre/tools/dpreparer/tests/files/manifest.yml')


@pytest.fixture(scope='module')
def fxt_manifest_err():
    return yatest.common.source_path('market/sre/tools/dpreparer/tests/files/manifest_error.yml')


@pytest.fixture(scope='module')
def fxt_manifest_mininal():
    return yatest.common.source_path('market/sre/tools/dpreparer/tests/files/manifest_minimal.yml')


@pytest.fixture(scope='module')
def fxt_json_manifest():
    return [
        {
            'dirs': [
                {'name': 'output_dir/test1'},
                {'name': 'output_dir/test2'}
            ],
            'files': [
                {'name': 'output_dir/test4'},
                {'name': 'output_dir/test5'}
            ],
            'links': [{'name': 'output_dir/test3', 'target': 'output_dir/test1'}],
            'name': 'application',
            'split_secrets': [{'name': 'output_dir/secret.json'}],
            'templater': {
                'destination': 'output_dir/test',
                'source': 'test_templates'
            }
        },
        {
            'dirs': [
                {'name': 'output_dir2/test1'},
                {'name': 'output_dir2/test2'}
            ],
            'files': [
                {'name': 'output_dir2/test4'},
                {'name': 'output_dir2/test5'}
            ],
            'links': [{'name': 'output_dir2/test3', 'target': 'output_dir2/test1'}],
            'name': 'additional',
            'templater': {
                'destination': 'output_dir2/test',
                'source': 'test_templates2'
            }
        },
        {'name': 'first_include'},
        {'name': 'second_include'}
    ]


@pytest.fixture(scope='module')
def fxt_not_exists_mfst():
    return 'test_templates/not_exists_manifest.yaml'


@pytest.fixture(scope='module')
def spec_container_good():
    return yatest.common.source_path('market/sre/tools/dpreparer/tests/files/human_readable_current_spec.json')


@pytest.fixture(scope='module')
def spec_container_bad():
    return yatest.common.source_path('market/sre/tools/dpreparer/tests/files/blablabla_spec.json')


@pytest.fixture(scope='module')
def meta_secret_file():
    return yatest.common.source_path('market/sre/tools/dpreparer/tests/files/secrets/secret.json')


@pytest.fixture(scope='module')
def fxt_certificate():
    data = {
        'name': 'selfsigned',
        'path': '.',
        'years': 10,
        'key_len': 2048,
        'split': True,
        'digest': 'sha256',
        'subject': {
            'country': 'RU',
            'locality': 'Moscow',
            'organization': 'Yandex LLC',
            'organizational_unit': 'ITO'
        }
    }
    crt = CertificateSection('certificate_full_data', data)
    crt.pkey = crt._generate_key()
    crt.x509 = crt._generate_x509(crt.pkey)
    return crt
