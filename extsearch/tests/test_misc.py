import pytest
from misc import process_json
import models
import json


def test_process_json():
    print()
    args = models.TestArgs()
    args.add('data', 'data/sample_0.json')
    args.add('processed_data', 'data/sample_0_processed.json')
    args.add('body', '''
import random
def main(data):
    return [x.upper() for x in data]
''')
    process_json.main(args.to_argv())
    args.ans_data = 'data/sample_0_ans.json'
    with open(args.processed_data) as processed_data_f, open(args.ans_data) as ans_data_f:
        processed_data = json.load(processed_data_f)
        ans_data = json.load(ans_data_f)
    assert processed_data == ans_data
