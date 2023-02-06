import os
import pytest

import yatest.common
from yalibrary.makelists import from_file

COMMENT = '//'
SEMICOLON = ';'


def get_datacamp_proto_files():
    datacamp_proto_dir = os.path.join('market', 'idx', 'datacamp', 'proto')
    make_list = from_file(yatest.common.source_path(os.path.join(datacamp_proto_dir, 'ya.make')))
    return [os.path.join(datacamp_proto_dir, src.name) for src in make_list.project.srcs.get_values()]


def pytest_generate_tests(metafunc):
    if 'proto_path' in metafunc.fixturenames:
        return metafunc.parametrize('proto_path', get_datacamp_proto_files())


@pytest.fixture()
def file_content(proto_path):
    with open(yatest.common.source_path(proto_path)) as f:
        yield f.readlines()


def test_inline_comments(file_content):
    """Библиотека для работы с протосообщениями на JS не поддерживает inline комментарии при описании поля.
    Тест проверяет, что библиотека фронтов сможет пропарсить схему без ручных правок с их стороны.
    Такое не парсится:
    ```
    optional int field = 1  // field description
        [(Market.partner_data) = true];
    ```,

    Можно написать:
    ```
    // field description
    optional int field = 1 [(Market.partner_data) = true];

    // field description
    optional int field = 1
        [(Market.partner_data) = true];

    optional int field = 1
        [(Market.partner_data) = true]; // field description
    ```
    """
    for line_num, line in enumerate(file_content):
        stripped = line.strip()
        comment_pos = stripped.find(COMMENT)
        semicolon_pos = stripped.find(SEMICOLON)

        if comment_pos == -1 or comment_pos == 0:
            continue

        assert semicolon_pos != -1 and semicolon_pos < comment_pos, "Field description contains inline comment on line {}," \
            "see https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/datacamp/proto/tests/test_js_compatibility.py for detailed info".format(line_num)
