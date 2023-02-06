#!/usr/bin/perl

use Direct::Modern;

use Test::More tests => 2;
use Test::Exception;

use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

use BS::ExportWorker ();
use Settings;

init_test_dataset({
    bs_export_queue => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [ {
                cid => 1,
                queue_time => '2015-01-01 00:00:00',
                camps_num => 0,
                banners_num => 0,
                contexts_num => 0,
                bids_num => 0,
                prices_num => 0
            } ],
        },
    },
});
my $initial_seq_time = get_one_field_sql(PPC(shard => 1), 'SELECT seq_time FROM bs_export_queue WHERE cid = ?', 1);

lives_ok { do_update_table(PPC(shard => 1), 'bs_export_queue',
                          {seq_time__dont_quote => "seq_time + INTERVAL $BS::ExportWorker::DELAY_TIME"},
                          where => {cid => 1}
                          )
} '$BS::ExportWorker::DELAY_TIME is correct';

my $new_seq_time = get_one_field_sql(PPC(shard => 1), 'SELECT seq_time FROM bs_export_queue WHERE cid = ?', 1);
cmp_ok($initial_seq_time, 'lt', $new_seq_time, 'new seq_time greater than previous value');
