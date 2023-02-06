import logging
import pandas as pd
import vh

from market.monetize.stapler.v1.operations.operations import NirvanaOp, NirvanaOperations
from market.monetize.stapler.v1.stapler.stapler import StaplerAbc
from market.assortment.ecom_log.bin import cli
from market.assortment.ecom_log.lib.default_classes import PathResolverMixin


class MyNirvanaOperations(NirvanaOperations):
    ultra_controller = NirvanaOp('c51ec4dd-b2db-441c-8a34-43510caba874')
    single_option_to_json = NirvanaOp('2fdd4bb4-4303-11e7-89a6-0025909427cc')
    single_option_to_text_op = NirvanaOp('2417849a-4303-11e7-89a6-0025909427cc')
    create_directory_op = NirvanaOp('33d81b2c-1d18-11e7-904c-3c970e24a776')
    text_to_mr_table_op = NirvanaOp('cdc2677c-46df-4e76-94e8-94c130ea9309')
    mr_table_path_as_text_op = NirvanaOp('a1691dbb-0d03-4cbf-9acf-5fd2be876f12')
    shop_title_dssm_matching = NirvanaOp('df3c84d1-d253-49d8-818e-5f8917a340ae')
    wrap_text_as_json = NirvanaOp('7167df37-076d-4567-a35d-8ce67be4edee')
    placeholder_replace = NirvanaOp('8a56423a-413c-11e7-89a6-0025909427cc')
    text_to_json = NirvanaOp('1f576d42-f66b-11e5-bdc7-0025909427cc')
    if_operation = NirvanaOp('325857d6-b157-4bb6-a76f-fd6aa70d814a')
    send_step_event = NirvanaOp('299f518d-f868-4cac-944f-867e457061de')
    get_mr_table = NirvanaOp('6ef6b6f1-30c4-4115-b98c-1ca323b50ac0')
    yt_remove = NirvanaOp('748d702a-75b8-4667-ab7d-57fa1af55317')


