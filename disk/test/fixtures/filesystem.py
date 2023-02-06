# -*- coding: utf-8 -*-
import cjson

from test.fixtures.users import default_user

nonempty_fields = ('id', 'uid', 'type', 'ctime', 'mtime', 'name',)
file_fields = nonempty_fields + ('visible', 'labels', 'meta',)
folder_fields = file_fields 
int_fields = ('ctime', 'mtime', 'size')
tree_depth = 2

moved_folder_id = "/disk/filesystem test folder moved"
copied_folder_id = "/disk/filesystem test folder copied"
trash_folder_id = "/trash/filesystem test folder"
complicated_moved = '/disk/complicated move'
complicated_moved_folder = '/disk/complicated move/filesystem test folder moved'

folder_id = "/disk/filesystem test folder"

overwritten_file_id = "/disk/overwritten file"
overwritten_copied_file_id = folder_id + "/overwritten copied file"
simply_moved_file_id = folder_id + "/simply moved file"
complicated_moved_file_id = copied_folder_id + "/complicated moved file"
complicated_overwrited_file_id = "/complicated overwrited file"

list_params = {
    'amt': 0,
    'sort': 'name',
    'order': 1,
}

bulk_action = cjson.encode(
    [
        {
            'action': 'move', 
            'params': {
              'uid': default_user.uid,
              'src': default_user.uid + ':' + folder_id,
              'dst': default_user.uid + ':' + moved_folder_id,
              'force': None,
            }
        },
        {
            'action': 'move',
            'params': {
              'uid': default_user.uid,
              'src': default_user.uid + ':' + moved_folder_id,
              'dst': default_user.uid + ':' + folder_id,
              'force': None,
            }
        },
        {
            'action': 'rm',
            'params': {
              'uid': default_user.uid,
              'path': default_user.uid + ':' + folder_id,
            }
        },
    ]
)

bulk_action_bad_params = '[ {\\\} ]'

file_data = {"meta": {"file_mid": "1000003.yadisk:89031628.249690056312488962060095667221",
                      "digest_mid": "1000005.yadisk:89031628.3983296384177350807526090116783",
                      "md5": "83e5cd52e94e3a41054157a6e33226f7",
                      "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865"},
             "size": 10000,
             "mimetype": "text/plain"}


DISK_FILE_RAW_DATA = {'uid': u'128280859', 'wh_version': '1484555747380427', 'type': 'file',
                               'version': '1484555747380426', 'key': u'/disk/test.txt',
                               'data': {u'mimetype': u'application/x-www-form-urlencoded', u'ctime': 1484555746,
                                        u'visible': 1, 'name': u'test.txt', u'source': u'disk', 'meta': {
                                       'digest_url': 'https://downloader.dst.yandex.ru/disk/02ba743cc66c3d186986e9d165359456569c4f47da3bd6e1fd93890751ee7de9/587cbe3a/kwEjxP1spucJoGCRBR433eJ4b0OSo3y62fA2LswsnuNvKJkpC2BSDOErZ_-0xSypq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=attachment&hash=&limit=0&content_type=application%2Foctet-stream&tknv=v2',
                                       u'drweb': 1, 'modify_uid': u'128280859',
                                       'preview': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=S&crop=0',
                                       'custom_preview': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=S&crop=0',
                                       'wh_version': '1484555747380426', 'sizes': [{
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2',
                                           'name': 'DEFAULT'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XXXS&crop=0',
                                           'name': 'XXXS'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XXS&crop=0',
                                           'name': 'XXS'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XS&crop=0',
                                           'name': 'XS'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=S&crop=0',
                                           'name': 'S'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=M&crop=0',
                                           'name': 'M'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=L&crop=0',
                                           'name': 'L'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XL&crop=0',
                                           'name': 'XL'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XXL&crop=0',
                                           'name': 'XXL'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XXXL&crop=0',
                                           'name': 'XXXL'}, {
                                           'url': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=CUSTOM_SIZE&crop=CUSTOM_CROP',
                                           'name': 'C'}],
                                       'file_url': u'https://downloader.dst.yandex.ru/disk/1521999a3fd765ecefd564ff8b31dd112499e9e957d1956b4a10ed8adbd8134f/587cbe3a/kwEjxP1spucJoGCRBR433dCSDcD5svRkgMVbtTUtvkZeZmUeE_GRyEmNjJOCy4yPq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=attachment&hash=&limit=0&content_type=application%2Fx-www-form-urlencoded&fsize=6755756&hid=40c7e605f7b9e2e48c191b8679970d15&media_type=document&tknv=v2&etag=05f2377e54a85557d1c2ffde665841b0',
                                       u'file_mid': u'100000.yadisk:128280859.8454815908298436432',
                                       u'etime': 1484566546,
                                       'thumbnail': 'https://downloader.dst.yandex.ru/preview/1c0b8ac240c6b54649f94d7f3b45a7d2c942ee0753b77aade1638188d6163224/inf/kwEjxP1spucJoGCRBR433docgvKLgM50n-HKEN2AFaSKMmCOAxN9uNP9YPvd1eTxq_J6bpmRyOJonT3VoXnDag%3D%3D?uid=128280859&filename=test.txt&disposition=inline&hash=&limit=0&content_type=image%2Fjpeg&tknv=v2&size=XXXS&crop=0',
                                       u'file_id': u'49d886ed95626b20d20b86744d68bc4d9b18b609391a4db6279e4e987b734571',
                                       u'pmid': u'100000.yadisk:128280859.1096444359114319179',
                                       u'sha256': u'14772c1fd6a4050d057bcc2178de3c795a577817241732ffd6bb8135d769dce7',
                                       'revision': 1484555747380426L,
                                       u'digest_mid': u'100000.yadisk:128280859.428465930279343447',
                                       u'md5': u'05f2377e54a85557d1c2ffde665841b0'},
                                        'hid': '40c7e605f7b9e2e48c191b8679970d15', 'version': '1484555747380426',
                                        u'mtime': 1484555746, 'media_type': 'document', u'size': 6755756,
                                        'type': 'file',
                                        u'utime': 1484555746}}
