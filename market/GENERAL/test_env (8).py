# coding: utf-8

import logging
import yatest.common
from mock import patch

from market.idx.datacamp.routines.lib.lb_controller import MbiTasksProcessor
from market.idx.datacamp.routines.lib.http_api import create_flask_app
from market.idx.datacamp.routines.lib.tasks.complete_commands import CompleteCommands
from market.idx.datacamp.routines.lib.tasks.datacamp_cleaner import DatacampCleaner
from market.idx.datacamp.routines.lib.tasks.datacamp_statistics import StatsCalc
from market.idx.datacamp.routines.lib.tasks.datacamp_united_dumper import UnitedDatacampDumper
from market.idx.datacamp.routines.lib.tasks.mr_hub import MRHub
from market.idx.datacamp.routines.lib.tasks.dco_uploader import DcoUploader
from market.idx.datacamp.routines.lib.tasks.delivery_diff import DeliveryDiff
from market.idx.datacamp.routines.lib.tasks.ecom_export_merged_offers_dumper import EcomExportMergedOffersDumper
from market.idx.datacamp.routines.lib.tasks.mboc_offers_diff_creator import MbocOffersDiffCreatorAndSender
from market.idx.datacamp.routines.lib.tasks.mboc_stat_offers_diff import MbocStatOffersDiffCreator
from market.idx.datacamp.routines.lib.tasks.mstat_dumper import MStatDumper
from market.idx.datacamp.routines.lib.tasks.offers_copier import OffersCopier
from market.idx.datacamp.routines.lib.tasks.partner_info_uploader import PartnerInfoUploader
from market.idx.datacamp.routines.lib.tasks.partner_stats_updater import PartnerStatsUpdater
from market.idx.datacamp.routines.lib.tasks.pictures_regainer import PicturesRegainer
from market.idx.datacamp.routines.lib.tasks.promo_description_table_dumper import PromoDescriptionTableDumper
from market.idx.datacamp.routines.lib.tasks.promo_saas_diff_builder import PromoSaasDiffBuilder
from market.idx.datacamp.routines.lib.tasks.resolved_redirect_tracker import ResolvedRedirectTracker
from market.idx.datacamp.routines.lib.tasks.saas_diff_builder import SaasDiffBuilder
from market.idx.datacamp.routines.lib.tasks.saas_dumper import SaasDumper, SaasPublisher
from market.idx.datacamp.routines.lib.tasks.sender_to_miner import SenderToMiner
from market.idx.datacamp.routines.lib.tasks.status_diff_batcher import StatusDiffBatcher, FreshStatusDiffBatcher
from market.idx.datacamp.yatf.datacamp_env import DataCampTestEnv
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class HttpRoutinesTestEnv(DataCampTestEnv):
    def __init__(self, yt_server, **resources):
        super(HttpRoutinesTestEnv, self).__init__(yt_server, **resources)

        self.routines_app = None
        self.test_client = None

    def __enter__(self):
        super(HttpRoutinesTestEnv, self).__enter__()
        self.routines_app = create_flask_app(self.config, self.config_path)
        return self

    def __exit__(self, *args):
        self.routines_app = None
        super(HttpRoutinesTestEnv, self).__exit__(*args)

    def do_request(self, method, path, data=None, headers=None):
        if self.test_client is None:
            self.test_client = self.routines_app.test_client()
        return self.test_client.open(path, method=method, data=data, headers=headers)

    def put(self, path, data=None):
        return self.do_request('PUT', path, data)

    def get(self, path, data=None):
        return self.do_request('GET', path, data)

    def post(self, path, data=None, headers=None):
        return self.do_request('POST', path, data, headers)


class Patcher(object):
    def __init__(self, env):
        paths = [
            'market.idx.datacamp.routines.lib.blueprints.send_partners_stock.exec_cmd',
            'market.idx.datacamp.routines.lib.cleaner.exec_cmd',
            'market.idx.datacamp.routines.lib.dumper.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.datacamp_statistics.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.offers_copier.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.prepare_publication_status_diff.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.saas_dumper.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.saas_diff_builder.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.pictures_regainer.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.ecom_export_merged_offers_dumper.exec_cmd',
            'market.idx.datacamp.routines.lib.tasks.resolved_redirect_tracker.exec_cmd',
        ]
        self.patches = [
            patch(path, side_effect=env._execute)
            for path in paths
        ]

    def __enter__(self):
        for p in self.patches:
            p.__enter__()

    def __exit__(self, *args):
        for p in self.patches:
            p.__exit__(*args)


