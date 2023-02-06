# coding: utf-8

from __future__ import unicode_literals

import os

from django.apps import apps
from django.core.management import call_command
from django.core.management.base import BaseCommand

INITIAL_SUFFIX = '_initial.json'


class Command(BaseCommand):
    def handle(self, **options):
        verbosity = options.get('verbosity', 1)

        use_arcadia_paths = options.get('use_arcadia_paths')
        if use_arcadia_paths is None:
            use_arcadia_paths = os.getenv('YA_TEST_RUNNER', False)

        for app_config in apps.get_app_configs():
            if use_arcadia_paths:
                fixtures = self._get_fixtures_for_app_in_arcadia(app_config)
            else:
                fixtures = self._get_fixtures_for_app(app_config)

            for fixture in fixtures:
                if verbosity:
                    self.stdout.write('Загружаем фикстуру {}\n'.format(fixture))
                call_command('loaddata', fixture, verbosity=verbosity)

        if verbosity:
            self.stdout.write('Начальные данные расписаний загружены.\n')

    @staticmethod
    def _get_fixtures_for_app(app_config):
        app_dir = os.path.join(app_config.path, 'fixtures')
        result = []
        if not os.path.exists(app_dir):
            return result

        for filename in sorted(os.listdir(app_dir)):
            filepath = os.path.join(app_dir, filename)
            if not os.path.isfile(filepath) or not filename.endswith(INITIAL_SUFFIX):
                continue
            result.append(filename)
        return sorted(result)

    @staticmethod
    def _get_fixtures_for_app_in_arcadia(app_config):
        from library.python import resource

        result = []
        fixtures_dir = os.path.join(app_config.module.__name__.replace('.', '/'), 'fixtures/')
        for filename in resource.iterkeys('resfs/file/' + fixtures_dir, strip_prefix=True):
            if filename.endswith(INITIAL_SUFFIX):
                result.append(os.path.join(fixtures_dir, filename))
        return sorted(result)
