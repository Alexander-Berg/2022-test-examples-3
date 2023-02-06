import re
from logging import DEBUG, ERROR


class ShardRegistration(object):
    def __init__(self):
        self.shard_dir = None
        self.required_shard_dir = None
        self.registration_count = 0
        self.install_script = None


iss_shard_registration = None
bs_shard_registration = None


def init():
    global iss_shard_registration
    global bs_shard_registration

    iss_shard_registration = ShardRegistration()
    bs_shard_registration = ShardRegistration()


def execute(popenargs, logger, stdout_log_level=DEBUG, stderr_log_level=ERROR, **kwargs):
    return 0


def sky_download(resource_url, dst_dir, logger, dl_speed, ul_speed):
    pass


_BSCONFIG_ITAGS = 'OPT_shardid=018 SAS_IMGS_MR_BASE a_ctype_test a_dc_sas a_geo_sas a_itype_shardtool ' \
                  'a_line_sas-1.1.2 a_metaprj_imgs a_prj_imgs-main a_tier_ImgTier0 ' \
                  'a_topology_group-SAS_IMGS_MR_BASE a_topology_stable-85-r75 ' \
                  'a_topology_version-stable-85-r75 shard_state=20160604-084317 ' \
                  'shard_name=imgsidx make_bsconfig_shard=1'


def get_instance_properties():
    properties = {
        'BSCONFIG_IHOST': 'slovo003',
        'BSCONFIG_INAME': 'slovo003:23490',
        'BSCONFIG_IPORT': '23490',
        'BSCONFIG_ITAGS': _BSCONFIG_ITAGS
    }
    return properties


def get_instance_tags():
    return dict(kv.split('=') for kv in re.findall('([^ .+]+=[^ .+]+)', _BSCONFIG_ITAGS))


def create_logger(file_path, log_level):
    pass


def reopen_log():
    pass


def iss_register_shard(shard_dir, required_shard_dir, install_script, logger):
    iss_shard_registration.shard_dir = shard_dir
    iss_shard_registration.required_shard_dir = required_shard_dir
    iss_shard_registration.install_script == install_script
    iss_shard_registration.registration_count += 1


def get_shard_info_with_full_shard(shard_dir, logger):
    return {
        'full': {
            'size': 57104050583,
            'urls': [
                'rbtorrent:0b8620838487070dba09e532519cdda81505b045'
            ]
        },
        'shardId': shard_dir
    }


def get_shard_info_with_inc_shard(shard_dir, logger):
    return {
        'full': {
            'size': 57104050583,
            'urls': [
                'rbtorrent:0b8620838487070dba09e532519cdda81505b045'
            ]
        },
        'incremental': {
            'requiredShardId': 'required_shard_dir',
            'size': 1002505147,
            'urls': [
                'rbtorrent:03b030de6b24a09ae29368c4c7dc149c5dc5cffa'
            ]
        },
        'shardId': shard_dir
    }


def get_shard_info_empty(shard_dir, logger):
    return {}


def bs_register_shard(cms_rpc_addr, shard_dir, required_shard_dir, shard_id, is_incremental, logger):
    bs_shard_registration.shard_dir = shard_dir
    bs_shard_registration.required_shard_dir = required_shard_dir
    bs_shard_registration.registration_count += 1
