# -*- coding: utf-8 -*-

SNAPSHOT = {
    "iteration_key": "1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;",
    "items": [
        # обычный файл
        {
            "path": "/disk/path_99",
            "type": "file",
            "mtime": 1450785671,
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "resource_id": "4000756161:9f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
                "revision": 1478082183645717,
            }
        },
        # директория (нет хэшей)
        {
            "path": "/disk/dir",
            "mtime": 1450797611,
            "type": "dir",
            "meta": {
                "resource_id": "4000756161:03e8165f51dd4c6e68bbffc8edabd824f1b350caac5b042b65dc62afec98ed90",
                "revision": 1478082143638469,
            }
        },
        # гостевая общая директория с доступом на чтение + запись
        {
            "path": "/disk/shared_dir_rw",
            "mtime": 1430792611,
            "type": "dir",
            "meta": {
                "resource_id": "4000756161:03e8165f51dd4c6e68b1f2c8edabd824f1b350caac5b042b65dc62afec5fed90",
                "revision": 1478082143238469,
                "group": {
                    "is_owned": 0,
                    "rights": 660,
                }
            }
        },
        # гостевая общая директория с доступом на чтение
        {
            "path": "/disk/shared_dir_ro",
            "mtime": 1450798612,
            "type": "dir",
            "meta": {
                "resource_id": "4000756161:2ae8165551dd4c6e68b1f2c8edabd824f1b350caac5b042b65dc22afed5fed91",
                "revision": 1478082143238469,
                "group": {
                    "is_owned": 0,
                    "rights": 640,
                }
            }
        },
        # гостевая общая директория
        {
            "path": "/disk/shared_dir_own",
            "mtime": 1450799812,
            "type": "dir",
            "meta": {
                "resource_id": "4000756161:63e8165f51dd4c6e68b1f268edabd824f1b350caac5b042b65dc62afec5fed66",
                "revision": 1478092243842369,
                "group": {
                    "is_owned": 1,
                }
            }
        },
        # директория с симлинком
        {
            "path": "/disk/dir_with_symlink",
            "mtime": 1450797611,
            "type": "dir",
            "meta": {
                "resource_id": "4000756161:13e8165f51dd4c6e68bbffc8edabd824f1b350caac5b042b65dc62afec98ed11",
                "revision": 1455082143638469,
                "discsw_symlink": "%25disk%25jntjq9ajpn7h65f3%25test508/12345",
            }
        },
        # публичный файл
        {
            "path": "/disk/public_path_99",
            "type": "file",
            "mtime": 1450785671,
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "resource_id": "4000756161:8f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
                "revision": 1478082184645817,
                "public_url": "http://dummy.ya.net/d/a0d77a14-a6f3-41e7-a92d-b96a47ba08e7"
            }
        },
    ],
    "revision": 1478082429130762
}

NO_MTIME_SNAPSHOT = {
    "iteration_key": "1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;",
    "items": [
        {
            "path": "/disk/path_77",
            "type": "file",
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "resource_id": "4000756161:9f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
                "revision": 1478082183645717,
            }
        }
    ],
    "revision": 1478082429130762
}

NO_PATH_SNAPSHOT = {
    "iteration_key": "1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;",
    "items": [
        {
            "type": "file",
            "mtime": 1450785671,
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "resource_id": "4000756161:9f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
                "revision": 1478082183645717,
            }
        }
    ],
    "revision": 1478082429130762
}

NO_TYPE_SNAPSHOT = {
    "iteration_key": "1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;",
    "items": [
        {
            "path": "/disk/path_77",
            "mtime": 1450785671,
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "resource_id": "4000756161:9f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
                "revision": 1478082183645717,
            }
        }
    ],
    "revision": 1478082429130762
}

NO_META_RESOURCE_ID_SNAPSHOT = {
    "iteration_key": "1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;",
    "items": [
        {
            "path": "/disk/path_77",
            "type": "file",
            "mtime": 1450785671,
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
                "revision": 1478082183645717,
            }
        }
    ],
    "revision": 1478082429130762
}

NO_META_REVISION_SNAPSHOT = {
    "iteration_key": "1478082429130762;055b2729c20b37394cb7d8eb15dbe7c2;;",
    "items": [
        {
            "path": "/disk/path_77",
            "type": "file",
            "mtime": 1450785671,
            "meta": {
                "md5": "d41d8cd98f00b204e9800998ecf8427e",
                "resource_id": "4000756161:9f8c4e04d79caa888674eddd83fe0620b4289ca4b86700be158a9f28194bf25f",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "size": 0,
            }
        }
    ],
    "revision": 1478082429130762
}
