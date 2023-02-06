#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import optparse
from assessing import AssessingFactory
from main import dump_query2loss
from main import dump_losses
import pprint
from v1 import V1
from qloss_statuses import *
from qloss_calcer import *
import traceback

METASEARCH_LOCATION = ''

def process_options(argv):
    opt = optparse.OptionParser()
    opt.add_option('--metasearch', help = 'metasearch location host:port', default = 'addrs:17140')
    opt.add_option('--db_sqlite', help = '', default = '')
    opts, args = opt.parse_args(argv)
    global METASEARCH_LOCATION
    METASEARCH_LOCATION = opts.metasearch
    return opts, args

def check(test_name, input_str, true_loss, meta_location):
    q, got_loss, docs, metaloss_counts = calc_qloss(input_str, V1, meta_location)
    only_losses = dict((k, v) for k, v in got_loss.items() if v == 1)
    if only_losses != true_loss:
        print '"%s" fail: %s' % (test_name, input_str)
        print 'Qloss got:'
        pprint.pprint(got_loss)
        print 'Leaves:'
        pprint.pprint(only_losses)
        print 'Qloss wanted:'
        pprint.pprint(true_loss)
        exit(1)
    else:
        print '"%s" passed: %s' % (test_name, input_str)
    return (q, got_loss)

SHORT_TESTS_DATA = [
    ('misspeller wrong autoreplace', 'масква\t213\t37.60,55.64@@0.02,0.01\tmobile-maps\t\t@@\t1253116773\t', {MISSPELLER_WRONG_AUTOREPLACE: 1}, lambda: METASEARCH_LOCATION),
]

TESTS_DATA = [
    ('no loss', 'большой театр москва\t213\t37.60,55.64@@0.02,0.01\tmobile-maps\t\tбольшой театр@@Москва\t1018044205\t', {NO_LOSS: 1}, lambda: METASEARCH_LOCATION),
    ('no loss by db.sqlite', 'большой театр\t213\t37.618514,55.760117@@0.024723,0.011101\tmobile-maps\t\t@@\t1\t1', {NO_LOSS: 1}, lambda: METASEARCH_LOCATION),
    ('not found in backa', 'блабла\t213\t37.6,55.6@@0.2,0.01\tmobile-maps\t\t@@\t\t', {BACKA_NOT_FOUND_MANUALLY: 1}, lambda: METASEARCH_LOCATION),
    ('misspeller incorrect suggest', 'блабла\t213\t37.60,55.64@@0.02,0.01\tmobile-maps\tбольшой театр\t@@\t1018044205\t', {MISSPELLER_INCORRECT_SUGGEST: 1}, lambda: METASEARCH_LOCATION),
    ('misspeller turned off', 'блабла\t213\t37.618493,55.760018@@0.02,0.01\twizbiz-new\tбольшой театр\t@@\t1018044205\t', {MISSPELLER_TURNED_OFF: 1}, lambda: METASEARCH_LOCATION),
    ('misspeller wrong autoreplace', 'масква\t213\t37.654321,55.654321@@0.9876543,0.9876543\tmobile-maps\t\t@@\t1253116773\t', {MISSPELLER_WRONG_AUTOREPLACE: 1}, lambda: METASEARCH_LOCATION),
    ('geoaddr', 'хрень хрень хрень\t213\t37.60,55.64@@0.02,0.01\tmobile-maps\tхрень хрюнь хрюнь\tбольшой театр@@москва\t1018044205\t', {GEOADDR: 1}, lambda: METASEARCH_LOCATION),
    ('misspell_classifier', 'kill fish\t2\t30.373,59.937@@0.016,0.002\tmobile-maps\t\t@@\t\t849780', {MISSPELL_CLASSIFIER: 1}, lambda: METASEARCH_LOCATION),
    ('bus_top_verticals_classifier', 'москва\t213\t37.78135421971051,55.673636858232065@@0.001,0.001\tmobile-maps\t\t@@\t1065617412\t', {BUSINESS_TOPONYM_VERTICALS_CLASSIFIER: 1}, lambda: METASEARCH_LOCATION),
    ('form_vertical filter by window', 'инжир балаклава\t213\t37.78,55.67@@0.001,0.001\tmobile-maps\t\t@@\t\t46987716', {MIDDLE_VERTICAL_FILTER_BY_WINDOW: 1}, lambda: METASEARCH_LOCATION),
    ('form_vertical deduplication', 'ночной клуб ананас\t213\t37.354842090000005,55.836645486991834@@0.008181,0.002647\tmobile-maps\t\t@@\t\t6906339', {MIDDLE_VERTICAL_DEDUPLICATION: 1}, lambda: METASEARCH_LOCATION),
    ('ranking', 'яндекс\t213\t37.78,55.67@@1,1\tmobile-maps\t\t@@\t\t29', {BASESEARCH_RANKING: 1}, lambda: METASEARCH_LOCATION),
    ('ranking sort', 'яндекс льва толстого, 20\t213\t37.78,55.67@@1,1\tmobile-maps\t\tяндекс@@льва толстого, 20\t1124715036\t', {BASESEARCH_RANKING_DISTANCE: 1}, lambda: METASEARCH_LOCATION),
    ('rubric ranking', 'кафе\t213\t37.78,55.67@@1,1\tmobile-maps\t\t@@\t1400454047\t', {BASESEARCH_RANKING_RUBRIC: 1}, lambda: METASEARCH_LOCATION),
    ('not in index business', 'блабла\t213\t37.6,55.6@@0.2,0.01\tmobile-maps\t\t@@\t1\t', {BASESEARCH_NOT_IN_INDEX: 1}, lambda: METASEARCH_LOCATION),
    ('not in index wiki', 'блабла\t213\t37.6,55.6@@0.2,0.01\tmobile-maps\t\t@@\t\t1', {BACKA_NOT_FOUND_MANUALLY: 1}, lambda: METASEARCH_LOCATION),
    ('basesearch not called', 'Россия, Москва\t213\t37.6,55.6@@0.2,0.01\tmobile-maps\t\t@@\t1\t1', {BASESEARCH_NOT_CALLED: 1}, lambda: METASEARCH_LOCATION),
    ('base not in search window', 'кафе\t213\t37.6,55.6@@0.2,0.01\tmobile-maps\t\t@@\t1018044205\t', {BASESEARCH_NOT_IN_SEARCH_WINDOW: 1}, lambda: METASEARCH_LOCATION),
    ('accept doc', 'блаблабла\t213\t37.6,55.6@@1,1\tmobile-maps\t\t@@\t1018044205\t', {BASESEARCH_ACCEPT_DOC: 1}, lambda: METASEARCH_LOCATION),
    ('accept doc rubric', 'кафе\t213\t37.6,55.6@@1,1\tmobile-maps\t\t@@\t1018044205\t', {BASESEARCH_ACCEPT_DOC_RUBRIC: 1}, lambda: METASEARCH_LOCATION),
    ('exception', 'кафе\t213\t37.6,55.6@@1,1\tmobile-maps\t\t@@\t1018044205\t', {EXCEPTION: 1}, lambda: ''),
]

