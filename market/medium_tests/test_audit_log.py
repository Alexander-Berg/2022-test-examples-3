# -*- coding: utf-8 -*-
import os
import json

from hamcrest import assert_that, equal_to

LOG_FIELDS = {'command', 'time_to_finish', 'start_time', 'return_code', 'user', 'real_user'}


def test_mindexer_clt_audit_file_created_and_data_logged(mindexer_clt, tmpdir):

    audit_log = tmpdir.join('mindexer_clt_audit.log')
    mindexer_clt.make_local_config({
        ('health', 'mindexer_clt_audit_log_file'): audit_log,
    })
    mindexer_clt.execute('make_me_master', '--both', '--no-publish')

    assert os.path.exists(str(audit_log))

    with open(str(audit_log), 'r') as f:
        data = json.loads(f.read())

        assert LOG_FIELDS.issubset(data.keys())
        assert_that(data.get('command'), equal_to('make_me_master'))
        assert_that(data.get('return_code'), equal_to(0))
