import datetime
import jinja2
import pytest
import pytz

from fan.message import template_extensions

pytestmark = pytest.mark.django_db


@pytest.fixture
def base_env():
    return jinja2.sandbox.SandboxedEnvironment()


class TestNowTag:
    @pytest.fixture
    def env(self, base_env):
        base_env.add_extension(template_extensions.NowTag)
        return base_env

    @pytest.fixture(params=["Europe/Moscow", "Europe/Paris", "Europe/Amsterdam", "Europe/London"])
    def tz(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def now_mock(self, mocker, tz):
        now_value = (
            datetime.datetime.utcnow().replace(tzinfo=pytz.UTC).astimezone(pytz.timezone(tz))
        )
        return mocker.patch("fan.utils.date.Now.__call__", return_value=now_value)

    def test_no_args_use(self, now_mock, env, tz):
        template = env.from_string("{% now %}")
        result = template.render()
        assert result == now_mock().isoformat(sep=str(" "))

    def test_format_usage(self, now_mock, env, tz):
        fmt = "%Y is year, %m is month, %d is day"
        template = env.from_string('{{% now "{fmt}" %}}'.format(fmt=fmt))
        result = template.render()
        assert result == now_mock().strftime(fmt)

    def test_format_and_timezone_usage(self, now_mock, env, tz):
        fmt = "%Y is year, %m is month, %d is day"
        template = env.from_string('{{% now "{fmt}", "{tz_name}" %}}'.format(fmt=fmt, tz_name=tz))
        result = template.render()
        assert result == now_mock(pytz.timezone(tz)).strftime(fmt)


class TestOpenCounter:
    @pytest.fixture
    def env(self, base_env):
        base_env.add_extension(template_extensions.OpenCounterTag)
        return base_env

    def _get_pixel_url(self):
        return "test_url"

    def test_render(self, env):
        templ = env.from_string("{% opens_counter %}")

        actual = templ.render({"_get_pixel_url": self._get_pixel_url})

        assert actual == '<img src="test_url" width="1" height="1" />'

    def test_stat(self, env):
        templ = env.from_string("{% opens_counter %}")
        stat_dict = {}
        templ.render(template_extensions.OpenCounterTag.get_stat_function(stat_dict))

        assert stat_dict == {"pixel_usage": 1}


class TestWrapLink:
    @pytest.fixture
    def env(self, base_env):
        base_env.add_extension(template_extensions.WrapLinkTag)
        return base_env

    def _wrap_link_url(self, id_, link):
        return "id={id} link={link}".format(id=id_, link=link)

    def test_render(self, env):
        templ = env.from_string('{% wrap "some_id" %}test_url{% endwrap %}')

        actual = templ.render({"_wrap_link_url": self._wrap_link_url})

        assert actual == "id=some_id link=test_url"

    def test_stat(self, env):
        templ = env.from_string(
            '{% wrap "some_id" %}test_url{% endwrap %}{% wrap "other_id" %}test_url2{% endwrap %}'
        )
        stat_dict = {}
        templ.render(template_extensions.WrapLinkTag.get_stat_function(stat_dict))

        assert stat_dict == {"wrapped_links": [("some_id", "test_url"), ("other_id", "test_url2")]}


class TestSharedUrl:
    @pytest.fixture
    def env(self, base_env):
        base_env.add_extension(template_extensions.SharedUrl)
        return base_env

    def _get_uploaded_url(self, link, tag):
        return "tag={tag} link={link}".format(tag=tag, link=link)

    @pytest.mark.parametrize("_tag", ["autoloaded_file"])
    def test_render(self, _tag, env):
        templ = env.from_string('{{% {} "test_url" %}}'.format(_tag))

        actual = templ.render({"_get_uploaded_url": self._get_uploaded_url})

        assert actual == "tag={} link=test_url".format(_tag)

    def test_stat(self, env):
        templ = env.from_string('{% autoloaded_file "1"%}')
        stat_dict = {}
        templ.render(template_extensions.SharedUrl.get_stat_function(stat_dict))

        assert stat_dict == {
            "attachments": ["1"],
        }


class TestRenderFunctions:
    @pytest.fixture
    def message(self, mocker, campaign, letter):
        message = mocker.MagicMock()
        message.campaign = campaign
        message.letter = letter
        attachment = mocker.MagicMock()
        attachment.uri = "attach"
        attachment.filename = "attach"
        attachment.content_id = "attach"
        message.attachments = [attachment]
        message.published_attacments = {
            "published/attachment": "/get-sender/1/published.png/orig",
            "attach": "should not be",
        }
        return message

    @pytest.fixture
    def generator(self, message):
        generator = template_extensions.RenderFunctions(message, "secret_part", wrap_links=True)
        generator._PIXEL_PATTERN = "pixel:{CAMPAIGN_ID},{LETTER_ID},{LETTER_SECRET}"
        generator._LINK_PATTERN = "link:{CAMPAIGN_ID},{LETTER_ID},{LINK_ID},{LETTER_SECRET}:"
        return generator

    def test_pixel(self, generator, campaign, letter):
        func = generator.to_dict()["_get_pixel_url"]
        expected = "pixel:{},{},secret_part".format(campaign.id, letter.id)

        assert func() == expected

    @pytest.mark.parametrize("_id", [None, "test_id"])
    def test_link(self, _id, generator, campaign, letter):
        url = "http://test.url/path/кириллица"
        if _id:
            expected = "link:{},{},{},secret_part:{}".format(campaign.id, letter.id, _id, url)
        else:
            expected = url
        expected = expected.encode()

        func = generator.to_dict()["_wrap_link_url"]

        assert func(_id, url) == expected

    def test_override_attach(self, generator):
        func = generator.to_dict()["_get_uploaded_url"]
        assert func("attach", "autoloaded_file") == "cid:attach"

    def test_published_attach(self, generator, settings):
        func = generator.to_dict()["_get_uploaded_url"]
        assert func(
            "published/attachment", "autoloaded_file"
        ) == "https://{}/get-sender/1/published.png/orig".format(settings.AVATARS_HOSTS["read"])


class TestRenderFunctionsNoStats:
    @pytest.fixture
    def message(self, mocker, campaign, letter):
        message = mocker.MagicMock()
        message.campaign = campaign
        message.letter = letter
        return message

    @pytest.fixture
    def generator(self, message):
        return template_extensions.RenderFunctions(message, "secret_part", wrap_links=False)

    @pytest.mark.parametrize("_id", [None, "test_id"])
    def test_link(self, _id, generator, campaign, letter):
        url = "http://test.url/path".encode()
        func = generator.to_dict()["_wrap_link_url"]
        assert func(_id, url) == url
