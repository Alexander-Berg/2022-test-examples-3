# -*- coding: utf-8 -*-


class CheckThreadMixin(object):
    def assertThreadStopsTimes(self, thread, expected_values):
        path = list(thread.path)

        self.assertEqual(len(path), len(expected_values), u'Неправильное количество остановок')

        for stop, (tz_arrival, tz_departure, timezone) in zip(path, expected_values):
            self.assertEqual(stop.tz_arrival, tz_arrival, u'Неправильное время прибытия')
            self.assertEqual(stop.tz_departure, tz_departure, u'Неправильное время отправления')
            self.assertEqual(stop.time_zone, timezone, u'Неправильная временная зона')

    def assertThreadStopsTimesAndFlags(self, thread, expected_values):
        path = list(thread.path)

        self.assertEqual(len(path), len(expected_values), u'Неправильное количество остановок')

        for stop, (tz_arrival, tz_departure, timezone, flags) in zip(path, expected_values):
            self.assertEqual(stop.tz_arrival, tz_arrival, u'Неправильное время прибытия')
            self.assertEqual(stop.tz_departure, tz_departure, u'Неправильное время отправления')
            self.assertEqual(stop.time_zone, timezone, u'Неправильная временная зона')

            if flags:
                for flag_name, value in flags.iteritems():
                    self.assertEqual(getattr(stop, flag_name), value, u'Неправильное значение флага %s' % flag_name)
