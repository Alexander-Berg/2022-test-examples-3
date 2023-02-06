# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries, contains_inanyorder
from mongoengine import NotUniqueError

from common.data_api.sendr.api import Attachment
from common.email_sender.factories import AttachmentInfoFactory, EmailIntentFactory
from common.email_sender.models import ImproperConfigureError

pytestmark = pytest.mark.mongouser('module')


def dummy_preprocessor(email):
    email.is_preprocessed = True


class TestAttachmentInfo(object):
    def test_as_sendr_attachment(self):
        attachement = AttachmentInfoFactory()
        assert attachement.as_sendr_attachment() == Attachment(
            filename=attachement.filename, mime_type=attachement.mime_type, content=attachement.content
        )


class TestEmailIntent(object):
    @pytest.mark.parametrize('preprocessor_name, attachments', [
        ('', [AttachmentInfoFactory()]),
        ('{}.{}'.format(dummy_preprocessor.__module__, dummy_preprocessor.__name__), []),
    ])
    def test_to_sendr_kwargs_ok(self, preprocessor_name, attachments):
        email_intent = EmailIntentFactory(preprocessor=preprocessor_name, attachments=attachments)
        result = email_intent.to_sendr_kwargs()

        if preprocessor_name:
            assert email_intent.is_preprocessed
        assert_that(result, has_entries(
            to_email=email_intent.email,
            args=email_intent.args,
            attachments=contains_inanyorder(*[a.as_sendr_attachment() for a in email_intent.attachments])
        ))

    @pytest.mark.parametrize('preprocessor_name', ['foo', '-'])
    def test_to_sendr_kwargs_improperly_configured(self, preprocessor_name):
        email_intent = EmailIntentFactory(preprocessor=preprocessor_name)
        with pytest.raises(ImproperConfigureError):
            email_intent.to_sendr_kwargs()

    def test_cant_create_unique(self):
        EmailIntentFactory(key='foo')
        with pytest.raises(NotUniqueError):
            EmailIntentFactory(key='foo')
