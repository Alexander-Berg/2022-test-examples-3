#!/usr/bin/python
import json
import os
import re
import subprocess

import yaml

PUSH_CLIENT_CONFIGS_FILE = '/etc/default/push-client'
WARN_STATUS = 1

DEVNULL = open(os.devnull, 'wb')

# Opening statbox-push-client startup config file. Getting list of statbox config files.
with open(PUSH_CLIENT_CONFIGS_FILE, 'r') as f:
    line = f.readline()
    match = re.match(r'DAEMON_CONF\s?=\s?"(.*)"', line)
    if match:
        push_client_configs = (re.findall(r'[0-9a-zA-Z-/_.]+', match.group(1)))
    else:
        push_client_configs = None
        print '{}; No push-client config files listed in: {}'.format(WARN_STATUS, PUSH_CLIENT_CONFIGS_FILE)
        exit(1)

status_all_fail = set()
logs_list = set()
on_disk_only = set()
logs_to_check = set()

for push_config in push_client_configs:
    try:
        with open(push_config, 'r') as stream:
            push_config_parsed = yaml.safe_load(stream)
            for push_config_file in push_config_parsed.get('files', []):
                logs_to_check.add(push_config_file['name'])
    except Exception:
        pass
    push_status = subprocess.Popen(['/usr/bin/push-client', '--status', '--json',
                                    '--check:commit-time=7200', '-c', push_config],
                                   stdout=subprocess.PIPE, stderr=DEVNULL)
    json_status = json.load(push_status.stdout)
    for record in json_status:
        # all logs from status output
        logs_list.add(record['name'])
        if record['status'] > 1:
            # all logs with bad status
            status_all_fail.add(record['name'])

# set of missing logs in status output
status_missing = set(logs_to_check) - logs_list
# set of logs with bad status
status_fail = set(logs_to_check) & status_all_fail

# verifying if logfile present on disk
for log in status_missing:
    if os.path.exists(log):
        on_disk_only.add(log)
status_missing = status_missing - on_disk_only

# overall check status
if status_missing or status_fail:
    check_status = '2'
elif on_disk_only:
    check_status = '2'
else:
    check_status = '0'

# final output string
result_text = []
if status_missing:
    result_text.append('Missing logs: {}.'.format(', '.join(status_missing)))
if status_fail:
    result_text.append('Failed logs: {}.'.format(', '.join(status_fail)))
if on_disk_only:
    result_text.append('On disk, but not in status: {}.'.format(', '.join(on_disk_only)))

summary = ' '.join(result_text)

if not summary:
    summary = 'Ok'

result = '{};{}'.format(check_status, summary)

print result
