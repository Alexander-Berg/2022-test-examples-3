#!/usr/bin/python
# -*- coding: utf-8 -*-

import requests
from urlparse import urljoin
# import module snippets
from ansible.module_utils.basic import *

class Lunapark(object):
    def __init__(self, url='https://lunapark.yandex-team.ru/', project='DISKBACK'):
        self._base_url = url
        self.project = project

    def lunapark_url(self, task_id):
        return "%s%s" % (self._base_url, task_id)

    def make_request(self, path, params=None, method='get', raise_for_status=True):
        url = urljoin(self._base_url, path)
        if method.lower() == 'get':
            f = requests.get
        elif method.lower() == 'post':
            f = requests.post
        else:
            raise NotImplementedError

        r = f(url, params=params)
        if raise_for_status:
            r.raise_for_status()
        return r

    def job_for_task(self, task_id):
        path = '/api/task/%s/joblist.json?full=true' % task_id
        r = self.make_request(path)
        return r.json()

    def job_sla(self, job_id):
        path = '/api/job/%s/sla.json' % job_id
        r = self.make_request(path)
        return r.json()

    @property
    def components(self):
        ret = {}
        path = '/api/regress/%s/componentlist.json' % self.project
        r = self.make_request(path)
        for item in r.json():
            ret[item['n']] = item['name']
        return ret

    def task_sla(self, task_id, version):
        r = {
            'passed': [],
            'failed': [],
        }
        components = self.components
        for job in self.job_for_task(task_id):
            if job['ver'] != version:
                continue
            component_name = components[job['component']]
            sla = self.job_sla(job['n'])
            resulution = 'passed' if sla[0]['resolution'] else 'failed'
            r[resulution].append(component_name)
        return r


def main():
    module = AnsibleModule(
        argument_spec=dict(
            issue=dict(type="str", default=None),
            version=dict(type="str", default=None)
        ),
        supports_check_mode=True
    )

    issue = module.params.get("issue")
    version = module.params.get("version")

    if not all((issue, version)):
        module.fail_json(msg="No valid options were provided.",
                         result=False)

    lunapark = Lunapark()
    task_sla = lunapark.task_sla(issue, version)
    result = {
        'failed': len(task_sla.get('failed')),
        'passed': len(task_sla.get('passed')),
        'url': lunapark.lunapark_url(issue),
    }

    result_args = dict(
        result=True,
        info=result
    )
    module.exit_json(**result_args)


if __name__ == '__main__':
    main()
