from unittest import mock

from django.contrib.auth.models import Group, Permission
from django.urls import reverse

from cars.admin.models.permissions import AdminPermissions
from cars.admin.permissions import AdminPermissionCode
from cars.core.mds.wrapper import MDSDocumentsWrapper
from cars.drive.tests.test_video_upload import FFmpegMock, make_ffprobe_mock
from cars.users.factories.user_documents import UserDocumentPhotoFactory
from cars.users.models.user_documents import UserDocumentBackgroundVideo

from .base import AdminAPITestCase


class UserDocumentBackgroundVideoConvertTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()

        permission = Permission.objects.get(
            content_type__model=AdminPermissions._meta.model_name,
            codename=AdminPermissionCode.VIEW_USER_DOCUMENT_VIDEOS.value,
        )
        group = Group.objects.create(name='test_ring0')
        group.permissions.add(permission)
        self.user.groups.add(group)

        self.photo = UserDocumentPhotoFactory.create(document__user=self.user)
        self.mds_client = MDSDocumentsWrapper.from_settings()

    def get_upload_url(self, photo):
        return reverse(
            'drive:user-document-photo-background-video',
            kwargs={
                'document_id': photo.document.id,
                'photo_id': photo.id,
            },
        )

    def get_convert_url(self, photo):
        return reverse(
            'cars-admin:user-document-photo-background-video-convert',
            kwargs={
                'user_id': photo.document.user.id,
                'document_id': photo.document.id,
                'photo_id': photo.id,
            },
        )

    def upload(self, data, content_type='video/mp4'):
        url = self.get_upload_url(self.photo)
        response = self.client.put(url, data=data, content_type=content_type)
        return response

    def convert(self):
        url = self.get_convert_url(self.photo)
        response = self.client.post(url, data='')
        return response

    @mock.patch('ffprobe3.FFProbe')
    @mock.patch('ffmpy.FFmpeg')
    def test_convert_existing_needs_conversion(self, ffmpy_mock, ffprobe_mock):
        ffprobe_mock.side_effect = make_ffprobe_mock('mpeg4')
        ffmpy_mock.side_effect = FFmpegMock
        self.upload(b'video_content_existing')
        response = self.convert()
        self.assert_response_ok(response)

        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(),
                         b'converted_converted_video_content_existing')

    @mock.patch('ffprobe3.FFProbe')
    @mock.patch('ffmpy.FFmpeg')
    def test_convert_existing_no_needs_conversion(self, ffmpy_mock, ffprobe_mock):
        ffprobe_mock.side_effect = make_ffprobe_mock('h264')
        ffmpy_mock.side_effect = FFmpegMock
        self.upload(b'video_content_existing')
        response = self.convert()
        self.assert_response_ok(response)

        video = UserDocumentBackgroundVideo.objects.get(photo=self.photo)
        mds_response = self.mds_client.get_user_document_background_video(video)
        self.assertEqual(mds_response['Body'].read(),
                         b'video_content_existing')
