#!/usr/bin/env python
# -*- coding: utf-8 -*-

import luigi

from crypta.profile.utils.config import production_config, testing_config
from crypta.profile.utils.luigi_utils import YtDailyRewritableTarget, ExternalInput, BaseYtTask


yuid_with_all_sample_query = """
INSERT INTO `{output_table}` WITH TRUNCATE
SELECT
    yandexuid,
    main_region,
    main_region_country,
    ip_activity_type
FROM `{input_table}`
WHERE yandexuid % 119 == 110
ORDER BY yandexuid;
"""


class GetYuidWithAllSample(BaseYtTask):
    date = luigi.Parameter()
    task_group = 'offline_classification_graph_sample'

    def requires(self):
        return ExternalInput(production_config.YUID_WITH_ALL_BY_YANDEXUID_TABLE)

    def output(self):
        return YtDailyRewritableTarget(testing_config.YUID_WITH_ALL_BY_YANDEXUID_TABLE, self.date)

    def run(self):
        with self.yt.Transaction() as transaction:
            self.yql.query(
                query_string=yuid_with_all_sample_query.format(
                    input_table=self.input().table,
                    output_table=self.output().table,
                ),
                transaction=transaction,
            )

            self.yt.set_attribute(
                self.output().table,
                'generate_date',
                self.date,
            )


testing_vertices_sample_query = """
$filtered_crypta_id = (
    SELECT DISTINCT cryptaId
    FROM (
        SELECT cryptaId
        FROM `{vertices_by_id_type_table}`
        WHERE id_type == 'yandexuid' AND CAST(id AS Uint64) IS NOT NULL AND CAST(id AS Uint64) % 119 == 110
    )
);


$testing_table = (
    SELECT *
    FROM `{vertices_by_crypta_id_table}` AS by_crypta_id
    INNER JOIN $filtered_crypta_id AS filtered_crypta_id
    USING (cryptaId)
);

INSERT INTO `{testing_vertices_table}` WITH TRUNCATE
SELECT *
FROM $testing_table
ORDER BY id, id_type;

INSERT INTO `{testing_vertices_by_crypta_id_table}` WITH TRUNCATE
SELECT *
FROM $testing_table
ORDER BY cryptaId;

INSERT INTO `{testing_vertices_by_id_type_table}` WITH TRUNCATE
SELECT *
FROM $testing_table
ORDER BY id_type;

INSERT INTO `{testing_indevice_yandexuid_by_id_type_table}` WITH TRUNCATE
SELECT id, id_type, target_id
FROM `{indevice_yandexuid_by_id_type_table}`
WHERE CAST(target_id AS Uint64) IS NOT NULL AND CAST(target_id AS Uint64) % 119 == 110
ORDER BY id_type, id;
"""


class GetVerticesSample(BaseYtTask):
    date = luigi.Parameter()
    task_group = 'offline_classification_graph_sample'

    def requires(self):
        return {
            'vertices_by_id_type': ExternalInput(production_config.VERTICES_NO_MULTI_PROFILE_BY_ID_TYPE),
            'vertices_by_crypta_id': ExternalInput(production_config.VERTICES_NO_MULTI_PROFILE_BY_CRYPTA_ID),
            'indevice_yandexuid_by_id_type': ExternalInput(production_config.INDEVICE_YANDEXUID_BY_ID_TYPE),
        }

    def output(self):
        return {
            'vertices': YtDailyRewritableTarget(testing_config.VERTICES_NO_MULTI_PROFILE, self.date),
            'vertices_by_crypta_id': YtDailyRewritableTarget(
                testing_config.VERTICES_NO_MULTI_PROFILE_BY_CRYPTA_ID,
                self.date,
            ),
            'vertices_by_id_type': YtDailyRewritableTarget(
                testing_config.VERTICES_NO_MULTI_PROFILE_BY_ID_TYPE,
                self.date,
            ),
            'indevice_yandexuid_by_id_type': YtDailyRewritableTarget(
                testing_config.INDEVICE_YANDEXUID_BY_ID_TYPE,
                self.date,
            ),
        }

    def run(self):
        with self.yt.Transaction() as transaction:
            self.yql.query(
                query_string=testing_vertices_sample_query.format(
                    vertices_by_id_type_table=self.input()['vertices_by_id_type'].table,
                    vertices_by_crypta_id_table=self.input()['vertices_by_crypta_id'].table,
                    indevice_yandexuid_by_id_type_table=self.input()['indevice_yandexuid_by_id_type'].table,

                    testing_vertices_table=self.output()['vertices'].table,
                    testing_vertices_by_crypta_id_table=self.output()['vertices_by_crypta_id'].table,
                    testing_vertices_by_id_type_table=self.output()['vertices_by_id_type'].table,
                    testing_indevice_yandexuid_by_id_type_table=self.output()['indevice_yandexuid_by_id_type'].table,
                ),
                transaction=transaction,
            )

            for target_name, target in self.output().iteritems():
                self.yt.set_attribute(
                    target.table,
                    'generate_date',
                    self.date,
                )
