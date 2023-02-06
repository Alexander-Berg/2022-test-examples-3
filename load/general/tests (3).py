import json
import pytest
from common.tests import CommonTestCase
from common.models import DataMeta
from django.test import TestCase


class RemoteComparePageTests(TestCase):
    """
    Тесты приложения compare, работающие на тестинге
    """

    @pytest.mark.skip
    def test_get_metrics_blank_response(self):
        # https://st.yandex-team.ru/LUNA-509#5d1314ccc38f24001e282da7
        resp = self.client.get('compare_metrics/?tag=1c272ec654f7429ca4e5139592a32517&tag=52a279c2895f4d39838950a41c830fb6&interval_offset=0&interval_offset=10376623')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content.decode('utf-8').startswith('''timestamp,1c272ec654f7429ca4e5139592a32517_,52a279c2895f4d39838950a41c830fb6_
623377,,4104895.0
1000000,4199041.0,4104704.25
1376623,4199041.0,4104513.5
1753246,4199041.0,4104513.5
2129869,4199041.0,4104322.75
2506492,4191259.0,4104132.0
2883115,4183477.0,4104132.0
'''))


class JobComparePageTests(CommonTestCase):
    """
    Тесты приложения compare
    """
    maxDiff = None

    @pytest.mark.skip
    def test_layout(self):
        resp = self.client.get('/compare_layout/?job={}&job={}'.format(self.job1.pk, self.job2.pk))
        self.assertEqual(resp.status_code, 200)
        self.assertIsInstance(json.loads(resp.content.decode('utf-8')), dict)

    # def test_layout_debug(self):
    #     """
    #     Отладочный тест, всегда фэйлится и принтит в stdout то, что ответила ручка.
    #     :return:
    #     """
    #     resp = self.client.get('/compare_layout/?job={}&job={}'.format(self.job1.pk, self.job2.pk))
    #     self.assertEqual(resp.status_code, 200)
    #     self.assertEqual(json.loads(resp.content.decode('utf-8')), {})

    # def test_get_metrics(self):
    #     """
    #     Отладочный тест, всегда фэйлится и принтит в stdout то, что ответила ручка.
    #     :return:
    #     """
    #     job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
    #     job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
    #     resp = self.client.get('/compare_metrics/?tag=%s&tag=%s' % (job1_current_tag, job2_current_tag))
    #     self.assertEqual(resp.status_code, 200)
    #     self.assertEqual(resp.content.decode('utf-8'), 'xff')

    # def test_get_metrics_interval(self):
    #     """
    #     Отладочный тест, всегда фэйлится и принтит в stdout то, что ответила ручка.
    #     :return:
    #     """
    #     job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
    #     job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
    #     resp = self.client.get('/compare_metrics/?tag=%s&tag=%s&interval=%s' % (job1_current_tag, job2_current_tag, 3*10**6))
    #     self.assertEqual(resp.status_code, 200)
    #     self.assertEqual(resp.content.decode('utf-8'), 'xff')

    @pytest.mark.skip
    def test_get_metrics_interval_offset(self):
        """
        Отладочный тест, всегда фэйлится и принтит в stdout то, что ответила ручка.
        :return:
        """
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
        resp = self.client.get('/compare_metrics/?tag=%s&tag=%s&interval=%s&interval_offset=%s&interval_offset=%s' %
                               (job1_current_tag, job2_current_tag, 3*10**6, 10**6, 2*10**6)
                               )
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(resp.content.decode('utf-8').startswith('''timestamp,a620a7da3a934593abfb923a62343655_
0,818.3691340000001
3000,817.6041936666666
6000,804.905618333333
9000,862.1256470000001
12000,862.1256369999999
15000,902.975242
18000,1012.2135296666664
21000,845.2961886666666
24000,847.8971296666671
27000,859.3717223333333
30000,858.4537516666666
33000,861.3606626666667
36000,857.9947593333333
39000,833.2096193333333
42000,812.2493453333334
45000,902.0572739999997
48000,916.897756
'''))

    @pytest.mark.skip
    def test_get_metrics_inavlid_params(self):
        resp = self.client.get('/compare_metrics/?tag=')
        self.assertEqual(resp.status_code, 400)
        resp = self.client.get('/compare_metrics/?interval=kdkj')
        self.assertEqual(resp.status_code, 400)
        resp = self.client.get('/compare_metrics/?dots=0')
        self.assertEqual(resp.status_code, 400)
        resp = self.client.get('/compare_metrics/?interval_offset=rfr')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_get_metrics_negative_offset(self):
        # https://st.yandex-team.ru/LUNA-509#5d1499ffc38f24001e2b1d3c
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        resp = self.client.get('/compare_metrics/?tag=%s&interval_offset=%s' %
                               (job1_current_tag, -1))
        self.assertEqual(resp.status_code, 200)
        self.assertNotEqual(resp.content.decode('utf-8'), '""\n')

    @pytest.mark.skip
    def test_get_summary(self):
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
        resp = self.client.get(
            '/compare_summary/?tag=%s&tag=%s&interval=%s&interval_offset=%s&interval_offset=%s' %
            (job1_current_tag, job2_current_tag, 3 * 10 ** 6, 10 ** 6, 10 ** 6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''tag,max,q99,q98,q95,q90,q85,q80,q75,q50,q25,q10,min,average,stddev
a620a7da3a934593abfb923a62343655,3589.2578,2239.8438,2056.25,1923.1445,1872.6562,1868.0664,1868.0664,1858.8867,1675.2927,330.46875,307.51953,192.77344,1204.3940324664004,698.8213235822473
48c839e3a4ff4a059c641f16e365a34d,93.7793,79.35792000000002,68.15918,56.154785,50.595707,45.358887,43.103024,41.57226,22.15576,15.791016,13.776854,7.976074,29.662522361655675,15.996811338018544
''')

    @pytest.mark.skip
    def test_get_summary_diff(self):
        job1_current_metric = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data
        job1_current_tag = job1_current_metric.uniq_id
        job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
        DataMeta.objects.create(data=job1_current_metric, key='_apply', value='diff')
        resp = self.client.get(
            '/compare_summary/?tag=%s&tag=%s&interval=%s&interval_offset=%s&interval_offset=%s' %
            (job1_current_tag, job2_current_tag, 3 * 10 ** 6, 10 ** 6, 2 * 10 ** 6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''tag,max,q99,q98,q95,q90,q85,q80,q75,q50,q25,q10,min,average,stddev
a620a7da3a934593abfb923a62343655,596.6794,110.1561999999999,59.667910000000006,22.94920000000002,13.769560000000013,13.76949999999988,9.179699999999912,4.590099999999893,0.0,-9.179669999999987,-13.769560000000013,-426.85530000000017,0.0307509233025566,30.731614927898907
48c839e3a4ff4a059c641f16e365a34d,93.7793,79.35792000000002,68.15918,56.154785,50.595707,45.358887,43.103024,41.57226,22.15576,15.791016,13.776854,7.976074,29.662522361655675,15.996811338018544
''')

    @pytest.mark.skip
    def test_get_aggregates(self):
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        resp = self.client.get(
            '/compare_aggregates/?tag=%s&interval=%s&interval_offset=%s' %
            (job1_current_tag, 3 * 10 ** 6, 10 ** 6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''timestamp,%(tag)s_max,%(tag)s_q99,%(tag)s_q98,%(tag)s_q95,%(tag)s_q90,%(tag)s_q85,%(tag)s_q80,%(tag)s_q75,%(tag)s_q50,%(tag)s_q25,%(tag)s_q10,%(tag)s_min
0,3589.2578,2584.082000000001,2239.8438,1982.8125,1932.3243,1881.8359,1877.2461,1872.6562,1757.9102,881.25,830.76166,431.4453
1000000,3566.3086,2239.8438,2111.3281,1881.8359,1872.6562,1868.0664,1868.0664,1863.4764,1831.3477,325.87888,321.28906,293.75
2000000,2203.125,1941.5039,1886.4259,1698.2422,1689.0625,1689.0625,1689.0625,1684.4725,458.98434,302.9297,298.3398,192.77344
''' % {'tag': job1_current_tag})

    @pytest.mark.skip
    def test_get_aggregates_fields(self):
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        resp = self.client.get(
            '/compare_aggregates/?tag=%s&interval=%s&interval_offset=%s&field=max&field=q90&field=q34234' %
            (job1_current_tag, 3 * 10 ** 6, 10 ** 6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''timestamp,%(tag)s_max,%(tag)s_q90
0,3589.2578,1932.3243
1000000,3566.3086,1872.6562
2000000,2203.125,1689.0625
''' % {'tag': job1_current_tag})

    @pytest.mark.skip
    def test_get_aggregates_diff(self):
        job1_current_metric = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data
        job1_current_tag = job1_current_metric.uniq_id
        DataMeta.objects.create(data=job1_current_metric, key='_apply', value='diff')
        resp = self.client.get(
            '/compare_aggregates/?tag=%s&interval=%s&interval_offset=%s' % (job1_current_tag, 3 * 10 ** 6, 10 ** 6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''timestamp,%(tag)s_max,%(tag)s_q99,%(tag)s_q98,%(tag)s_q95,%(tag)s_q90,%(tag)s_q85,%(tag)s_q80,%(tag)s_q75,%(tag)s_q50,%(tag)s_q25,%(tag)s_q10,%(tag)s_min
0,596.6794,169.82420000000005,128.51559999999995,55.078300000000134,18.359500000000025,13.769500000000107,13.769459999999981,9.179699999999912,0.0,-9.17970000000014,-22.94924000000003,-426.85530000000017
1000000,472.75399999999985,41.308700000000044,22.949200000000477,13.769560000000013,13.76949999999988,9.179799999999886,9.179689999999994,4.589879999999994,0.0,-4.589999999999918,-13.769499999999995,-270.80069999999984
2000000,362.59750000000014,32.12910000000011,22.949499999999947,13.769800000000034,13.769699999999832,9.179800000000114,9.179699999999912,4.589999999999918,0.0,-4.59010000000012,-13.769530000000033,-142.28521999999995
''' % {'tag': job1_current_tag})

    @pytest.mark.skip
    def test_get_aggregates_two_metrics(self):
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
        resp = self.client.get(
            '/compare_aggregates/?tag=%s&tag=%s&interval=%s&interval_offset=%s&interval_offset=%s' %
            (job1_current_tag, job2_current_tag, 3 * 10**6, 10**6, 10**6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''timestamp,%(tag1)s_max,%(tag1)s_q99,%(tag1)s_q98,%(tag1)s_q95,%(tag1)s_q90,%(tag1)s_q85,%(tag1)s_q80,%(tag1)s_q75,%(tag1)s_q50,%(tag1)s_q25,%(tag1)s_q10,%(tag1)s_min,%(tag2)s_max,%(tag2)s_q99,%(tag2)s_q98,%(tag2)s_q95,%(tag2)s_q90,%(tag2)s_q85,%(tag2)s_q80,%(tag2)s_q75,%(tag2)s_q50,%(tag2)s_q25,%(tag2)s_q10,%(tag2)s_min
0,3589.2578,2584.082000000001,2239.8438,1982.8125,1932.3243,1881.8359,1877.2461,1872.6562,1757.9102,881.25,830.76166,431.4453,120.84962,106.75048,78.39110600000002,70.092766,64.45313,60.102535,57.927246,55.671387,45.036617,41.57226,40.283203,17.080078
1000000,3566.3086,2239.8438,2111.3281,1881.8359,1872.6562,1868.0664,1868.0664,1863.4764,1831.3477,325.87888,321.28906,293.75,78.06885,57.766117,53.093258,45.358887,44.069824,40.041504,31.662598,27.150877,18.369139,14.663087,13.776854,9.42627
2000000,2203.125,1941.5039,1886.4259,1698.2422,1689.0625,1689.0625,1689.0625,1684.4725,458.98434,302.9297,298.3398,192.77344,93.7793,84.75586,81.69433000000002,76.13525,57.60498000000001,52.932125,47.29248,44.79492,41.169434,37.62451,34.079586,11.682129
''' % {'tag1': job1_current_tag, 'tag2': job2_current_tag})

    @pytest.mark.skip
    def test_get_aggregates_two_metrics_fields_different_interval_offsets(self):
        job1_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job1.pk).data.uniq_id
        job2_current_tag = DataMeta.objects.get(key='name', value='current', data__job_id=self.job2.pk).data.uniq_id
        resp = self.client.get(
            '/compare_aggregates/?tag=%s&tag=%s&interval=%s&interval_offset=%s&interval_offset=%s&field=max&field=q90&field=q34234' %
            (job1_current_tag, job2_current_tag, 3 * 10**6, 2 * 10**6, 10**6)
        )
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.content.decode('utf-8'),
            '''timestamp,%(tag1)s_max,%(tag1)s_q90,%(tag2)s_max,%(tag2)s_q90
0,3566.3086,1872.6562,120.84962,64.45313
1000000,2203.125,1689.0625,78.06885,44.069824
2000000,2047.0702,1689.0625,93.7793,57.60498000000001
''' % {'tag1': job1_current_tag, 'tag2': job2_current_tag})
