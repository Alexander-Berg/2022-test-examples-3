import pytest

from mail.ciao.ciao.core.entities.scenario_response import ScenarioResponse


class TestPostInit:
    @pytest.fixture
    def text(self, rands):
        return rands()

    @pytest.fixture
    def speech(self, rands):
        return rands()

    def test_text_fills_speech(self, text):
        assert ScenarioResponse(text=text).speech == text

    def test_both_text_speech_passed(self, text, speech):
        response = ScenarioResponse(text=text, speech=speech)
        assert response.text == text and response.speech == speech
