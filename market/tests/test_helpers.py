from market.front.tools.server_exps.lib import prepare_description, read_flags_config


def test_read_json_file(fixtures_dir):
    contents = read_flags_config(fixtures_dir + '/json-file.json')
    assert contents == {'contents': 'json file contents'}


def test_prepare_description():
    result = prepare_description('Some text and TICKET-123: with description')
    assert result == 'Some text and https://st.yandex-team.ru/TICKET-123 : with description'
