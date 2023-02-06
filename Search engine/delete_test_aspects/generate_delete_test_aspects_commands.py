#!/usr/bin/env python3

# Generate SQL commands to DELETE aspects from an URL list.

import re

aspect_string = """\
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/support_saas?evaluation=CV
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/support_saas?evaluation=SAAS
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/some_new_name?evaluation=IMAGES
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/Kotiki%20mimimi?evaluation=VIDEO
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/test_aspect_1?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/some_new_aspect_2?evaluation=IMAGES
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/new_test_aspect?evaluation=CV
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/my_new_aspect?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/lp_test_aspect?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/test_aspect_1?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/test_aspect_2?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/zzzz?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/zzzzz?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/zzzzzz?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/updated_aspect?evaluation=WEB
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/zzzzz?evaluation=IMAGES
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/zzzzz?evaluation=VIDEO
https://staging.metrics.yandex-team.ru/admin/aspect-metrics/new_____aspect?evaluation=NEWS
"""


def main():
    sql_lines = create_commands(aspect_string)
    for sl in sql_lines:
        print(sl)


def create_commands(aspect_string):
    aspect_lines = [line.strip() for line in aspect_string.splitlines()]
    sql_lines = ["BEGIN;"]
    for line in aspect_lines:
        if command := convert_url_to_delete_command(line):
            sql_lines.append(command)
    return sql_lines


def convert_url_to_delete_command(line):
    if m := re.match(
        r"https://staging.metrics.yandex-team.ru/admin/aspect-metrics/(?P<aspect>.*)\?evaluation=(?P<evaluation>.*)",
        line
    ):
        ev, asp = m["evaluation"], m["aspect"]
        return f"DELETE FROM metric_layout WHERE evaluation = '{ev}' AND aspect = '{asp}';"


if __name__ == "__main__":
    main()
