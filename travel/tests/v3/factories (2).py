# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_station, create_thread

create_station = create_station.mutate(t_type='suburban')
create_thread = create_thread.mutate(t_type='suburban', __={'calculate_noderoute': True})
