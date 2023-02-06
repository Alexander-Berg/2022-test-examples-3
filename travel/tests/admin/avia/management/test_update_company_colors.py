# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import mock
import pytest

from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.tester.factories import create_company
from travel.avia.admin.avia.management.commands import update_company_colors as update_company_colors_module
from travel.avia.admin.avia.management.commands.update_company_colors import update_company_colors


LOGO_CONTENT = b"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generator: Adobe Illustrator 16.0.0, SVG Export Plug-In . SVG Version: 6.00 Build 0)  -->
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg version="1.1" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
  x="0px" y="0px" width="30px" height="30px" viewBox="-290.5 368.5 30 30"
  enable-background="new -290.5 368.5 30 30" xml:space="preserve">
<rect x="-290.5" y="368.5" fill="#0552A1" width="30" height="30"/>
<path fill="#FFFFFF" d="M-267.5,391.5v-2l-8.853-7.999c-0.343-0.316-0.23-0.487,0.097-0.487c0.378,0,6.507,0,6.507,0l-2.958,2.325
  l1.207,1.161l6-5h-9.587c-1.356,0-1.854,0.029-2.174,0.124c-0.308,0.094-0.708,0.287-0.923,0.757
  c-0.157,0.348-0.166,0.969,0.058,1.353c0.311,0.534,1.281,1.348,2.626,2.767l-5.172-3.484h1.172l-0.002-1.516h-5.998L-267.5,391.5z"
/>
</svg>"""


@pytest.mark.dbuser
def test_update_company_color():
    company = create_company(svg_logo2='some-url', logo_bgcolor='')

    with mock.patch.object(update_company_colors_module, 'download_logo', return_value=LOGO_CONTENT) as m_download_logo:
        with mock.patch.object(update_company_colors_module, 'build_company_qs',
                               return_value=Company.objects.filter(id=company.id)) as m_build_company_qs:
            update_company_colors(force=False, html_path=None)

            company.refresh_from_db()
            assert company.logo_bgcolor == '#0552a1'
            assert m_download_logo.call_count == 1
            assert m_build_company_qs.call_count == 1
