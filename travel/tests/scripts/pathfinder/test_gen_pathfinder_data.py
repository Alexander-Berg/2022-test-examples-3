# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from travel.rasp.rasp_scripts.scripts.pathfinder.gen_pathfinder_data import run, DATA_PATH


@pytest.mark.parametrize('thegraph_only', (
    True, False
))
def test_run(thegraph_only):
    with mock.patch('travel.rasp.rasp_scripts.scripts.pathfinder.gen_pathfinder_data.generate_currency_map') \
            as m_generate_currency_map:
        with mock.patch('travel.rasp.rasp_scripts.scripts.pathfinder.gen_pathfinder_data.generate_map_files') \
                as m_generate_map_files:
            with mock.patch('travel.rasp.rasp_scripts.scripts.pathfinder.gen_pathfinder_data.gen_thegraph_from_baris') \
                    as m_gen_thegraph_from_baris:
                with mock.patch(
                    'travel.rasp.rasp_scripts.scripts.pathfinder.gen_pathfinder_data.gen_thegraph_from_rasp_db') \
                        as m_gen_thegraph_from_rasp_db:

                    run(thegraph_only=thegraph_only)

                    m_generate_currency_map.assert_called_once_with(DATA_PATH)

                    if not thegraph_only:
                        m_generate_map_files.assert_called_once_with(DATA_PATH)
                    else:
                        m_generate_map_files.assert_not_called()

                    m_gen_thegraph_from_baris.assert_called_once_with(mock.ANY)
                    m_gen_thegraph_from_rasp_db.assert_called_once_with(mock.ANY)
