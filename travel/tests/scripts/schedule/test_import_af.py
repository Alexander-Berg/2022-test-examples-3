# coding: utf-8

from datetime import date, datetime
from xml.etree.ElementTree import Element

import mock

from common.models.geo import Country
from common.models.schedule import RThread
from common.models.transport import TransportType
from common.utils.calendar_matcher import YCalendar

from travel.rasp.admin.lib.mask_builder.bounds import MaskBounds
from travel.rasp.admin.lib.mask_description import make_dates_from_mask_description
from travel.rasp.admin.lib.unittests import MaskComparisonMixIn
from travel.rasp.admin.scripts.schedule.af_processors.af_multiparam_mask_parser import make_runmask
from tester.testcase import TestCase
from tester.utils.datetime import replace_now


start = date(2011, 3, 1)
today = date(2011, 3, 10)
end = date(2011, 3, 31)

module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class MakeMaskTest(TestCase, MaskComparisonMixIn):
    class_fixtures = module_fixtures

    def setUp(self):
        self.bounds = MaskBounds(date(2011, 3, 1), date(2011, 3, 31))
        self.russia = Country.objects.get(pk=Country.RUSSIA_ID)

    @replace_now(datetime(2011, 3, 1))
    def test_make_runmask_weekends(self):
        thread = RThread()
        thread.t_type = TransportType.objects.get(code='suburban')
        thread.schedule_plan = None
        thread.changemode = "change"

        thread_el = Element('thread')
        thread_el.set('period_start', '2011-03-01')
        thread_el.set('period_end', '2011-03-31')
        thread_el.set('weekend', 'Y')

        with mock.patch.object(YCalendar, 'get_weekends') as m_get_weekends:
            m_get_weekends.return_value = make_dates_from_mask_description(u"""
            март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5  # 6
               # 7  # 8    9   10   11  #12  #13
                14   15   16   17   18  #19  #20
                21   22   23   24   25  #26  #27
                28   29   30   31
            """)

            mask = make_runmask(thread_el, self.bounds, self.russia, today)

            m_get_weekends.assert_called_with(date(2011, 3, 1), date(2011, 3, 31), self.russia)

        self.assert_mask_equal_description(mask, u"""
            март 2011
             пн   вт   ср   чт   пт   сб   вс
                   1    2    3    4    5  # 6
            # 7  # 8    9   10   11  #12  #13
             14   15   16   17   18  #19  #20
             21   22   23   24   25  #26  #27
             28   29   30   31
        """)

    @replace_now(datetime(2011, 3, 1))
    def test_make_runmask_workdays(self):
        thread = RThread()
        thread.t_type = TransportType.objects.get(code='suburban')
        thread.schedule_plan = None
        thread.changemode = 'change'

        thread_el = Element('thread')
        thread_el.set('period_start', '2011-03-01')
        thread_el.set('period_end', '2011-03-31')
        thread_el.set('workdays', 'Y')

        with mock.patch.object(YCalendar, 'get_workdays') as m_get_workdays:
            m_get_workdays.return_value = make_dates_from_mask_description(u"""
            март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5    6
                 7    8  # 9  #10  #11   12   13
               #14  #15  #16  #17  #18   19   20
               #21  #22  #23  #24  #25   26   27
               #28  #29  #30  #31
            """)

            mask = make_runmask(thread_el, self.bounds, self.russia, today)

            m_get_workdays.assert_called_with(date(2011, 3, 1), date(2011, 3, 31), self.russia)

        self.assert_mask_equal_description(mask, u"""
           март 2011
            пн   вт   ср   чт   пт   сб   вс
                # 1  # 2  # 3  # 4  # 5    6
             7    8  # 9  #10  #11   12   13
           #14  #15  #16  #17  #18   19   20
           #21  #22  #23  #24  #25   26   27
           #28  #29  #30  #31
        """)

    @replace_now(datetime(2011, 3, 1))
    def test_make_runmask_daily(self):
        thread = RThread()
        thread.t_type = TransportType.objects.get(code='suburban')
        thread.schedule_plan = None
        thread.changemode = 'change'

        thread_el = Element('thread')
        thread_el.set('period_start', '2011-03-01')
        thread_el.set('period_end', '2011-03-31')
        thread_el.set('daily', 'Y')

        mask = make_runmask(thread_el, self.bounds, self.russia, today)

        self.assert_mask_equal_description(mask, u"""
            март 2011
             пн   вт   ср   чт   пт   сб   вс
                 # 1  # 2  # 3  # 4  # 5  # 6
            # 7  # 8  # 9  #10  #11  #12  #13
            #14  #15  #16  #17  #18  #19  #20
            #21  #22  #23  #24  #25  #26  #27
            #28  #29  #30  #31
        """)

    @replace_now(datetime(2011, 3, 1))
    def test_make_runmask_noweekends(self):
        thread = RThread()
        thread.t_type = TransportType.objects.get(code='suburban')
        thread.schedule_plan = None
        thread.changemode = "change"

        thread_el = Element('thread')
        thread_el.set('period_start', '2011-03-01')
        thread_el.set('period_end', '2011-03-31')
        thread_el.set('even', 'Y')
        thread_el.set('noweekend', 'Y')

        with mock.patch.object(YCalendar, 'get_weekends') as m_get_weekends:
            m_get_weekends.return_value = make_dates_from_mask_description(u"""
            март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5  # 6
               # 7  # 8    9   10   11  #12  #13
                14   15   16   17   18  #19  #20
                21   22   23   24   25  #26  #27
                28   29   30   31
            """)

            mask = make_runmask(thread_el, self.bounds, self.russia, today)

            m_get_weekends.assert_called_with(date(2011, 3, 1), date(2011, 3, 31), self.russia)

        self.assert_mask_equal_description(mask, u"""
            март 2011
             пн   вт   ср   чт   пт   сб   вс
                   1  # 2    3  # 4    5    6
              7    8    9  #10   11   12   13
            #14   15  #16   17  #18   19   20
             21  #22   23  #24   25   26   27
            #28   29  #30   31
        """)
