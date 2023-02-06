import os
import json

from jinja2 import Template

PASSPORT_RESPONSES = {}
DEFAULT_INFO = None

cur_file_path = os.path.dirname(os.path.abspath(__file__))
responses_path = os.path.join(cur_file_path, 'responses/')
responses_files = [os.path.join(responses_path, f) for f in os.listdir(responses_path) if f.endswith(".json")]


def get_response_template_from_file(resp_file):
    with open(resp_file, 'r') as file:
        return Template(file.read().decode('utf-8'))


def get_response_by_uid(uid):
    passport_response = PASSPORT_RESPONSES.get(uid, DEFAULT_INFO)
    return passport_response


for responses_file in responses_files:
    try:
        response_template = get_response_template_from_file(responses_file)
        uid = json.loads(response_template.render())['users'][0]['uid']['value']
    except Exception as e:
        print ('error while parsing file %s: %s' % (responses_file, e))
        raise

    if uid.isdigit():
        PASSPORT_RESPONSES[uid] = response_template
        if responses_file.endswith("default.json"):
            DEFAULT_INFO = PASSPORT_RESPONSES[uid]
