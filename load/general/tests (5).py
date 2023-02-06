# coding=utf-8
import pytest
from common.tests import CommonTestCase
from common.models import Data, Job
import logging
import json

from job_page.layout import render_layout, _get_sections
from settings.base import BASE_DIR


class TestJobPageTests(CommonTestCase):

    def test_get_layout(self):
        resp = self.client.get('/get_layout/?job=16')
        self.assertIsInstance(json.loads(resp.content.decode('utf-8'))['sections'], list)
        self.assertIsInstance(json.loads(resp.content.decode('utf-8'))['sections'][0]['widgets'], list)

    @pytest.mark.skip
    def test_get_aggregates_valid_tag(self):
        # правильный тэг
        resp = self.client.get('/get_aggregates/?tag=a620a7da3a934593abfb923a62343655')  # тэг метрики тока
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content.decode('utf-8').split('\n')), 55)

    def test_get_aggregates_no_tag(self):
        resp = self.client.get('/get_aggregates/')
        self.assertEqual(resp.status_code, 400)

    def test_get_aggregates_notexistent_tag(self):
        resp = self.client.get('/get_aggregates/?tag=blablabla')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    # TODO: проверить записывается ли в талибцу aggregates
    def test_get_summary_valid_tag(self):
        # правильный тэг
        resp = self.client.get('/get_summary/?tag=a620a7da3a934593abfb923a62343655&tag=04ac1a233bc94be589532872193e74f3')  # тэг метрики тока
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content.decode('utf-8').strip().split('\n')), 3)
        self.assertEqual(len(resp.content.decode('utf-8').split('\n')[1].split(',')), 15)

    def test_get_summary_no_tag(self):
        # нет тэга
        resp = self.client.get('/get_summary/')
        self.assertEqual(resp.status_code, 400)

    def test_get_summary_notexistent_tag(self):
        # несуществующий тэг
        resp = self.client.get('/get_summary/?tag=blablabla')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_get_metric_valid_tag(self):
        # правильный тэг
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content), 575)

    @pytest.mark.skip
    def test_get_metric_several_tags(self):
        # несколько тегов
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3&tag=3fa3dd11075d42e3add1e110b70eb185')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content), 664)

    @pytest.mark.skip
    def test_get_metric_normalize(self):
        # normalize
        resp = self.client.get('/get_metric/?tag=a620a7da3a934593abfb923a62343655&normalize=1')  # тэг метрики тока
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(
            any([bool(len(resp.content.decode('utf-8').split('\n')) == l) for l in range(1000 - 10, 1000 + 10)]))
        self.assertEqual(len(resp.content.decode('utf-8').split('\n')), 1004)
        self.assertTrue(all([float(v.split(',')[1]) <= 1 for v in resp.content.decode('utf-8').split('\n')[1:] if v]))

    @pytest.mark.skip
    def test_get_metric_500_dots(self):
        # 500 dots
        resp = self.client.get('/get_metric/?tag=a620a7da3a934593abfb923a62343655&dots=500')  # тэг метрики тока
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(any([bool(len(resp.content.decode('utf-8').split('\n')) == l) for l in range(500-10, 500+10)]))
        self.assertEqual(len(resp.content.decode('utf-8').split('\n')), 511)

    @pytest.mark.skip
    def test_get_metric_start(self):
        # start
        data_object = Data.objects.get(uniq_id='04ac1a233bc94be589532872193e74f3')
        start = 5000000 + data_object.job.test_start + data_object.offset
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3&start=%s' % start)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content), 480)

    @pytest.mark.skip
    def test_get_metric_end(self):
        # end
        data_object = Data.objects.get(uniq_id='04ac1a233bc94be589532872193e74f3')
        end = 23000000 + data_object.job.test_start + data_object.offset
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3&end=%s' % end)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content), 480)

    @pytest.mark.skip
    def test_get_metric_start_end(self):
        # start end
        data_object = Data.objects.get(uniq_id='04ac1a233bc94be589532872193e74f3')
        start = 5000000 + data_object.job.test_start + data_object.offset
        end = 23000000 + data_object.job.test_start + data_object.offset
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3&start=%s&end=%s' % (start, end))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.content), 385)

    @pytest.mark.skip
    def test_get_metric_start_end_out_of_interval(self):
        # start end вне интервала
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3&start=%s&end=%s' % (0, 10))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'timestamp')

    def test_get_metric_start_end_not_digital(self):
        # start end not digital
        resp = self.client.get('/get_metric/?tag=04ac1a233bc94be589532872193e74f3&start=%s&end=%s' % ('wdfwf', 23234))
        self.assertEqual(resp.status_code, 400)

    # TODO: проверить сами значения.

    def test_get_metric_no_tag(self):
        # нет тэга
        resp = self.client.get('/get_metric/')
        self.assertEqual(resp.status_code, 400)

    def test_get_metric_not_existent_tag(self):
        # несуществующий тэг
        resp = self.client.get('/get_metric/?tag=blablabla')
        self.assertEqual(resp.status_code, 400)