class Generator(StaplerAbc, PathResolverMixin):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.calculation_date_str = None
        self.calculation_date = None
        self.environment = None
        self.is_regular_run = None
        self.spark_dir_path = vh.data_from_str('//home/market/production/analytics_platform/ecom_log_v2/sys/spark_testing')
        self.source_names_vh = {
            # 'metrika': vh.data_from_str('metrika'),
            # 'sovetnik': vh.data_from_str('sovetnik'),
            # 'ga': vh.data_from_str('ga'),
            # 'csv': vh.data_from_str('csv'),
            # 'lavka': vh.data_from_str('lavka'),
            # 'beru': vh.data_from_str('beru'),
            'appmetrika': vh.data_from_str('appmetrika'),
            # 'ga_shops': vh.data_from_str('ga_shops'),
            # 'csv_shops': vh.data_from_str('csv_shops')
        }

        self.extraction_operations = {
            # 'sovetnik': cli.getSovetnikLogYqlTask,
            # 'lavka': cli.getLavkaLog,
            # 'beru': cli.getBeruLog,
            'appmetrika': cli.getAppMetrikaLog
        }

    @property
    def auto_start(self):
        return self.ctx.auto_start

    @property
    def op(self):
        return MyNirvanaOperations

    def matching_workaround(self, input_table=None, input_date=None, environment=None, source_name=None):
        """
        Костыльная таска, полностью воспроизводящая матчинг в старом процессе. См. MARKETANSWERS-23778
        """
        getTableToUK = self.run(
            cli.getTableToMatch,
            environment=environment,
            input_date=self.calculation_date,
            task_dependencies=[input_table],
            table_to_match=input_table,
            source_name=self.source_names_vh[source_name]
        )

        resolved_path = vh.data_from_str('//tmp/analytics_platform/matching/resolved/finished/{}'.format(source_name))
        remove_resolved_path_if_exists = self.run(
            cli.ytRemove,
            path=resolved_path,
            input_date=self.calculation_date,
            task_dependencies=[input_table]
        )

        table_to_match = self.op.get_mr_table(cluster='hahn', fileWithTableName=getTableToUK)
        # step_event_sent = self.op.send_step_event(
        #     yt_table=table_to_match,
        #     step_token=vh.Secret('analytics_platform-sandbox-token'),
        #     ctp_type='market-analyst-cooked-log',
        #     ctp_group='analytics-platform-prepared-to-resolve-' + source_name,
        #     _after=[getTableToUK, remove_resolved_path_if_exists]
        # )
        wait_matched = self.run(
            cli.ytWait,
            path=resolved_path,
            # retry_timeout_minutes=1,
            # max_waiting_minutes=24*60,
            input_date=self.calculation_date #,
            # task_dependencies=[step_event_sent]
        )

        getUpdatedDictTable = self.run(
            cli.getUpdatedMatchingDict,
            environment=environment,
            task_dependencies=[wait_matched],
            input_date=self.calculation_date,
            source_name=self.source_names_vh[source_name]
        )

        getJoinedTableWithMatchingDict = self.run(
            cli.joinWithMatchingDict,
            environment=environment,
            task_dependencies=[getUpdatedDictTable],
            table_to_match=input_table,
            input_date=input_date,
            source_name=self.source_names_vh[source_name]
        )

        return getJoinedTableWithMatchingDict

    def wait_source(self, source_name):
        wait_source_op = self.run(
            cli.waitOrSkip,
            input_date=self.calculation_date,
            environment=self.environment,
            source_name=self.source_names_vh[source_name],
            is_regular_run=self.is_regular_run,
            ttl=24*60
        )
        wait_source_result = self.op.text_to_json(input=wait_source_op)
        wait_source_condition = self.op.if_operation(_dynamic_options=wait_source_result)
        # степлеровская таска не поддерживает _dynamic_options, поэтому втыкаем костыльный кубик для переключения ветки
        wait_source_fork = self.op.single_option_to_text(
            input='-',
            _dynamic_options=wait_source_condition.output_true)
        return wait_source_fork

    def filter_chain(self, input_source, source_name, task_dependencies=None):
        """
        Костыльная таска для разворачивания фильтра за несколько дат в несколько кубиков фильтрации, по одному на дату.
        Нужно потому что при историческом пересчёте за несколько дат фильтрация падает по OOM.
        """
        if '..' in self.calculation_date_str:
            date1, date2 = self.calculation_date_str.split('..')
            dates = list(pd.date_range(date1, date2).astype(str))
        else:
            dates = [self.calculation_date_str]

        if task_dependencies is None:
            task_dependencies = []

        run_chain = []
        for i, date in enumerate(dates):
            input = input_source if i == 0 else run_chain[-1]
            run_chain.append(
                self.run(
                    cli.salesFilter,
                    input_date=vh.data_from_str(date),
                    environment=self.environment,
                    input_path=input,
                    source_name=self.source_names_vh[source_name],
                    spark_dir_path=self.spark_dir_path,
                    task_dependencies=task_dependencies
                )
            )
        return run_chain[-1]

    def preparation_tasks(self):
        percentiles_waiting_paths = [
            '//home/market/production/analytics_platform/analyst/ecom_log/utils/orders_percentiles',
            '//home/market/production/analytics_platform/analyst/ecom_log/utils/median_offers_price',
            '//home/market/production/analytics_platform/analyst/ecom_log/utils/hid_percentiles'
        ]
        wait_percentiles = self.run(
            cli.ytWaitTablesRange,
            paths=vh.data_from_str(','.join(percentiles_waiting_paths)),
            input_date=self.calculation_date,
            ttl=24*60
        )
        sovetnik_multipliers_waiting_paths = [
            '//home/market/production/analytics_platform/analyst/trafic_and_orders'
        ]
        wait_sovetnik_multipliers = self.run(
            cli.ytWaitTablesRange,
            paths=vh.data_from_str(','.join(sovetnik_multipliers_waiting_paths)),
            input_date=self.calculation_date,
            ttl=24*60
        )
        # prepare_order_percentiles = self.run(
        #     cli.prepareFilterPercentiles,
        #     environment=get_environment,
        #     input_date=calculation_date
        # )
        return wait_percentiles, wait_sovetnik_multipliers

    def extract_metrika(self):
        """
        Выгрузка, подготовка, матчинг и фильтрация логов метрики
        """
        wait_source = self.run(
            cli.ytWaitTablesRange,
            paths=vh.data_from_str('//logs/visit-v2-log/1d,//logs/visit-v2-private-log/1d'),
            input_date=self.calculation_date
        )
        self.run(
            cli.sendActionCompletedStep,
            action_name=vh.data_from_str(f"actions/analytics-platform-metrika-source"),
            scale=vh.data_from_str("1d"),
            cluster=vh.data_from_str("hahn"),
            task_dependencies=[wait_source]
        )
        getMetrikaLog = self.run(
            cli.getMetrikaLogNileTask,
            environment=self.environment,
            task_dependencies=[wait_source],
            input_date=self.calculation_date
        )
        getMetrikaLog_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=getMetrikaLog
        )
        get_updated_no_title_dict = self.run(
            cli.getNoTitleDictYqlTask,
            environment=self.environment
        )
        copy_no_title_dict_from_arnold_to_hahn = self.run(
            cli.copyNoTitleDictFromArnoldToHahn,
            token=vh.data_from_str(self.ctx.yt_token),
            task_dependencies=[get_updated_no_title_dict]
        )
        get_joined_metrika_sales_with_no_title_dict_yql_task = self.run(
            cli.getJoinedMetrikaSalesWithNoTitleDictYqlTask,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=getMetrikaLog,
            task_dependencies=[getMetrikaLog, copy_no_title_dict_from_arnold_to_hahn],
        )
        get_joined_metrika_sales_with_no_title_dict_yql_task_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=get_joined_metrika_sales_with_no_title_dict_yql_task
        )
        return get_joined_metrika_sales_with_no_title_dict_yql_task

    def match_uk_and_dssm(self, input_op, source_name):
        matched_uk = self.matching_workaround(
            input_table=input_op,
            input_date=self.calculation_date,
            environment=self.environment,
            source_name=source_name
        )
        matched_uk_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=matched_uk,
            task_dependencies=[matched_uk]
        )
        matched_dssm = self.op.shop_title_dssm_matching(
            input_table=matched_uk,
            model=vh.data(id='a6da69cb-f444-4f13-aec3-d4750b85635f')
        )
        matched_dssm_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=matched_dssm,
            task_dependencies=[matched_dssm]
        )
        matched_corrected = self.run(
            cli.matchingCascadeCorrection,
            input_path=matched_dssm,
            input_date=self.calculation_date,
            environment=self.environment,
            source_name=self.source_names_vh[source_name],
            ttl=600
        )
        matched_corrected_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=matched_corrected,
        )
        return matched_corrected

    def filter_metrika(self, matched_result, task_dependencies=None):
        if task_dependencies is None:
            task_dependencies = []
        if self.calculation_date_str != '-':
            filtered_metrika = self.filter_chain(matched_result, 'metrika', task_dependencies)
        else:
            filtered_metrika = self.run(
                cli.salesFilter,
                input_date=self.calculation_date,
                environment=self.environment,
                input_path=matched_result,
                source_name=self.source_names_vh['metrika'],
                spark_dir_path=self.spark_dir_path,
                task_dependencies=task_dependencies
            )
        return filtered_metrika

    def filter_source(self, matched_result, source_name, task_dependencies=None):
        if task_dependencies is None:
            task_dependencies = []
        filtered_source = self.run(
            cli.salesFilter,
            input_date=self.calculation_date,
            environment=self.environment,
            input_path=matched_result,
            source_name=self.source_names_vh[source_name],
            spark_dir_path=self.spark_dir_path,
            task_dependencies=task_dependencies
        )
        return filtered_source

    def extract_log(self, source_name):
        sources_to_wait = {
            # 'metrika': [
            #     '//logs/visit-v2-log/1d/',
            #     '//logs/visit-v2-private-log/1d/'
            # ],
            # 'sovetnik': [
            #     '//logs/sovetnik-buy-log/1d',
            #     '//logs/sovetnik-log/1d',
            #     '//logs/sovetnik-domain-data-log/1d'
            # ],
            'appmetrika': [
                '//home/logfeller/logs/appmetrica-external-events/1d'
            ]
        }
        wait_source = self.run(
            cli.ytWaitTablesRange,
            paths=vh.data_from_str(','.join(sources_to_wait[source_name])),
            input_date=self.calculation_date
        )
        # self.run(
        #     cli.sendActionCompletedStep,
        #     action_name=vh.data_from_str(f"actions/analytics-platform-{source_name}-source"),
        #     scale=vh.data_from_str("1d"),
        #     cluster=vh.data_from_str("hahn"),
        #     task_dependencies=[wait_source]
        # )
        get_log = self.run(
            self.extraction_operations[source_name],
            environment=self.environment,
            task_dependencies=[wait_source],
            input_date=self.calculation_date
        )
        get_log_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=get_log
        )
        return get_log

    def multiply_sovetnik(self, filtered_result, task_dependencies=None):
        if task_dependencies is None:
            task_dependencies = []
        multiplied_sovetnik = self.run(
            cli.multiplySovetnik,
            input_date=self.calculation_date,
            environment=self.environment,
            input_path=filtered_result,
            spark_dir_path=self.spark_dir_path,
            task_dependencies=task_dependencies
        )
        return multiplied_sovetnik

    def extract_external_log(self, source_name):
        shop_label, preparation_op, get_log_op = None, None, None
        if source_name == 'ga':
            shop_label = 'ga_shops'
            preparation_op = cli.prepareDailyGALog
            get_log_op = cli.getGALog
        elif source_name == 'csv':
            shop_label = 'csv_shops'
            preparation_op = cli.prepareDailyCSVLog
            get_log_op = cli.getCSVLog
        elif source_name == 'lavka':
            get_log_op = cli.getLavkaLog

        if source_name in ['ga', 'csv']:
            wait_preparation_fork = self.wait_source(shop_label)
            prepare_daily_log = self.run(
                preparation_op,
                environment=self.environment,
                input_date=self.calculation_date,
                task_dependencies=[wait_preparation_fork]
            )

        wait_source_fork = self.wait_source(source_name)
        self.run(
            cli.sendActionCompletedStep,
            action_name=vh.data_from_str(f"actions/analytics-platform-{source_name}-source"),
            scale=vh.data_from_str("1d"),
            cluster=vh.data_from_str("hahn"),
            task_dependencies=[wait_source_fork]
        )
        get_log = self.run(
            get_log_op,
            environment=self.environment,
            input_date=self.calculation_date,
            task_dependencies=[wait_source_fork]
        )
        get_log_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=get_log,
            task_dependencies=[get_log]
        )
        return get_log, wait_source_fork

    def extract_beru(self) -> vh.ext.nirvana.frontend.OpPartial:
        """
        Выгрузка и подготовка данных Беру
        """
        table = "//home/market/production/mstat/analyst/cubes_staging/orders/items/1d/${input_date}"
        expected_table = self.op.placeholder_replace(
            template=self.op.single_option_to_text(input=table),
            placeholders=self.op.wrap_text_as_json(text=self.calculation_date, field_name="input_date")
        )
        expected_table = self.op.text_to_mr_table_op(input=expected_table)
        waiting_result = NirvanaOperations.yt_wait_op(
            _inputs={"table": expected_table},
            environment="hahn",
            encryptedOauthToken=self.ctx.secret_name,
            recheckIntervalMinutes=5,
            maxTtl="24h"
        )

        table = "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict_flattened/${input_date}"
        # Table format: YYYY-MM
        # calculation_date_str format: YYYY-MM-DD
        calculation_date = self.calculation_date_str[:-3]
        calculation_date = self.op.single_option_to_text_op(input=calculation_date or '-', _name='calculation date [YYYY-MM]')
        expected_table = self.op.placeholder_replace(
            template=self.op.single_option_to_text(input=table),
            placeholders=self.op.wrap_text_as_json(text=calculation_date, field_name="input_date")
        )
        updated_table=self.run(
            cli.ytWaitTableUpdate,
            update_date=self.calculation_date,
            environment=self.environment,
            input_path=expected_table,
        )
        self.run(
            cli.sendActionCompletedStep,
            action_name=vh.data_from_str(f"actions/analytics-platform-beru-source"),
            scale=vh.data_from_str("1d"),
            cluster=vh.data_from_str("hahn"),
            task_dependencies=[waiting_result, updated_table]
        )
        get_beru_log = self.run(
            cli.getBeruLog,
            environment=self.environment,
            input_date=self.calculation_date,
            task_dependencies=[waiting_result, updated_table]
        )
        get_beru_log_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=get_beru_log
        )
        return get_beru_log_split

    def choose_source(self, task_dependencies):
        choose_source = self.run(
            cli.chooseSourceForAP,
            environment=self.environment,
            input_date=self.calculation_date,
            spark_dir_path=self.spark_dir_path,
            is_regular_run=self.is_regular_run,
            task_dependencies=task_dependencies
        )
        choose_source_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            # костыльный параметр: в спарк-таске нельзя записать в выход имя итоговой таблицы
            input_path=vh.data_from_str('merged'),
            task_dependencies=[choose_source]
        )
        return choose_source

    def custom_operations(self, chosen_source):
        custom_operations = self.run(
            cli.customOperations,
            environment=self.environment,
            input_date=self.calculation_date,
            task_dependencies=[chosen_source]
        )
        custom_operations_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=custom_operations
        )
        return custom_operations

    def aggregation(self, custom_operations_result):
        aggregation = self.run(
            cli.aggregation,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=custom_operations_result,
        )
        aggregation_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=aggregation
        )
        return aggregation_split

    def fill_missing_sales(self, aggregation_splitted):
        missing_sales = self.run(
            cli.fillMissingSales,
            environment=self.environment,
            input_date=self.calculation_date,
            task_dependencies=[aggregation_splitted]
        )
        return missing_sales

    def blur(self, filled):
        blurred = self.run(
            cli.blur,
            environment=self.environment,
            input_date=self.calculation_date,
            task_dependencies=[filled],
        )
        blur_split = self.run(
            cli.splitByDate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=blurred
        )
        return blurred

    def update_master(self, blurred):
        master_updated = self.run(
            cli.salesMasterUpdate,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=blurred,
            is_regular_run=self.is_regular_run
        )
        return master_updated

    def define_graph(self, calculation_date_str, environment):
        logging.info('Define graph')
        self.calculation_date_str = calculation_date_str
        self.calculation_date = self.op.single_option_to_text_op(input=calculation_date_str or '2021-11-29', _name='calculation date [YYYY-MM-DD]')
        self.environment = self.op.single_option_to_text_op(input=environment or 'testing', _name='environment')
        self.is_regular_run = self.op.single_option_to_text_op(input='true', _name='is_regular_run')

        #
        # runSparkCluster_op = self.run(
        #     runSparkCluster,
        #     yt_pool='',
        #     spark_dir_path=spark_dir_path
        # )
        """
        Вспомогательные расчёты
        """
        wait_percentiles, wait_sovetnik_multipliers = self.preparation_tasks()

        """
        Выгрузка, подготовка, матчинг и фильтрация логов метрики
        """
        # extracted_metrika = self.extract_metrika()
        # matched_metrika = self.match_uk_and_dssm(extracted_metrika, 'metrika')
        # filtered_metrika = self.filter_metrika(matched_metrika, task_dependencies=[wait_percentiles])

        """
        Выгрузка, подготовка, матчинг и фильтрация логов советника
        """
        # extracted_sovetnik = self.extract_log('sovetnik')
        # matched_sovetnik = self.match_uk_and_dssm(extracted_sovetnik, 'sovetnik')
        # filtered_sovetnik = self.filter_source(matched_sovetnik, 'sovetnik', [wait_percentiles])
        # multiplied_sovetnik = self.multiply_sovetnik(filtered_sovetnik, task_dependencies=[wait_sovetnik_multipliers])

        """
        Выгрузка, подготовка, матчинг и фильтрация логов GA
        """
        # extracted_ga, wait_ga_fork = self.extract_external_log('ga')
        # matched_ga = self.match_uk_and_dssm(extracted_ga, 'ga')
        # filtered_ga = self.filter_source(matched_ga, 'ga', task_dependencies=[wait_percentiles])

        """
        Выгрузка, подготовка, матчинг и фильтрация логов CSV
        """
        # extracted_csv, wait_csv_fork = self.extract_external_log('csv')
        # matched_csv = self.match_uk_and_dssm(extracted_csv, 'csv')
        # csv пока решили не фильтровать MARKETANSWERS-22466
        # filtered_csv = self.filter_source(matched_csv, 'csv', task_dependencies=[wait_percentiles])

        """
        Выгрузка, подготовка, матчинг и фильтрация логов Лавки
        """
        # extracted_lavka, wait_lavka_fork = self.extract_external_log('lavka')
        # matched_lavka = self.match_uk_and_dssm(extracted_lavka, 'lavka')
        # Лавку пока решили не фильтровать MARKETANSWERS-22466
        # filtered_lavka = self.filter_source(matched_lavka, 'lavka', task_dependencies=[wait_percentiles])

        """
        Выгрузка и подготовка данных Беру
        """
        # extracted_beru_splitted = self.extract_beru()

        """
        Выгрузка, подготовка, матчинг и фильтрация логов аппметрики
        """
        extracted_appmetrika = self.extract_log('appmetrika')
        matched_appmetrika = self.match_uk_and_dssm(extracted_appmetrika, 'appmetrika')
        filtered_appmetrika = self.filter_source(matched_appmetrika, 'appmetrika', [wait_percentiles])

        """
        Слияние источников, выбор источника, кастомная фильтрация, агрегация, восстановление продаж, блёр
        """
        task_dependencies = [
                # filtered_metrika,
                # multiplied_sovetnik,
                # wait_ga_fork, filtered_ga,
                # wait_csv_fork, matched_csv,
                # wait_lavka_fork, matched_lavka,
                # extracted_beru_splitted,
                filtered_appmetrika
            ]
        chosen_source = self.choose_source(task_dependencies=task_dependencies)
        custom_operations_result = self.custom_operations(chosen_source)
        # aggregated_splitted = self.aggregation(custom_operations_result)
        # filled = self.fill_missing_sales(aggregated_splitted)
        # blurred = self.blur(filled)
        # blur_split_table = self.op.get_mr_table(cluster='hahn', fileWithTableName=blurred)
        # self.op.send_step_event(
        #     yt_table=blur_split_table,
        #     step_token=vh.Secret('analytics_platform-sandbox-token'),
        #     ctp_type='market-analyst-cooked-log',
        #     ctp_group='analytics-platform-blur-table-by-data',
        #     _after=[blurred]
        # )
        # self.run(
        #     cli.sendActionCompletedStep,
        #     action_name=vh.data_from_str("actions/analytics-platform-blur-table-by-data"),
        #     scale=vh.data_from_str("1d"),
        #     cluster=vh.data_from_str("hahn"),
        #     task_dependencies=[blurred]
        # )
        #
        # updated_master = self.update_master(blurred)
        # master_table = self.op.get_mr_table(cluster='hahn', fileWithTableName=updated_master)
        # self.op.send_step_event(
        #     yt_table=master_table,
        #     step_token=vh.Secret('analytics_platform-sandbox-token'),
        #     ctp_type='market-analyst-cooked-log',
        #     ctp_group='analytics-platform-master-table',
        #     _after=[updated_master]
        # )
        # self.run(
        #     cli.sendActionCompletedStep,
        #     action_name=vh.data_from_str("actions/analytics-platform-master-table"),
        #     scale=vh.data_from_str("1d"),
        #     cluster=vh.data_from_str("hahn"),
        #     task_dependencies=[updated_master]
        # )

        # spark_dir_path = '//home/market/testing/analytics_platform/analyst/spark_discovery'
        #
        # runSparkCluster_op = self.run(
        #     runSparkCluster,
        #     yt_pool='',
        #     spark_dir_path=spark_dir_path
        # )
        #
        # sparkTasks = [filterLogSparkTask, multiplySovetnikSparkTask, chooseSourceSparkTask, cookSalesForExportSparkTaks]
        # build_target_ops = []
        # for task in sparkTasks:
        #     getTargetSparkTask_op = self.run(
        #         task,
        #         root_path=_root,
        #         spark_dir_path=spark_dir_path,
        #         task_dependencies=[
        #             runSparkCluster_op
        #         ]
        #     )
        #     build_target_ops.append(getTargetSparkTask_op)
        #
        # self.run(
        #     stopSparkCluster,
        #     spark_dir_path=spark_dir_path,
        #     task_dependencies=[runSparkCluster_op]
        # )
