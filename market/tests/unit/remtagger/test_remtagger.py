import pytest
import requests
import responses

from yamarec1.remtagger.exceptions import RemTaggerError


def test_remtagger_checks_for_a_nonexistent_tag(remtagger, tag):
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), body="null\n")
        assert not remtagger.is_set(tag, 5)
        assert len(r.calls) == 1


def test_remtagger_checks_for_a_tag_that_has_not_set(remtagger, tag):
    body = """{
      "tag_name":"%s",
      "is_set":false,
      "tag_version":1,
      "last_reset_version":1,
      "last_reset_comment":""
    }""" % (remtagger.prefix + tag)
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), body=body)
        assert not remtagger.is_set(tag, 5)
        assert len(r.calls) == 1


def test_remtagger_checks_for_a_obsolete_tag(remtagger, tag):
    body = """{
      "tag_name":"%s",
      "is_set":true,
      "tag_version":1,
      "last_reset_version":1,
      "last_reset_comment":""
    }""" % (remtagger.prefix + tag)
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), body=body)
        assert not remtagger.is_set(tag, 5)
        assert len(r.calls) == 1


def test_remtagger_checks_for_a_tag_that_has_set_and_is_not_obsolete(remtagger, tag):
    body = """{
      "tag_name":"%s",
      "is_set":true,
      "tag_version":5,
      "last_reset_version":1,
      "last_reset_comment":""
    }""" % (remtagger.prefix + tag)
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), body=body)
        assert remtagger.is_set(tag, 5)
        assert len(r.calls) == 1


def test_remtagger_set_a_tag(remtagger, tag):
    body = """{
          "tag_name":"%s",
          "is_set":true,
          "tag_version":5,
          "last_reset_version":1,
          "last_reset_comment":""
        }""" % (remtagger.prefix + tag)
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/reset_tag?tag=%s" % (remtagger.prefix + tag), body="Ok\n")
        r.add(r.GET, remtagger.url + "/set_tag?tag=%s" % (remtagger.prefix + tag), body="Ok\n")
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), body=body)
        assert remtagger.set(tag) == 5
        assert len(r.calls) == 3


def test_remtagger_raises_exception_if_connection_failed(remtagger, tag):
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), body=requests.ConnectionError())
        with pytest.raises(RemTaggerError):
            remtagger.is_set(tag, 5)
        assert len(r.calls) == 1


def test_remtagger_raises_exception_if_tags_cloud_failed(remtagger, tag):
    with responses.RequestsMock() as r:
        r.add(r.GET, remtagger.url + "/lookup_tag?tag=%s" % (remtagger.prefix + tag), status=500)
        with pytest.raises(RemTaggerError):
            remtagger.is_set(tag, 5)
        assert len(r.calls) == 1
