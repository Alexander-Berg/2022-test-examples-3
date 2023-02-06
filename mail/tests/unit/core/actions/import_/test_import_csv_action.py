from dataclasses import asdict

import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_properties, instance_of, match_equality, not_none

from mail.ipa.ipa.core.actions.import_.csv import (
    CreateImportFromCSVAction, ParseCSVAction, _map_csv_entry_to_user_info, _password_placeholder
)
from mail.ipa.ipa.core.actions.import_.org import InitOrgImportAction
from mail.ipa.ipa.core.entities.import_params import GeneralInitImportParams
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.core.entities.user_info import UserInfo
from mail.ipa.ipa.core.exceptions import CSVFieldRequiredError
from mail.ipa.ipa.interactions.mds import MDSClient
from mail.ipa.ipa.storage.mappers.organization import OrganizationMapper
from mail.ipa.ipa.tests.contracts import ActionTestContract, GeneratesStartEventContract


class TestCreateImportFromCSVAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return CreateImportFromCSVAction

    @pytest.fixture
    def mds_key(self):
        return '1234/filename'

    @pytest.fixture(autouse=True)
    def mock_mds_upload(self, mocker, coromock, mds_key):
        return mocker.patch('mail.ipa.ipa.interactions.mds.MDSClient.upload', coromock(mds_key))

    @pytest.fixture(autouse=True)
    def spy_ensure_organization(self, mocker, coromock, storage):
        return mocker.spy(OrganizationMapper, 'ensure_exists')

    @pytest.fixture(autouse=True)
    def mock_prepare_upload_csv(self, mocker):
        async def data():
            yield b'data1'
            yield b'data2'
        return mocker.patch('mail.ipa.ipa.core.actions.import_.csv.prepare_csv_for_upload',
                            mocker.Mock(side_effect=lambda *args, **kwargs: data()))

    @pytest.fixture
    def import_params(self, org_id, admin_uid, user_ip):
        return GeneralInitImportParams(
            server='server.test',
            port=993,
            imap=False,
            ssl=True,
            mark_archive_read=False,
            delete_msgs=True,
            org_id=org_id,
            admin_uid=admin_uid,
            user_ip=user_ip,
        )

    @pytest.fixture
    def csv(self, mocker):
        return mocker.NonCallableMock()

    @pytest.fixture
    def csv_name(self):
        return 'csv-name'

    @pytest.fixture
    def params(self, csv, csv_name, import_params):
        return {
            'import_params': import_params,
            'stream': csv,
            'name': csv_name,
        }

    def test_returned(self, returned):
        assert_that(returned, not_none())

    @pytest.mark.asyncio
    async def test_mds_called(self, returned, mock_prepare_upload_csv, mock_mds_upload):
        expected_data = b''.join(await alist(mock_prepare_upload_csv()))
        mock_mds_upload.assert_called_once_with('csv', expected_data)

    @pytest.mark.asyncio
    async def test_task_created(self,
                                returned,
                                storage,
                                mds_key,
                                org_id,
                                import_params,
                                csv_name):
        task = await storage.task.get(returned)
        assert_that(
            task,
            has_properties({
                'entity_id': org_id,
                'params': {
                    'import_params': asdict(import_params),
                    'csv_key': mds_key,
                },
                'meta_info': {
                    'name': csv_name,
                }
            }),
        )

    def test_org_id_ensured(self, returned, spy_ensure_organization, org_id):
        spy_ensure_organization.assert_called_once_with(
            match_equality(instance_of(OrganizationMapper)),
            org_id,
        )

    def test_upload_csv_called(self, returned, csv, action, mock_prepare_upload_csv):
        mock_prepare_upload_csv.assert_called_once_with(csv, validate_cb=action.validate_csv_entry)

    class TestGeneratesEvent(GeneratesStartEventContract):
        pass


