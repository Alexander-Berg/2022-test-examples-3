class SocialImportOperationFixture(object):
    img_url = 'http://vk.com/pic'
    latitude = 65.123
    longitude = 12.34
    created = 14234231123

    albums = {
        'state': 'success',
        'result': [
            {
                'aid': 94595474,
                'title': 'party'
            }
        ]
    }

    photos = [{
        'pid': 289068348,
        'location': {
            'latitude': 42.431887000000003,
            'longitude': 18.711272999999998
        },
        'created': 1346504298,
        'url': 'http://cs315627.vk.me/v315627352/cb1/xLYpRYrJ43s.jpg'
    }, {
        'pid': 123456789,
        'created': 1339514391,
        'url': 'http://cs311123.vk.me/v311123382/b91c/MFXJUu8-b5A.jpg'
    }, {
        'pid': 223456789,
        'created': 1339514391,
        'url': 'http://cs605526.vk.me/v605526297/4143/xk9XYgjKnKA.jpg'
    }]

    personal_photos = [{
       'pid': 289068311,
       'location': {
           'latitude': 43.0,
           'longitude': 10.7,
       },
       'created': 1346504298,
       'url': 'http://cs607923.vk.me/v607923352/5a53/QyEHxJ8eMSY.jpg'
    }]

    saved_photos = [{
        'pid': 343456777,
        'created': 1339514391,
        'url': 'http://cs311123.vk.me/v311123382/b91c/asdasdasd-b5A.jpg'
    }]


class OperationStatusFixture(object):
    OPERATION_STATUS = {
        "at_version": 1468932199277047,
        "params": {
            "source": "4000756161:/disk/path_1",
            "target": "4000756161:/disk/path_17"
        },
        "status": 0,
        "type": "copy"
    }


