import unittest

from lib.Service import Service
from lib.DAU import DAU_Modeller


class ServiceTest(unittest.TestCase):
    def setUp(self):
        self.report = (Service(current_dau=1000, current_rps=100)
            .add_instances(dc='A', instances_count=5, instance_rps_limit=10)
            .add_instances(dc='B', instances_count=5, instance_rps_limit=10, immutable=True, closing=True)
            .add_instances(dc='C', instances_count=5, instance_rps_limit=10)
            .add_instances(dc='D', instances_count=5, instance_rps_limit=20)
        ) # capacity 1k

        self.front_touch = (Service(current_dau=10000, current_rps=100)
            .add_instances(dc='A', instances_count=5, instance_rps_limit=100)
            .add_instances(dc='B', instances_count=5, instance_rps_limit=100, immutable=True, closing=True)
            .add_instances(dc='C', instances_count=5, instance_rps_limit=100)
            .add_instances(dc='D', instances_count=5, instance_rps_limit=100)
        ) # capacity 100k

        self.front_desktop = (Service(current_dau=10000, current_rps=100)
            .add_instances(dc='A', instances_count=5, instance_rps_limit=100)
            .add_instances(dc='B', instances_count=5, instance_rps_limit=100, immutable=True, closing=True)
            .add_instances(dc='C', instances_count=5, instance_rps_limit=100)
            .add_instances(dc='D', instances_count=5, instance_rps_limit=100)
        ) # capacity 100k

        self.kgb = (Service(current_dau=100, current_rps=100)
            .add_instances(dc='A', instances_count=5, instance_rps_limit=150)
            .add_instances(dc='B', instances_count=5, instance_rps_limit=150, immutable=True, closing=True)
            .add_instances(dc='C', instances_count=5, instance_rps_limit=150)
            .add_instances(dc='D', instances_count=5, instance_rps_limit=200)
        ) # capacity 1500

    def test_calc(self):
        test = DAU_Modeller(services={
            'touch': [self.report, self.front_touch],
            'desktop': [self.report, self.front_desktop],
        }).calc_dau_capacity(dc_minus_1=True, peak_coef=1.5)

        self.assertEqual(500, test['touch'])
        self.assertEqual(500, test['desktop'])

        test  = DAU_Modeller(services={
            'touch': [self.report, self.front_touch],
            'desktop': [self.front_desktop],
        }).calc_dau_capacity(dc_minus_1=True, peak_coef=1.5)

        self.assertEqual(1000, test['touch'])
        self.assertEqual(100_000, test['desktop'])

        test = DAU_Modeller(services={
            'touch': [self.report, self.front_touch],
            'desktop': [self.kgb, self.front_desktop],
        }).calc_dau_capacity(dc_minus_1=True, peak_coef=1.5)

        self.assertEqual(1000, test['touch'])
        self.assertEqual(1500, test['desktop'])

        # todo, более сложные тесты KGB

        kgb = (Service(current_dau=100, current_rps=100)
            .add_instances(dc='A', instances_count=5, instance_rps_limit=75)
            .add_instances(dc='B', instances_count=5, instance_rps_limit=75, immutable=True, closing=True)
            .add_instances(dc='C', instances_count=5, instance_rps_limit=75)
            .add_instances(dc='D', instances_count=5, instance_rps_limit=200)
        ) # capacity 1500/2

        test = DAU_Modeller(services={
            'touch': [self.report, self.front_touch, kgb],
            'desktop': [self.report, self.front_desktop, kgb],
        }).calc_dau_capacity(dc_minus_1=True, peak_coef=1.5)

        self.assertEqual(375, test['touch'])
        self.assertEqual(375, test['desktop'])


if __name__ == '__main__':
    unittest.main()