class TestParseCSVAction(ActionTestContract):
    @pytest.fixture
    def expected_user_infos(self):
        return [
            UserInfo(login='foo', password=Password.from_plain('foopasswd'), src_login='foomail@example.test'),
            UserInfo(login='bar', password=Password.from_plain('barpasswd'), src_login='barmail@example.test'),
        ]

    @pytest.fixture
    def mds_data(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def downloaded_data(self, ipa_settings):
        return [
            {
                ipa_settings.CSV_FIELD_YANDEXMAIL_LOGIN: 'foo',
                ipa_settings.CSV_FIELD_SRC_PASSWORD: 'foopasswd',
                ipa_settings.CSV_FIELD_SRC_LOGIN: 'foomail@example.test'
            },
            {
                ipa_settings.CSV_FIELD_YANDEXMAIL_LOGIN: 'bar',
                ipa_settings.CSV_FIELD_SRC_PASSWORD: 'barpasswd',
                ipa_settings.CSV_FIELD_SRC_LOGIN: 'barmail@example.test'
            },
        ]

    @pytest.fixture(autouse=True)
    def mock_init_org_import(self, mock_action):
        return mock_action(InitOrgImportAction)

    @pytest.fixture(autouse=True)
    def mock_mds_download(self, mocker, coromock, mds_data):
        return mocker.patch.object(MDSClient, 'download', coromock(mds_data))

    @pytest.fixture(autouse=True)
    def mock_csv_download(self, mocker, downloaded_data):
        async def download():
            for data in downloaded_data:
                yield data
        return mocker.patch('mail.ipa.ipa.core.actions.import_.csv.read_downloaded_csv',
                            mocker.Mock(return_value=download()))

    @pytest.fixture
    def action_class(self):
        return ParseCSVAction

    @pytest.fixture
    def csv_key(self):
        return 'csv_key'

    @pytest.fixture
    def params(self, general_init_import_params, csv_key):
        return {
            'import_params': general_init_import_params,
            'csv_key': csv_key,
        }

    def test_calls_mds_download(self, returned, mock_mds_download, csv_key):
        mock_mds_download.assert_called_once_with(csv_key)

    def test_calls_csv_download(self, returned, mock_csv_download, mds_data):
        mock_csv_download.assert_called_once_with(mds_data)

    def test_calls_init_org_import(self,
                                   returned,
                                   mock_init_org_import,
                                   general_init_import_params,
                                   expected_user_infos):
        mock_init_org_import.assert_called_once_with(general_init_import_params, expected_user_infos)


class TestCSVEntryOps:
    @pytest.fixture
    def entry(self, request, ipa_settings):
        entry = request.param
        mapping = {
            'yandexmail_login': ipa_settings.CSV_FIELD_YANDEXMAIL_LOGIN,
            'yandexmail_password': ipa_settings.CSV_FIELD_YANDEXMAIL_PASSWORD,
            'src_password': ipa_settings.CSV_FIELD_SRC_PASSWORD,
            'src_login': ipa_settings.CSV_FIELD_SRC_LOGIN,
            'first_name': ipa_settings.CSV_FIELD_FIRST_NAME,
            'last_name': ipa_settings.CSV_FIELD_LAST_NAME,
            'middle_name': ipa_settings.CSV_FIELD_MIDDLE_NAME,
            'birthday': ipa_settings.CSV_FIELD_BIRTHDAY,
            'gender': ipa_settings.CSV_FIELD_GENDER,
            'language': ipa_settings.CSV_FIELD_LANGUAGE,
        }
        return {value: entry.get(key) for key, value in mapping.items()}

    @pytest.mark.parametrize('entry, with_password, expected', (
        pytest.param(
            {
                'yandexmail_login': 'yalogin',
                'src_password': 'spasswd',
                'src_login': 'src',
                'first_name': 'fname',
                'last_name': 'lname',
                'middle_name': 'mname',
                'gender': 'bender',
                'birthday': 'bday',
                'language': 'lang',
            },
            True,
            UserInfo(
                login='yalogin',
                password=Password.from_plain('spasswd'),
                src_login='src',
                new_password=None,
                first_name='fname',
                last_name='lname',
                middle_name='mname',
                gender='bender',
                birthday='bday',
                language='lang',
            ),
            id='full-entry'
        ),
        pytest.param(
            {
                'yandexmail_login': 'yalogin',
                'src_password': 'spasswd',
                'src_login': 'src',
            },
            True,
            UserInfo(
                login='yalogin',
                password=Password.from_plain('spasswd'),
                src_login='src',
                new_password=None,
                first_name=None,
                last_name=None,
                middle_name=None,
                gender=None,
                birthday=None,
                language=None,
            ),
            id='only required',
        ),
        pytest.param(
            {
                'yandexmail_login': 'yalogin',
                'src_password': 'spasswd',
                'src_login': 'src',
                'yandexmail_password': 'yapwd',
            },
            True,
            UserInfo(
                login='yalogin',
                password=Password.from_plain('spasswd'),
                src_login='src',
                new_password=Password.from_plain('yapwd'),
                first_name=None,
                last_name=None,
                middle_name=None,
                gender=None,
                birthday=None,
                language=None,
            ),
            id='with new password'
        ),
        pytest.param(
            {
                'yandexmail_login': 'yalogin',
                'src_password': 'spasswd',
                'src_login': 'src',
                'yandexmail_password': 'yapwd',
            },
            False,
            UserInfo(
                login='yalogin',
                password=_password_placeholder,
                src_login='src',
                new_password=None,
                first_name=None,
                last_name=None,
                middle_name=None,
                gender=None,
                birthday=None,
                language=None,
            ),
            id='without password'
        ),
    ), indirect=['entry'])
    def test_map_csv_entry_to_user_info(self, entry, with_password, expected):
        assert_that(
            _map_csv_entry_to_user_info(entry, with_password),
            equal_to(expected),
        )

    @pytest.mark.parametrize('entry, exc_type', (
        ({'yandexmail_login': 'ya', 'src_password': 'pwd', 'src_login': 'src'}, None),
    ), indirect=['entry'])
    def test_validate_csv_entry_ok(self, entry, exc_type):
        CreateImportFromCSVAction.validate_csv_entry(entry, 1)

    @pytest.mark.parametrize('entry, exc_type', (
        ({'yandexmail_login': '', 'src_password': 'spasswd', 'src_login': 'src'}, CSVFieldRequiredError),
        ({'yandexmail_login': 'ya', 'src_password': '', 'src_login': 'src'}, CSVFieldRequiredError),
        ({'yandexmail_login': 'ya', 'src_password': 'pwd', 'src_login': ''}, CSVFieldRequiredError),
    ), indirect=['entry'])
    def test_validate_csv_entry_error(self, entry, exc_type):
        with pytest.raises(exc_type):
            CreateImportFromCSVAction.validate_csv_entry(entry, 1)
