# coding: utf-8


import re


REGEXP = re.compile(r'^(\d+)[\w\d._-]+?\.([\w_-]+)\.yaml$')


# Смотри детали в тикете CSADMIN-23607
def test_configs_name_unique(get_primer_configs, environment, excludes):

    hashes = set()

    for config_data in get_primer_configs(environment):

        filename = __get_filename(config_data)
        number, condgroup = __get_number_and_condgroup(filename)
        v_hash = '{}-{}'.format(number, condgroup)

        if filename in excludes:
            continue

        if v_hash in hashes:
            assert False, 'Config {} with number {} and conductor group {} already exists!'.format(
                filename,
                number,
                condgroup
            ) + ' Use a unique combination of a number and a conductor group or add it to exclude list.'

        hashes.add(v_hash)


def __get_filename(config_data):

    path = config_data[0]['filename']
    return path.split('/')[-1]


def __get_number_and_condgroup(filename):
    return REGEXP.search(filename).groups()
