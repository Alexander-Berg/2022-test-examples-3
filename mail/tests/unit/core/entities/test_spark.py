from datetime import date, datetime

import pytest

from mail.payments.payments.core.entities.enums import MerchantType, PersonType
from mail.payments.payments.core.entities.merchant import AddressData, OrganizationData, PersonData
from mail.payments.payments.core.entities.spark import (
    LeaderData, PhoneData, SparkAddressData, SparkData, SparkOrganizationData
)


class TestSparkData:
    @pytest.fixture
    def leader_name(self):
        return 'Волож Аркадий Юрьевич'

    @pytest.fixture
    def leader_birth_date(self):
        return date(1964, 2, 11)

    @pytest.fixture
    def organization_factory(self):
        def _inner(prefix):
            return OrganizationData(
                type=MerchantType.IP,
                name=f'{prefix}_organization_name',
                english_name=f'{prefix}_english_name',
                full_name=f'{prefix}_full_name',
                inn=f'{prefix}_inn',
                kpp=f'{prefix}_kpp',
                ogrn=f'{prefix}_ogrn',
            )
        return _inner

    @pytest.fixture
    def person_factory(self):
        def _inner(person_type):
            return PersonData(
                type=person_type,
                name='person_name',
                surname='person_surname',
                patronymic='person_patronymic',
                birth_date=None,
                email='person_email',
                phone='person_phone',
            )
        return _inner

    @pytest.fixture
    def address_factory(self):
        def _inner(address_type, prefix):
            return AddressData(
                type=address_type,
                city=f'{prefix}_city',
                country=f'{prefix}_country',
                home=f'{prefix}_home',
                street=f'{prefix}_street',
                zip=f'{prefix}_zip',
            )
        return _inner

    @pytest.fixture
    def spark_data(self, organization_factory, leader_name, leader_birth_date, address_factory):
        return SparkData(
            spark_id='spark_id',
            registration_date=None,
            organization_data=SparkOrganizationData(
                organization=organization_factory('spark'),
                actual_date=date(2020, 12, 1),
            ),
            okved_list=[],
            leaders=[
                LeaderData(
                    name='',
                    birth_date=date(1965, 3, 7),
                    actual_date=date(2020, 12, 5),
                ),
                LeaderData(
                    name=leader_name,
                    birth_date=leader_birth_date,
                    actual_date=date(2020, 12, 4),
                ),
            ],
            addresses=[
                SparkAddressData(
                    address=address_factory('legal', 'spark'),
                    actual_date=date(2020, 12, 2),
                )
            ],
            phones=[
                PhoneData(
                    code='111',
                    number='',
                    verification_date=date(2020, 12, 8),
                ),
                PhoneData(
                    code='',
                    number='222 22 22',
                    verification_date=date(2020, 12, 7),
                ),
                PhoneData(
                    code='333',
                    number='333 33 33',
                    verification_date=date(2020, 12, 6),
                ),
            ],
        )

    stored_ts_old_variants = (
        pytest.param(datetime(2019, 1, 1), id='stored_ts_older'),
    )
    stored_ts_new_variants = (
        pytest.param(None, id='stored_ts_empty'),
        pytest.param(datetime(2020, 12, 9), id='stored_ts_newer'),
    )
    stored_ts_all_variants = (
        *stored_ts_old_variants,
        *stored_ts_new_variants,
    )

    def test_phone__filled(self, spark_data):
        assert spark_data.phones[2] == spark_data.phone

    def test_phone__empty(self, spark_data):
        spark_data.phones = None
        assert spark_data.phone is None

    def test_leader__filled(self, spark_data, leader_name, leader_birth_date):
        assert spark_data.leaders[1] == spark_data.leader

    @pytest.mark.parametrize('leader_name', ('',))
    def test_leader__empty(self, spark_data):
        assert spark_data.leader is None

    def test_leader_name__filled(self, spark_data, leader_name):
        assert spark_data.leader_name == leader_name

    @pytest.mark.parametrize('leader_name', ('',))
    def test_leader_name__empty(self, spark_data):
        assert spark_data.leader_name is None

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_organization__no_organuzation(self, spark_data, organization_factory, stored_ts):
        assert organization_factory('spark') == spark_data.get_patched_organization(organization=None,
                                                                                    stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_organization__empty_organization(self, spark_data, organization_factory, stored_ts):
        assert organization_factory('spark') == spark_data.get_patched_organization(organization=OrganizationData(),
                                                                                    stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_new_variants)
    def test_get_patched_organization__no_patch(self, spark_data, organization_factory, stored_ts):
        filled_org = organization_factory('balance')
        assert filled_org == spark_data.get_patched_organization(organization=filled_org,
                                                                 stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_old_variants)
    def test_get_patched_organization__patch(self, spark_data, organization_factory, stored_ts):
        filled_org = organization_factory('balance')
        assert organization_factory('spark') == spark_data.get_patched_organization(organization=filled_org,
                                                                                    stored_ts=stored_ts)

    @pytest.mark.parametrize('leader_name', ('',))
    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_persons__empty_name(self, person_factory, spark_data, rands, stored_ts):
        persons = [person_factory(PersonType.CEO)]
        assert persons == spark_data.get_patched_persons(persons=persons,
                                                         leader_name=rands(),
                                                         leader_surname=rands(),
                                                         leader_patronymic=None,
                                                         stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_persons__no_ceo(self,
                                         spark_data,
                                         person_factory,
                                         leader_name,
                                         leader_birth_date,
                                         rands,
                                         stored_ts):
        signer = person_factory(PersonType.SIGNER)
        name, patronymic, surname = leader_name.split()
        actual = spark_data.get_patched_persons(persons=[signer],
                                                leader_name=name,
                                                leader_surname=surname,
                                                leader_patronymic=patronymic,
                                                stored_ts=stored_ts)
        assert [
            signer,
            PersonData(
                type=PersonType.CEO,
                name=name,
                surname=surname,
                patronymic=patronymic,
                birth_date=leader_birth_date,
                email='',
                phone='+7 333 333 33 33',
            ),
        ] == actual

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_persons__empty_phone(self, spark_data, person_factory, rands, stored_ts):
        ceo = person_factory(PersonType.CEO)
        ceo.phone = ''
        actual = spark_data.get_patched_persons(persons=[ceo],
                                                leader_name=rands(),
                                                leader_surname=rands(),
                                                leader_patronymic=None,
                                                stored_ts=stored_ts)

        assert ['+7 333 333 33 33'] == [person.phone for person in actual]

    @pytest.mark.parametrize('stored_ts', stored_ts_old_variants)
    def test_get_patched_persons__patch_phone(self, spark_data, person_factory, rands, stored_ts):
        actual = spark_data.get_patched_persons(persons=[person_factory(PersonType.CEO)],
                                                leader_name=rands(),
                                                leader_surname=rands(),
                                                leader_patronymic=None,
                                                stored_ts=stored_ts)
        assert ['+7 333 333 33 33'] == [person.phone for person in actual]

    @pytest.mark.parametrize('stored_ts', stored_ts_new_variants)
    def test_get_patched_persons__no_patch_phone(self, spark_data, person_factory, rands, stored_ts):
        persons = [person_factory(PersonType.CEO)]
        assert persons == spark_data.get_patched_persons(persons=persons,
                                                         leader_name=rands(),
                                                         leader_surname=rands(),
                                                         leader_patronymic=None,
                                                         stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_persons__patch_empty_fio_and_birth_date(self,
                                                                 spark_data,
                                                                 leader_name,
                                                                 leader_birth_date,
                                                                 rands,
                                                                 stored_ts):
        empty_person = PersonData(
            type=PersonType.CEO,
            name='',
            surname='',
            patronymic='',
            birth_date=None,
            email=rands(),
            phone=rands(),
        )
        name, surname, patronymic = leader_name.split()
        actual = spark_data.get_patched_persons(persons=[empty_person],
                                                leader_name=name,
                                                leader_surname=surname,
                                                leader_patronymic=patronymic,
                                                stored_ts=stored_ts)
        assert [
            PersonData(
                type=PersonType.CEO,
                name=name,
                surname=surname,
                patronymic=patronymic,
                birth_date=leader_birth_date,
                email=empty_person.email,
                phone=empty_person.phone,
            )
        ] == actual

    @pytest.mark.parametrize('stored_ts', stored_ts_new_variants)
    def test_get_patched_persons__no_patch_half_empty_fio(self, spark_data, person_factory, rands, stored_ts):
        ceo = person_factory(PersonType.CEO)  # ceo patronymic is filled
        ceo.name = ''
        ceo.surname = ''
        ceo.birth_date = None

        assert [ceo] == spark_data.get_patched_persons(persons=[ceo],
                                                       leader_name=rands(),
                                                       leader_surname=rands(),
                                                       leader_patronymic=None,
                                                       stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_old_variants)
    def test_get_patched_persons__patch_fio_and_birth_date(self,
                                                           spark_data,
                                                           person_factory,
                                                           leader_name,
                                                           leader_birth_date,
                                                           rands,
                                                           stored_ts):
        ceo = person_factory(PersonType.CEO)
        name, surname, patronymic = leader_name.split()
        actual = spark_data.get_patched_persons(persons=[ceo],
                                                leader_name=name,
                                                leader_surname=surname,
                                                leader_patronymic=patronymic,
                                                stored_ts=stored_ts)
        assert [
            PersonData(
                type=PersonType.CEO,
                name=name,
                surname=surname,
                patronymic=patronymic,
                birth_date=leader_birth_date,
                email=ceo.email,
                phone='+7 333 333 33 33',
            )
        ] == actual

    @pytest.mark.parametrize('spark_address', (list(), None), ids=('empty', 'not_defined'))
    def test_get_patched_addresses__no_spark_address(self, spark_data, address_factory, spark_address):
        spark_data.address = spark_address
        addresses = [address_factory('legal', 'balance')]
        assert addresses == spark_data.get_patched_addresses(addresses=addresses, stored_ts=None)

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_addresses__no_legal(self, spark_data, address_factory, stored_ts):
        post_address = address_factory('post', 'balance')
        assert [
            post_address,
            address_factory('legal', 'spark'),
        ] == spark_data.get_patched_addresses(addresses=[post_address], stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_all_variants)
    def test_get_patched_addresses__patch_empty(self, spark_data, address_factory, stored_ts):
        post_address = address_factory('post', 'balance_post')
        branch_address = address_factory('branch', 'balance_branch')
        empty_address = AddressData(
            type='legal',
            city='',
            country='',
            home='',
            street='',
            zip='',
        )
        assert [
            post_address,
            address_factory('legal', 'spark'),
            branch_address,
        ] == spark_data.get_patched_addresses(addresses=[post_address, empty_address, branch_address],
                                              stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_new_variants)
    def test_get_patched_addresses__no_patch_half_empty(self, spark_data, address_factory, stored_ts):
        address = address_factory('legal', 'balance')  # zip is filled
        address.city = ''
        address.country = ''
        address.home = ''
        address.street = ''
        assert [address] == spark_data.get_patched_addresses(addresses=[address], stored_ts=stored_ts)

    @pytest.mark.parametrize('stored_ts', stored_ts_old_variants)
    def test_get_patched_addresses__patch(self, spark_data, address_factory, stored_ts):
        post_address = address_factory('post', 'balance_post')
        branch_address = address_factory('branch', 'balance_branch')
        legal_address = address_factory('legal', 'balance_branch')
        assert [
            post_address,
            address_factory('legal', 'spark'),
            branch_address,
        ] == spark_data.get_patched_addresses(addresses=[post_address, legal_address, branch_address],
                                              stored_ts=stored_ts)
