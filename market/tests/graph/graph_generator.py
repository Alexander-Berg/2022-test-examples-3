import vh

from market.assortment.ecom_log.graphs.ap.graph_generator import Generator
from market.assortment.ecom_log.bin import cli


class CustomGenerator(Generator):
    def define_graph(self, calculation_date_str, environment):
        self.calculation_date_str = calculation_date_str
        self.calculation_date = self.op.single_option_to_text_op(input=calculation_date_str or '-', _name='calculation date')
        self.environment = self.op.single_option_to_text_op(input=environment or '-', _name='environment')
        self.is_regular_run = self.op.single_option_to_text_op(input='false', _name='is_regular_run')
        spark_dir_path = vh.data_from_str('//home/market/production/analytics_platform/ecom_log_v2/sys/spark_testing')

        """
        Вспомогательные расчёты
        """
        wait_percentiles, wait_sovetnik_multipliers = self.preparation_tasks()

        """
        Выгрузка, подготовка, матчинг и фильтрация логов метрики
        """
        extracted_metrika = self.extract_metrika()
        matched_metrika = self.match_uee_and_dssm(extracted_metrika, 'metrika')
        filtered_metrika = self.filter_metrika(matched_metrika, task_dependencies=[wait_percentiles])

        """
        Выгрузка, подготовка, матчинг и фильтрация логов советника
        """
        extracted_sovetnik = self.extract_log('sovetnik')
        matched_sovetnik = self.match_uee_and_dssm(extracted_sovetnik, 'sovetnik')
        filtered_sovetnik = self.filter_source(matched_sovetnik, 'sovetnik', [wait_percentiles])
        multiplied_sovetnik = self.multiply_sovetnik(filtered_sovetnik, task_dependencies=[wait_sovetnik_multipliers])

        # """
        # Выгрузка, подготовка, матчинг и фильтрация логов GA
        # """
        # extracted_ga, wait_ga_fork = self.extract_external_log('ga')
        # matched_ga = self.match_uee_and_dssm(extracted_ga, 'ga')
        # filtered_ga = self.filter_source(matched_ga, 'ga', task_dependencies=[wait_percentiles])
        #
        # """
        # Выгрузка, подготовка, матчинг и фильтрация логов CSV
        # """
        # extracted_csv, wait_csv_fork = self.extract_external_log('csv')
        # matched_csv = self.match_uee_and_dssm(extracted_csv, 'csv')
        # # csv пока решили не фильтровать MARKETANSWERS-22466
        # # filtered_csv = self.filter_source(matched_csv, 'csv', task_dependencies=[wait_percentiles])
        #
        # """
        # Выгрузка, подготовка, матчинг и фильтрация логов Лавки
        # """
        # extracted_lavka, wait_lavka_fork = self.extract_external_log('lavka')
        # matched_lavka = self.match_uee_and_dssm(extracted_lavka, 'lavka')
        # # Лавку пока решили не фильтровать MARKETANSWERS-22466
        # # filtered_lavka = self.filter_source(matched_lavka, 'lavka', task_dependencies=[wait_percentiles])

        """
        Выгрузка и подготовка данных Беру
        """
        extracted_beru_splitted = self.extract_beru()

        choose_source_for_export = self.run(
            cli.chooseSourceForExport,
            input_date=self.calculation_date,
            environment=self.environment,
            spark_dir_path=spark_dir_path,
            task_dependencies=[filtered_metrika, multiplied_sovetnik, extracted_beru_splitted]
        )

        self.run(
            cli.internalExport,
            environment=self.environment,
            input_date=self.calculation_date,
            input_path=choose_source_for_export
        )

        """
        Выгрузка, подготовка, матчинг и фильтрация логов аппметрики
        """
        extracted_appmetrika = self.extract_log('appmetrika')
        matched_appmetrika = self.match_uee_and_dssm(extracted_appmetrika, 'appmetrika')
        filtered_appmetrika = self.filter_source(matched_appmetrika, 'appmetrika', [wait_percentiles])