class RoutinesTestEnv(DataCampTestEnv):
    def __init__(self, yt_stuff, task_cls, **resources):
        super(RoutinesTestEnv, self).__init__(yt_stuff, **resources)

        self.task_cls = task_cls

    @property
    def description(self):
        return 'routines_env'

    def _execute(self, cmd, logger):
        return self.try_execute_under_gdb(cmd).exit_code

    def __enter__(self):
        super(RoutinesTestEnv, self).__enter__()

        self.task = self.task_cls()
        self.task.set_logger(logging.getLogger(self.__class__.__name__))

        with Patcher(self):
            self.task.run(self.config, self.config_path)

        return self

    def __exit__(self, *args):
        super(RoutinesTestEnv, self).__exit__(*args)


class DcoUploaderTestEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        if resources.get('dco_table') is None:
            raise RuntimeError("You should create dco table before init the Env")

        super(DcoUploaderTestEnv, self).__init__(yt_stuff, task_cls=DcoUploader, **resources)

    @property
    def description(self):
        return 'dco_uploader_env'


class SenderToMinerMock(SenderToMiner):
    def run_cmd(self, cmd):
        # TODO(@bzz13) try_execute_under_gdb
        res = yatest.common.execute(cmd)
        return res.returncode, res.stdout, res.stderr


class SenderToMinerTestEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(SenderToMinerTestEnv, self).__init__(yt_stuff, task_cls=SenderToMinerMock, **resources)

    @property
    def description(self):
        return 'sender_to_miner_env'


class DatacampTaskRunner(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, cmd, cmd_args, **resources):
        super(DatacampTaskRunner, self).__init__(**resources)
        self.cmd = cmd
        self.cmd_args = cmd_args

    def __enter__(self):
        BaseEnv.__enter__(self)
        cmd = [
            self.resources['config'].tasks_bin,
            self.cmd,
        ] + list(map(str, self.cmd_args))
        self.exec_result = self.try_execute_under_gdb(cmd)

        return self


class SenderToMinerJobTestEnv(BaseEnv):
    def __init__(self, **resources):
        super(SenderToMinerJobTestEnv, self).__init__(**resources)
        self.config = resources['config']

    @property
    def description(self):
        return 'sender_to_miner_job_env'

    def __enter__(self):
        if not self.config.enable_force_mining_job:
            self.log.info('mining job is disabled')
            return

        BaseEnv.__enter__(self)

        cmd = [
            self.resources['config'].tasks_bin,
            'force-mining-job',
            '--config-path', str(self.resources['config'].path),
        ]
        logging.info('Executing arguments: {}'.format(cmd))
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            wait=False,
            check_exit_code=False,
        )

        if not self.exec_result.running:
            logging.error('SenderToMinerJob failed, exit_code={}'.format(self.exec_result.exit_code))
            raise Exception('SenderToMinerJob not started')
        return self

    def __exit__(self, *args):
        BaseEnv.__exit__(self, args)

        if self.exec_result is not None and self.exec_result.running:
            self.exec_result.kill()
        if self.exec_result:
            self.exec_result.wait(check_exit_code=False)
        self.exec_result = None


class PartnerInfoUploaderTestEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(PartnerInfoUploaderTestEnv, self).__init__(yt_stuff, task_cls=PartnerInfoUploader, **resources)

    @property
    def description(self):
        return 'partner_info_updater_env'


class PartnerStatsUpdaterTestEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(PartnerStatsUpdaterTestEnv, self).__init__(yt_stuff, task_cls=PartnerStatsUpdater, **resources)

    @property
    def description(self):
        return 'partner_stats_updater_env'


class UnitedDatacampDumperEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(UnitedDatacampDumperEnv, self).__init__(yt_stuff, task_cls=UnitedDatacampDumper, **resources)

    @property
    def description(self):
        return 'united_datacamp_dumper_env'


class MRHubEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(MRHubEnv, self).__init__(yt_stuff, task_cls=MRHub, **resources)

    @property
    def resource_dependencies(self):
        return {
            'stock_sku_table_recent_link': 'stock_sku_table'
        }

    @property
    def description(self):
        return 'united_datacamp_dumper_env'


class DatacampCleanerEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(DatacampCleanerEnv, self).__init__(yt_stuff, task_cls=DatacampCleaner, **resources)

    @property
    def description(self):
        return 'datacamp_cleaner_env'


class OffersCopierEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(OffersCopierEnv, self).__init__(yt_stuff, task_cls=OffersCopier, **resources)

    @property
    def description(self):
        return 'offers_copier_env'


class PromoDescriptionDumperEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(PromoDescriptionDumperEnv, self).__init__(yt_stuff, task_cls=PromoDescriptionTableDumper, **resources)

    @property
    def description(self):
        return 'promo_description_dumper_env'


class MbocOffersDiffCreatorAndSenderEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(MbocOffersDiffCreatorAndSenderEnv, self).__init__(yt_stuff, task_cls=MbocOffersDiffCreatorAndSender, **resources)

    @property
    def description(self):
        return 'mboc_offers_diff_creator_and_sender_env'


class MbocStatOffersDiffCreatorEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(MbocStatOffersDiffCreatorEnv, self).__init__(yt_stuff, task_cls=MbocStatOffersDiffCreator, **resources)

    @property
    def description(self):
        return 'mboc_stat_offers_diff_creator_env'


class SaasDiffBuilderEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(SaasDiffBuilderEnv, self).__init__(yt_stuff, task_cls=SaasDiffBuilder, **resources)

    @property
    def description(self):
        return 'saas_diff_builder_env'


class PromoSaasDiffBuilderEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(PromoSaasDiffBuilderEnv, self).__init__(yt_stuff, task_cls=PromoSaasDiffBuilder, **resources)

    @property
    def description(self):
        return 'promo_saas_diff_builder_env'


class SaasDumperEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(SaasDumperEnv, self).__init__(yt_stuff, task_cls=SaasDumper, **resources)

    @property
    def description(self):
        return 'saas_dumper_env'


class SaasPublisherEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(SaasPublisherEnv, self).__init__(yt_stuff, task_cls=SaasPublisher, **resources)

    @property
    def description(self):
        return 'saas_publisher_env'


class CompleteCommandsEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(CompleteCommandsEnv, self).__init__(yt_stuff, task_cls=CompleteCommands, **resources)

    @property
    def description(self):
        return 'complete_commands_env'


class MbiTasksProcessorEnv(DataCampTestEnv):
    def __init__(self, *args, **kwargs):
        super(MbiTasksProcessorEnv, self).__init__(*args, **kwargs)
        self.processor = None

    def __enter__(self):
        super(MbiTasksProcessorEnv, self).__enter__()

        self.processor = MbiTasksProcessor(self.config)
        self.processor.start()

        return self

    def __exit__(self, *args, **kwargs):
        super(MbiTasksProcessorEnv, self).__exit__(*args, **kwargs)

        self.processor.stop()


class MStatDumperEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(MStatDumperEnv, self).__init__(yt_stuff, task_cls=MStatDumper, **resources)

    @property
    def description(self):
        return 'mstat_dumper_env'


class PicturesRegainerEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(PicturesRegainerEnv, self).__init__(yt_stuff, task_cls=PicturesRegainer, **resources)

    @property
    def description(self):
        return 'pictures_regainer_env'


class StatusDiffBatcherEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(StatusDiffBatcherEnv, self).__init__(yt_stuff, task_cls=StatusDiffBatcher, **resources)

    @property
    def description(self):
        return 'status_diff_batcher_env'


class FreshStatusDiffBatcherEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(FreshStatusDiffBatcherEnv, self).__init__(yt_stuff, task_cls=FreshStatusDiffBatcher, **resources)

    @property
    def description(self):
        return 'fresh_status_diff_batcher_env'


class EcomExportMergedOffersDumperEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(EcomExportMergedOffersDumperEnv, self).__init__(yt_stuff, task_cls=EcomExportMergedOffersDumper, **resources)

    @property
    def description(self):
        return 'ecom_export_merged_offers_dumper_env'


class DeliveryDiffEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(DeliveryDiffEnv, self).__init__(yt_stuff, task_cls=DeliveryDiff, **resources)

    @property
    def description(self):
        return 'delivery_diff_env'


class ResolvedRedirectTrackerEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(ResolvedRedirectTrackerEnv, self).__init__(yt_stuff, task_cls=ResolvedRedirectTracker, **resources)

    @property
    def description(self):
        return 'resolved_redirect_tracker_env'


class StatsCalcEnv(RoutinesTestEnv):
    def __init__(self, yt_stuff, **resources):
        super(StatsCalcEnv, self).__init__(yt_stuff, task_cls=StatsCalc, **resources)

    @property
    def description(self):
        return 'stats_calc_env'
