import unittest
from duty_assistant.util import calc_stats, find_duty


class TestUtil(unittest.TestCase):
    def test_calc_stats(self):
        events_list = [['host1', 'dc1', 'service1', 'date1', 'description1'],
                       ['host2', 'dc2', 'service1', 'date2', 'description2'],
                       ['host3', 'dc3', 'service1', 'date3', 'description3'],
                       ['host1', 'dc2', 'service1', 'date4', 'description4'],
                       ['host1', 'dc2', 'service2', 'date5', 'description5']]
        result = [('host1: service1', 2),
                  ('host1: service2', 1),
                  ('host3: service1', 1),
                  ('host2: service1', 1)]
        self.assertEqual(calc_stats(events_list), result)

    def test_extract_duty(self):
        duty_list = [('dukeartem@incident', 'dukeartem'),
                     ('asimakov@incident', 'asimakov'),
                     ('maxk@incident', 'maxk'),
                     ('pashayelkin@incident', 'pashayelkin')]
        for (text, login) in duty_list:
            cal_event = {
                # не относящиеся к тесту поля пропущены
                "name": text,
            }

            self.assertEqual(find_duty(cal_event), login)


if __name__ == '__main__':
    unittest.main()
