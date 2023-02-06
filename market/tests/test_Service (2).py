import unittest

from lib.Service import Service


class ServiceTest(unittest.TestCase):
    def setUp(self):
        service = (
            Service(current_dau=1000, current_rps=100)
                .add_instances(dc='A', instances_count=5, instance_rps_limit=10)
                .add_instances(dc='B', instances_count=5, instance_rps_limit=10, immutable=True, closing=True)
                .add_instances(dc='C', instances_count=5, instance_rps_limit=10)
                .add_instances(dc='D', instances_count=5, instance_rps_limit=20)
        )
        self.service = service

    def test_settings_getter(self):
        settings = self.service.get_current_settings(as_df=False)
        self.assertEqual(1000, settings['DAU'])
        self.assertEqual(100, settings['current_rps'])

    def test_biggest_group_getter(self):
        self.assertEqual('group_D', self.service.get_biggest_group())

    def test_biggest_dc_getter(self):
        self.assertEqual('D', self.service.get_biggest_dc())

    def test_rps_capacity_getter(self):
        self.assertEqual(250, self.service.get_rps_capacity(dc_minus_1=False, use_closing_dc=True))
        self.assertEqual(200, self.service.get_rps_capacity(dc_minus_1=False, use_closing_dc=False))
        self.assertEqual(150, self.service.get_rps_capacity(dc_minus_1=True, use_closing_dc=True))
        self.assertEqual(100, self.service.get_rps_capacity(dc_minus_1=True, use_closing_dc=False))

    def test_dc_count_getter(self):
        self.assertEqual(4, self.service.get_dc_count(dc_minus_1=False, use_closing_dc=True))
        self.assertEqual(3, self.service.get_dc_count(dc_minus_1=True, use_closing_dc=True))
        self.assertEqual(2, self.service.get_dc_count(dc_minus_1=True, use_closing_dc=False))

    def test_dau_capacity_calc(self):
        self.assertEqual(2500/2, self.service.calc_dau_capacity(dc_minus_1=False, use_closing_dc=True, peak_coef=2))
        self.assertEqual(2500, self.service.calc_dau_capacity(dc_minus_1=False, use_closing_dc=True, peak_coef=1))

        self.assertEqual(1500, self.service.calc_dau_capacity(dc_minus_1=True, use_closing_dc=True, peak_coef=1))
        self.assertEqual(500, self.service.calc_dau_capacity(dc_minus_1=True, use_closing_dc=False, peak_coef=2))

    def test_rps_capacity_calc(self):
        # calc_rps_capacity(self, *, dc_minus_1=False, use_closing_dc=True, peak_coef=DEFAULT_PEAK_COEF, as_df=False):
        self.assertEqual(250/2, self.service.calc_rps_capacity(dc_minus_1=False, use_closing_dc=True, peak_coef=2))
        self.assertEqual(250, self.service.calc_rps_capacity(dc_minus_1=False, use_closing_dc=True, peak_coef=1))

        self.assertEqual(150, self.service.calc_rps_capacity(dc_minus_1=True, use_closing_dc=True, peak_coef=1))
        self.assertEqual(50, self.service.calc_rps_capacity(dc_minus_1=True, use_closing_dc=False, peak_coef=2))

    def test_grow_by_rps(self):
        self.assertEqual(35, self.service.calc_grow_by_rps(future_max_rps=400, dc_minus_1=True, peak_coef=1, align_deltas=False, align_groups_capacity=False).iloc[0].delta_sum)
        self.assertEqual(45, self.service.calc_grow_by_rps(future_max_rps=400, dc_minus_1=True, peak_coef=1, align_deltas=True).iloc[0].delta_sum)

        self.assertEqual(
            self.service.calc_grow_by_rps(future_max_rps=600, dc_minus_1=True, peak_coef=1).iloc[0].delta_sum,
            self.service.calc_grow_by_rps(future_max_rps=400, dc_minus_1=True, peak_coef=1.5).iloc[0].delta_sum
        )

    def test_grow_by_dau(self):
        # calc_grow_by_dau(self, *, future_dau, dc_minus_1=False, peak_coef=DEFAULT_PEAK_COEF):
        self.assertEqual(10, self.service.calc_grow_by_dau(future_dau=2000, dc_minus_1=True, peak_coef=1, align_deltas=False, align_groups_capacity=False).iloc[0].delta_sum)
        self.assertEqual(
            self.service.calc_grow_by_dau(future_dau=3000, dc_minus_1=True, peak_coef=1, align_deltas=False).iloc[0].delta_sum,
            self.service.calc_grow_by_dau(future_dau=2000, dc_minus_1=True, peak_coef=1.5, align_deltas=False).iloc[0].delta_sum
        )

    def test_grow_by_dau_and_rps(self):
        self.assertEqual(
            self.service.calc_grow_by_dau(future_dau=5000, dc_minus_1=True, peak_coef=1.5, align_deltas=False).iloc[0].delta_sum,
            self.service.calc_grow_by_rps(future_max_rps=500, dc_minus_1=True, peak_coef=1.5, align_deltas=False).iloc[0].delta_sum
        )

    def test_report_service(self):
        report_white_main = (
            Service( name="report_white_main", current_dau=100*10**3, current_rps=600 )
                .add_instances(dc='SAS', instances_count=2, instance_rps_limit=100)
                .add_instances(dc='VLA', instances_count=2, instance_rps_limit=100)
                .add_instances(dc='MAN', instances_count=2, instance_rps_limit=100)
        )

        self.assertEqual(report_white_main.get_rps_capacity(dc_minus_1=False, consider_reload=False, use_closing_dc=True), 2*300)
        self.assertEqual(report_white_main.get_rps_capacity(dc_minus_1=True, consider_reload=False, use_closing_dc=True), 2*200)
        self.assertEqual(report_white_main.get_rps_capacity(dc_minus_1=False, consider_reload=True, use_closing_dc=True), 300)
        self.assertEqual(report_white_main.get_rps_capacity(dc_minus_1=True, consider_reload=True, use_closing_dc=True), 200)

        self.assertEqual(report_white_main.calc_rps_capacity(dc_minus_1=False, consider_reload=False, use_closing_dc=True, peak_coef=1), 2*300)
        self.assertEqual(report_white_main.calc_rps_capacity(dc_minus_1=True, consider_reload=False, use_closing_dc=True, peak_coef=1), 2*200)
        self.assertEqual(report_white_main.calc_rps_capacity(dc_minus_1=False, consider_reload=True, use_closing_dc=True, peak_coef=1), 300)
        self.assertEqual(report_white_main.calc_rps_capacity(dc_minus_1=True, consider_reload=True, use_closing_dc=True, peak_coef=1), 200)

        self.assertEqual(int(report_white_main.calc_grow_by_dau(future_dau=100*10**3, peak_coef=1).iloc[0]['delta_sum']), 0)
        self.assertEqual(int(report_white_main.calc_grow_by_dau(future_dau=200*10**3, peak_coef=1).iloc[0]['delta_sum']), 6)
        self.assertEqual(int(report_white_main.calc_grow_by_dau(future_dau=200*10**3, peak_coef=2).iloc[0]['delta_sum']), 18)
        self.assertEqual(int(report_white_main.calc_grow_by_dau(future_dau=400*10**3, peak_coef=1).iloc[0]['delta_sum']), 18)

        self.assertEqual(int(report_white_main.calc_grow_by_dau(future_dau=400*10**3, dc_minus_1=True, peak_coef=1).iloc[0]['delta_sum']), 30)


if __name__ == '__main__':
    unittest.main()
