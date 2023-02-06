from __future__ import print_function
from OpenSSL import crypto
from shutil import rmtree
from hashlib import sha256
from struct import unpack
from threading import Lock
import os


class SSLCertCache(object):
    def __init__(self, cachedir):
        self.cachedir = cachedir
        self._verify_cachedir()

    def _verify_cachedir(self):
        if os.path.exists(self.cachedir):
            rmtree(self.cachedir)
        os.mkdir(self.cachedir)

    def get_cert_key_files(self, host):
        return os.path.join(self.cachedir, '{}.pem'.format(host)), os.path.join(self.cachedir, '{}.key'.format(host))

    def has_cert(self, certfile, keyfile):
        return os.path.exists(certfile) and os.path.exists(keyfile)


class SSLCertManager(object):
    def __init__(self, ca_cert, ca_privkey, cache):
        self.ca_cert = crypto.load_certificate(crypto.FILETYPE_PEM, ca_cert)
        self.ca_key = crypto.load_privatekey(crypto.FILETYPE_PEM, ca_privkey)
        self.cache = cache
        self.mutex = Lock()
        self.serial = 1

    @staticmethod
    def _get_cert_serial(host):
        return unpack('I', sha256(host).digest()[:4])[0]

    def _gen_cert_key_pair(self, host, certfile, keyfile):
        self.serial += 1
        print('generating cert {} for {}'.format(self.serial, host))
        newkey = crypto.PKey()
        newkey.generate_key(crypto.TYPE_RSA, 1024)
        newcert = crypto.X509()
        newcert.set_version(2)
        newcert.set_serial_number(self.serial)
        subj = newcert.get_subject()
        subj.commonName = host
        newcert.add_extensions([
            crypto.X509Extension('basicConstraints', True, 'CA:FALSE'),
            crypto.X509Extension('keyUsage', True, 'digitalSignature, keyEncipherment'),
            crypto.X509Extension('extendedKeyUsage', True, 'TLS Web Server Authentication'),
            crypto.X509Extension('subjectAltName', False, 'DNS:{}'.format(host))
        ])
        newcert.set_issuer(self.ca_cert.get_subject())
        newcert.set_pubkey(newkey)
        newcert.gmtime_adj_notBefore(0)
        newcert.gmtime_adj_notAfter(365*24*60*60)
        newcert.sign(self.ca_key, 'sha256')
        open(certfile, 'w').write(crypto.dump_certificate(crypto.FILETYPE_PEM, newcert))
        open(keyfile, 'w').write(crypto.dump_privatekey(crypto.FILETYPE_PEM, newkey))

    def get_host_certificate(self, host):
        with self.mutex:
            certfile, keyfile = self.cache.get_cert_key_files(host)
            if not self.cache.has_cert(certfile, keyfile):
                self._gen_cert_key_pair(host, certfile, keyfile)
            else:
                print('reusing certificate for {}'.format(host))
            return certfile, keyfile
