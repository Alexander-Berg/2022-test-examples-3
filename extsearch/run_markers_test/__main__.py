# -*-coding:utf-8-*-
import argparse
import codecs
import json
import logging
import math
import yt.wrapper as yt

import numpy as np
import scipy.stats as ss

from collections import defaultdict, namedtuple
from jinja2 import Environment, PackageLoader


from extsearch.ymusic.quality.offline.shooter_lib import shooter


logger = logging.getLogger(__name__)


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-c', '--conf')
    parser.add_argument('-o', '--output')
    return parser.parse_args()


def read_etalon_data(etalon_table):
    etalon_data = {}
    for rec in yt.read_table(etalon_table):
        if 'real_url' in rec:
            qs_start = rec['real_url'].find('?')
            url = rec['real_url']
            if qs_start >= 0:
                url = url[:qs_start]
        else:
            url = rec.get('url', 'empty')
        url = url.replace('/mp/', '/')
        entity_id = rec['entity_id']
        if 'user_id' in rec and rec['user_id'] is not None:
            entity_id = '{}:{}'.format(rec['user_id'], entity_id)
        etalon_data[rec['query']] = {
            'url': url,
            'blockType': rec['blockType'],
            'entityId': entity_id,
            'extraCgi': rec.get('extraCgi', {}),
        }
    return etalon_data


def process_shooter_result(result):
    if result is None:
        return {
            'url': 'empty',
            'blockType': None,
            'entityId': None,
        }
    url, block_type, entity_id, _ = result
    return {
        'url': url,
        'blockType': block_type,
        'entityId': entity_id,
    }


def is_different(left_result, rights_result):
    if left_result['blockType'] != rights_result['blockType']:
        return True
    return left_result['entityId'] != rights_result['entityId']


ShootingResult = namedtuple('ShootingResult', ['stable_matches', 'exp_matches', 'diff_results'])


def shoot_single_query(query, conf, **extra_cgi):
    extra_params = conf['default_cgi']
    extra_params.update(extra_cgi)
    stable_result = process_shooter_result(shooter.get_most_popular_answer_safe(
        query,
        method='post',
        host_type='STABLE_NIRVANA',
        service='YMUSIC_PRIEMKA',
        **dict(extra_params, **conf['production'])
    ))
    logger.debug('Query="%s", stable result: %s', query, stable_result)
    experiments = {}
    for exp_name, exp in conf['experiments'].items():
        if exp_name == 'etalon':
            raise ValueError('Cant use "etalon" as experiment name')
        exp_result = process_shooter_result(shooter.get_most_popular_answer_safe(
            query,
            method='post',
            host_type='STABLE_NIRVANA',
            service='YMUSIC_PRIEMKA',
            **dict(extra_params, **exp)
        ))
        experiments[exp_name] = exp_result
        logger.debug('Query=%s, %s result: %s', query, exp_name, exp_result)
    return stable_result, experiments


def shoot_for_results(conf, etalon):
    stable_matches = []
    exp_matches = defaultdict(list)
    diff_results = []
    for query, etalon_result in etalon.items():
        stable_result, experimental_results = shoot_single_query(query, conf, **etalon_result['extraCgi'])
        have_diff = False
        stable_differ = is_different(stable_result, etalon_result)
        stable_matches.append(int(not stable_differ))
        have_diff = have_diff or stable_differ
        experiments = {}
        for exp_name, result in experimental_results.items():
            exp_differ = is_different(result, etalon_result)
            have_diff = have_diff or exp_differ
            exp_matches[exp_name].append(int(not exp_differ))
            experiments[exp_name] = {
                'is_different': exp_differ,
                'result': result
            }
        if have_diff:
            diff_results.append({
                'query': query,
                'etalon': etalon_result,
                'production': {'result': stable_result, 'is_different': stable_differ},
                'experiments': experiments,
            })
    return ShootingResult(stable_matches=stable_matches, exp_matches=exp_matches, diff_results=diff_results)


def process_experimental_results(stable_matches, exp_matches):
    experiments_data = {}
    num_matched_stable = np.sum(stable_matches)
    for exp_name, matches in exp_matches.items():
        num_matched_exp = np.sum(matches)
        differ_from_stable = int(abs(num_matched_stable - num_matched_exp))
        diff_percentage = float(200.0 * differ_from_stable / (num_matched_exp + num_matched_stable))
        ttest_result = ss.ttest_ind(stable_matches, matches)
        logger.debug("T-Test result:  %s", ttest_result)
        signed_pvalue = ttest_result.pvalue
        pvalue_sign = math.copysign(1, signed_pvalue)
        pvalue = abs(float(signed_pvalue))
        experiments_data[exp_name] = {
            'mean': float(np.mean(matches)),
            'diff': differ_from_stable,
            'diff_percentage': diff_percentage,
            'ttest_pvalue': pvalue,
            'diff_importance': pvalue <= 0.05,
            'better': pvalue_sign > 0,
        }
    return experiments_data


def render_report(output_path, template_data):
    jinja_env = Environment(
        loader=PackageLoader('extsearch.ymusic.quality.offline.run_markers_test', 'templates')
    )
    template = jinja_env.get_template('report.j2')
    with codecs.open(output_path, 'w', 'utf-8') as out:
        out.write(template.render(template_data))


def main():
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s %(levelname)s %(module)s %(message)s')
    args = parse_args()

    with open(args.conf) as f:
        conf = json.load(f)

    logger.info('Obtaining etalon data...')
    etalon = read_etalon_data(conf['etalon_table'])

    logger.info('Shooting for results...')
    shooting_results = shoot_for_results(conf, etalon)

    logger.debug('Processing experimental results...')
    experiments_data = process_experimental_results(shooting_results.stable_matches, shooting_results.exp_matches)

    logger.debug('Building template...')
    render_report(output_path=args.output, template_data={
        'diff_results': shooting_results.diff_results,
        'production_mean': float(np.mean(shooting_results.stable_matches)),
        'experiments': experiments_data,
    })


if __name__ == "__main__":
    main()
