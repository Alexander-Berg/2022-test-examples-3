import re

import pytest

from mail.payments.payments.interactions.search_wizard import SearchWizardClient
from mail.payments.payments.interactions.search_wizard.entities import Fio


class TestParseJsonResponse:
    @pytest.fixture
    def returned(self, search_wizard_client, payload):
        return search_wizard_client._parse_json_response(payload)

    @pytest.mark.parametrize('payload,expected', [
        (
            {
                'Fio': [
                    {
                        'FirstName': 'иван',
                        'LastName': 'иванов',
                        'Patronymic': 'иванович'
                    }
                ]
            },
            Fio(first_name='иван', middle_name='иванович', last_name='иванов')
        ),
        (
            {
                'Fio': [
                    {
                        'FirstName': 'иван',
                        'LastName': 'иванов'
                    }
                ]
            },
            Fio(first_name='иван', middle_name=None, last_name='иванов')
        ),
        ({}, None),
        ({'Fio': []}, None),
        (
            {
                'Fio': [
                    {
                        'LastName': 'иванов',
                        'Patronymic': 'иванович'
                    }
                ]
            },
            None
        ),
    ])
    def test_parse_json_response(self, payload, expected, returned):
        assert expected == returned


class TestMatchResultWithInput:
    @pytest.mark.parametrize('input_string,wizard_response,expected_result', [
        (
            'Иванов Иван Иванович',
            Fio(first_name='иван', middle_name='иванович', last_name='иванов'),
            Fio(first_name='Иван', middle_name='Иванович', last_name='Иванов')
        ),
        (
            'Иванов И.И.',
            Fio(first_name='и', middle_name='и', last_name='иванов'),
            Fio(first_name='И', middle_name='И', last_name='Иванов')
        ),
        (
            'Иванов Иван',
            Fio(first_name='иван', middle_name=None, last_name='иванов'),
            Fio(first_name='Иван', middle_name=None, last_name='Иванов')
        ),
        (
            'Салтыков-Щедрин Михаил Евграфович',
            Fio(first_name='михаил', middle_name='евграфович', last_name='салтыков'),
            Fio(first_name='Михаил', middle_name='Евграфович', last_name='Салтыков-Щедрин')
        ),
        (
            'М. Е. Салтыков-Щедрин',
            Fio(first_name='м', middle_name='е', last_name='салтыков'),
            Fio(first_name='М', middle_name='Е', last_name='Салтыков-Щедрин')
        ),
        (
            'Барнаби Мармадюк Алоизий Бенджи Кобвеб Дартаньян Эгберт Феликс Гаспар Гумберт Игнатий '
            'Джейден Каспер Лерой Максимилиан Недди Объяхулу Пепин Кьюллиам Розенкранц Секстон Тедди '
            'Апвуд Виватма Уэйленд Ксилон Ярдли Закари',
            Fio(first_name='кьюллиам', middle_name='гаспар', last_name='алоизий'),
            Fio(
                first_name='Кьюллиам Розенкранц Секстон Тедди Апвуд Виватма Уэйленд Ксилон Ярдли Закари',
                middle_name='Гаспар Гумберт Игнатий Джейден Каспер Лерой Максимилиан Недди Объяхулу Пепин',
                last_name='Барнаби Мармадюк Алоизий Бенджи Кобвеб Дартаньян Эгберт Феликс'
            )
        ),
        ('ABC', Fio('a', 'b', 'c'), None),  # unmatching parameters
    ])
    def test_match_result_with_input(self, input_string, wizard_response, expected_result):
        assert expected_result == SearchWizardClient._match_result_with_input(input_string, wizard_response)


class TestSplitFio:
    @pytest.fixture
    def payload(self):
        return {
            'Fio': [
                {
                    'FirstName': 'иван',
                    'LastName': 'иванов',
                    'Patronymic': 'иванович'
                }
            ]
        }

    @pytest.fixture
    def mock_wizard_response(self, aioresponses_mocker, payments_settings, payload):
        return aioresponses_mocker.get(
            re.compile(f'^{payments_settings.SEARCH_WIZARD_URL}.*$'),
            payload=payload
        )

    @pytest.fixture
    def input_fio(self):
        return 'Иванов Иван Иванович'

    @pytest.fixture
    async def returned(self, search_wizard_client, mock_wizard_response, input_fio):
        return await search_wizard_client.split_fio(input_fio)

    @pytest.fixture
    def mock_parse_json_response(self, mocker):
        mocker.patch.object(SearchWizardClient, '_parse_json_response',
                            mocker.Mock(return_value=None))

    @pytest.fixture
    def mock_match_result_with_input(self, mocker):
        mocker.patch.object(SearchWizardClient, '_match_result_with_input',
                            mocker.Mock(return_value=None))

    @pytest.mark.asyncio
    async def test_request(self, mock_wizard_response, mocker, search_wizard_client, input_fio):
        mocker.spy(search_wizard_client, 'get')
        await search_wizard_client.split_fio(input_fio)

        search_wizard_client.get.assert_called_with(
            'search_wizard',
            url='http://hamzard.yandex.net:8891/wizard',
            params={
                'action': 'markup',
                'markup=layers': 'Fio',
                'text': input_fio,
                'wizclient': 'payments',
            }
        )

    @pytest.mark.asyncio
    async def test_success(self, returned):
        assert Fio('Иван', 'Иванович', 'Иванов') == returned

    @pytest.mark.parametrize('payload', [{}])
    @pytest.mark.asyncio
    async def test_empty_payload(self, returned):
        assert returned is None

    @pytest.mark.asyncio
    async def test_parse_json_reponse_failed(self, mock_parse_json_response, returned):
        assert returned is None

    @pytest.mark.asyncio
    async def test_parse_match_result_with_input_failed(self, mock_match_result_with_input, returned):
        assert returned is None
