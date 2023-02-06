# -*- coding: utf-8 -*-
from datetime import datetime, timedelta

import mock

from travel.rasp.api_public.api_public.old_versions.core.json_helpers import schedule_route2json, Segment2Json
from stationschedule.type.suburban import SuburbanSchedule

from common.tester.testcase import TestCase
from common.tester.factories import create_station, create_rthread_segment, create_train_schedule_plan, create_thread


class TestScheduleRoute2Json(TestCase):
    def test_take_direction_from_schedule_item(self):
        schedule = SuburbanSchedule(create_station())
        schedule_route = mock.Mock()
        schedule_route.schedule_item.direction_code = u'На Берлин'

        with mock.patch.object(SuburbanSchedule, 'get_direction_title_by_code') as m_get_title:
            m_get_title.side_effect = lambda code: code

            result = schedule_route2json(schedule_route, schedule, None, 'suburban')
            assert result['direction'] == u'На Берлин'

    def test_train_schedule_plan(self):
        schedule = SuburbanSchedule(create_station())
        schedule_route = mock.Mock(mask_shift=0)

        plan = create_train_schedule_plan(start_date=datetime.now() + timedelta(days=2))
        current_plan, next_plan = plan.get_current_and_next(datetime.now().date())
        schedule_route.thread = create_thread(
            schedule_plan=plan,
            translated_days_texts=u'[{}, {"ru": "ежедневно"}]')

        result = schedule_route2json(schedule_route, schedule, None, 'suburban', next_plan=next_plan)
        assert result['days'] == u'ежедневно ' + next_plan.L_appendix()

    def test_no_train_schedule_plan(self):
        schedule = SuburbanSchedule(create_station())
        schedule_route = mock.Mock(mask_shift=0)
        schedule_route.thread = create_thread(translated_days_texts=u'[{}, {"ru": "ежедневно"}]')
        result = schedule_route2json(schedule_route, schedule, None, 'suburban')
        assert result['days'] == u'ежедневно'


class TestSegment2Json(TestCase):
    def test_next_plan(self):
        segment = create_rthread_segment()

        def check_call(next_plan):
            with mock.patch('common.models.schedule.RThread.L_days_text_dict') as m_text:
                m_text.return_value = {'days_text': 'bla'}
                result = Segment2Json()(segment, create_station(), create_station(), None, None)

                assert len(m_text.call_args_list) == 1
                assert m_text.call_args_list[0][1] == {'next_plan': next_plan}

                assert result['days'] == 'bla'

        check_call(None)

        segment.next_plan = create_train_schedule_plan()
        check_call(segment.next_plan)
