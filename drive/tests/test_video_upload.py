import uuid
from ffmpy import FFRuntimeError
from unittest import mock
from django.urls import reverse

import cars.settings
from cars.core.constants import AppPlatform
from cars.core.mds.wrapper import MDSDocumentsWrapper
from cars.django.tests import CarsharingAPITestCase
from cars.users.factories.user import UserFactory
from cars.users.factories.user_documents import UserDocumentPhotoFactory
from cars.users.models.user_documents import UserDocumentBackgroundVideo
from cars.users.models.app_install import AppInstall


class FFmpegMock(object):

    def __init__(self, inputs, outputs):
        self.inputs = inputs
        self.outputs = outputs

    def run(self, stdout, stderr):
        '''Modifies first input's content and writes it to outputs.'''
        with open(next(iter(self.inputs.keys())), 'rb') as infile:
            modified_data = b'converted_' + infile.read()
            for outfile_name in self.outputs:
                with open(outfile_name, 'wb') as outfile:
                    outfile.write(modified_data)


def make_ffprobe_mock(return_format):

    class FFProbeMock(object):

        def __init__(self, name):
            video_mock = mock.MagicMock(codec_name=return_format)
            self.video = [video_mock]

    return FFProbeMock


class UserDocumentBackgroundVideoUploadTestCase(CarsharingAPITestCase):

    def setUp(self):
        user_uid = cars.settings.YAUTH_TEST_USER['login']
        self.user = UserFactory.create(uid=user_uid, username=user_uid)
        self.app_install = AppInstall.objects.get(user=self.user)
        self.photo = UserDocumentPhotoFactory.create(document__user=self.user)
        self.mds_client = MDSDocumentsWrapper.from_settings()
        # no convertions for common tests
        self.set_platform(AppPlatform.IOS.value)

    def set_platform(self, platform):
        self.app_install.platform = platform
        self.app_install.save()

    def get_url(self, photo):
        return reverse(
            'drive:user-document-photo-background-video',
            kwargs={
                'document_id': photo.document.id,
                'photo_id': photo.id,
            },
        )

    def upload(self, data, content_type='video/mp4'):
        url = self.get_url(self.photo)
        response = self.client.put(url, data=data, content_type=content_type)
        return response

    def test_ok(self):
        response = self.upload(b'test')
        self.assert_response_ok(response)
        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(), b'test')

    def test_different_content_type(self):
        self.upload(b'test', content_type='video/webm')
        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        self.assertEqual(video.mime_type, 'video/webm')

    def test_no_data(self):
        response = self.upload(None)
        self.assert_response_bad_request(response)

    def test_other_user(self):
        user = UserFactory.create()
        photo = UserDocumentPhotoFactory.create(document__user=user)
        url = self.get_url(photo)
        response = self.client.put(url, data=b'test', content_type='video/mp4')
        self.assert_response_not_found(response)

    @mock.patch('ffprobe3.FFProbe')
    @mock.patch('ffmpy.FFmpeg')
    def test_conversion_not_needed_platform(self, ffmpy_mock, ffprobe_mock):
        '''Check that if platform is iOS, no ffmpeg-conversion is run.'''
        self.set_platform(AppPlatform.IOS.value)
        response = self.upload(b'video_content_0')
        self.assert_response_ok(response)
        self.assertFalse(ffmpy_mock.called)
        self.assertFalse(ffprobe_mock.called)

        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(), b'video_content_0')

    @mock.patch('ffprobe3.FFProbe')
    @mock.patch('ffmpy.FFmpeg')
    def test_conversion_not_needed_format(self, ffmpy_mock, ffprobe_mock):
        '''Check that if ffprobe returns h264, no ffmpeg-conversion is run.'''
        ffprobe_mock.side_effect = make_ffprobe_mock('h264')

        self.set_platform(AppPlatform.ANDROID.value)
        response = self.upload(b'video_content_0')
        self.assert_response_ok(response)
        self.assertTrue(ffprobe_mock.called)
        self.assertFalse(ffmpy_mock.called)

        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(), b'video_content_0')

    @mock.patch('ffprobe3.FFProbe')
    @mock.patch('ffmpy.FFmpeg')
    def test_conversion_needed(self, ffmpy_mock, ffprobe_mock):
        '''Check that if platform is Android, video is being converted with ffmpeg.'''
        ffprobe_mock.side_effect = make_ffprobe_mock('mpeg4')
        ffmpy_mock.side_effect = FFmpegMock
        self.set_platform(AppPlatform.ANDROID.value)
        response = self.upload(b'video_content_1')
        self.assert_response_ok(response)

        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(), b'converted_video_content_1')

    @mock.patch('ffprobe3.FFProbe')
    @mock.patch('ffmpy.FFmpeg')
    def test_conversion_fails(self, ffmpy_mock, ffprobe_mock):
        '''Check that if ffmpeg run fails, unconverted video is uploaded to mds.'''
        ffprobe_mock.side_effect = make_ffprobe_mock('mpeg4')
        ffmpy_mock.side_effect = FFRuntimeError('<ffmpeg cmd>', 42,
                                                b'<stdout content>', b'<stderr content>')
        self.set_platform(AppPlatform.ANDROID.value)
        response = self.upload(b'video_content_2')
        self.assert_response_ok(response)

        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(), b'video_content_2')
