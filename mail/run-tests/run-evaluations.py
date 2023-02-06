import sys
import yt.wrapper as yt

if __name__ == "__main__":
    date = sys.argv[1].split('=')[1][0:11]
    yt.config["proxy"]["url"] = "hahn.yt.yandex.net"
    input_table = "//home/mail-logs/m3/input/daily/scenarios/compose_and_send/d-user-channel-scenario_id/testopithecus/sessions/" + date
    output_table = "//home/mail-logs/m3/input/daily/scenarios/compose_and_send/d-user-channel-scenario_id/testopithecus/scenarios/" + date

    batch_size = yt.row_count(input_table)

    yt.run_map(
        'bash run-evaluations.sh',
        local_files=["testopithecus.zip", "run-evaluations.sh"],
        input_format='json',
        output_format='json',
        memory_limit=8 * yt.common.GB,
        source_table=input_table,
        destination_table=output_table,
        spec={
            'mapper': {
                'layer_paths': [
                    '//home/cocainum/xrater/porto/node-packages-19-12-5/node_layer.tar.gz',
                    '//home/cocainum/xrater/porto/node-packages-19-12-5/porto_layer_search_ubuntu_trusty_subagent_yt-2019-07-18-17.42.49.tar.gz',
                ],
                'job_time_limit': 30 * 60 * 1000
            },
            'scheduling_tag_filter': 'porto',
            'job_count': 4,
            'max_failed_job_count': 1
        }
    )
