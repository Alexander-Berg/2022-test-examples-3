import psycopg2
import threading

from pycommon import *
from subscriber import *

test_gid = 7
test_uid = str(test_gid)
max_gid = 65535

queue_timeout = 1.0
migration_timeout = 2.0

resharddb_dev_conninfo = '''
    host=localhost
    port=8432
    dbname=xivadb
    user=xiva_user
    password=xiva_password
    target_session_attrs=read-write
'''

def prepare_migration(hub, gid, role):
    return hub.raw().post("/resharding/xtable/prepare_migration", {'gid': gid, 'role': role})

def start_migration(hub, gid, role, reaction = 'delay'):
    return hub.raw().post("/resharding/xtable/start_migration", {'gid': gid, 'role': role, 'request_reaction': reaction})

def finalize_migration(hub, gid, role):
    return hub.raw().post("/resharding/xtable/finalize_migration", {'gid': gid, 'role': role})

def abort_migration(hub, gid, role):
    return hub.raw().post("/resharding/xtable/abort_migration", {'gid': gid, 'role': role})

class MigrationStorage:
    def __init__(self, conninfo):
        self.connection = psycopg2.connect(conninfo)
        self.connection.set_isolation_level(0)
        self.cursor = self.connection.cursor()
        self.strings = [
            'pending',
            'ready',
            'inprogress',
            'finished'
        ]
        self.states = { self.strings[i]: i for i in range(0, len(self.strings)) }

    def get(self):
        self.cursor.execute('select state, gid_range from resharding.migrations order by gid_range;')
        return [(self.strings[s[0]], s[1]) for s in self.cursor.fetchall()]

    def get_state(self, gid):
        self.cursor.execute('select state from resharding.migrations where gid_range @> %s::bigint', (gid,))
        return self.strings[self.cursor.fetchall()[0][0]]

    def set_state(self, gid, state):
        self.cursor.execute('select resharding.set_migration_state(%s,%s,%s);',
            (gid, [0, 1, 2, 3], self.states[state]))

    def reset(self):
        self.cursor.execute('delete from resharding.migrations;')
        self.cursor.execute('''
            insert into resharding.migrations(gid_range, state)
            values (int8range(0, 65536), 0);''')

    def close(self):
        self.connection.close()

class Shard:
    def __init__(self, conninfo):
        self.connection = psycopg2.connect(conninfo)
        self.connection.set_isolation_level(0)
        self.cursor = self.connection.cursor()

    def close(self):
        self.connection.close()

    def list(self, uid, service):
        bound_sql = self.cursor.mogrify('''
            select id, callback from xiva.subscriptions
                where uid = %s and service = %s
        ''', (uid, service))
        self.cursor.execute(bound_sql)
        return [{'id': r[0], 'callback': r[1]} for r in self.cursor.fetchall()]

    def unsubscribe_all(self, uid, service):
        bound_sql = self.cursor.mogrify('''
            delete from xiva.subscriptions
                where uid = %s and service = %s
        ''', (uid, service))
        self.cursor.execute(bound_sql)

def shard_for(gid, shards):
    conninfo = next(s['master'] for s in shards['shards']
        if s['start_gid'] <= gid and gid <= s['end_gid'])
    conninfo += ' connect_timeout=5'
    return Shard(conninfo)

def old_shard_for(hub, gid):
    return shard_for(gid, hub.resharding_status()['old_shards'])

def new_shard_for(hub, gid):
    return shard_for(gid, hub.resharding_status()['new_shards'])

class FakeHubs:
    @classmethod
    def setup_hubs(cls):
        hub1 = Client(Testing.host(), Testing.port())
        hub2 = Client(Testing.host(), Testing.port2())

        if hub1.is_control_leader():
            cls.master = hub1
            cls.slave = hub2
        elif hub2.is_control_leader():
            cls.master = hub2
            cls.slave = hub1
        else:
            raise Exception('no control leader found')

        cls.migration_storage = MigrationStorage(resharddb_dev_conninfo)

    @classmethod
    def teardown_hubs(cls):
        cls.migration_storage.reset()

