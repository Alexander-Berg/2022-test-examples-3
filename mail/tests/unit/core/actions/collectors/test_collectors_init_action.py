import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains, equal_to, has_entries, has_properties, match_equality

from mail.ipa.ipa.core.actions.collectors.init import InitCollectorAction
from mail.ipa.ipa.core.actions.collectors.remove import RemoveCollectorAction
from mail.ipa.ipa.core.entities.enums import TaskType
from mail.ipa.ipa.core.entities.import_params import ImportParams
from mail.ipa.ipa.core.exceptions import BaseCoreError, UnknownCollectorFoundError
from mail.ipa.ipa.interactions.yarm import YarmClient
from mail.ipa.ipa.interactions.yarm.entities import YarmCollectorState
from mail.ipa.ipa.interactions.yarm.exceptions import YarmBaseError, YarmDuplicateError


@pytest.fixture
def user(existing_user):
    return existing_user


@pytest.fixture
def action(general_init_import_params, user, dir_user, user_info, src_login):
    return InitCollectorAction(
        user=user,
        directory_user=dir_user,
        src_password=user_info.new_password or user_info.password,
        src_login=src_login,
        import_params=general_init_import_params,
    )


class HandleContract:
    @pytest.fixture(autouse=True)
    async def persistent_collectors(self, create_collector, storage, user):
        await create_collector(user_id=user.user_id)
        await create_collector(user_id=user.user_id)
        return await alist(storage.collector.find(user_id=user.user_id))

    @pytest.fixture
    def collector(self, persistent_collectors):
        return persistent_collectors[0]

    @pytest.fixture(autouse=True)
    def mock_get_collector_stub(self, mocker, coromock, collector):
        return mocker.patch.object(InitCollectorAction,
                                   '_get_collector_stub',
                                   coromock(collector))

    @pytest.fixture(autouse=True)
    def mock_create_collector_in_yarm(self, mocker, coromock):
        return mocker.patch.object(InitCollectorAction, '_create_collector_in_yarm', coromock(mocker.Mock()))

    @pytest.fixture(autouse=True)
    def mock_on_duplicate_collector(self, mocker, coromock):
        return mocker.patch.object(InitCollectorAction, '_on_duplicate_collector', coromock(mocker.Mock()))

    def test_calls_get_collector_stub(self, mock_get_collector_stub, persistent_collectors):
        mock_get_collector_stub.assert_called_once_with(
            match_equality(
                contains(*[
                    has_properties({'collector_id': collector.collector_id}) for collector in persistent_collectors
                ])
            )
        )

    @pytest.mark.asyncio
    async def test_calls_create_collector_in_yarm(self, mock_create_collector_in_yarm, mock_get_collector_stub):
        mock_create_collector_in_yarm.assert_called_once_with(await mock_get_collector_stub())


class TestHandle(HandleContract):
    @pytest.fixture(autouse=True)
    async def returned(self, action):
        return await action.run()


class TestHandleWhenDuplicateCollector(HandleContract):
    @pytest.fixture(autouse=True)
    def mock_create_collector_in_yarm(self, mocker, coromock):
        exc = YarmDuplicateError(200,
                                 code='duplicate error',
                                 message='duplicate error',
                                 service=None,
                                 method=None,
                                 )

        return mocker.patch.object(InitCollectorAction, '_create_collector_in_yarm', coromock(exc=exc))

    @pytest.fixture(autouse=True)
    async def returned(self, action):
        return await action.run()

    @pytest.mark.asyncio
    async def test_calls_on_duplicate_error(self,
                                            mock_create_collector_in_yarm,
                                            mock_on_duplicate_collector,
                                            mock_get_collector_stub,
                                            persistent_collectors):
        mock_on_duplicate_collector.assert_called_once_with(
            await mock_get_collector_stub(),
            persistent_collectors,
        )


class TestHandleOnException(HandleContract):
    @pytest.fixture
    def exc(self):
        return YarmBaseError(status=200,
                             code='custom_error_code',
                             service='yarm',
                             method='create',
                             )

    @pytest.fixture(autouse=True)
    def mock_create_collector_in_yarm(self, mocker, coromock, exc):
        return mocker.patch.object(InitCollectorAction,
                                   '_create_collector_in_yarm',
                                   coromock(exc=exc))

    @pytest.fixture(autouse=True)
    async def returned(self, action):
        with pytest.raises(BaseCoreError) as exc_info:
            await action.run()

        return exc_info

    @pytest.mark.asyncio
    async def test_error_persisted(self, exc, storage, collector):
        collector = await storage.collector.get(collector.collector_id)
        assert_that(collector.status, equal_to(exc.CODE))

    @pytest.mark.asyncio
    async def test_raises_exc(self, exc, returned, collector):
        assert_that(returned.value.__cause__, equal_to(exc))


