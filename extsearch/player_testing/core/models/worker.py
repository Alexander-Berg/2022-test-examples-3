import ydb
from time import time, sleep
from util import get_node_fqdn, get_node_dc
import logging
import six


STATE_OFFLINE = 'offline'
STATE_IDLE = 'idle'
STATE_LOCKED = 'locked'
STATE_BUSY = 'busy'


class YDBConnection(object):
    RETRY_COUNT = 10
    RETRY_DELAY = 0.01

    def __init__(self, config):
        driver_config = ydb.DriverConfig(
            config['endpoint'], database=config['database'], credentials=ydb.construct_credentials_from_environ())
        self.driver = ydb.Driver(driver_config)
        self.driver.wait(timeout=5)
        self.session = self.driver.table_client.session().create()

    def execute(self, do_query):
        delay = self.RETRY_DELAY
        for i in range(self.RETRY_COUNT):
            try:
                with self.session.transaction(tx_mode=ydb.SerializableReadWrite()) as tx:
                    return do_query(self.session, tx)
            except Exception as e:
                if isinstance(e, ydb.Aborted):
                    logging.info('YDB transaction aborted, retrying')
                    continue
                reset = isinstance(e, (ydb.BadSession, ydb.ConnectionError, ydb.SessionExpired))
                retry = isinstance(e, (ydb.Timeout, ydb.Overloaded, ydb.Unavailable, ydb.Aborted))
                if reset or retry:
                    logging.info('YDB query retry: {} {}'.format(type(e), e))
                    if retry:
                        sleep(delay)
                        delay *= 2
                    elif reset:
                        self.session = self.driver.table_client.session().create()
                    continue
                logging.info('YDB fatal error: {} {}'.format(type(e), e))
                raise
        raise Exception('YDB retry count exceeded')


class OutOfWorkers(Exception):
    pass


class WorkerBusy(Exception):
    pass


class WorkerNotFound(Exception):
    pass


