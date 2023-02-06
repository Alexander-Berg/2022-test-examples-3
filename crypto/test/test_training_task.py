import yatest.common

from crypta.lib.python.yt.test_helpers import tests


def test_training_new_model(local_yt, local_yt_and_yql_env, config_file, metrics_file, resulting_segments_table,
                            segments_info_table, cat_features_tables, raw_sample_table, puid_matching_table,
                            indevice_yandexuid_table, yandexuid_user_data_table, catboost_features_table, yandexuid_crypta_id_table):
    local_yt_and_yql_env.update({'CRYPTA_ENVIRONMENT': 'local_testing'})
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path('crypta/profile/services/train_custom_model/bin/crypta-ml-train-custom-model'),
        args=[
            '--config', config_file,
        ],
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (raw_sample_table, [tests.TableIsNotChanged()]),
            (puid_matching_table, [tests.TableIsNotChanged()]),
            (indevice_yandexuid_table, [tests.TableIsNotChanged()]),
            (yandexuid_user_data_table, [tests.TableIsNotChanged()]),
            (segments_info_table, [tests.TableIsNotChanged()]),
            (catboost_features_table, [tests.TableIsNotChanged()]),
            (yandexuid_crypta_id_table, [tests.TableIsNotChanged()]),
        ] + [(table, [tests.TableIsNotChanged()]) for table in cat_features_tables],
        output_tables=[
            (metrics_file, [tests.Diff()]),
            (resulting_segments_table, [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