def main(argv):
    opts, args = process_options(argv)
    if opts.db_sqlite:
        try:
            AssessingFactory.init_factory(opts.db_sqlite)
        except:
            traceback.print_exc()
            raise Exception('Can\'t load %s as an assesments db' % opts.db_sqlite)
    queries_w_loss = []
    for test_msg, inp_str, valid_result, metasearch in TESTS_DATA:
        q, qloss_stages = check(test_msg, inp_str, valid_result, metasearch())
        queries_w_loss.append((q, qloss_stages))

    average_losses = merge_losses([loss for (q, loss) in queries_w_loss])
    print '\nLoss summary:'
    pprint.pprint(average_losses)
    print '\nQueries and losses:'
    dump_query2loss(queries_w_loss, sys.stdout)
    print '\nNo aggregation:'
    dump_losses(average_losses, sys.stdout)
    print '\nGeneral aggregation:'
    dump_losses(aggregate(average_losses, GENERAL_AGGREGATION), sys.stdout, GENERAL_AGGREGATION)
    print '\nMisspeller aggregation:'
    dump_losses(aggregate(average_losses, MISSPELLER_AGGREGATION), sys.stdout, MISSPELLER_AGGREGATION)
    print '\nBacka aggregation:'
    dump_losses(aggregate(average_losses, BACKA_AGGREGATION), sys.stdout, BACKA_AGGREGATION)
    print '\nRanking aggregation:'
    dump_losses(aggregate(average_losses, RANKING_AGGREGATION), sys.stdout, RANKING_AGGREGATION)
    print '\nMisc search aggregation:'
    dump_losses(aggregate(average_losses, SEARCH_MISC_AGGREGATION), sys.stdout, SEARCH_MISC_AGGREGATION)
    print '\n Accept doc aggregation:'
    dump_losses(aggregate(average_losses, ACCEPT_DOC_AGGREGATION), sys.stdout, ACCEPT_DOC_AGGREGATION)

if __name__ == '__main__':
    main(sys.argv[1:])
