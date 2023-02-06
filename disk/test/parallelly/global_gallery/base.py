from mpfs.core.global_gallery.dao.source_id import SourceIdDAOItem, SourceIdDAO
from mpfs.core import factory


class GlobalGalleryTestCaseMixin(object):

    def _insert_source_id_record(self, uid, hid, source_id, is_live_photo=False):
        SourceIdDAO().save(SourceIdDAOItem.build_by_params(uid, hid, source_id, is_live_photo=is_live_photo))

    def _upload_sample_files_for_live_photo_case(self, live_photo_source_id, photo_source_id):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        file_data = {
            'md5': md5,
            'sha256': sha256,
            'size': size,
        }

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/r1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/r1/r2'})
        live_photo_with_source_id_path = '/disk/r1/r2/1.jpg'
        photo_with_source_id_path = '/disk/r1/r2/2.jpg'
        live_photo_no_source_id_path = '/disk/r1/r2/3.jpg'
        photo_no_source_id_path = '/disk/r1/r2/4.jpg'

        self.store_live_photo_with_video(live_photo_with_source_id_path, file_data=file_data,
                                         opts={'source_id': live_photo_source_id})
        self.upload_file(self.uid, photo_with_source_id_path, file_data=file_data, opts={'source_id': photo_source_id})
        self.store_live_photo_with_video(live_photo_no_source_id_path)
        self.upload_file(self.uid, photo_no_source_id_path)

        resources = [factory.get_resource_by_path(self.uid, path) for path in
                     [live_photo_with_source_id_path, photo_with_source_id_path,
                      live_photo_no_source_id_path, photo_no_source_id_path]]
        return resources
