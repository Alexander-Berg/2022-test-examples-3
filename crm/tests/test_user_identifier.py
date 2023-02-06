import pytest

from crm.supskills.direct_skill.src.views.dialog_handler import DialogHandler


class TestSkillResponse:
    dialog_handler = DialogHandler(5, 'bunker', 'direct5_client', 'direc4_client')

    @pytest.mark.parametrize('args, expected_result', [
        (dict(session=dict(floyd_user=dict(puid='12345', login='qwerty@yandex.ru'))), 'qwerty@yandex.ru'),
        (dict(session=dict(floyd_user=dict(puid='12345', login=None))), None)
    ])
    def test_login_getter(self, args, expected_result):
        self.dialog_handler.request_data = args
        login = self.dialog_handler.get_user_identifier('login')
        assert login == expected_result

    @pytest.mark.parametrize('args, expected_result', [
        (dict(session=dict(floyd_user=dict(puid='12345', login='qwerty@yandex.ru'))), '12345'),
        (dict(session=dict(floyd_user=dict(puid=None, login='qwerty@yandex.ru'))), None)
    ])
    def test_puid_getter(self, args, expected_result):
        self.dialog_handler.request_data = args
        puid = self.dialog_handler.get_user_identifier('puid')
        assert puid == expected_result

    @pytest.mark.parametrize('args, expected_result', [
        (dict(session=dict(floyd_user=dict(puid='1', login='qwerty@ya.ru', operator_chat_id='test'))), 'test'),
        (dict(session=dict(floyd_user=dict(puid='12345', login='qwerty@yandex.ru'))), None)
    ])
    def test_operator_chat_id_getter(self, args, expected_result):
        self.dialog_handler.request_data = args
        operator_chat_id = self.dialog_handler.get_user_identifier('operator_chat_id')
        assert operator_chat_id == expected_result

    def test_identifiers_getter_no_floyd_user(self):
        self.dialog_handler.request_data = dict(session=dict(blabla='albalb'))
        puid = self.dialog_handler.get_user_identifier('puid')
        login = self.dialog_handler.get_user_identifier('login')
        assert puid is None and login is None
