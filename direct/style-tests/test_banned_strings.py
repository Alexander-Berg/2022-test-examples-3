# -*- coding: utf-8 -*-

import os
import os.path
import pytest
import re
import style_utils
import yatest.common

ROOT = yatest.common.source_path()

IGNORE_BANNED_STRINGS_REGEXP = re.compile(r'IGNORE-BAD-STYLE DIRECT-\d+\b')

BANNED_STRINGS = [
    # вариант select * в jOOQ;
    # поля для выборки в SQL надо перечислять явно
    'selectFrom',

    # эти функции используют selectFrom и выбирают все известные колонки
    'fetchByExample',

    # пакет служебных интерфейсов, ссылок на которые
    # не должно быть в рукописном коде проекта
    'model.prop.',

    # сейчас транзакционность обеспечиваем явно, эту аннотацию никто не обрабатывает
    'org.springframework.transaction.annotation.Transactional',

    # bolts постепенно становится нельзя
    # но из импортов иногда идея сама удаляет Option, а он бывает нужен
    # 'ru.yandex.bolts.collection.Option',

    # использовать нормальную guava
    'jersey.repackaged',

    # используем org.assertj.core.api.Assertions.assertThat
    'Java6Assertions',

    # вместо этого используем TestUtils
    'org.junit.Assume.assumeThat',
    'Assumptions',

    # пока что не готовы тесты 'org.junit.Assert.assertThat'

    # без коментариев
    'printStackTrace()'
]


@pytest.mark.parametrize("dir", style_utils.get_java_dirs(ROOT), ids=lambda d: os.path.relpath(d, ROOT))
@pytest.mark.parametrize("string", BANNED_STRINGS)
def test_banned_strings(dir, string):
    """
    Проверка на отсутствие в скриптах, модулях и шаблонах определённых подстрок, которых там быть не должно
    """
    errors = []
    for subdir, dirnames, files in os.walk(dir):
        if 'generated' in subdir:
            continue
        for file in files:
            abs_file = os.path.join(subdir, file)
            rel_file = os.path.relpath(abs_file, ROOT)
            if not os.path.isfile(abs_file) or os.path.islink(abs_file):
                continue
            for i, line in enumerate(open(abs_file)):
                if string in line:
                    if not IGNORE_BANNED_STRINGS_REGEXP.search(line):
                        errors.append("{}:{}".format(rel_file, i + 1))
    assert not errors, string + " is found in files:\n" + "\n".join(errors)


GD_ENUM_OVER_DBSCHEMA_ENUM = 'gd_enum_over_dbschema_enum'

BAD_PATTERNS = [
    # "не для продакшена" (NO_PRODUCTION)
    # Этой меткой удобно пользоваться при разработке в бранче,
    # чтобы отмечать временные тестово-отладочные конструкции,
    # которые обязательно надо заменить до мержа в trunk
    (
        'no_production',
        'NO_PRODUCTION',
        r'^\S+$',
        r'\bNO[ _]?(?:PROD\b|PRODUCTION)',
        re.IGNORECASE
    ),
    # ещё один вариант select * в jOOQ – select().from(...),
    # который может занимать более одной строки
    (
        'jooq_select_from',
        'jOOQ select().from(...) statement',
        r'^\S+$',
        r'^.*?\.\s*select\s*\(\)\s*\.\s*from\s*\(.*?$',
        re.MULTILINE
    ),
    # ещё один вариант select * в jOOQ – select(TABLE.fields())
    (
        'jooq_select_table_fields',
        'jOOQ select(TABLE.fields()) statement',
        r'^\S+$',
        r'^.*?\.\s*select\s*\(\s*[A-Z_]+\s*\.fields\(\)\s*\)\s*.*?$',
        re.MULTILINE
    ),
    # создание gd enum поверх dbschema enum
    (
        GD_ENUM_OVER_DBSCHEMA_ENUM,
        'generating gd enum over dbschema enum',
        r'^gd_\S+$',
        r'\s*valuesSource:\s*ru\.yandex\.direct\.dbschema\.ppc\.enums\s*',
        re.MULTILINE
    )
]

