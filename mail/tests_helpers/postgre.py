import psycopg2
import retrying
import yatest.common

work_dir = yatest.common.work_path()


def start_db(container_name, db_password):
    cmd = ['docker',
           'run',
           '-p',
           '12000:12000',
           '-e OPT_pgbouncer_listen_port=12000',
           '-e OPT_db_user=sherlock',
           '-e OPT_db_grants=SUPERUSER',
           '-e OPT_db_passwd={}'.format(db_password),
           '-e OPT_db_name=sherlockdb',
           '--name', container_name,
           '-t',
           '-d',
           '-i',
           'registry.yandex.net/dbaas/minipgaas']
    return yatest.common.execute(cmd, shell=True, wait=True, cwd=work_dir)


@retrying.retry(
    retry_on_exception=lambda e: isinstance(e, psycopg2.DatabaseError),
    wait_exponential_multiplier=1000, wait_exponential_max=10000,
)
def wait_db(dsn):
    with psycopg2.connect(dsn) as conn:
        conn.cursor().execute('SELECT 1')


def init_db(dsn, arc_init_path):
    sherlockdb_init = yatest.common.source_path(arc_init_path)
    with psycopg2.connect(dsn) as conn:
        with open(sherlockdb_init) as fd:
            conn.cursor().execute(fd.read())
