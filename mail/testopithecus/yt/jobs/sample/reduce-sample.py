import base64
import errno
import os
from datetime import datetime

import yt.wrapper as yt

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise

if __name__ == "__main__":
    yt.config["proxy"]["url"] = "hahn.yt.yandex.net"
    input_table = "//home/cocainum/amosov-f/dump"
    output_table = '//home/cocainum/amosov-f/mail/run_' + datetime.now().strftime('%Y-%m-%d_%H:%M:%S')

    batch_size = yt.row_count(input_table)
    yt.run_map(
        'bash run-script-mail.sh',
        local_files=["run-script-mail.sh", "mail2-v2147483647-beta-debug.apk", "mail2-v2147483647-beta-debug-androidTest.apk"],
        yt_files=["//home/mail-logs/salavat/avd/android_28_google_apis_x86_64.zip"],
        input_format='json',
        output_format='json',
        memory_limit=4 * yt.common.GB,
        source_table=input_table,
        destination_table=output_table,
        spec={
            'mapper': {
                'layer_paths': [
                    '//home/mail-logs/salavat/porto_layers_x86_64/android_layer.tar.gz',
                    '//home/mail-logs/salavat/porto_layers_x86_64/porto_layer_search_ubuntu_xenial_subagent_yt-2019-07-18-17.42.58.tar.gz',
                ],
                'job_time_limit': 2400000,
                'cpu_limit': 3,
                'memory_limit': 16000000000,
                'memory_reserve_factor': 1,
                "environment": {
                    "YT_ALLOW_HTTP_REQUESTS_TO_YT_FROM_JOB": "1",
                    "YT_FORBID_REQUESTS_FROM_JOB": "0"
                },
            },
            'scheduling_tag_filter': 'kvm',
            'job_count': 11,
            'max_failed_job_count': 1
        }
    )

    dir = output_table.split('/')[-1]
    i = 1
    for row in yt.read_table(output_table, format='json'):
        logs = row['logs']
        mkdir_p(dir + '/logs')
        with open(dir + '/logs/' + str(i) + '.txt', 'w') as f:
            f.write(base64.b64decode(logs))
        screen = row['screen']
        mkdir_p(dir + '/screenshots')
        with open(dir + '/screenshots/' + str(i) + '.png', 'w') as f:
            f.write(base64.b64decode(screen))
        i += 1

