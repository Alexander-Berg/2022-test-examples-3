import pytest

from mail.ciao.ciao.core.entities.missing import MissingType
from mail.ciao.ciao.core.exceptions import CoreIrrelevantScenarioError
from mail.ciao.ciao.core.scenarios.base import BaseScenario


@pytest.fixture
def slot_key():
    return 'slot_key'


@pytest.fixture
def slot_value():
    return 'slot_value'


class FinalScenario(BaseScenario):
    def _get_params(self):
        pass

    async def handle(self):
        pass


def test_requires__get_params_implementation():
    class Scenario(BaseScenario):
        async def handle(self):
            pass

    with pytest.raises(TypeError):
        Scenario()


def test_requires_handle_implementation():
    class Scenario(BaseScenario):
        def _get_params(self):
            pass

    with pytest.raises(TypeError):
        Scenario()


@pytest.mark.parametrize('_get_params_result', (
    {'frame_name': 'some value'},
    {'slots': 'some value'},
    {'commit': True},
    {'frame_name': 1, 'slots': 2},
    {'frame_name': 1, 'slots': 2, 'commit': False},
))
def test_get_params_asserts(mocker, _get_params_result):
    mocker.patch.object(FinalScenario, '_get_params', mocker.Mock(return_value=_get_params_result))
    with pytest.raises(AssertionError):
        FinalScenario().get_params()


class BaseTestSlotAccess:
    @pytest.fixture
    def scenario(self, slot_key, slot_value):
        return FinalScenario(slots={slot_key: slot_value})

    @pytest.fixture
    def ok_kwargs(self, slot_key):
        return dict(slot_name=slot_key, slot_type=str)

    @pytest.fixture
    def wrong_type_kwargs(self, slot_key):
        return dict(slot_name=slot_key, slot_type=int)

    @pytest.fixture
    def missing_key_kwargs(self, slot_key):
        return dict(slot_name='some_other_key', slot_type=str)


class TestGetSlot(BaseTestSlotAccess):
    def test_ok(self, slot_value, scenario, ok_kwargs):
        assert scenario.get_slot(**ok_kwargs) == slot_value

    def test_wrong_type(self, scenario, wrong_type_kwargs):
        assert scenario.get_slot(**wrong_type_kwargs) is MissingType.MISSING

    def test_missing_key(self, scenario, missing_key_kwargs):
        assert scenario.get_slot(**missing_key_kwargs) is MissingType.MISSING

    def test_missing_slots(self, slot_key, slot_value):
        assert FinalScenario().get_slot(
            slot_name=slot_key,
            slot_type=type(slot_value),
        ) is MissingType.MISSING

    def test_default(self, scenario, missing_key_kwargs):
        default = object()
        assert scenario.get_slot(**missing_key_kwargs, default=default) is default


class TestRequireSlot(BaseTestSlotAccess):
    def test_ok(self, slot_value, scenario, ok_kwargs):
        assert scenario.require_slot(**ok_kwargs) == slot_value

    def test_wrong_type(self, scenario, wrong_type_kwargs):
        with pytest.raises(CoreIrrelevantScenarioError):
            assert scenario.require_slot(**wrong_type_kwargs) is MissingType.MISSING

    def test_missing_key(self, scenario, missing_key_kwargs):
        with pytest.raises(CoreIrrelevantScenarioError):
            assert scenario.require_slot(**missing_key_kwargs)

    def test_missing_slots(self, slot_key, slot_value):
        # It is a logical error, so it still must fail
        with pytest.raises(CoreIrrelevantScenarioError):
            assert FinalScenario().require_slot(
                slot_name=slot_key,
                slot_type=type(slot_value),
            )