class ActiveOperationsFixture(object):
    OPERATION_ID = 'a268af25c98f53a76dccd8a4d580c105713d287c39c5401dd0a9fd8bdd92b2fa'
    NON_EXISTENT_OPERATION_ID = 'NON_EXISTENT_OPERATION_ID'

    COPY = [{
        "ctime": 1468932202,
        "data": {
            "at_version": 1468932199277047,
            "callback": "",
            "connection_id": "",
            "file_id": "512b55b983817ec036c226d547f88f7478cf34e4aa3a808d162a6940392fb8a6",
            "source_resource_id": "4000756161:512b55b983817ec036c226d547f88f7478cf34e4aa3a808d162a6940392fb8a6",
            "force": 0,
            "free_space": 21474836480,
            "resource_type": "file",
            "source": "4000756161:/disk/path_1",
            "target": "4000756161:/disk/path_17"
        },
        "id": "a268af25c98f53a76dccd8a4d580c105713d287c39c5401dd0a9fd8bdd92b2fa",
        "md5": "",
        "mtime": 1468932202,
        "state": 0,
        "subtype": "disk_disk",
        "type": "copy",
        "uid": "4000756161",
        "uniq_id": "257993dd98676aa963343f9440dd1f2e"
        },
        {
        "ctime": 1468932203,
        "data": {
            "at_version": 1468932199277048,
            "callback": "",
            "connection_id": "",
            "file_id": "612b55b983817ec036c226d547f88f7478cf34e4aa3a808d162a6940392fb8a6",
            "source_resource_id": "4000756161:612b55b983817ec036c226d547f88f7478cf34e4aa3a808d162a6940392fb8a6",
            "force": 0,
            "free_space": 31474836480,
            "resource_type": "file",
            "source": "4000756161:/disk/path_2",
            "target": "4000756161:/disk/path_27"
        },
        "id": "b268af25c98f53a76dccd8a4d580c105713d287c39c5401dd0a9fd8bdd92b2fa",
        "md5": "",
        "mtime": 1468932203,
        "state": 0,
        "subtype": "mail_disk",
        "type": "copy",
        "uid": "4000756161",
        "uniq_id": "357993dd98676aa963343f9440dd1f2e"
    }]
    STORE = [{
        "ctime": 1469461717,
        "data": {
            "at_version": 1469461646374065,
            "callback": "",
            "changes": {},
            "client_type": "json",
            "connection_id": "",
            "file_id": "b5c20426377849e332c0a7d8903931356b9a1ef3f8553c3b53aaf9b3419d1832",
            "free_space": 21474836480,
            "kladun_data": {
                "file-id": "b5c20426377849e332c0a7d8903931356b9a1ef3f8553c3b53aaf9b3419d1832",
                "free_space": 21474836480,
                "oid": "264361cbed0395d6fb19ed523e0b3e40e715a41311795fd5bde869726bbce8eb",
                "path": "4000756161:/disk/path_21",
                "service": "disk",
                "uid": "4000756161"
            },
            "md5": "",
            "path": "4000756161:/disk/path_21",
            "replace_md5": None,
            "set_public": None,
            "sha256": "",
            "size": 0,
            "status_url": "http://uploader1f.dst.yandex.net:8080/request-status/20160725T184837.107.utd.5nm16fmmtpxn3exwroaq8eicl-k1f.10108",
            "upload_url": "https://uploader1f.dst.yandex.net:443/upload-target/20160725T184837.107.utd.5nm16fmmtpxn3exwroaq8eicl-k1f.10108"
        },
        "id": "264361cbed0395d6fb19ed523e0b3e40e715a41311795fd5bde869726bbce8eb",
        "md5": "",
        "mtime": 1469461717,
        "state": 1,
        "subtype": "disk",
        "type": "store",
        "uid": "4000756161"
    }]

    SOCIAL = [{
        "ctime": 1469462882,
        "data": {
            "at_version": 1469461646374065,
            "filedata": {},
            "show_groups": 1,
            "social_tasks": {},
            "stages": {}
        },
        "id": "01569c3acfae2b6cad3864d893c62f36506076c4fc7bab6906db41beec200056",
        "md5": "",
        "mtime": 1469462882,
        "state": 0,
        "subtype": "list_contacts",
        "type": "social",
        "uid": "4000756161",
        "uniq_id": None
    }]
    TRASH_APPEND = [{
        "ctime": 1469782887,
        "data": {
            "at_version": 1469461646374065,
            "callback": "",
            "connection_id": "",
            "file_id": "545199b70a4ea3d9a21a521959103274a746285fa42064b5a0bbb88762e223c4",
            "source_resource_id": "4000756161:545199b70a4ea3d9a21a521959103274a746285fa42064b5a0bbb88762e223c4",
            "path": "4000756161:/disk/path_1"
        },
        "id": "79866290eac564662de0d00f3f52a0ef79540cdafe20ffa469fff58b7c828c51",
        "md5": "",
        "mtime": 1469782887,
        "state": 0,
        "subtype": "append",
        "type": "trash",
        "uid": "4000756161",
        "uniq_id": "58e1436472a6dafc6fabd970f3d48138"
    }]
    MOVE = [{
        "ctime": 1469783681,
        "data": {
            "at_version": 1469461646374065,
            "callback": "",
            "connection_id": "",
            "file_id": "4ca4be73f6321be201841a3465742f4f7cdf9507c64b0e876dbfaa175fc56051",
            "source_resource_id": "4000756161:4ca4be73f6321be201841a3465742f4f7cdf9507c64b0e876dbfaa175fc56051",
            "force": 0,
            "source": "4000756161:/disk/path_2",
            "target": "4000756161:/disk/path_222"
        },
        "id": "00bf35f5e458ea70803168ea64d0d5383e299be6a6524ee7ec9fe5ca3266f281",
        "md5": "",
        "mtime": 1469783681,
        "state": 0,
        "subtype": "disk_disk",
        "type": "move",
        "uid": "4000756161",
        "uniq_id": "30c3ae8084f4e7c2e30c30d069467e7b"
    }]
    REMOVE = [{
        "ctime": 1470662838,
        "data": {
            "at_version": 1470662123120416,
            "callback": "",
            "connection_id": "",
            "path": "4000756161:/disk/path_21",
            "source_resource_id": "4000756161:4ca4be73f6321be201841a3465742f4f7cdf9507c64b0e876dbfaa175fc56051",
        },
        "id": "19bb0c10e57e00cde64d84d478dbd3e0a8a096c3034a468a0027a5a80048fa7b",
        "md5": "",
        "mtime": 1470662838,
        "state": 0,
        "subtype": "disk",
        "type": "remove",
        "uid": "4000756161"
    }]
