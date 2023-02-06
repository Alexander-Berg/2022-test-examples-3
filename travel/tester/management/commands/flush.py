# coding: utf-8

from __future__ import unicode_literals

from django.core.management import call_command
from django.core.management.commands import flush


class Command(flush.Command):
    def handle(self, **options):
        super_options = dict(options)
        super_options['load_initial_data'] = False

        super(Command, self).handle(**super_options)

        if options['load_initial_data']:
            call_command('loadinitialdata', verbosity=options.get('verbosity', 1))