class TestGetCollectorStub:
    @pytest.fixture
    def import_params(self, general_init_import_params, src_login):
        return ImportParams(
            src_login=src_login,
            server=general_init_import_params.server,
            port=general_init_import_params.port,
            ssl=general_init_import_params.ssl,
            imap=general_init_import_params.imap,
            mark_archive_read=general_init_import_params.mark_archive_read,
            delete_msgs=general_init_import_params.delete_msgs,
        )

    @pytest.fixture
    async def existing_stub(self, create_collector, user):
        return await create_collector(user_id=user.user_id)

    @pytest.fixture
    async def alien_stub(self, storage, create_collector, user):
        collector = await create_collector(user_id=user.user_id)
        collector.params.port += 1
        return await storage.collector.save(collector)

    @pytest.mark.asyncio
    async def test_returns_existing_stub_if_any(self, action, existing_stub, storage):
        async with action.storage_setter():
            assert_that(
                await action._get_collector_stub([existing_stub]),
                equal_to(existing_stub),
            )

    @pytest.mark.asyncio
    async def test_returns_new(self, action, storage, user, alien_stub, import_params):
        async with action.storage_setter():
            assert_that(
                await action._get_collector_stub([alien_stub]),
                has_properties({
                    'user_id': user.user_id,
                    'params': import_params,
                })
            )


class TestCreateCollectorInYarm:
    @pytest.fixture(autouse=True)
    def mock_yarm(self, mocker, coromock, pop_id):
        return mocker.patch.object(YarmClient, 'create', coromock(pop_id))

    @pytest.fixture
    async def collector(self, create_collector, user):
        return await create_collector(user_id=user.user_id)

    @pytest.fixture(autouse=True)
    async def returned(self, action, collector):
        async with action.storage_setter():
            return await action._create_collector_in_yarm(collector),

    def test_calls_yarm(self, mock_yarm, action, user, dir_user):
        mock_yarm.assert_called_once_with(src_login=action.src_login,
                                          password=action.src_password.value(),
                                          user_ip=action.import_params.user_ip,
                                          suid=user.suid,
                                          dst_email=dir_user.email,
                                          params=action.import_params,
                                          )

    @pytest.mark.asyncio
    async def test_collector(self, storage, collector, pop_id):
        updated_collector = await storage.collector.get(collector.collector_id)
        assert_that(updated_collector.pop_id, equal_to(pop_id))


