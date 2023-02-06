import exts.fs
import os
import shutil
import yatest.common

from test.util import tools

from library.python.testing.recipe import declare_recipe

CRM_RAM_RESOURCES = "crm_ram_resources"


def create_link(src_path, dest_path):
    if os.path.isfile(src_path):
        print("{0} is file, create link to {1}".format(src_path, dest_path))
        exts.fs.hardlink(src_path, dest_path)
    else:
        print("{0} is dir, create symlink to {1}".format(src_path, dest_path))
        tools.link_dir(src_path, dest_path)


def copy_file(src_path, dest_path):
    if dest_path is not None and not os.path.exists(dest_path):
        try:
            shutil.copy(src_path, dest_path)
        except IOError as e:
            print(e)


def copy_files(src):
    src_files = os.listdir(src)
    for file_name in src_files:
        src_path = os.path.join(src, file_name)

        ram_dest_path = yatest.common.ram_drive_path(file_name)
        print("Copy {0} to {1}".format(src_path, ram_dest_path))
        copy_file(src_path, ram_dest_path)

        work_dest_path = yatest.common.work_path(file_name)
        print("Copy {0} to {1} as link".format(src_path, work_dest_path))
        create_link(src_path, work_dest_path)


def start(argv):
    copy_files(yatest.common.binary_path(CRM_RAM_RESOURCES))


def stop(argv):
    pass


if __name__ == "__main__":
    declare_recipe(start, stop)