class WorkerModel(object):
    def __init__(self, db, table):
        self.db = db
        self.table = table
        self.timeout = 1200

    def _get_current_ts(self):
        return int(time() * 10**6)

    def alloc_worker(self):
        def do_query(session, tx):
            query = '''
                DECLARE $timeout as Uint64;
                DECLARE $now as Timestamp;
                DECLARE $loc as String;
                $GetWorkerDc = ($fqdn) -> {
                    $vec = String::SplitToList($fqdn,".");
                    return IF(ListLength($vec)=5, $vec{1}, "unk");
                };
                $WorkerPrio = ($last_access, $fqdn, $dc) -> {
                    RETURN RANDOM($last_access) + IF($GetWorkerDc($fqdn)=$dc, 1.0, 0.0);
                };
                SELECT fqdn,last_access FROM %s  WHERE state='%s'
                    AND DateTime::ToSeconds($now) - DateTime::ToSeconds(last_access) < $timeout
                    ORDER BY $WorkerPrio(last_access, fqdn, $loc) DESC LIMIT 1
            ''' % (self.table, STATE_IDLE)
            prepared = session.prepare(query)
            params = {
                '$timeout': self.timeout,
                '$now': self._get_current_ts(),
                '$loc': bytes(get_node_dc(), 'ascii')
            }
            results = tx.execute(prepared, params, commit_tx=False)
            if len(results[0].rows) != 1:
                tx.rollback()
                raise OutOfWorkers()
            worker = results[0].rows[0].fqdn.decode('ascii')
            self._set_state(session, tx, worker, STATE_LOCKED, stream_id=None, commit_tx=True)
            logging.info('worker {} is LOCKED'.format(worker))
            return worker
        return self.db.execute(do_query)

    def find_worker(self, stream_id):
        def do_query(session, tx):
            query = '''
                DECLARE $stream_id as String;
                SELECT fqdn,last_access FROM {table} WHERE state='{state}' AND stream_id=$stream_id ORDER BY last_access DESC LIMIT 1
            '''.format(table=self.table, state=STATE_BUSY)
            prepared = session.prepare(query)
            results = tx.execute(prepared, {'$stream_id': bytes(stream_id, 'ascii')}, commit_tx=True)
            if len(results[0].rows) != 1:
                return None
            worker = results[0].rows[0].fqdn.decode('ascii')
            logging.info('found shared worker {} for {}'.format(worker, stream_id))
            return worker
        return self.db.execute(do_query)

    def find_by_sid(self, sid, timeout):
        def do_query(session, tx):
            query = '''
                DECLARE $sid as String;
                DECLARE $now as Timestamp;
                DECLARE $timeout as Uint64;
                SELECT fqdn FROM session WHERE sid=$sid AND DateTime::ToSeconds($now) - DateTime::ToSeconds(last_access) < $timeout;
            '''
            prepared = session.prepare(query)
            params = {
                '$sid': bytes(sid, 'ascii'),
                '$now': self._get_current_ts(),
                '$timeout': timeout
            }
            results = tx.execute(prepared, params, commit_tx=True)
            if len(results[0].rows) != 1:
                return None
            worker = results[0].rows[0].fqdn.decode('ascii')
            logging.info('found worker {} for vsid {}'.format(worker, sid))
            return worker
        return self.db.execute(do_query)

    def insert_sid(self, sid, worker):
        if not sid or worker is None:
            return
        def do_query(session, tx):
            query = '''
                DECLARE $sid as String;
                DECLARE $fqdn as String;
                DECLARE $now as Timestamp;
                UPSERT INTO session (sid, fqdn, created, last_access) VALUES ($sid, $fqdn, $now, $now)
            '''
            params = {
                '$sid': bytes(sid, 'ascii'),
                '$fqdn': bytes(worker, 'ascii'),
                '$now': self._get_current_ts()
            }
            prepared = session.prepare(query)
            tx.execute(prepared, params, commit_tx=True)
        return self.db.execute(do_query)

    def update_sid(self, sid):
        def do_query(session, tx):
            query = '''
                DECLARE $sid as String;
                DECLARE $now as Timestamp;
                UPDATE session SET last_access=$now WHERE sid=$sid;
            '''
            prepared = session.prepare(query)
            params = {
                '$sid': bytes(sid, 'ascii'),
                '$now': self._get_current_ts(),
            }
            tx.execute(prepared, params, commit_tx=True)
        return self.db.execute(do_query)

    def _set_state(self, session, tx, worker, state, stream_id, commit_tx):
        query = '''
            DECLARE $worker as String;
            DECLARE $state as String;
            DECLARE $now as Timestamp;
            DECLARE $stream_id as String;
            UPSERT INTO {table} (fqdn, last_access, state, stream_id) VALUES ($worker, $now, $state, $stream_id)
        '''.format(table=self.table)
        prepared = session.prepare(query)
        props = {
            '$worker': bytes(worker, 'ascii'),
            '$state': bytes(state, 'ascii'),
            '$now': self._get_current_ts(),
            '$stream_id': bytes(stream_id if stream_id is not None else '', 'ascii')
        }
        tx.execute(prepared, props, commit_tx=commit_tx)

    def busy_worker(self, stream_id, worker=get_node_fqdn()):
        def do_query(session, tx):
            query = '''
                DECLARE $worker as String;
                SELECT state FROM {table} WHERE fqdn=$worker
            '''.format(table=self.table)
            prepared = session.prepare(query)
            results = tx.execute(prepared, {'$worker': bytes(worker, 'ascii')}, commit_tx=False)
            if len(results[0].rows) != 1:
                tx.rollback()
                raise Exception('worker {} not found'.format(worker))
            if results[0].rows[0].state.decode('ascii') not in [STATE_IDLE, STATE_LOCKED]:
                tx.rollback()
                raise WorkerBusy()
            self._set_state(session, tx, worker, STATE_BUSY, stream_id, commit_tx=True)
            logging.info('worker {} is BUSY with {} '.format(worker, stream_id))
        return self.db.execute(do_query)

    def update_worker(self, worker=get_node_fqdn()):
        def do_query(session, tx):
            query = '''
                DECLARE $worker as String;
                DECLARE $now as Timestamp;
                UPSERT INTO {table} (fqdn, last_access) VALUES ($worker, $now)
            '''.format(table=self.table)
            prepared = session.prepare(query)
            tx.execute(prepared, {'$worker': bytes(worker, 'ascii'), '$now': self._get_current_ts()}, commit_tx=True)
        return self.db.execute(do_query)

    def release_worker(self, worker=get_node_fqdn()):
        def do_query(session, tx):
            self._set_state(session, tx, worker, STATE_IDLE, stream_id=None, commit_tx=True)
            logging.info('worker {} is IDLE'.format(worker))
        return self.db.execute(do_query)

    def offline_worker(self, worker=get_node_fqdn()):
        def do_query(session, tx):
            self._set_state(session, tx, worker, STATE_OFFLINE, stream_id=None, commit_tx=True)
            logging.info('worker {} is OFFLINE'.format(worker))
        return self.db.execute(do_query)

    def stream_stats(self):
        def do_query(session, tx):
            try:
                query = '''
                    DECLARE $now as Timestamp;
                    SELECT COUNT(*) as cnt, stream_id FROM %s WHERE state='%s' AND DateTime::ToSeconds($now) - DateTime::ToSeconds(last_access) < 300 GROUP BY stream_id
                ''' % (self.table, STATE_BUSY)
                results = tx.execute(session.prepare(query), {'$now': self._get_current_ts()}, commit_tx=False)
                return [(row.stream_id.decode('ascii'), row.cnt) for row in results[0].rows]
            finally:
                tx.commit()
        n_tries = 20
        sleep_time = 3
        for i in range(n_tries):
            try:
                return self.db.execute(do_query)
            except:
                logging.info('attemp {} after sleep {} sec'.format(i, sleep_time))
                sleep(sleep_time)
        raise Exception('failed after {} tries'.format(n_tries))
