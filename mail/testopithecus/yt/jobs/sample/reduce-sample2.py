import yt.wrapper as yt
import argparse

default_yt_files = [
                '//home/mail-logs/salavat/avd/android_28_google_apis.zip',
                '//home/mail-logs/salavat/apk/app-debug-androidTest.apk',
                '//home/mail-logs/salavat/apk/app-debug.apk'
            ]


def main():
    parser = argparse.ArgumentParser(description='Extracting suggestions from gboard')
    parser.add_argument('--token', required=True, help='Yt Token')
    parser.add_argument('--cluster', required=False, default='hahn', help='Yt Cluster')
    parser.add_argument('--yt_files', nargs='+', required=False, default=default_yt_files, help='Yt files to bring into job(Android AVD, apk, test apk)')
    parser.add_argument('--single_job_timeout', type=int, required=False, default=1200000, help='timeout for single job in YT in ms, after which job is failed, default = 20min')
    parser.add_argument('--max_failed_jobs_ratio', type=float, required=False, default=0.5, help='Ratio of allowed to fail jobs, default = 0.5')
    parser.add_argument('--batch_size', type=int, required=False, default=10000, help='Number of rows to process in one map operation')
    parser.add_argument('--jobs_count', type=int, required=False, default=90, help='Number of jobs to start per batch in one map operation')
    parser.add_argument('--input_path', type=str, required=True, help='Input table')
    parser.add_argument('--output_path', type=str, required=True, help='Output table with suggests')
    args = parser.parse_args()
    yt.config["token"] = args.token
    yt.config["proxy"]["url"] = "{}.yt.yandex.net".format(args.cluster)
    input_table_full = yt.TablePath(args.input_path)
    output_table = yt.TablePath(args.output_path, append=True)
    rows_to_process_count = args.batch_size
    jobs_count = args.jobs_count
    max_failed_jobs_ratio = args.max_failed_jobs_ratio
    job_timeout_limit_in_ms = args.single_job_timeout
    max_failed_jobs_count = int(jobs_count * max_failed_jobs_ratio)

    cur_batch_start_index = 0
    total_rows_count = yt.row_count(input_table_full)
    while (cur_batch_start_index + rows_to_process_count <= total_rows_count):
        batch_step = min(rows_to_process_count, total_rows_count - cur_batch_start_index)
        input_table = yt.TablePath(args.input_path,
                                   start_index=cur_batch_start_index,
                                   end_index=cur_batch_start_index + batch_step)
        try:
            yt.run_map(
                'bash run-script2.sh',
                local_files=["run-script2.sh", "parse_hierarchy_dump.py"],
                yt_files=args.yt_files,
                input_format='json',
                output_format=yt.JsonFormat(attributes={"encode_utf8": False}),
                memory_limit=8 * yt.common.GB,
                source_table=input_table,
                destination_table=output_table,
                spec={
                    'mapper': {
                        'layer_paths': [
                            '//home/mail-logs/salavat/porto_layers/android_layer.tar.gz',
                            '//home/mail-logs/salavat/porto_layers/porto_layer_search_ubuntu_xenial_subagent_yt-2019-07-18-17.42.58.tar.gz',
                        ],
                        'job_time_limit': job_timeout_limit_in_ms,
                        'cpu_limit': 3,
                        'memory_limit': 16000000000,
                        'memory_reserve_factor': 1,
                        "environment": {
                            "YT_ALLOW_HTTP_REQUESTS_TO_YT_FROM_JOB": "1",
                            "YT_FORBID_REQUESTS_FROM_JOB": "0"
                        },
                    },
                    'scheduling_tag_filter': 'kvm',
                    'job_count': jobs_count,
                    'max_failed_job_count': max_failed_jobs_count
                }
            )
        except:
            print("failed to get suggest for rows {}-{}".format(cur_batch_start_index, cur_batch_start_index + batch_step))
            pass
        # increment current rows index
        cur_batch_start_index += batch_step

if __name__ == "__main__":
    main()