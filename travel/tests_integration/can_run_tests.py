# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function


import travel.rasp.admin.scripts.load_project  # noqa

from django.conf import settings
from django.db import connections
from common.db.mysql_info import render_mysql_info
from travel.rasp.library.python.common23.db.switcher import switcher


def check_geobase_available():
    """ Проверяем, что геобаза загружается, чтобы не упасть на ней через два часа в середине тестов"""
    print('check geobase: loading')
    try:
        from travel.rasp.library.python.common23.utils_db.geobase import get_geobase_lookup
        lookup = get_geobase_lookup()
        lookup.region_by_id(213)
    except Exception:
        print("geobase failed to load -> can't run tests")
        raise
    print('check geobase: loaded successfully')


def can_run_integration_check():
    print(render_mysql_info())

    work_alias = switcher.get_db_alias(settings.WORK_DB)
    work_db_name = connections[work_alias].get_db_name()
    service_alias = switcher.get_db_alias(settings.SERVICE_DB)
    service_db_name = connections[service_alias].get_db_name()

    errors = []

    if settings.APPLIED_CONFIG != 'development':
        errors.append("APPLIED_CONFIG != 'development'")
    if settings.INSTANCE_ROLE.code != 'service':
        errors.append("INSTANCE_ROLE != service")
    if 'integration' not in work_db_name:
        errors.append("'integration' not in work_db_name: {}".format(work_db_name))
    if 'integration' not in service_db_name:
        errors.append("'integration' not in service_db_name: {}".format(service_db_name))

    check_geobase_available()

    return not bool(errors), errors


def main():
    is_good, errors = can_run_integration_check()
    if not is_good:
        raise Exception("Can't run integration tests in this environment", errors)


if __name__ == '__main__':
    main()
