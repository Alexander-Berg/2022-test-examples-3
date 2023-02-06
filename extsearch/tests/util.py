# coding: utf-8

import os


def link_configs(index_config_dir, dynamic_config_dir, target_dir):
    def link_if_needed(src, link):
        if not os.path.exists(link):
            os.symlink(src, link)

    map(lambda filename: link_if_needed(os.path.join(index_config_dir, filename),
                         os.path.join(target_dir, filename)), os.listdir(index_config_dir))
    map(lambda filename: link_if_needed(os.path.join(dynamic_config_dir, filename),
                         os.path.join(target_dir, filename)), os.listdir(dynamic_config_dir))
