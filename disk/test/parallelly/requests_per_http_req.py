# -*- coding: utf-8 -*-
"""
В этих тестах проверяем высокачастотные ручки на кол-во походов в базу
"""
import mock
import re
from test.base import DiskTestCase
from mpfs.dao.session import Session


class RequestsPerHTTPReqTestCase(DiskTestCase):
    """Проверяем, что ручки делают нужное кол-во запросов в базу"""
    select_from_disk_info = re.compile(r'SELECT.+FROM\s+disk\.disk_info\s+', re.MULTILINE | re.DOTALL)
    select_from_user_index = re.compile(r'SELECT.+FROM\s+disk\.user_index\s+', re.MULTILINE | re.DOTALL)

    @staticmethod
    def print_db_calls(db_call_stub):
        tmpl = "%0.2i | %s | %s | %s"
        print "Catch %i db execute calls" % len(db_call_stub.call_args_list)
        for i, call_args in enumerate(db_call_stub.call_args_list):
            args, kwargs = call_args
            sql_tmpl = str(args[1])
            print tmpl % (i, sql_tmpl, args[2:], kwargs)

    @staticmethod
    def get_sql_tmpl(call_args):
        args, kwargs = call_args
        return str(args[1])

    def test_user_info(self):
        with mock.patch('mpfs.dao.session.Session.execute', side_effect=Session.execute, autospec=True) as db_call_stub:
            self.json_ok('user_info', {'uid': self.uid})
        # TODO убрать 2 лишних похода
        # в нормальной ситуации тут должно быть 2 запроса - один в user_index и один в disk_info
        # Данные надо брать из кеша DiskInfoCollection. Лишнии запросы в disk_info:
        # * self.fs.quota.report
        # * self.settings.list_all()
        self.print_db_calls(db_call_stub)
        assert len(db_call_stub.call_args_list) == 5

        calls = (
            self.select_from_user_index,
            self.select_from_disk_info,
            self.select_from_disk_info,
            self.select_from_disk_info,
        )

        for call_args, sql_tmpl_check_re in zip(db_call_stub.call_args_list, calls):
            sql_tmpl =self.get_sql_tmpl(call_args)
            assert sql_tmpl_check_re.search(sql_tmpl), 'Re not found: %s | %s' % (sql_tmpl_check_re, sql_tmpl)
