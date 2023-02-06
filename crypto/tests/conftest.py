import sys
import socket
sys.path.append('../python')

import pytest
import os
import shutil
import yt.local as yt_local
import yt.wrapper as yt

from rtcconf import config

pytest_plugins = 'libs.graph_runner'


@pytest.fixture(scope="module")
def ytlocal(request):
    proxy_port = getattr(request.module, 'PROXY_PORT', 9013)
    yt_dir = getattr(request.module, 'YT_DIR', 'local_yt_dir')
    yt_id = getattr(request.module, 'YT_ID', 'yt-graph-test-working')

    # A function that will maybe to some magic and return a path to a dir to synchronize yt cypress with
    prepare_cypress_dir_func = getattr(request.module, 'prepare_cypress', None)

    # A function that will return a list of tuples of form "[(filesystem_path, yt_path)]"
    # for all the files to upload to local YT after it's started
    get_file_uploads_func = getattr(request.module, 'get_file_uploads', None)

    master_config = getattr(request.module, 'MASTER_CONFIG', None)
    get_scheduler_config_func = getattr(request.module, 'get_scheduler_config', None)

    # A function that will return a path to file with YT node config
    get_node_config_func = getattr(request.module, 'get_node_config', None)

    proxy_config = getattr(request.module, 'PROXY_CONFIG', None)

    if not os.path.exists(yt_dir):
        os.makedirs(yt_dir)

    if prepare_cypress_dir_func is not None:
        cypress_root = prepare_cypress_dir_func()
    else:
        cypress_root = None

    if get_node_config_func is not None:
        node_config = get_node_config_func()
    else:
        node_config = None

    if get_scheduler_config_func is not None:
        scheduler_config = get_scheduler_config_func()
    else:
        scheduler_config = None

    ytenv = None
    try:
        yt_local.get_proxy(yt_id, path=yt_dir)
    except Exception:
        ytenv = yt_local.start(
            master_count=1, node_count=5, scheduler_count=1, http_proxy_count=1,
            master_config=master_config, node_config=node_config, scheduler_config=scheduler_config,
            proxy_config=proxy_config, proxy_port=proxy_port, id=yt_id,
            local_cypress_dir=cypress_root, use_proxy_from_yt_source=False,
            enable_debug_logging=False, tmpfs_path=None, port_range_start=None,
            path=yt_dir)

        # UI is dead :( https://github.yandex-team.ru/yt/python/pull/341
        yt_client_conf_path = os.path.join(yt_dir, yt_id, "runtime_data/proxy/ui/config.js")
        yt_fqdn = socket.getfqdn()
        with open(yt_client_conf_path, 'a') as f:
            f.write('\n' + ('\n'.join([
                "YT.clusters.ui.secure = false;",
                "YT.clusters.ui.proxy = '{}:{}';".format(yt_fqdn, proxy_port),
                "YT.clusters.ui.authentication = 'none';"
            ])))
    yt.config.set_proxy('localhost:' + str(proxy_port))

    def module_filter(module):
        return hasattr(module, '__file__') and \
               '/pytz/' not in module.__file__ and \
               'statbox_bindings2' not in module.__file__ and \
               'qb2' not in module.__file__
    yt.config['pickling']['module_filter'] = module_filter

    if get_file_uploads_func is not None:
        file_uploads = get_file_uploads_func()
        for fu in file_uploads:
            fpath, ytpath = fu
            print 'Uploading %s to %s' % (fpath, ytpath)
            yt.smart_upload_file(fpath, destination=ytpath, placement_strategy='replace')

    def fin():
        yt_local.stop(yt_id, path=yt_dir)
        # Part of the kludge to restart dead nodes
        done_file = os.path.join(yt_dir, yt_id, '.done')
        with open(done_file, 'w') as f:
            f.write('Done')
        # shutil.rmtree(os.path.join(yt_dir, yt_id))

    request.addfinalizer(fin)
