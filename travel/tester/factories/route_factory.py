# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from copy import copy

from travel.rasp.library.python.common23.models.core.schedule.route import Route
from travel.rasp.library.python.common23.tester.factories.base_factory import ModelFactory, factories
from travel.rasp.library.python.common23.tester.factories.factories import DEFAULT_TRANSPORT_TYPE


class RouteFactory(ModelFactory):
    Model = Route

    default_kwargs = {
        '__': {
            'threads': []
        },
        't_type': DEFAULT_TRANSPORT_TYPE,
        'supplier': {},
        'route_uid': lambda: RouteFactory.gen_route_uid(),
        'script_protected': False,
    }

    @classmethod
    def gen_route_uid(cls):
        cls.uid_counter = getattr(cls, 'uid_counter', 0) + 1
        return 'ROUTE_UID_{}'.format(cls.uid_counter)

    def create_object(self, kwargs):
        from travel.rasp.library.python.common23.tester.factories.thread_factory import create_thread

        extra_params = kwargs.pop('__', None) or {}
        threads_params = extra_params.get('threads', [])

        route = super(RouteFactory, self).create_object(kwargs)

        for index, thread_kwargs in enumerate(threads_params):
            thread_kwargs = copy(thread_kwargs)
            thread_kwargs['route'] = route
            thread_kwargs.setdefault('t_type', route.t_type)
            thread_kwargs.setdefault('supplier', route.supplier)
            thread_kwargs.setdefault('ordinal_number', index + 1)
            thread_kwargs.setdefault('uid', 'THREAD_UID_{}_{}'.format(route.route_uid, index + 1))

            create_thread(**thread_kwargs)

        return route


create_route = RouteFactory()
factories[Route] = create_route
