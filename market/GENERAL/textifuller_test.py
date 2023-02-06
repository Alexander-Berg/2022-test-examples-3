import json
import unittest
import os

SNAPSHOTS_FOLDER = 'snapshots'
OUT_FOLDER = 'out'

FILE_NAME = 'result.json'

SNAPSHOT = os.path.join(SNAPSHOTS_FOLDER, FILE_NAME)
OUTPUT = os.path.join(OUT_FOLDER, FILE_NAME)


class TestOutput(unittest.TestCase):
    def test_should_process_correctly(self):
        skipped = 0
        for row in self.output:
            sn = f'{row.get("model_id")}'
            if not self.snapshot.get(sn):
                skipped += 1
                continue

            self.assertEqual(row.get('title'), self.snapshot[sn])

        print('Skipped: ', skipped)

    def setUp(self) -> None:
        with open(SNAPSHOT, 'r') as snapshot_f:
            self.snapshot = json.load(snapshot_f)

        with open(OUTPUT, 'r') as output_f:
            self.output = json.loads('[{0}]'.format(','.join(output_f.readlines())))


if __name__ == '__main__':
    unittest.main()
