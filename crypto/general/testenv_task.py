import yaml

from testenv.core.declarative import job_creator


def parse_task(str, task_type=None):
    tasks = yaml.safe_load(str)

    jobs = {}

    for name, task in tasks.iteritems():
        task["name"] = name
        jobs.update({job.yaml_job["name"]: job.yaml_job for job in job_creator.create_jobs(task, None)})

    if task_type is None:
        if len(jobs) == 1:
            return next(iter(jobs.values()))
        else:
            raise Exception("Multiple tasks in file but no task type was explicitly specified")

    return jobs[task_type]


def serialize_task(task):
    return yaml.dump({'filter': task}, default_flow_style=False)


def get_filter(task):
    return task['filter']


def is_ya_package(task):
    return task["check_task"]["name"] == "YA_PACKAGE"


def get_package(task):
    return task["check_task"]["params"]["ctx"]["packages"]
