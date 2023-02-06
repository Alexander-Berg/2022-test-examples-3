# coding=utf-8
import json
import os
import subprocess
import unittest
import urllib
import uuid
import xml.etree.ElementTree
from subprocess import PIPE

import cv2
import numpy as np
import requests
import yt.wrapper as yt
from screenqual.core.screenshot import Screenshot
from screenqual.filter.broken_images_detector import BrokenImagesAnalyser
from screenqual.filter.general_detector import GeneralDetector
from screenqual.filter.min_colors_checker import MinColorsChecker
from screenqual.filter.min_size_checker import MinSizeChecker
from screenqual.filter.scrollbars_detector import ScreenshotAnalyser
from screenqual.filter.scrollbars_detector import ScrollBarAnalyser
from screenqual.filter.clipped_document_detector.clipped_document_detector import ClippedDocumentDetector


class NovichokTest(unittest.TestCase):
    base_detectors = [
        MinSizeChecker(), MinColorsChecker(), ScrollBarAnalyser()
    ]

    def test_google_serp_should_contain_all_images(self):
        self._assert_no_anomaly([BrokenImagesAnalyser()], {
            'file': 'google_images.html',
            'viewportWidth': 360,
            'minViewportHeight': 640
        })

    def test_infinite_scroll_should_stop_on_8192_height(self):
        class MaxHeightDetector(ScreenshotAnalyser):
            def execute(self, screenshot):
                h = screenshot.image.shape[0]
                return h < 8192

        self._assert_no_anomaly([MaxHeightDetector()], {
            'url': 'https://mobile.twitter.com/realDonaldTrump',
            'viewportWidth': 360,
            'minViewportHeight': 640
        })

    def test_infinite_resource_loading_of_maps_wizard_should_stop(self):
        self._assert_no_anomaly([], {
            'file': 'maps.html'
        })

    def test_can_screenshot_many_urls(self):
        self._assert_no_anomaly([], {
            'url': 'https://www.google.com',
            'proxy': 'ru'
        }, {
            'url': 'https://www.google.com',
            'proxy': 'tr'
        })

    def test_can_screenshot_many_documents_with_padding(self):
        self._assert_no_anomaly([ClippedDocumentDetector()], {
            'url': 'https://yandex.ru/search?text=pizza',
            'selector': '[class*=serp-item][data-cid]',
            'padding': {'left': 28, 'right': 12, 'top': 12, 'bottom': 12}
        })

    def test_zoom_without_padding(self):
        zoom = 3
        screenshots = self._get_screenshots(
            {
                'url': 'https://yandex.ru/search/?text=kitten',
                'selector': "[data-cid=\"2\"]",
            },
            {
                'url': 'https://yandex.ru/search/?text=kitten',
                'selector': "[data-cid=\"2\"]",
                'zoom': zoom
            },
        )
        self._compare_zoomed(screenshots, zoom)

    def test_zoom(self):
        zoom = 3
        padding = {"left": 5, "top": 5, "right": 5, "bottom": 5}
        screenshots = self._get_screenshots(
            {
                'url': 'https://yandex.ru/search/?text=kitten',
                'selector': "[data-cid=\"2\"]",
                "padding": padding
            },
            {
                'url': 'https://yandex.ru/search/?text=kitten',
                'selector': "[data-cid=\"2\"]",
                "padding": padding,
                'zoom': zoom
            },
        )
        self._compare_zoomed(screenshots, zoom)
        self._assert_no_anomaly_on_existing_screenshots([ClippedDocumentDetector()], screenshots)

    def _compare_zoomed(self, screenshots, zoom):
        normal_sized, _, _ = screenshots[0]
        super_sized, _, _ = screenshots[1]
        n_h, n_w = normal_sized.image.shape[:2]
        s_h, s_w = super_sized.image.shape[:2]
        assert n_h * zoom == s_h
        assert n_w * zoom == s_w

    def test_no_oom_if_many_urls_per_job(self):
        tasks = [{'url': 'https://yandex.ru'} for _ in range(0, 5)]

        yt.config['proxy']['url'] = 'hahn'

        table = yt.create_temp_table()
        yt.write_table(table, tasks, format='json', force_create=True)

        yt.run_map(
            'node novichok.js yt_config.json',
            local_files=['novichok.js', 'yt_config.json'],
            input_format='json',
            output_format='json',
            source_table=table,
            destination_table=table,
            spec={
                'mapper': {
                    'layer_paths': ['//home/cocainum/novichok/layers/yt_puppeteer.tar.gz']
                },
                'scheduling_tag_filter': 'porto',
                'job_count': 1,
                'max_failed_job_count': 1
            }
        )

    def test_full_integration(self):
        self._assert_no_anomaly_on_yt([BrokenImagesAnalyser()], {
            'url': 'https://storage.mds.yandex.net/get-sbs/750943/af9f163c-fe84-4667-b986-a13ad8c0b77c?content_type=text/html;charset=utf-8',
            'viewportWidth': 360,
            'minViewportHeight': 640
        })


    def _get_screenshots(self, *tasks):
        result = []
        for task in tasks:
            self._upload_file_if_present(task)
        p = subprocess.Popen(["node", "novichok.js", "local_config.json"], stdout=PIPE, stdin=PIPE, stderr=PIPE)
        stdin = '\n'.join(map(lambda task: json.dumps(task), tasks))
        print('===Novichok STDIN===\n' + stdin)
        print()
        stdout, stderr = p.communicate(input=stdin)
        print('===Novichok STDERR===\n' + stderr)
        print('===Novichok STDOUT===\n' + stdout)
        screenshot_lines = stdout.strip().split('\n')
        assert len(screenshot_lines) >= len(tasks)
        for line in screenshot_lines:
            processed_task = json.loads(line)
            url = processed_task['url']
            screenshot_url = processed_task['screenshot']
            print('Made screenshot {} for source {}'.format(screenshot_url, url))

            resp = urllib.urlopen(screenshot_url)
            arr = np.asarray(bytearray(resp.read()), dtype=np.uint8)
            img = cv2.imdecode(arr, -1)
            result.append((Screenshot(img), screenshot_url, url))
        return result

    def _assert_no_anomaly(self, custom_detectors, *tasks):
        self._assert_no_anomaly_on_existing_screenshots(custom_detectors, self._get_screenshots(*tasks))

    def _assert_no_anomaly_on_existing_screenshots(self, custom_detectors, screenshots):
        for screenshot, screenshot_url, url in screenshots:
            for detector in self.base_detectors + custom_detectors:
                name = self._detector_name(detector)
                anomaly = detector.execute(screenshot)
                msg = '{} detected anomaly at screenshot {} for source {}'.format(name, screenshot_url, url)
                self.assertFalse(anomaly, msg)

    def _assert_no_anomaly_on_yt(self, custom_detectors, *tasks):
        for task in tasks:
            self._upload_file_if_present(task)

        yt.config['proxy']['url'] = 'hahn'

        table = yt.create_temp_table()
        yt.write_table(table, tasks, format='json', force_create=True)

        yt.run_map(
            'node novichok.js yt_config.json',
            local_files=['novichok.js', 'yt_config.json'],
            input_format='json',
            output_format='json',
            source_table=table,
            destination_table=table,
            spec={
                'mapper': {
                    'layer_paths': ['//home/cocainum/novichok/layers/yt_puppeteer.tar.gz']
                },
                'scheduling_tag_filter': 'porto',
                'max_failed_job_count': 1
            }
        )
        yt.run_map(
            'python defect_detector.py',
            local_files='../defects/defect_detector.py',
            input_format='json',
            output_format='json',
            source_table=table,
            destination_table=table,
            spec={
                'mapper': {
                    'layer_paths': ['//home/cocainum/novichok/layers/yt_opencv_python.tar.gz']
                },
                'scheduling_tag_filter': 'porto',
                'max_failed_job_count': 1
            }
        )

        assert yt.row_count(table) == len(tasks)

        for row in yt.read_table(table, format='json'):
            url = row['url']
            screenshot_url = row['screenshot']
            for detector in self.base_detectors + custom_detectors:
                name = self._detector_name(detector)
                column = 'defect' + name
                assert column in row
                anomaly = row[column]
                msg = '{} detected anomaly at screenshot {} for source {}'.format(name, screenshot_url, url)
                self.assertFalse(anomaly, msg)

    def _detector_name(self, detector):
        return detector.__class__.__name__

    def _upload_file_if_present(self, task):
        if 'url' in task:
            return
        if not 'file' in task:
            raise Exception('"url" or "file" required!')
        cur_dir = os.path.dirname(os.path.realpath(__file__))
        file =  '{}/source/{}'.format(cur_dir, task['file'])
        screenshot_url = self._upload_to_mds(str(uuid.uuid4()) + '.html', open(file).read())
        task['url'] = screenshot_url

    def _upload_to_mds(self, name, data):
        cfg = json.load(open('local_config.json'))
        url = 'http://storage-int.mdst.yandex.net:1111/upload-{}/{}?expire=30d'.format(cfg['mdsNamespace'], name)
        response = requests.post(url, data, headers={
            "Authorization": cfg['mdsToken']
        })
        key = self._parse_mds_answer(response.text)
        return 'http://storage.mdst.yandex.net/get-{}/{}'.format(cfg['mdsNamespace'], key)

    def _parse_mds_answer(self, mds_answer):
        root = xml.etree.ElementTree.fromstring(mds_answer)
        try:
            return root.attrib["key"]
        except:
            return root.find("key").text

if __name__ == '__main__':
    unittest.main()
