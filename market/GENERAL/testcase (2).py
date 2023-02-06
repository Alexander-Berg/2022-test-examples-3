__author__ = 'khaipov'

import os
import market.content_storage_service.lite.env as env
from .index import Index
from .sandbox import Sandbox
from .types.autogen import Autoscope
from market.content_storage_service.beam.service import change_with_ya_make

with_ya_make = True


class TestCase(env.MarketContentStorageSuite):
    index = Index()

    @classmethod
    def setup(cls):
        # Инициализация запуска без сборки
        for name in dir(cls):
            if name.startswith('prepare_') or 'prepare' == name:
                prepare_method = getattr(cls, name)
                prepare_method()
        cls._after_prepare()

    @classmethod
    def _after_prepare(cls):
        cls.commit_data()
        cls.setup_mocks()

    @classmethod
    def commit_data(cls):
        cls.index.commit()

    @classmethod
    def setup_mocks(cls):
        with Autoscope(False):
            # Положить данные в локальный saas
            if cls.market_content_storage.with_saas_server:
                cls.index.model_storage.load_to_saas(cls.market_content_storage.saas_server.config, cls.index.gltype_storage)
                cls.index.sku_storage.load_to_saas(cls.market_content_storage.saas_server.config, cls.index.gltype_storage)
                cls.index.fast_card_storage.load_to_saas(cls.market_content_storage.saas_server.config, cls.index.gltype_storage)
            # Положить данные в локальный report
            if cls.market_content_storage.with_report_server:
                cls.index.virtual_card_storage.load_to_report(cls.market_content_storage.report_server.config, cls.index.gltype_storage)
                cls.index.fast_card_storage.load_to_report(cls.market_content_storage.report_server.config, cls.index.gltype_storage)

        if with_ya_make:
            # Положить данные в локальный YT
            TestCase.svc_cls.local_yt.write_gltypes(cls.index.get_gltypes_for_yt())
            TestCase.svc_cls.local_yt.write_gumoful_templates(cls.index.get_gumoful_templates_for_yt())
            TestCase.svc_cls.local_yt.write_hids(cls.index.get_hids_for_yt())
            TestCase.svc_cls.local_yt.write_nids(cls.index.get_nids_for_yt())
            TestCase.svc_cls.local_yt.write_pers(cls.index.get_rating_for_yt())
            TestCase.svc_cls.local_yt.write_vendors(cls.index.get_vendors_for_yt())
            TestCase.svc_cls.local_yt.write_reasons_to_buy(cls.index.get_reasons_to_buy())

            # Запуск sandbox-задачи локально: читаются данные из локального YT и варятся csv-файлы
            sb = Sandbox()
            sb.run(TestCase.svc_cls.local_yt, TestCase.svc_cls.data_path)
        else:
            # Запуск кода sandbox-задачи, который пишет данные в файлик
            # Флажок is_local говорит, о запуске без ya make, в нем не работает локальный yt
            # Поэтому сразу пишем данные в файлики
            sb = Sandbox()
            sb.local_run(
                TestCase.svc_cls.data_path,
                cls.index.get_vendors_for_yt(),
                cls.index.get_nids_for_yt(),
                cls.index.get_hids_for_yt(),
                cls.index.get_gltypes_for_yt(),
                cls.index.get_gumoful_templates_for_yt(),
                cls.index.get_rating_for_yt(),
                cls.index.get_reasons_to_buy()
            )

        # TODO: добавить в лайты тесты для навигации
        nav_file = open(os.path.join(TestCase.svc_cls.data_path, 'recent_navigation'), 'w')
        nav_file.write('mock-nav-tree')
        nav_file.close()

        # Записываем дефолтные значения экспериментов
        exp_file = open(os.path.join(TestCase.svc_cls.data_path, 'default_experiments'), 'w')
        for kv in cls.index.default_experiments:
            exp_file.write(kv + '\n')
        exp_file.close()

    @classmethod
    def render_gumoful_request_card(cls, doc_id):
        # Сформировать из модели протобуф-сообщение для отправки в ручку render_gumoful
        model = cls.index.model_storage.get_doc(doc_id)
        card = model.to_render_gumoful_request(cls.index.gltype_storage)
        return card


# запуск теста без сборки
def main():
    change_with_ya_make()
    global with_ya_make
    with_ya_make = False
    env.main()
