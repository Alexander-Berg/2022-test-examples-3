import click
import logging
from market.monetize.stapler.v1.context.context import Context as BaseContext
from market.monetize.stapler.v1.utils.uploader import UploadedFile
from market.monetize.stapler.v1.utils.log import init_logging
from market.assortment.ecom_log.tests.rozhkov_graph.ap.graph_generator import Generator


class Context(BaseContext):
    environment: str = 'testing'
    auto_start: str

    def __init__(self,
                 secret_name=None,
                 tvm_secret_name=None,
                 pool=None,
                 yt_token=None,
                 proxy=None,
                 quota=None,
                 yt_max_concurrent_tasks=None,
                 root=None,
                 tmp=None,
                 keep_going=False,
                 auto_start=None,
                 yql_secret_name=None,
                 environment=None):
        self.environment = environment
        self.auto_start = auto_start
        super().__init__(secret_name, tvm_secret_name, pool, yt_token, proxy, quota, yt_max_concurrent_tasks, root, tmp,
                         yql_secret_name, keep_going)


@click.command('main')
@click.option('--calculator', required=True, help='calculator_app file name')
@click.option('--secret_name', required=True, help='Nirvana secret name with credentials to call Nirvana API')
@click.option('--tvm_secret_name', required=True, help='TVM secret name')
@click.option('--pool', required=True, help='YT pool name', )
@click.option('--quota', required=True, help='Nirvana quota name', default='market-analytics-platform')
@click.option('--yt_token', required=False, help='Nirvana API token', default=None)
@click.option('--yql_secret_name', required=False, help='YQL secret token', default=None)
@click.option('--keep_going/--stop_on_error', required=False, help='Stop on error', default=False)
@click.option('--proxy', required=False, help='YT proxy [hahn, arnold...]', default='hahn', show_default=True)
@click.option('--workflow_guid', required=False, help='Nirvana workflow GUID')
@click.option('--log_level', required=False, help='graph_generator logging level', default='INFO', show_default=True)
@click.option('--calculation_date', required=False, help='calculation date', default='2021-12-25')
@click.option('--environment', required=False, help='production|testng|custom', default='production')
@click.option('--auto_start', required=False, help='Run graph immediately', is_flag=True)
def main(calculator, secret_name, tvm_secret_name, pool, quota, yt_token, yql_secret_name, keep_going,
         proxy, workflow_guid, log_level, calculation_date, environment, auto_start, *args, **kwargs):
    logging.info('RUN graph generator')

    logging.info('*' * 20)
    logging.info(f'calculator = {calculator}')
    logging.info(f'secret_name = {secret_name}')
    logging.info(f'pool = {pool}')
    logging.info(f'quota = {quota}')
    logging.info(f'yt_token is not None = {yt_token is not None}')
    logging.info(f'proxy = {proxy}')
    logging.info(f'workflow_guid = {workflow_guid}')
    logging.info(f'log_level = {log_level}')
    logging.info('*' * 20)

    init_logging(level=log_level)

    ctx = Context(
        secret_name=secret_name,
        tvm_secret_name=tvm_secret_name,
        pool=pool,
        proxy=proxy,
        yt_token=yt_token,
        yql_secret_name=yql_secret_name,
        quota=quota,
        keep_going=keep_going,
        auto_start=auto_start,
        environment=environment,
        root='//home/market/production/analytics_platform/ecom_log_v2/sys'
    )

    main_app = UploadedFile(calculator, 'ecom_log_collector', ctx)

    graph = Generator(
        main_app=main_app,
        ctx=ctx,
        label='Ecom log daily process',
        workflow_guid=workflow_guid,
    )

    graph.define_graph(calculation_date, environment)
    graph.run_graph()


if __name__ == '__main__':
    main(['--help'])
