# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os

from django.apps import apps
from django.core.management import call_command
from django.core.management.base import BaseCommand

INITIAL_SUFFIX = '_initial.json'


class Command(BaseCommand):
    def handle(self, **options):
        verbosity = options.get('verbosity', 1)

        for app_config in apps.get_app_configs():
            fixtures = self._get_fixtures_for_app(app_config)
            for fixture in fixtures:
                if verbosity:
                    self.stdout.write('Загружаем фикстуру {}\n'.format(fixture))
                call_command('loaddata', fixture, verbosity=verbosity)

        if verbosity:
            self.stdout.write('Начальные данные расписаний загружены.\n')

    @staticmethod
    def _get_fixtures_for_app(app_config):
        from library.python import resource

        result = []
        fixtures_dir = os.path.join(app_config.module.__name__.replace('.', '/'), 'fixtures/')
        for filename in resource.iterkeys('resfs/file/' + fixtures_dir, strip_prefix=True):
            if filename.endswith(INITIAL_SUFFIX):
                result.append(os.path.join(fixtures_dir, filename))
        return sorted(result)