PATTERN_NAME_TO_IGNORED_FILES = {
    GD_ENUM_OVER_DBSCHEMA_ENUM: {
        'gd_campaign.conf',
        'gd_mobile_app_tracker.conf',
        'gd_package_strategy.conf',
        'gd_bid_modifier_demographics_adjustment.conf',
        'gd_bid_modifier_mobile_adjustment.conf',
        'gd_campaign_input_filter.conf'
    }
}


@pytest.mark.parametrize("dir", style_utils.get_java_dirs(ROOT), ids=lambda d: os.path.relpath(d, ROOT))
@pytest.mark.parametrize("pattern_name, description, filename_pattern, pattern, flags", BAD_PATTERNS)
def test_banned_regexps(dir, pattern_name, description, filename_pattern, pattern, flags):
    """
    Проверка на отсутствие в скриптах, модулях и шаблонах некоторых нежелательных паттернов, в т.ч. многострочных
    :param dir          директория для рекурсивного обхода
    :param pattern_name уникальное название паттерна
    :param description  описание нежелательного паттерна, которое отобразится в тексте ошибки
    :param filename_pattern маска названий файлов, которые нужно проверять
    :param pattern      тот самый паттерн для проверки содержимого файлов
    """
    errors = []
    for subdir, dirnames, files in os.walk(dir):
        if 'generated' in subdir:
            continue
        for file in files:
            abs_file = os.path.join(subdir, file)
            rel_file = os.path.relpath(abs_file, ROOT)
            if not os.path.isfile(abs_file) or os.path.islink(abs_file) or not re.match(filename_pattern, file) \
                or (pattern_name in PATTERN_NAME_TO_IGNORED_FILES
                    and file in PATTERN_NAME_TO_IGNORED_FILES[pattern_name]):
                continue
            if re.findall(pattern, open(abs_file).read(), flags):
                errors.append(rel_file)
    assert not errors, description + " is found in files:\n" + "\n".join(errors)


BAD_JOIN_REGEXP = re.compile(r'^.*?\.?\s*join\s*\(\)')
BAD_JOIN_WITH_METHOD_REFERENCE_REGEXP = re.compile(r'^.*?::join\)')
IS_NOT_COMPLETABLE_FUTURE_JOIN = 'IS-NOT-COMPLETABLE-FUTURE-JOIN'
IGNORE_BAD_JOIN_REGEXP = re.compile(r'IGNORE-BAD-JOIN DIRECT-\d+\b')
TEST_DIRS_REGEXP = re.compile(r'/src/.*test.*/')


@pytest.mark.parametrize("dir", style_utils.get_java_dirs(ROOT), ids=lambda d: os.path.relpath(d, ROOT))
def test_bad_completable_future_join(dir):
    """
    Проверка на отсутствие в коде вызова опасного метода join() из CompletableFuture, т.к. не указывается таймаут...
    Рекомендуется использовать метод get с таймаутом, вместо join'a.
    Подробнее можно почитать тут: https://st.yandex-team.ru/DIRECT-148137

    Метод join() может вызываться из других классов, например Thread и чтобы тест не ругался на такие строки нужно
    указать комментарий с текстом IS_NOT_COMPLETABLE_FUTURE_JOIN
    """
    errors = []
    for subdir, dirnames, files in os.walk(dir):
        if 'generated' in subdir or TEST_DIRS_REGEXP.search(subdir):
            continue

        for file in files:
            abs_file = os.path.join(subdir, file)
            rel_file = os.path.relpath(abs_file, ROOT)
            if not os.path.isfile(abs_file) or os.path.islink(abs_file):
                continue
            for i, line in enumerate(open(abs_file)):
                if BAD_JOIN_REGEXP.search(line) or BAD_JOIN_WITH_METHOD_REFERENCE_REGEXP.search(line):
                    if not IGNORE_BAD_JOIN_REGEXP.search(line) and IS_NOT_COMPLETABLE_FUTURE_JOIN not in line:
                        errors.append("{}:{}".format(rel_file, i + 1))
    assert not errors, "bad join() is found in files:\n" + "\n".join(errors)
