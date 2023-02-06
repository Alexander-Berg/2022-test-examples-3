# encoding: UTF-8

from hamcrest import *

from appcore.data.session import require_master_session
from appcore.injection import Injected
from appcore.tx.plugin import tx_manager
from dns_hosting.dao.domains import DomainRepository
from dns_hosting.dao.domains import RecordRepository
from dns_hosting.models.domains import Domain
from dns_hosting.models.domains import Record
from dns_hosting.models.domains import RecordType
from dns_hosting.services.auth.model import Scope
from dns_hosting.views.domains import RecordCollectionView, DkimRecordInitThread
from tests.dns_hosting.test_app import BaseAppTestCase


class DomainsApiTestCase(BaseAppTestCase):
    domain_repository = Injected('domain_repository')  # type: DomainRepository
    record_repository = Injected('record_repository')  # type: RecordRepository

    def setUp(self):
        super(DomainsApiTestCase, self).setUp()
        with self.app.app_context(), tx_manager:
            require_master_session()
            self.domain_repository.session.execute('''
                    DELETE FROM change_log;
                    DELETE FROM records;
                    DELETE FROM domains;
                ''')

            new_domain = Domain(
                name='example.com.',
                serial=1,
                org_id=1,
                revision=1,
                pdd_sync_enabled=False,
            )
            new_record = Record(
                name='www',
                type=RecordType.A,
                content='127.0.0.1',
                ttl=900,
            )

            new_domain.records.append(new_record)

            self.domain = self.domain_repository.save(new_domain)
            self.domain_repository.session.flush()
            self.record = self.domain.records[0]

            self.domain_repository.session.expunge(self.record)
            self.domain_repository.session.expunge(self.domain)

            new_domain = Domain(
                name=u'домен.рф.'.encode('idna'),
                serial=1,
                org_id=1,
                revision=1,
                pdd_sync_enabled=False,
            )
            new_record = Record(
                name='www',
                type=RecordType.A,
                content='127.0.0.1',
                ttl=900,
            )

            new_domain.records.append(new_record)

            self.punycode_domain = self.domain_repository.save(new_domain)
            self.domain_repository.session.flush()
            self.punycode_record = self.punycode_domain.records[0]

            self.domain_repository.session.expunge(self.punycode_record)
            self.domain_repository.session.expunge(self.punycode_domain)

            self.domains = ['example.ru.', 'новыйдомен1.рф.', 'xn--2-ctbgestdfdd4l.xn--p1ai.']
            self.exists_domains = {
                'example.com.': self.domain,
                'домен.рф.': self.punycode_domain,
            }
            self.exists_domains_records = {
                'example.com.': self.record,
                'домен.рф.': self.punycode_record,
            }
            self.auth_without_scopes({Scope.manage_any_domain})

        self.headers = {
            'X-Org-ID': 1,
            'X-UID': 1,
            'X-Real-IP': '127.0.0.1',
        }

    def assert_serial_changed(self, domain, old_serial):
        with self.app.test_client() as client:
            response = client.get(
                '/public/domains/{}/'.format(domain),
                headers=self.headers
            )
            assert_that(
                response.status_code,
                equal_to(200),
                response.data,
            )
            assert_that(
                response.json,
                has_entries(serial=not_(old_serial)),
            )

    def assert_pdd_sync_state(self, domain, enabled):
        with self.app.test_client() as client:
            response = client.get(
                '/public/domains/{}/'.format(domain),
                headers=self.headers
            )
            assert_that(
                response.status_code,
                equal_to(200),
                response.data,
            )
            assert_that(
                response.json,
                has_entries(pdd_sync_enabled=is_(enabled)),
            )

    def test_domain_enabled(self):
        with self.app.test_client() as client:
            for domain in self.domains:
                response = client.post(
                    '/public/domains/',
                    json={
                        'name': domain,
                    },
                    headers=self.headers
                )
                assert_that(
                    response.status_code,
                    equal_to(201),
                    response.data
                )

                with self.app.app_context(), tx_manager:
                    require_master_session()
                    domain_name = domain.decode('utf-8').encode('idna')
                    all_records = self.record_repository.find(domain_name)
                    assert_that(
                        all_records,
                        contains_inanyorder(
                            has_properties(
                                type=RecordType.MX,
                                name='@',
                                content='10 mx.yandex.net.',
                            ),
                            has_properties(
                                type=RecordType.TXT,
                                name='@',
                                content='"v=spf1 redirect=_spf.yandex.net"',
                            ),
                            has_properties(
                                type=RecordType.CNAME,
                                name='mail',
                                content='domain.mail.yandex.net.',
                            ),
                        )
                    )

    def test_get_domains(self):
        with self.app.test_client() as client:
            response = client.get(
                '/public/domains/',
                headers=self.headers
            )
            assert_that(
                response,
                has_properties(
                    status_code=equal_to(200),
                    json=has_entries(
                        items=contains_inanyorder(
                            has_entries(
                                name=u'example.com.',
                            ),
                            has_entries(
                                name=u'домен.рф.',
                            ),
                        )
                    )
                ),
                response.data,
            )

    def test_tech_domain_enabled_without_scope(self):
        self.auth_without_scopes({Scope.manage_any_domain,Scope.manage_technical})

        with self.app.test_client() as client:
            response = client.post(
                '/public/domains/',
                json={
                    'name': 'example.yaconnect.com.',
                },
                headers=self.headers
            )
            assert_that(
                response,
                has_properties(
                    status_code=equal_to(403),
                    json=has_entries(
                        code='scope_required'
                    ),
                ),
                response.data,
            )

    def test_domain_enabled_twice(self):
        with self.app.test_client() as client:
            for domain in self.domains:
                response = client.post(
                    '/public/domains/',
                    json={
                        'name': domain,
                    },
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(201), response.data)

                response = client.post(
                    '/public/domains/',
                    json={
                        'name': domain,
                    },
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(422), response.data)

    def test_domain_disabled(self):
        with self.app.test_client() as client:

            for domain, record in self.exists_domains.items():
                with self.app.app_context(), tx_manager:
                    require_master_session()
                    assert_that(
                        self.record_repository.find_all_by_domain_name(record.name),
                        not_(
                            empty()
                        )
                    )
                response = client.delete(
                    '/public/domains/{}'.format(domain),
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(200), response.data)

                with self.app.app_context(), tx_manager:
                    require_master_session()
                    assert_that(
                        self.record_repository.find_all_by_domain_name(record.name),
                        empty()
                    )

    def test_a_record_creation(self):
        with self.app.test_client() as client:
            for domain, record in self.exists_domains.items():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'name': '@',
                        'type': 'A',
                        'rdata': {'address': '127.0.0.2'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(201), response.data)

            self.assert_serial_changed(domain, record.serial)
            self.assert_pdd_sync_state(domain, False)

    def test_cname_record_creation(self):
        with self.app.test_client() as client:
            for domain, record in self.exists_domains.items():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'name': 'www1',
                        'type': 'CNAME',
                        'rdata': {'target': 'my.domain.com.'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(201), response.data)

            self.assert_serial_changed(domain, record.serial)
            self.assert_pdd_sync_state(domain, False)

    def test_uppercase_name_record_creation(self):
        with self.app.test_client() as client:
            for domain, record in self.exists_domains.items():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'name': '*.GAZ-784.gazprom',
                        'type': 'A',
                        'rdata': {'address': '127.0.0.2'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(201), response.data)

            self.assert_serial_changed(domain, record.serial)
            self.assert_pdd_sync_state(domain, False)

    def test_record_edit(self):
        with self.app.test_client() as client:
            for domain, record in self.exists_domains_records.items():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'id': record.id,
                        'name': '@',
                        'type': 'A',
                        'rdata': {'address': '127.0.0.2'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(
                    response.status_code,
                    equal_to(200),
                    response.data

                )

                self.assert_serial_changed(domain, record.domain.serial)
                self.assert_pdd_sync_state(domain, False)

    def test_cname_record_edit(self):
        with self.app.test_client() as client:
            for domain, record in self.exists_domains_records.items():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'id': record.id,
                        'name': '@',
                        'type': 'A',
                        'rdata': {'address': '127.0.0.2'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(
                    response.status_code,
                    equal_to(200),
                    response.data

                )

                self.assert_serial_changed(domain, record.domain.serial)
                self.assert_pdd_sync_state(domain, False)

    def test_record_creation_with_nonexistent_id(self):
        with self.app.test_client() as client:
            for domain in self.exists_domains_records.keys():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'id': 100500,
                        'name': '@',
                        'type': 'A',
                        'rdata': {'address': '127.0.0.1'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(response,
                has_properties(
                    status_code=equal_to(404),
                    json=has_entries(
                        code='record_not_found'
                    ),
                ),
                response.data,
            )

    def test_record_duplication_fails_properly(self):
        new_record = {
            'name': '@',
            'type': 'A',
            'rdata': {'address': '127.0.0.1'},
            'ttl': 900,
        }

        with self.app.test_client() as client:
            for domain, record in self.exists_domains_records.items():
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json=new_record,
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(201), response.data)

                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json=new_record,
                    headers=self.headers
                )
                assert_that(
                    response,
                    has_properties(
                        status_code=equal_to(422),
                        json=has_entries(
                            errors=has_entries(
                                _schema=has_item(
                                    RecordCollectionView.MSG_DUPLICATES_RECORD,
                                ),
                            ),
                        )
                    ),
                    response.data,
                )

                self.assert_serial_changed(domain, record.domain.serial)
                self.assert_pdd_sync_state(domain, False)

    def test_fails_properly_on_edit_makes_duplicated(self):

        with self.app.test_client() as client:
            for domain, record in self.exists_domains.items():
                record1 = {
                    'name': '@',
                    'type': 'A',
                    'rdata': {'address': '127.0.0.1'},
                    'ttl': 900,
                }
                record2 = {
                    'name': '@',
                    'type': 'A',
                    'rdata': {'address': '127.0.0.2'},
                    'ttl': 900,
                }
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json=record1,
                    headers=self.headers
                )

                assert_that(response.status_code, equal_to(201), response.data)

                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json=record2,
                    headers=self.headers
                )
                assert_that(response.status_code, equal_to(201), response.data)

                record2_id = response.json['id']
                record1['id'] = record2_id

                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json=record1,
                    headers=self.headers
                )
                assert_that(
                    response,
                    has_properties(
                        status_code=equal_to(422),
                        json=has_entries(
                            errors=has_entries(
                                _schema=has_item(
                                    RecordCollectionView.MSG_DUPLICATES_RECORD,
                                ),
                            ),
                        )
                    ),
                    response.data,
                )

                self.assert_serial_changed(domain, record.serial)
                self.assert_pdd_sync_state(domain, False)

    def test_cname_cname_conflict(self):
        with self.app.test_client() as client:
            for domain in self.exists_domains:
                client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'name': 'www1',
                        'type': 'CNAME',
                        'rdata': {'target': 'test1.com.'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'name': 'www1',
                        'type': 'CNAME',
                        'rdata': {'target': 'test2.com.'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(
                    response,
                    has_properties(
                        status_code=equal_to(422),
                        json=has_entries(
                            code='cname_record_conflict',
                            errors=has_entries(
                                _schema=has_item(
                                    RecordCollectionView.MSG_CNAME_CONFLICT,
                                ),
                            ),
                        )
                    ),
                    response.data,
                )

    def test_cname_a_conflict(self):
        with self.app.test_client() as client:
            for domain in self.exists_domains:
                response = client.post(
                    '/public/domains/{}/records/'.format(domain),
                    json={
                        'name': 'www',
                        'type': 'CNAME',
                        'rdata': {'target': 'test.com.'},
                        'ttl': 900,
                    },
                    headers=self.headers
                )
                assert_that(
                    response,
                    has_properties(
                        status_code=equal_to(422),
                        json=has_entries(
                            code='cname_record_conflict',
                            errors=has_entries(
                                _schema=has_item(
                                    RecordCollectionView.MSG_CNAME_CONFLICT,
                                ),
                            ),
                        )
                    ),
                    response.data,
                )


class DomainsAdminApiTestCase(BaseAppTestCase):
    domain_repository = Injected('domain_repository')  # type: DomainRepository
    record_repository = Injected('record_repository')  # type: RecordRepository

    def setUp(self):
        super(DomainsAdminApiTestCase, self).setUp()
        with self.app.app_context(), tx_manager:
            require_master_session()
            self.domain_repository.session.execute('''
                    DELETE FROM change_log;
                    DELETE FROM records;
                    DELETE FROM domains;
                ''')

            new_domain = Domain(
                name='some.example.com.',
                serial=1,
                org_id=1,
                revision=1,
                pdd_sync_enabled=False,
            )
            self.domain_repository.save(new_domain)
            self.domain_repository.session.flush()
            self.auth_without_scopes(
                {Scope.fire_events, Scope.manage_technical, Scope.authorize_user_by_xuid}
            )
        self.headers = {
            'X-Org-ID': 1,
            'X-Real-IP': '127.0.0.1',
        }

    def test_enable_domain(self):
        with self.app.test_client() as client:
            response = client.post(
                '/public/domains/',
                json={
                    'name': 'any.domain.com.',
                },
                headers=self.headers
            )
            assert_that(
                response.status_code,
                equal_to(201),
                response.data
            )

    def test_mange_records(self):
        with self.app.test_client() as client:
            response = client.post(
                '/public/domains/some.example.com./records/',
                json={
                    'name': '*.GAZ-784.gazprom',
                    'type': 'A',
                    'rdata': {'address': '127.0.0.2'},
                    'ttl': 900,
                },
                headers=self.headers
            )
            assert_that(response.status_code, equal_to(201), response.data)


class TestDkimRecordInitThread(BaseAppTestCase):
    domain_repository = Injected('domain_repository')  # type: DomainRepository
    record_repository = Injected('record_repository')  # type: RecordRepository

    def setUp(self):
        super(TestDkimRecordInitThread, self).setUp()
        with self.app.app_context(), tx_manager:
            require_master_session()
            self.domain_repository.session.execute('''
                    DELETE FROM change_log;
                    DELETE FROM records;
                    DELETE FROM domains;
                ''')
            self.domain_name = 'some.example.com.'

            new_domain = Domain(
                name=self.domain_name,
                serial=1,
                org_id=1,
                revision=1,
                pdd_sync_enabled=False,
            )
            self.domain_repository.save(new_domain)
            self.domain_repository.session.flush()

    def test_init_dkim(self):
        with self.app.app_context(), tx_manager:
            assert_that(
                self.record_repository.find_all_by_domain_name_and_record_type(
                    self.domain_name,
                    RecordType.TXT
                ),
                has_length(0)
            )

            thread = DkimRecordInitThread(self.domain_name)
            thread.start()
            thread.join()

            assert_that(
                self.record_repository.find_all_by_domain_name_and_record_type(
                    self.domain_name,
                    RecordType.TXT
                ),
                contains(
                    has_properties(
                        name='mail._domainkey',
                        ttl=21600,
                        content=starts_with('"v=DKIM1; k=rsa; t=s; p=')
                    )
                )
            )
