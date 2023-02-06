# -*- coding: utf-8 -*-
from travel.avia.library.python.tester.factories import create_airport, create_settlement
from travel.avia.ticket_daemon_api.jsonrpc.query import Query


def create_query(when='2017-09-01', return_date=None, passengers='1_0_0',
                 national_version='ru', lang='ru', from_is_settlement=True,
                 to_is_settlement=True):
    from_c = create_settlement(title='CITY-FROM')
    to_c = create_settlement(title='CITY-TO')

    def get_airport(point):
        return create_airport(title='AIRPORT-{}'.format(point.id), iata='IATA-{}'.format(point.id), settlement=point)

    from_a = get_airport(from_c)
    to_a = get_airport(to_c)

    from_p = from_c if from_is_settlement else from_a
    to_p = to_c if to_is_settlement else to_a

    key = '{from_key}_{to_key}_{when}_{return_date}_economy_{passengers}_{nv}'.format(
        from_key=from_p.point_key,
        to_key=to_p.point_key,
        when=when,
        return_date=return_date,
        passengers=passengers,
        nv=national_version
    )

    return Query.from_key(key, service='ticket', lang=lang, t_code='plane')