class TestXtableReshardingControl(FakeHubs):
    @classmethod
    def setup_class(cls):
        cls.setup_hubs()

    @classmethod
    def teardown_class(cls):
        cls.teardown_hubs()

    def setup(self):
        self.migration_storage.reset();
        abort_migration(self.master, test_gid, 'master')

    def check_state_transition(self, hub, role, source_state, method, target_state, expected_status):
        # finalize_migration and abort_migration on master require queue setup
        if hub == self.master and role == 'master' and source_state == 'inprogress' and (target_state == 'finished' or target_state == 'pending'):
            hub.prepare_migration(test_gid, role)
            hub.start_migration(test_gid, role)
        else:
            self.migration_storage.set_state(test_gid, source_state)

        assert_equals(method(hub, test_gid, role).status, expected_status)

        state_after = self.migration_storage.get_state(test_gid)
        assert_equals(state_after, target_state)

    def test_master_migration_control(self):
        cases = [
            ['pending', prepare_migration, 'ready', 200],
            ['pending', start_migration, 'pending', 409],
            ['pending', finalize_migration, 'pending', 409],
            ['pending', abort_migration, 'pending', 409],
            ['ready', prepare_migration, 'ready', 409],
            ['ready', start_migration, 'inprogress', 200],
            ['ready', finalize_migration, 'ready', 409],
            ['ready', abort_migration, 'pending', 200],
            ['inprogress', prepare_migration, 'inprogress', 409],
            ['inprogress', start_migration, 'inprogress', 409],
            ['inprogress', finalize_migration, 'finished', 200],
            ['inprogress', abort_migration, 'pending', 200],
            ['finished', prepare_migration, 'finished', 409],
            ['finished', start_migration, 'finished', 409],
            ['finished', finalize_migration, 'finished', 409],
            ['finished', abort_migration, 'finished', 409],
        ]
        bad_role_cases = [ [c[0], c[1], c[0], 409] for c in cases ]
        for c in cases:
            yield self.check_state_transition, self.master, 'master', c[0], c[1], c[2], c[3]
        for c in bad_role_cases:
            yield self.check_state_transition, self.master, 'slave', c[0], c[1], c[2], c[3]

    # Slave is not supposed to change migration state, but is supposed
    # to read migrations from db and return based on read success.
    def test_slave_migration_control(self):
        cases = [
            ['pending', prepare_migration, 'pending', 200],
            ['pending', start_migration, 'pending', 200],
            ['pending', finalize_migration, 'pending', 200],
            ['pending', abort_migration, 'pending', 200],
            ['ready', prepare_migration, 'ready', 200],
            ['ready', start_migration, 'ready', 200],
            ['ready', finalize_migration, 'ready', 200],
            ['ready', abort_migration, 'ready', 200],
            ['inprogress', prepare_migration, 'inprogress', 200],
            ['inprogress', start_migration, 'inprogress', 200],
            ['inprogress', finalize_migration, 'inprogress', 200],
            ['inprogress', abort_migration, 'inprogress', 200],
            ['finished', prepare_migration, 'finished', 200],
            ['finished', start_migration, 'finished', 200],
            ['finished', finalize_migration, 'finished', 200],
            ['finished', abort_migration, 'finished', 200],
        ]
        bad_role_cases = [ [c[0], c[1], c[0], 409] for c in cases ]
        for c in cases:
            yield self.check_state_transition, self.slave, 'slave', c[0], c[1], c[2], c[3]
        for c in bad_role_cases:
            yield self.check_state_transition, self.slave, 'master', c[0], c[1], c[2], c[3]

class AsyncSubscriber(threading.Thread):
    def __init__(self, host, port):
        threading.Thread.__init__(self)
        self.error = None
        self.daemon = True
        self.hub = Client(host, port)

    def run(self):
        try:
            self.hub.subscribe(test_uid, 'fake', 'http://localhost/fake')
        except Exception as e:
            self.error = e

class FakeMaster:
    def __init__(self, port):
        self.impl = fake_server(host="::", port=port, start=False)
        self.url = 'http://' + socket.gethostname() + ':' + str(port) + '/'
        self.paths = []
        self.queries = []

    def set_response(self, code=200, response=None, raw_response=None):
        self.impl.response_code = code
        self.impl.set_response(response, raw_response)

    def start(self):
        self.impl.set_request_hook(self.handle_request)
        self.impl.start();

    def stop(self):
        self.impl.fini();

    def handle_request(self, req):
        self.paths.append(req.path)
        self.queries.append(msgpack.unpackb(req.body))

