# coding: utf8
import pytest

# route_search имеет зависимость от stationschedule в tablo/add_z_tablos_to_segments
# Не во всех проектах подключен stationschedule, поэтому мы должны пропускать некоторые тесты.
stationschedule_skip_reason = u'There is no stationschedule submodule'
try:
    import stationschedule  # noqa
    HAS_STATIONSCHEDULE = True
except ImportError:
    HAS_STATIONSCHEDULE = False

has_stationschedule = pytest.mark.skipif(not HAS_STATIONSCHEDULE, reason=stationschedule_skip_reason)
