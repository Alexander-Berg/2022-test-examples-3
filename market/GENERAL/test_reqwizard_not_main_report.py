#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Wildcard


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.reqwizard.on_default_request().respond()

        # not market-main-report
        cls.reqwizard.wizclient = 'market-main-report-api'

    @classmethod
    def prepare_not_begemot_not_on_main_report(cls):
        qtree = 'cHicrZTPaxNBFMffm2zjOK0SWqpxIRrWg2tACOKhCKkiHoKI1OCh7sW2aWgqsZG0aMnFHERCD-IPEFoqQrVNUjBqPWuv_ugh_Qe86cmjvXjyze5ku4bNYsFAeLsz3_edz5u3M-KS6OMQgSjEmcmS0A86GJCA03C2PQ4mJOF8T7pnBEZhDPL4CGEJYQVhA-EDAv0-IbRwUq-huKrsOKVJu9CdqXEd465lyGMJaZCW-d8fWdtSyjtckzCEF9IcI6DLWQNMTOLIU-awlBJCjkZtxyHIsGrTwtWJMKnBHJjgFNmJWTOUh3lWZpxVEGgtX9JSdpxqx3gA6Y_HmktK8gBSmvUhpdGoHA0kRS-pcYDfi1QwyuIhsycJia8hkVbgmgLv1rE2dPd-_WK2GYsw8g82U-0PMLuPYkyR7VffUjhbnJnLzduWYWOqlMvN2MbhrpQqYwWV3G-HD8k1pKe9veqbLIPqbBXFZAAGN7KF8dnZ6ey_grgJe0YxHAoW16R_EofQGZeZzlPiO_7Hbj5EcbOj8t5bxclc4Ua2WCiWVBfu5qfncrZ7b1d3b5pshZ2z5_p365RP1jvkMCjz9RcornUcvnBro_W-9dZTu9_xc2tXcj-klH3-lECBvWwfwaNCTbinENoHMM_nwe2cJuFMzXrFOCfm8kX9CRNWJ_N2ZfsBMeNJA80A6uVn3167F5xK8iNvoIPuKDrRzwg1EUWFrlWbm18sXLewbmHNwjULG54bhe4RLGV8spY3P1ts_Q39mxarU6xTrFGsUVyjuEaxQbFBcZXiatPrSXcTuttEV4cpN20Bjwl70_oPc9QHeHhwa-tyKhYZvZ6KwylZaFvBXMXz26lzsczS4PBfCuZRLC4eGY7t_Dy-qxhQHr2cZ_Zx7A9dGcktYJ9aW-MHS-C8Shv7lTj_AKbnRy8,'  # noqa

        cls.reqwizard.on_request('vga rca').respond(
            qtree=qtree,
        )

        cls.reqwizard.on_request('vga rca', wizextras='market-lingboost').respond(
            qtree=qtree, expansions='2tACOKhCKkiHoKI1OCh7sW'
        )

    def test_not_begemot_not_on_main_report(self):
        """
        Проверяем, что, не будет использоваться бегемотная инсталляция реквизарда,
        если в конфиге WizClient не равен market-main-report
        """
        request = 'place=prime&debug=da&text=vga+rca'

        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"logicTrace": [Wildcard('*/wizard*wizclient=market-main-report-api*')]})
        self.assertFragmentNotIn(response, {"logicTrace": [Wildcard('*/wizard*wizextra=market-lingboost*')]})
        self.assertFragmentNotIn(response, {"logicTrace": [Wildcard('*Got expansions for the request*')]})


if __name__ == '__main__':
    main()
