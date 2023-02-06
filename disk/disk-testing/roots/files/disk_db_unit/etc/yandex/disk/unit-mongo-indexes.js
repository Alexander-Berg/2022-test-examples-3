dbs = ['attach_data', 'changelog', 'disk_info', 'hidden_data', 'link_data', 'misc_data', 'operations', 'trash', 'user_data', 'narod_data', 'user_index', 'filesystem_locks', 'archive', 'album_items', 'albums'];

indexes = {
    'attach_data': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}}
    ],
    'archive': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.mt': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.mtime': 1}, 'opt': {}},
        {'index': {'uid': 1, 'data.utime': 1}, 'opt': {}}
    ],
    'user_data': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.mt': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.mtime': 1}, 'opt': {}},
        {'index': {'uid': 1, 'data.utime': 1}, 'opt': {}}
    ],
    'narod_data': [ 
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}},
    ],
    'hidden_data': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}}
    ],
    'trash': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}}
    ],
    'user_index': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}}
    ],
    'misc_data': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'hid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.file_id': 1}, 'opt': {'sparse': true}},
        {'index': {'uid': 1, 'data.mt': 1}, 'opt': {'sparse': true}}
    ],
    'link_data': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'parent': 1}, 'opt': {}},
        {'index': {'dtime': 1}, 'opt': {'expireAfterSeconds': 2592000}}
    ],
    'changelog': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'version': 1}, 'opt': {}},
        {'index': {'uid': 1, 'gid': 1, 'version': 1}, 'opt': {}},
        {'index': {'data.stids.stid': 1}, 'opt': {'sparse': true}},
        {'index': {'dtime': 1}, 'opt': {'expireAfterSeconds': 2592000}}
    ],
    'operations': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'state': 1}, 'opt': {}},
        {'index': {'uid': 1, 'md5': 1}, 'opt': {}},
        {'index': {'dtime': 1}, 'opt': {'expireAfterSeconds': 2592000}}
    ],
    'disk_info': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'parent': 1}, 'opt': {}}
    ],
    'filesystem_locks': [ 
        {'index': {'uid': 1}, 'opt': {}},
        {'index': {'uid': 1, 'key': 1}, 'opt': {}},
        {'index': {'dtime': 1}, 'opt': {'expireAfterSeconds': 3600}}
    ],
    'album_items' : [
        {'index': {'uid': 1, 'album_id': 1}, 'opt': {}},
        {'index': {'uid': 1, '_id': 1}, 'opt': {}},
        {'index': {'uid': 1, 'obj_type': 1, 'obj_id': 1}, 'opt':{}},
    ],
    'albums' : [
        {'index': {'uid': 1, '_id': 1}, 'opt': {}},
    ],


};

dbs.forEach(function(x) {
    curr_db = db.getSiblingDB(x);
    indexes[x].forEach(function(idx) {
        print(x + '.' + x + '.ensureIndex(' + tojson(idx.index) + ', ' + tojson(idx.opt) + ')');
//        curr_db[x].ensureIndex(idx.index, idx.opt);
    });
});
