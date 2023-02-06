
import yaml

from admin.monitors.bazinga_job_count.check_jobs_counts import TaskChecker


def read_sibling_file(filename):
    with open(filename, 'r') as f:
        return f.read()


def read_checkers(filename):
    checks = yaml.load(read_sibling_file(filename))
    return [TaskChecker.from_dict(task_name, checks) for task_name, checks in checks.iteritems()]


t = read_checkers('/Users/dk666/arcadia/disk/admin/monitors/bazinga_job_count/java_checks.yaml')

print t
