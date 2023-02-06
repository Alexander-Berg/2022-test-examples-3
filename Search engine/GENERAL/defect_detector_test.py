import unittest

import yt.wrapper as yt


class DefectDetectorTest(unittest.TestCase):
    def test_integration(self):
        task = {
            'screenshot': 'http://storage.mds.yandex.net/get-mturk/223390/91ff0286-8483-4fbf-a10a-12d501c3a942.png'
        }
        yt.config['proxy']['url'] = 'hahn'
        table = yt.create_temp_table()
        yt.write_table(table, [task], format='json')
        yt.run_map(
            'python defect_detector.py',
            local_files='defect_detector.py',
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
        assert yt.row_count(table) == 1
        starts_with_defect = False
        for row in yt.read_table(table, format='json'):
            for k, v in row.iteritems():
                if k.startswith('defect'):
                    starts_with_defect = True
        assert starts_with_defect


if __name__ == '__main__':
    unittest.main()