class GetEventsTests(CommonTestCase):

    # коды ответов

    def test_no_tag_400(self):
        # нет тэга
        resp = self.client.get('/get_events/')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_not_existent_tag_400(self):
        # несуществующий тэг
        resp = self.client.get('/get_events/?tag=blablabla')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_metrics_tag_instead_of_event_400(self):
        # тэг метрики а не евентов
        resp = self.client.get('/get_events/?tag=04ac1a233bc94be589532872193e74f3')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_valid_tag_200(self):
        # правильный тэг
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_valid_grep_200(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&grep=torch')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_valid_ts_200(self):
        # ts
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236910171297')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_valid_index_from_200(self):
        # index
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&index_from=14&index_to=19')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_interval_200(self):
        # interval
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&interval_offset=5000&interval=500000')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_index_from_and_interval_and_ts_200(self):
        # interval index ts
        resp = self.client.get(
            '/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&index_from=14&length=20&ts=1520236910171297&interval_offset=5000&interval=5000000')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_interval_ts_200(self):
        # interval ts
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236910171297&interval_offset=5000&interval=5000000')
        self.assertEqual(resp.status_code, 200)

    # теги
    @pytest.mark.skip
    def test_content_tag_only(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680')
        lines = self.get_data_from_resp(resp)

        self.assertEqual(len(lines), 76)
        for line in lines[1:]:
            self.assertTrue(line.startswith('afca'))

    @pytest.mark.skip
    def test_content_two_tags(self):
        resp = self.client.get('/get_events/?tag=6abfd1e53d174f6ba29391c086166ef6&tag=0e10d29ba93f4fb2aa888be9b26fe680')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 151)

    # ts
    @pytest.mark.skip
    def test_content_ts_without_length(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236910171297')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 2)
        self.assertEqual(lines[0], 'tag,ts,value,data_index')
        self.assertTrue(lines[1], '0e10d29ba93f4fb2aa888be9b26fe680,1520236909853297,"onTorchStatusChangedLocked: Torch status changed for cameraId=0, newStatus=1",24')

    @pytest.mark.skip
    def test_content_two_tags_ts_without_length(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236910171297')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 2)
        self.assertEqual(lines[0], 'tag,ts,value,data_index')
        self.assertTrue(lines[1], '0e10d29ba93f4fb2aa888be9b26fe680,1520236909853297,"onTorchStatusChangedLocked: Torch status changed for cameraId=0, newStatus=1",24')

    @pytest.mark.skip
    def test_content_two_tags_ts_with_length(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&tag=6abfd1e53d174f6ba29391c086166ef6&ts=1520236922265297&length=6')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 8)
        self.assertEqual(lines[0], 'tag,ts,value,data_index')
        self.assertTrue(lines[1], '0e10d29ba93f4fb2aa888be9b26fe680,1520236909853297,"onTorchStatusChangedLocked: Torch status changed for cameraId=0, newStatus=1",24')

    @pytest.mark.skip
    def test_ts_length(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236910171297&length=6')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 5)
        self.assertTrue('1520236910171297' in lines[4])

    @pytest.mark.skip
    def test_not_existent_ts(self):
        #  Случай, когда в запросе передается таймстемп, которого нет в базе
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236910171298&length=6')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 5)
        self.assertTrue('1520236910171297' in lines[3])

    @pytest.mark.skip
    def test_first_ts_from_tag(self):
        # https://st.yandex-team.ru/LUNA-355
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&ts=1520236904269297&length=7')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 5)

    @pytest.mark.skip
    def test_ts_beginning_index_with_several_tags(self):
        # Случай, когда в датафрейме до таймстемпа записей меньше чем length/2
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&tag=6abfd1e53d174f6ba29391c086166ef6&ts=1516366213855030&length=4')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 4)

    @pytest.mark.skip
    def test_ts_middle_index_with_several_tags(self):
        # Случай, когда запрошенный интервал попадает  на стык двух датафреймов для каждого тега
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&tag=6abfd1e53d174f6ba29391c086166ef6&ts=1516366234624030&length=10')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 7)

    @pytest.mark.skip
    def test_last_ts_from_tag(self):
        # Случай, когда в датафрейме до таймстемпа записей меньше чем length/2
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&tag=6abfd1e53d174f6ba29391c086166ef6&ts=1520236925051297&length=5')
        lines = self.get_data_from_resp(resp)
        self.assertEqual(len(lines), 4)

    # grep
    @pytest.mark.skip
    def test_content_grep(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&grep=torch')
        lines = self.get_data_from_resp(resp)
        found_strings = [line for line in lines if 'torch' in line.lower()]
        self.assertEqual(len(lines)-1, len(found_strings))

    @pytest.mark.skip
    def test_notexistent_grep(self):
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&grep=Obama')
        lines = self.get_data_from_resp(resp)
        found_strings = [line for line in lines if 'Obama' in line.lower()]
        self.assertEqual(len(lines)-1, len(found_strings))

    # interval_offset
    @pytest.mark.skip
    def test_content_interval(self):
        # interval
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&interval_offset=5000&interval=5000000')
        lines = self.get_data_from_resp(resp)
        self.assertTrue(lines[1].startswith('0e10d29ba93f4fb2aa888be9b26fe680,1520236904282297,'))
        self.assertTrue(lines[-1].endswith('[volta] 17915785969577 sync 4 rise,18'))

    # index_from
    @pytest.mark.skip
    def test_content_index_from_0(self):
        # interval_from is 0
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&index_from=0&length=5')
        lines = self.get_data_from_resp(resp)
        # response contains 5 strings plus header
        self.assertEqual(len(lines), 6)
        self.assertEqual(
            lines[1],
            '0e10d29ba93f4fb2aa888be9b26fe680,1520236904269297,Connecting to camera service,0'
        )
        self.assertEqual(
            lines[5],
            '0e10d29ba93f4fb2aa888be9b26fe680,1520236905996297,"onTorchStatusChangedLocked: Torch status changed for cameraId=0, newStatus=1",4'
        )

    @pytest.mark.skip
    def test_content_index_from_5(self):
        # interval_from is 0
        resp = self.client.get('/get_events/?tag=0e10d29ba93f4fb2aa888be9b26fe680&index_from=5&length=5')
        lines = self.get_data_from_resp(resp)
        # response contains 5 strings plus header
        self.assertEqual(len(lines), 6)
        self.assertEqual(
            lines[1],
            '0e10d29ba93f4fb2aa888be9b26fe680,1520236905999297,[volta] 17912572030481 sync 0 fall,5'
        )
        self.assertEqual(
            lines[5],
            '0e10d29ba93f4fb2aa888be9b26fe680,1520236907241297,[volta] 17913814458894 sync 1 fall,9'
        )


class TestGetSummary(CommonTestCase):
    maxDiff = None

    @pytest.mark.skip
    def test_aggregates_with_histograms(self):
        resp = self.client.get('/get_summary/?tag=378fd3481e3f4ea3a31e8a2fc577fb07')
        content = resp.content.decode('utf-8')
        with open(BASE_DIR + '/job_page/files/summary_378fd3481e3f4ea3a31e8a2fc577fb07.csv') as ffile:
            expected = ffile.read()
        self.assertEqual(content, expected)
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_aggregates_without_histograms(self):
        resp = self.client.get('/get_summary/?tag=50cac18ca1a34b83a77c54c2943945a0')
        content = resp.content.decode('utf-8')
        with open(BASE_DIR + '/job_page/files/summary_50cac18ca1a34b83a77c54c2943945a0.csv') as ffile:
            expected = ffile.read()
        self.assertEqual(content, expected)

    @pytest.mark.skip
    def test_histograms(self):
        resp = self.client.get('/get_summary/?tag=b61e7705cffd4eaeb5603f7f88e3fa1e')
        content = resp.content.decode('utf-8')
        expected = 'tag,0,200\nb61e7705cffd4eaeb5603f7f88e3fa1e,6060,40771\n'
        self.assertEqual(content, expected)

    @pytest.mark.skip
    def test_raw_metrics(self):
        self.maxDiff = None
        resp = self.client.get('/get_summary/?tag=e0367e571e304924a26753c587fd6924')
        content = resp.content.decode('utf-8')
        with open(BASE_DIR + '/job_page/files/summary_e0367e571e304924a26753c587fd6924.csv') as ffile:
            expected = ffile.read()
        self.assertRegex(content, expected)

    @pytest.mark.skip
    def test_aggregates_several_tags(self):
        resp = self.client.get(
            '/get_summary/?tag=84e2b9c4f7b241e2b795d54bdfc5c4aa&tag=7d2e3573f42946659139a227a33732cd')
        content = resp.content.decode('utf-8')
        with open(BASE_DIR + '/job_page/files/summary_two_tags.csv') as ffile:
            expected = ffile.read()
        self.assertEqual(content, expected)
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_aggregates_with_start(self):
        resp = self.client.get('/get_summary/?tag=84e2b9c4f7b241e2b795d54bdfc5c4aa&from=3118707447499367')
        content = resp.content.decode('utf-8')
        with open(BASE_DIR + '/job_page/files/summary_start.csv') as ffile:
            expected = ffile.read()
        self.assertRegex(content, expected)

    @pytest.mark.skip
    def test_aggregates_with_start_and_end(self):
        resp = self.client.get('/get_summary/?tag=84e2b9c4f7b241e2b795d54bdfc5c4aa&from=3118707447499367&to=3118707504374367')
        content = resp.content.decode('utf-8')
        with open(BASE_DIR + '/job_page/files/summary_start_end.csv') as ffile:
            expected = ffile.read()
        self.assertRegex(content, expected)


class JobLayoutTests(CommonTestCase):
    fixtures = ['job4664.json', 'dataset4867.json']

    def test_attributes(self):
        layout = render_layout(Job.objects.get(pk=4664), {})
        assert layout['attributes']['status'] == 'finished'

    def test_actual_rps_in_widget(self):
        tank_section_widgets = _get_sections(Job.objects.get(pk=4867))[2]['widgets']
        for widget in tank_section_widgets:
            for view in widget['views']:
                if view['type'] == 'plot':
                    self.assertEqual(
                        view['line'],
                        {'left': [], "right": [{'name': 'actual_rps', 'tag': 21377, 'label': 'rps'}]}
                    )
