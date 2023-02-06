# coding: utf-8


from datetime import datetime
from pathlib import PosixPath
from socket import gethostname
from market.sre.tools.dpreparer.lib.sections.certificate import CertificateSection


def test_certificate_normilize_data(fxt_certificate):
    certificate_data_minimal = CertificateSection('certificate_minimal_data', {'name': 'selfsigned', 'path': '.'})
    certificate_data_minimal._normalize_data()
    assert fxt_certificate.data == certificate_data_minimal.data


def test_get_cert_files_split(fxt_certificate):
    expected = {
        'key': {
            'path': PosixPath('./selfsigned.key'),
            'mode': 0o0600
        },
        'x509': {
            'path': PosixPath('./selfsigned.pem'),
            'mode': 0o0644
        }
    }
    assert expected == fxt_certificate._get_cert_files('key', 'x509')


def test_get_cert_files_single(fxt_certificate):
    expected = {
        'keyx509': {
            'path': PosixPath('./selfsigned.crt'),
            'mode': 0o0600
        }
    }
    fxt_certificate.data['split'] = False
    assert expected == fxt_certificate._get_cert_files('key', 'x509')


def test_pkey_len(fxt_certificate):
    assert fxt_certificate.data['key_len'] == fxt_certificate.pkey.bits()


def test_x509_signature_algorithm(fxt_certificate):
    expected = '{}WithRSAEncryption'.format(fxt_certificate.data['digest']).encode()
    assert expected == fxt_certificate.x509.get_signature_algorithm()


def test_x509_years(fxt_certificate):
    valid_years = datetime.strptime(fxt_certificate.x509.get_notAfter().decode(), '%Y%m%d%H%M%SZ').year - datetime.now().year
    assert fxt_certificate.data['years'] == valid_years


def test_x509_subject(fxt_certificate):
    expected = [
        (b'C', fxt_certificate.data['subject']['country'].encode()),
        (b'L', fxt_certificate.data['subject']['locality'].encode()),
        (b'O', fxt_certificate.data['subject']['organization'].encode()),
        (b'OU', fxt_certificate.data['subject']['organizational_unit'].encode()),
        (b'CN', gethostname().encode()),
    ]
    assert expected == fxt_certificate.x509.get_subject().get_components()
