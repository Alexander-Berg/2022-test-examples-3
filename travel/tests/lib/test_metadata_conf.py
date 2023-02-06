# coding: utf-8

from tester.utils.replace_setting import replace_setting
from travel.rasp.admin.lib.metadata_conf import write_conf


def test_write_conf_to_file(tmpdir):
    conf_file = tmpdir.join('maintenance.conf')
    with replace_setting('CONF_FILE', str(conf_file)):
        write_conf({'my_key': 'my_value'})
    with conf_file.open('r') as f:
        assert f.read() == 'my_key my_value\n'