class TestXtableReshardingQueryRedirection(FakeHubs):
    @classmethod
    def setup_class(cls):
        cls.setup_hubs()
        cls.old_shard = old_shard_for(cls.master, test_gid)
        cls.new_shard = new_shard_for(cls.master, test_gid)
        cls.fake_master = FakeMaster(16080)
        cls.fake_master.start()

    @classmethod
    def teardown_class(cls):
        cls.fake_master.stop()
        cls.old_shard.close()
        cls.new_shard.close()
        cls.teardown_hubs()

    def setup(self):
        self.old_shard.unsubscribe_all(test_uid, 'fake')
        self.new_shard.unsubscribe_all(test_uid, 'fake')
        self.migration_storage.reset()
        self.fake_master.paths = []
        self.fake_master.queries = []

    def assert_subscribe_have_not_happened(self):
        assert_equals(len(self.old_shard.list(test_uid, 'fake')), 0)
        assert_equals(len(self.new_shard.list(test_uid, 'fake')), 0)

    def assert_subscribe_happened_on_old_shard(self):
        assert_equals(len(self.old_shard.list(test_uid, 'fake')), 1)
        assert_equals(len(self.new_shard.list(test_uid, 'fake')), 0)

    def assert_subscribe_happened_on_new_shard(self):
        assert_equals(len(self.old_shard.list(test_uid, 'fake')), 0)
        assert_equals(len(self.new_shard.list(test_uid, 'fake')), 1)

    def test_master_pending_query_handling(self):
        # to update cached migration storage state
        self.master.resharding_status()
        self.master.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_happened_on_old_shard()

    def test_master_ready_query_handling(self):
        self.master.prepare_migration(test_gid, 'master')
        self.master.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_happened_on_old_shard()

    def test_master_inprogress_query_handling(self):
        self.master.prepare_migration(test_gid, 'master')
        self.master.start_migration(test_gid, 'master')

        subscriber = AsyncSubscriber(self.master.host(), self.master.port())
        subscriber.start()

        # emulate the time it takes to copy subscriptions
        time.sleep(queue_timeout / 2)

        self.assert_subscribe_have_not_happened()

        self.master.finalize_migration(test_gid, 'master')
        subscriber.join(1.0)
        assert_false(subscriber.isAlive())
        if (subscriber.error is not None):
            raise subscriber.error

        self.assert_subscribe_happened_on_new_shard()

    def test_master_finished_query_handling(self):
        self.master.prepare_migration(test_gid, 'master')
        self.master.start_migration(test_gid, 'master')
        self.master.finalize_migration(test_gid, 'master')

        self.master.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_happened_on_new_shard()

    def test_slave_pending_query_handling(self):
        self.migration_storage.set_state(test_gid, 'pending')
        self.slave.resharding_status()

        self.slave.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_happened_on_old_shard()

    def test_slave_ready_query_handling(self):
        self.migration_storage.set_state(test_gid, 'ready')
        self.slave.prepare_migration(test_gid, 'slave')

        self.fake_master.set_response(code=200, raw_response=msgpack.packb([int(0)]))

        self.slave.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_have_not_happened() # because it got redirected to fake master

        assert_equals(len(self.fake_master.paths), 1)
        assert_equals(self.fake_master.paths[0], '/resharding/xtable/execute_query?type=subscribe')
        assert_equals(len(self.fake_master.queries), 1)
        # not validating query args here, since it's unmaintainable

    def test_slave_inprogress_query_handling(self):
        self.migration_storage.set_state(test_gid, 'inprogress')
        self.slave.start_migration(test_gid, 'slave')

        self.fake_master.set_response(code=200, raw_response=msgpack.packb([int(0)]))

        self.slave.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_have_not_happened() # because it got redirected to fake master

        assert_equals(len(self.fake_master.paths), 1)
        assert_equals(self.fake_master.paths[0], '/resharding/xtable/execute_query?type=subscribe')
        assert_equals(len(self.fake_master.queries), 1)
        # not validating query args here, since it's unmaintainable

    def test_slave_finished_query_handling(self):
        self.migration_storage.set_state(test_gid, 'finished')
        self.slave.finalize_migration(test_gid, 'slave')

        self.slave.subscribe(test_uid, 'fake', 'http://localhost/fake')
        self.assert_subscribe_happened_on_new_shard()

    def test_master_flushes_queries_on_abort(self):
        self.master.prepare_migration(test_gid, 'master')
        self.master.start_migration(test_gid, 'master')

        subscriber = AsyncSubscriber(self.master.host(), self.master.port())
        subscriber.start()

        # emulate the time it takes to copy subscriptions
        time.sleep(queue_timeout / 2)

        self.assert_subscribe_have_not_happened()

        self.master.abort_migration(test_gid, 'master')
        subscriber.join(1.0)
        assert_false(subscriber.isAlive())
        if (subscriber.error is not None):
            raise subscriber.error

        self.assert_subscribe_happened_on_old_shard()

    def test_master_query_queue_timeout(self):
        self.master.prepare_migration(test_gid, 'master')
        self.master.start_migration(test_gid, 'master')

        subscriber = AsyncSubscriber(self.master.host(), self.master.port())
        subscriber.start()

        time.sleep(queue_timeout + 0.5)

        self.assert_subscribe_happened_on_old_shard()
        assert_equals(self.migration_storage.get_state(test_gid), 'pending')

        subscriber.join(1.0)
        assert_false(subscriber.isAlive())
        if (subscriber.error is not None):
            raise subscriber.error

    def test_master_migration_timeout(self):
        self.master.prepare_migration(test_gid, 'master')
        self.master.start_migration(test_gid, 'master')

        assert_equals(self.migration_storage.get_state(test_gid), 'inprogress')

        time.sleep(migration_timeout + 0.5)

        assert_equals(self.migration_storage.get_state(test_gid), 'pending')
