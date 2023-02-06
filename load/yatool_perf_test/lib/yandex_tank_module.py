import os
import yatest.common
import json
import yaml
import boto3
import logging
import binascii

from botocore.exceptions import ClientError


def crc32_from_file(filename):
    try:
        with open(filename, 'rb') as f:
            buf = f.read()
            buf = (binascii.crc32(buf) & 0xFFFFFFFF)
    except Exception:
        logging.error('Failed to open file', exc_info=True)
        return

    return buf


def get_secret():
    access_key = os.environ.get('S3_KEY')
    secret_key = os.environ.get('S3_SECRET')

    return access_key, secret_key


def update_pandora_config(config, pandora_config, port):
    with open(config, 'r+b') as f:
        conf = f.read()
        conf_yaml = yaml.safe_load(conf)
        if conf_yaml['pandora'].get('config_file'):
            conf_yaml['pandora']['config_file'] = pandora_config
            with open(conf_yaml['pandora']['config_file'], 'r+b') as p:
                p_conf = p.read()
                pandora_yaml = yaml.safe_load(p_conf)
                pandora_yaml['pools'][0]['gun'].update({'target': 'localhost:%s' % port})
                p.write(yaml.dump(pandora_yaml, encoding=('utf-8')))
        else:
            conf_yaml['pandora']['config_content']['pools'][0]['gun'].update({'target': 'localhost:%s' % port})

        f.write(yaml.dump(conf_yaml, encoding=('utf-8')))

    return config


def upload_to_s3(path_data, access_key, secret_key):

    id = crc32_from_file(path_data)

    if id is not None:
        s3_client = boto3.client('s3',
                                 endpoint_url='http://s3.mds.yandex.net',
                                 aws_access_key_id=access_key,
                                 aws_secret_access_key=secret_key,
                                 use_ssl=False, verify=False)

        try:
            s3_client.upload_file(path_data, 'luna', 'offline_report/offline_data_%d.json' % id)
        except ClientError as e:
            logging.error(e)

    return id


def check_sla(path_report, path_sla):
    r = open(path_report, "r")
    report = json.load(r)
    r.close()
    s = open(path_sla, "r")
    sla = yaml.safe_load(s)
    for k in sla['quantiles'].keys():
        if report['quantiles'][k] > sla['quantiles'][k]:
            return False

    return True


def run_yandex_tank(config, sla_conf, gun='phantom', pandora_path='load/projects/yandex-tank-package/pandora', pandora_config=None):
    port = str(os.environ.get('RECIPE_PORT'))

    if gun == 'phantom':
        res = yatest.common.execute(
            [
                yatest.common.build_path("load/projects/yandex-tank-package/yandex-tank"),
                "-c", config,
                "-o", "core.artifacts_base_dir={}".format(yatest.common.output_path()),
                "-o", "core.artifacts_dir=artifacts",
                "-o", "phantom.phantom_path={}".format(yatest.common.build_path("load/projects/yandex-tank-package/phantom")),
                "-o", "phantom.port={}".format(port)
            ],
            check_exit_code=True
        )
    elif gun == 'pandora':
        path_config = update_pandora_config(config, pandora_config, port)
        res = yatest.common.execute(
            [
                yatest.common.build_path("load/projects/yandex-tank-package/yandex-tank"),
                "-c", path_config,
                "-o", "core.artifacts_base_dir={}".format(yatest.common.output_path()),
                "-o", "core.artifacts_dir=artifacts",
                "-o", "pandora.pandora_cmd={}".format(yatest.common.build_path(pandora_path))
            ],
            check_exit_code=True
        )

    path_report = yatest.common.output_path("artifacts/offline_report.json")
    path_data = yatest.common.output_path("artifacts/offline_data.log")

    access_key, secret_key = get_secret()

    if access_key and secret_key:
        upload_id = upload_to_s3(path_data, access_key, secret_key)
    else:
        logging.error('Variable S3_KEY and S3_SECRET not found')
        upload_id = None

    return res.exit_code, check_sla(path_report, sla_conf), upload_id
