#!/usr/bin/python
import sys
import os
import collections
import time
import re

saved_log_filename = sys.argv[1]
keep_time = int(sys.argv[2])

now = time.time()
new_failed_tasks = []
failed_tasks = []


def check_timestamp(logline, delta):
    timestamp_re = re.match('.*\sunixtime=(?P<timestamp>\d+)\s.*', logline)
    time_threshold = time.time() - delta
    if timestamp_re:
        timestamp = int(timestamp_re.group('timestamp'))
        return timestamp > time_threshold
    else:
        return True

for line in sys.stdin:
    if 'handle_billing_' in line and 'task_status: FAIL' in line:
            new_failed_tasks.append(line.strip())

# reading previously found log records
if os.path.exists(saved_log_filename):
    with open(saved_log_filename, 'r') as saved_log_file:
        failed_tasks.extend([line.strip() for line in saved_log_file])

for task in new_failed_tasks:
    if not task in failed_tasks:
        failed_tasks.append(task)

# deleting old log records from failed_tasks list
failed_tasks = filter(lambda x: check_timestamp(x, keep_time), failed_tasks)

# saving filtered log records and setting error code, or just truncating file
with open(saved_log_filename, 'w') as saved_log_file:
    if failed_tasks:
        failed_tasks_data = '\n'.join(failed_tasks) + '\n'
        saved_log_file.write(failed_tasks_data)
        error_code = 2
    else:
        error_code = 0

print('{};{} failed billing tasks found'.format(error_code, len(failed_tasks)))
