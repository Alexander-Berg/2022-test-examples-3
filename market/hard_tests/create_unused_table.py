import os
import sys
import optparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__name__)))

from market.idx.marketindexer.marketindexer import miconfig
import market.pylibrary.database as database


def main():
    parser = optparse.OptionParser(usage='usage: %prog TABLE_PREFIX')
    parser.add_option('-f', dest='feedlog_files', help='feedlog-uploader input files', type='string', action='append', default=[])
    parser.add_option('-r', dest='auction_results_files', help='auction-results-uploader input files', type='string', action='append', default=[])
    parser.add_option('-g', dest='generation', help='current generation', type='string', default='00000000_0000')

    (options, args) = parser.parse_args()
    if not args:
        return

    table_prefix = args[0]
    print >>sys.stderr, options

    ds = miconfig.default().datasources
    super_connection = database.connect(**ds['super'])

    with super_connection.begin():
        super_connection.execute_sql('create table if not exists `%s` (not_used tinyint not null primary key)' %
                                     (table_prefix + options.generation))

    print >>sys.stdout, 'OK'

if '__main__' == __name__:
    main()
