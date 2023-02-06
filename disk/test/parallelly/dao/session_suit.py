# -*- coding: utf-8 -*-
from mock import patch
from hamcrest import assert_that, calling, raises
from psycopg2 import extensions, OperationalError

from mpfs.metastorage.postgres.exceptions import SetAutocommitError, EofDetectedError
from mpfs.metastorage.postgres.query_executer import ReadPreference as PGReadPreference
from mpfs.metastorage.postgres.services import SlavesNotFoundError
from test.parallelly.dao.base import BaseMigrationTestCase, BasePostgresTestCase
from test.base import PostgresUnits
from mpfs.dao.session import Session


class DocsDAOTestCase(BaseMigrationTestCase):
    SELECT_QUERY = """SELECT table_name
                      FROM information_schema.tables
                      WHERE table_schema='disk';"""

    def test_set_autocommit_if_transaction_opened(self):
        """Проверяем rollback потерянной транзакции.

        Если при создании новой транзакции видим, что еще существует другая, но в списке известных транзакций ее нет,
        то должны откатить изменения из этой транзакции и нормально начать новую.
        """
        session = Session.create_from_shard_id(PostgresUnits.UNIT1)

        # Сначала у нас проблемы с шардом, где живет юзер и получим ошибку при попытке коннекта
        with patch('sqlalchemy.engine.base.Connection.begin',
                   side_effect=OperationalError("Connection refused")):
            assert_that(calling(session.begin), raises(OperationalError))

        assert not session._conn.connection.connection.autocommit, u"Ожидаем не сброшенный в autocommit=True, т.к этого фикса нет еще"

        # Выполняем SELECT-запрос, который не закоммитится и следующее начало сессии будет с ошибкой
        session.execute(self.SELECT_QUERY)

        # Тут раньше мы получали ошибку:
        # ProgrammingError: autocommit cannot be used inside a transaction
        # теперь обрабатываем ее: закрываем транзакцию и бросаем тоже исключение
        # (чтобы проинформировать что возникла эта ошибка)
        assert_that(calling(session.begin), raises(SetAutocommitError))
        assert session._pg_connection.connection.connection.get_transaction_status() == extensions.TRANSACTION_STATUS_IDLE
        # И дальше будем без ошибок начинать подготовку к новой транзакции
        session.begin()

    def test_correct_execute_for_session_created_with_connect(self):
        """
        Проверяем кейс, когда сессия была создана с указанием коннекта, без shard_id, в этом случае, при возникновении
        ошибки `SSL SYSCALL error: EOF detected` в методе execute, мы должны корректно закрывать коннект и не ретраить
        запрос, а прокинуть ошибку наружу
        """
        sessions = Session.create_for_all_shards()
        assert sessions
        session = sessions[0]
        with patch('sqlalchemy.engine.base.Connection.execute', side_effect=EofDetectedError("Connection refused")):
            assert_that(calling(session.execute).with_args('SELECT 1'), raises(EofDetectedError))


class SessionTestCase(BasePostgresTestCase):
    SELECT_QUERY = "SELECT 1;"

    def teardown_method(self, method):
        super(SessionTestCase, self).teardown_method(method)
        Session.clear_cache()

    def test_read_preference_in_create(self):
        """
        Проверяем кейс, когда сессия создана с read_preference Secondary.
        В этом случае, соединение должно быть установлено в реплику
        """
        cache_before_creation = {i for j in Session._shard_id_cache.values() for i in j.values()}
        session = Session.create_from_shard_id(PostgresUnits.UNIT1, read_preference=PGReadPreference.secondary)
        assert session._read_preference == PGReadPreference.secondary
        assert_that(calling(session.begin), raises(SlavesNotFoundError))
        cache_after_creation = {i for j in Session._shard_id_cache.values() for i in j.values()}
        assert cache_after_creation.issuperset(cache_before_creation)

    def test_read_preference_when_primary_connected(self):
        """
        Проверяем кейс, когда сессия создана с read_preference Primary, а потом берется новая в тот же шард с Secondary
        В этом случае, соединение должно быть установлено в реплику
        """
        cache_before_creation = {i for j in Session._shard_id_cache.values() for i in j.values()}
        session = Session.create_from_shard_id(PostgresUnits.UNIT1, read_preference=PGReadPreference.primary)
        assert session._read_preference == PGReadPreference.primary
        assert session.execute(self.SELECT_QUERY)
        assert cache_before_creation == {i for j in Session._shard_id_cache.values() for i in j.values()}

        session = Session.create_from_shard_id(PostgresUnits.UNIT1, read_preference=PGReadPreference.secondary)
        assert session._read_preference == PGReadPreference.secondary
        assert_that(calling(session.begin), raises(SlavesNotFoundError))

    def test_read_preference_when_secondary_connected(self):
        """
        Проверяем кейс, когда сессия создана с read_preference Secondary, а потом берется новая в тот же шард с Primary
        В этом случае, соединение должно быть установлено в праймари
        """
        session = Session.create_from_shard_id(PostgresUnits.UNIT1, read_preference=PGReadPreference.secondary)
        assert session._read_preference == PGReadPreference.secondary
        assert_that(calling(session.begin), raises(SlavesNotFoundError))

        session = Session.create_from_shard_id(PostgresUnits.UNIT1, read_preference=PGReadPreference.primary)
        assert session._read_preference == PGReadPreference.primary
        assert session.execute(self.SELECT_QUERY)
