import copy
import sys
from pathlib import Path

import pytest

from featureFlow import set_secret

file = Path(__file__).resolve()
parent, root = file.parent, file.parents[1]
sys.path.append(str(root))

from featureFlow.help_functions import get_components_config, get_issue_dict_separate, get_issues_params_template, \
    get_issue_dict_cumulative

set_secret.set_secrets()


class TestClass:
    input_data = {
        'project_name': 'Выгрузка инфы по тарифам пользователей',
        'feature_size': 'Средний. Точно один этап.',
        'umbrella': 'B2B',
        'goal_id': '123321',
        'is_it_crit': 'Нет',
        'priority_defence': 'Очень важно, обосновываю',
        'deadline': '2022-07-29',
        'description': 'description',
        'main_task': 'MAILBACKZBPTEST-42',
        'main_service': 'MAILBACKZBPTEST - очередь для теста',
        'disk_web_services': '',
        'disk_mob_services': '',
        'tel_mob_services': '',
        'mail_mob_services': '',
        'unknown_service': '',
        'test_services': 'Тестовый компонент1',
        'disk_po_services': ''
    }
    input_additional_data = {
        'manager': 'me_the_manager'
    }

    output_issue = {
        'assignee': 'a-zoshchuk',
        'author': 'me_the_manager',
        'managerInCharge': 'me_the_manager',
        'queue': 'MAILBACKZBPTEST',
        'summary': '[Тестовый компонент1]Выгрузка инфы по тарифам пользователей',
        'type': {'key': 'project'},
        'description': '***Зонтик:***\nB2B\n\n***Обоснование критичности:***\nОчень важно, обосновываю\n\n***Размер фичи:***\nСредний. Точно один этап.\n\n\n***Описание:***\ndescription\n\n',
        'deadline': '2022-07-29',
        'components': ['component0', 'component11', 'component12'],
        'sizeOfRelease': 'M',
        'boards': [{'id': 26693}],
        'tags': ['featureFlow_umbrella_B2B', 'testTag1'],
        'weight': 5.0,
        'parent': 'MAILBACKZBPTEST-42',
        'goals': [123321],
        'fixVersions': ['fixVersion11']
    }

    def test_components_summation(self):
        """Получаем конфиг необходимых компонентов"""
        expected_result = {
            'Тестовый компонент1': {
                'components': ['component11', 'component12'],
                'fixVersions': ['fixVersion11'],
                'tags': ['testTag1'],
            }
        }
        assert get_components_config('MAILBACKZBPTEST', self.input_data)['Тестовый компонент1'] == expected_result['Тестовый компонент1']

    def test_issue_separate(self):
        """Заводим отдельную задачу для компонента"""
        issue_json = get_issue_dict_separate(
            get_issues_params_template(self.input_additional_data, self.input_data),
            'Тестовый компонент1',
            get_components_config('MAILBACKZBPTEST', self.input_data)['Тестовый компонент1']
        )
        assert issue_json == self.output_issue

    def test_field_overflow(self):
        """Проверяем перетирание поля, если оно есть и в общем конфиге, и в конфиге компонента"""
        test_comp_input_2 = copy.deepcopy(self.input_data)
        test_comp_input_2['test_services'] = 'Тестовый компонент2'

        test_comp_output_2 = copy.deepcopy(self.output_issue)
        test_comp_output_2['assignee'] = 'assignee2'
        test_comp_output_2['components'] = ['component0', 'component21', 'component22']
        test_comp_output_2['fixVersions'] = ['fixVersion21']
        test_comp_output_2['tags'] = ['featureFlow_umbrella_B2B']
        test_comp_output_2['summary'] = '[Тестовый компонент2]Выгрузка инфы по тарифам пользователей'
        issue_json = get_issue_dict_separate(
            get_issues_params_template(self.input_additional_data, test_comp_input_2),
            'Тестовый компонент2',
            get_components_config('MAILBACKZBPTEST', test_comp_input_2)['Тестовый компонент2']
        )
        assert issue_json == test_comp_output_2

    def test_issue_cumulative(self):
        """Суммируем поля, если для всех компонентов нужна одна задача"""
        cumulative_output = copy.deepcopy(self.output_issue)
        cumulative_output['components'] += ['component21', 'component22']
        cumulative_output['fixVersions'] += ['fixVersion21']
        cumulative_output['assignee'] = 'assignee2'
        cumulative_output['summary'] = 'Выгрузка инфы по тарифам пользователей'

        cumulative_input = copy.deepcopy(self.input_data)
        cumulative_input['test_services'] = 'Тестовый компонент1, Тестовый компонент2'

        issue_json = get_issue_dict_cumulative(
            get_issues_params_template(self.input_additional_data, cumulative_input),
            get_components_config('MAILBACKZBPTEST', cumulative_input)
        )
        assert issue_json == cumulative_output

    @pytest.mark.parametrize(
        'field,expected',
        [
            pytest.param('goal_id', {'goals': None}),
            pytest.param('deadline', {'deadline': ''})
        ]
    )
    def test_without_unnecessary_fields(self, field, expected):
        """Проверяем, что всё ок без необязательных полей"""
        field_without_input = copy.deepcopy(self.input_data)
        field_without_input[field] = ''

        field_without_output = copy.deepcopy(self.output_issue)
        key_to_change = list(expected.keys())[0]
        value_to_set = list(expected.values())[0]
        if value_to_set is None:
            del field_without_output[key_to_change]
        else:
            field_without_output[key_to_change] = value_to_set
        issue_json = get_issue_dict_separate(
            get_issues_params_template(self.input_additional_data, field_without_input),
            'Тестовый компонент1',
            get_components_config('MAILBACKZBPTEST', field_without_input)['Тестовый компонент1']
        )
        assert issue_json == field_without_output


