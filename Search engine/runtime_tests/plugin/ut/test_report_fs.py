# -*- coding: utf-8 -*-


def test_fs_manager(fs_manager):
    data = 'Pink Floyd'
    path = fs_manager.create_file('temp.txt')
    with open(path, 'w') as f:
        f.write(data)
    assert data == fs_manager.read_file(path)