class OnDuplicateCollectorContract:
    @pytest.fixture
    def collector(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def mock_remove(self, mock_action):
        return mock_action(RemoveCollectorAction)

    @pytest.fixture(autouse=True)
    def mock_create_collector_in_yarm(self, mocker, coromock):
        return mocker.patch.object(InitCollectorAction, '_create_collector_in_yarm', coromock())

    @pytest.fixture(autouse=True)
    def mock_status(self, mocker, coromock, status):
        return mocker.patch.object(YarmClient, 'status', coromock(mocker.Mock(**status)))

    @pytest.fixture
    def yarm_collectors_entities(self, mocker, yarm_collectors):
        return [mocker.Mock(**collector) for collector in yarm_collectors]

    @pytest.fixture(autouse=True)
    def mock_list(self, mocker, coromock, yarm_collectors_entities):
        async def gen(*args, **kwargs):
            for c in yarm_collectors_entities:
                yield c

        return mocker.patch.object(YarmClient, 'list', mocker.Mock(side_effect=gen))

    @pytest.fixture
    def persistent_collectors(self, yarm_collectors_entities):
        return yarm_collectors_entities

    @pytest.fixture(autouse=True)
    async def returned_coro(self, action, collector, persistent_collectors):
        return action._on_duplicate_collector(collector, persistent_collectors)

    def test_list_call(self, mock_list, user):
        mock_list.assert_called_once_with(suid=user.suid)


class OnDuplicateCollectorIsSingleAndEmpty(OnDuplicateCollectorContract):
    @pytest.fixture(autouse=True)
    async def returned(self, action, returned_coro, yarm_collectors, status):
        async with action.storage_setter():
            return await returned_coro

    def test_calls_status(self, mock_status, yarm_collectors_entities):
        mock_status.assert_called_once_with(yarm_collectors_entities[0].pop_id)


SHOULD_RECREATE_CASES = (
    ([{'pop_id': '1', 'error_status': 'bad'}], {'collected': None}),
)

SHOULD_NOT_RECREATE_CASES = (
    pytest.param([{'pop_id': '1', 'error_status': 'ok'}], {'collected': None}, id='error-ok'),
    pytest.param([{'pop_id': '1', 'error_status': 'bad'}], {'collected': 0}, id='error-bad-but-has-collected'),
)


@pytest.mark.parametrize('yarm_collectors, status', SHOULD_RECREATE_CASES)
class TestOnDuplicateCollectorIsSingleAndEmptyShouldRecreate(OnDuplicateCollectorIsSingleAndEmpty):
    def test_calls_remove_collector(self, mock_remove, persistent_collectors, user):
        mock_remove.assert_called_once_with(collector=persistent_collectors[0])

    def test_calls_create_collector(self, mock_create_collector_in_yarm, collector):
        mock_create_collector_in_yarm.assert_called_once_with(collector)


@pytest.mark.parametrize('yarm_collectors, status', SHOULD_NOT_RECREATE_CASES)
class TestOnDuplicateCollectorIsSingleAndEmptyShouldNotRecreate(OnDuplicateCollectorIsSingleAndEmpty):
    def test_calls_remove_to_delete_stub(self, mock_remove, collector):
        mock_remove.assert_called_once_with(collector=collector)

    def test_calls_create_collector(self, mock_create_collector_in_yarm):
        mock_create_collector_in_yarm.assert_not_called()


SAME_PARAMS = 'same-params'
TARGET_COLLECTOR_ID = 1


class OnDuplicateCollectorRest(OnDuplicateCollectorContract):
    @pytest.fixture(autouse=True)
    def mock_get_import_params(self, action, mocker, yarm_collectors):
        return mocker.patch.object(action, '_get_collector_import_params', mocker.Mock(return_value=SAME_PARAMS))

    @pytest.fixture
    def status(self):
        return {'collected': 0}

    @pytest.fixture
    def src_login(self):
        return 'src_login'

    @pytest.fixture
    async def edit_tasks(self, storage, user):
        return await alist(storage.task.find(filters={'task_type': TaskType.EDIT_COLLECTOR, 'entity_id': user.user_id}))

    @pytest.fixture(autouse=True)
    async def returned(self, action, returned_coro, yarm_collectors, status):
        async with action.storage_setter():
            return await returned_coro

    def test_calls_remove_to_delete_stub(self, mock_remove, collector):
        mock_remove.assert_called_once_with(collector=collector)


SHOULD_EDIT_CASES = (
    [{
        'pop_id': '1',
        'state': YarmCollectorState.TEMPORARY_ERROR,
        'collector_id': TARGET_COLLECTOR_ID,
        'params': SAME_PARAMS,
    }],
    [
        {
            'pop_id': '2',
            'state': YarmCollectorState.TEMPORARY_ERROR,
            'collector_id': TARGET_COLLECTOR_ID + 1,
            'params': 'not' + SAME_PARAMS},
        {
            'pop_id': '1',
            'state': YarmCollectorState.TEMPORARY_ERROR,
            'collector_id': TARGET_COLLECTOR_ID,
            'params': SAME_PARAMS
        },
    ],
)

SHOULD_NOT_EDIT_CASES = (
    [{'pop_id': '1', 'state': YarmCollectorState.TEMPORARY_ERROR, 'params': 'not' + SAME_PARAMS}],
    [{'pop_id': '1', 'state': YarmCollectorState.ON, 'params': SAME_PARAMS}],
)


@pytest.mark.parametrize('yarm_collectors', SHOULD_NOT_EDIT_CASES)
class TestOnDuplicateCollectorRestShouldNotEdit(OnDuplicateCollectorRest):
    @pytest.mark.asyncio
    async def test_not_calls_edit(self, edit_tasks):
        assert_that(edit_tasks, equal_to([]))


@pytest.mark.parametrize('yarm_collectors', SHOULD_EDIT_CASES)
class TestOnDuplicateCollectorRestShouldEdit(OnDuplicateCollectorRest):
    def test_calls_edit(self, edit_tasks):
        assert_that(
            edit_tasks,
            contains(
                has_properties({
                    'params': has_entries({
                        'collector_id': TARGET_COLLECTOR_ID
                    })
                })
            ),
        )


@pytest.mark.parametrize('yarm_collectors', (
    [{}], [{}, {}],
))
class TestShouldFailOnUnknownCollector(OnDuplicateCollectorContract):
    @pytest.fixture
    def status(self):
        return {'collected': None}

    @pytest.fixture
    def persistent_collectors(self):
        return []

    @pytest.fixture(autouse=True)
    async def returned(self, action, returned_coro, yarm_collectors):
        with pytest.raises(UnknownCollectorFoundError) as exc_info:
            async with action.storage_setter():
                return await returned_coro
        return exc_info

    def test_raises(self, returned):
        assert isinstance(returned.value, UnknownCollectorFoundError)
