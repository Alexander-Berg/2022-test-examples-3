#!/usr/bin/env python2.7
import search, net
import common_lib as cl
import json
import pandas as pd


def compare_tables(date, old_path, new_path):
    print('comparing tables for {}...'.format(date))
    old_path = old_path + '/' + date
    new_path = new_path + '/' + date
    old = cl.download_df(old_path).set_index('interval').sort_index()
    new = cl.download_df(new_path).set_index('interval').sort_index()

    assert set(old.columns) == set(new.columns)
    eps_base = 1e-4
    eps_integral_cost_search_moffice = 1e-2
    is_error = False
    for col in old.columns:
        ocol = old[col]
        ncol = new[col]
        if ocol.dtype == float:
            eps = eps_base if col != 'integral_cost_search_moffice' else eps_integral_cost_search_moffice
            diff = (ocol - ncol).abs() / ((ocol + ocol + 1e-15) / 2) > eps

            if diff.sum() > 0:
                print(ocol.name)
                print(pd.merge(
                    ocol.rename('OLD'),
                    ncol.rename('NEW'),
                    left_index=True, right_index=True, suffixes=('', '')))
                print()
                is_error = True
    print('{} {} ok'.format(date, 'not' if is_error else ''))
    return is_error


if __name__ == '__main__':
    TEST = 'search'
    module = search if TEST == 'search' else net
    config = cl.Config(**json.load(open('test_{}_config.json'.format(TEST), 'r')))

    if config.test_type == 'compare':
        old_path = module.FOLDER + '/stats'
        new_path = getattr(config, 'save_folder') + '/stats'

        error_dates = []
        for date in cl.date_range_incl(config.start_date, config.end_date):
            status = compare_tables(date, old_path, new_path)
            if status is True:
                error_dates.append(date)
        print('bad dates: ')
        from pprint import pprint
        pprint(error_dates)

    if config.test_type == 'compute':
        module.main(config)
