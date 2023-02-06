# -*- coding: utf-8 -*-

from lxml import etree

from jinja2 import Template

from test.base_suit import ServiceApiTestCaseMixin
from test.helpers.stubs.services import KladunStub
from mpfs.core.filesystem.hardlinks.common import FileChecksums


class KladunMocker(ServiceApiTestCaseMixin):

    def do_three_callbacks(self, uid, oid, file_info, file_upload, final):
        with KladunStub(
            status_values=(
                etree.fromstring(file_info),
                etree.fromstring(file_upload),
                etree.fromstring(final)
            )
        ):
            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFileInfo',
                'status_xml': file_info
            })

            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFileUpload',
                'status_xml': file_upload
            })

            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFinal',
                'status_xml': final
            })

    def mock_kladun_faulty_callback_for_upload_from_service(self, uid, oid):
        with open('fixtures/xml/kladun/upload-from-service/commitFileInfoFail.xml') as f:
            commit_file_info_xml_data = f.read().replace(
                '{{ uid }}', str(uid)
            ).replace('{{ oid }}', str(oid))

        with KladunStub(status_values=(etree.fromstring(commit_file_info_xml_data),)):
            self.service_ok('kladun_callback', {
                'uid': uid,
                'oid': oid,
                'type': 'commitFileInfo',
                'status_xml': commit_file_info_xml_data
            })

    def mock_kladun_callbacks_for_upload_from_service(self, uid, oid):
        with open('fixtures/xml/kladun/upload-from-service/commitFileInfo.xml') as f:
            commit_file_info_xml_data = f.read().replace(
                '{{ uid }}', str(uid)
            ).replace('{{ oid }}', str(oid))

        with open('fixtures/xml/kladun/upload-from-service/commitFileUpload.xml') as f:
            commit_file_upload_xml_data = f.read().replace(
                '{{ uid }}', str(uid)
            ).replace('{{ oid }}', str(oid))

        with open('fixtures/xml/kladun/upload-from-service/commitFinal.xml') as f:
            commit_final_xml_data = f.read().replace(
                '{{ uid }}', str(uid)
            ).replace('{{ oid }}', str(oid))

        self.do_three_callbacks(uid, oid, commit_file_info_xml_data, commit_file_upload_xml_data, commit_final_xml_data)

    def mock_kladun_callbacks_for_store(self, uid, oid, checksums_obj=None):
        if checksums_obj is None:
            checksums_obj = FileChecksums(
                size=55462,
                md5='aea4e017db139080ffff4dd2bf738612',
                sha256='7b0c3bf5dbfb2bc34b54832a995db6fde4062f7775c20d1adff42b622ef33412'
            )
        render_data = {
            'uid': uid,
            'oid': oid
        }
        render_data.update(checksums_obj.as_dict())
        with open('fixtures/xml/kladun/upload-to-default/commitFileInfo.xml') as f:
            commit_file_info_xml_data = Template(f.read()).render(**render_data)

        with open('fixtures/xml/kladun/upload-to-default/commitFileUpload.xml') as f:
            commit_file_upload_xml_data = Template(f.read()).render(**render_data)

        with open('fixtures/xml/kladun/upload-to-default/commitFinal.xml') as f:
            commit_final_xml_data = Template(f.read()).render(**render_data)

        self.do_three_callbacks(uid, oid, commit_file_info_xml_data, commit_file_upload_xml_data, commit_final_xml_data)

    def mock_kladun_callbacks_for_dstore(self, uid, oid):
        with open('fixtures/xml/kladun/patch-info/commitFileInfo.xml') as f:
            commit_file_info_xml_data = Template(f.read()).render(uid=uid, oid=oid)

        with open('fixtures/xml/kladun/patch-info/commitFileUpload.xml') as f:
            commit_file_upload_xml_data = Template(f.read()).render(uid=uid, oid=oid)

        with open('fixtures/xml/kladun/patch-info/commitFinal.xml') as f:
            commit_final_xml_data = Template(f.read()).render(uid=uid, oid=oid)

        self.do_three_callbacks(uid, oid, commit_file_info_xml_data, commit_file_upload_xml_data, commit_final_xml_data)
