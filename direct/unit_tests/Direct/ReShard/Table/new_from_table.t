#!/usr/bin/perl 

=head1

    Тест фиксирует поведение Direct::ReShard::Table->new_from_rule на таблицах с интересными фичами

=cut

use strict;
use warnings;

use FindBin qw/$Bin/;

use Test::More;
use Test::Deep;

use Settings;

use Yandex::DBUnitTest qw/:all/;

use Direct::ReShard::Table;

# подкладываем свои определения таблиц
$Yandex::DBSchema::DB_SCHEMA_ROOT = "$Bin/dbschema";

my @test_cases = (
    {
        title => "GENERATED-колонка",
        table => 'bs_resync_queue_test',
        rule => { 
            autoinc => 1,
            key => 'cid', 
        },
        result => {
            'key_type' => 'cid',
            'name' => 'bs_resync_queue_test',
            'select_sqls' => [
                'SELECT STRAIGHT_JOIN `t`.`Id`,`t`.`cid`,`t`.`bid`,`t`.`pid`,`t`.`sequence_time`,`t`.`priority` FROM bs_resync_queue_test AS t WHERE cid in (%s) '
            ],
            'key' => 'cid',
            '_cols_schema' => [
                'Id',
                'cid',
                'bid',
                'pid',
                'sequence_time',
                'priority',
                'Priority_Inverse'
            ],
            'autoinc_key_idx' => 0,
            'insert_sql' => 'INSERT  INTO bs_resync_queue_test (`cid`, `bid`, `pid`, `sequence_time`, `priority`) VALUES %s',
            '_cols_by_shards' => {},
            'delete_keys_idx' => [
                1
            ],
        },
    },
    {
        title => 'auto_increment не первой колонкой, несколько колонок на одной строке',
        table => 'events_test',
        rule => { 
            key_type => 'uid', 
            key     => 'objectuid',
            autoinc => 1, 
        },
        result => {
            'key_type' => 'uid',
            'key' => 'objectuid',
            '_cols_schema' => [
                'eventobject',
                'eventtype',
                'eventtime',
                'objectid',
                'objectuid',
                'eid',
                'uid',
                'cid',
                'json_data'
            ],
            'select_sqls' => [
                'SELECT STRAIGHT_JOIN `t`.`eventobject`,`t`.`eventtype`,`t`.`eventtime`,`t`.`objectid`,`t`.`objectuid`,`t`.`eid`,`t`.`uid`,`t`.`cid`,`t`.`json_data` FROM events_test AS t WHERE objectuid in (%s) '
            ],
            'insert_sql' => 'INSERT  INTO events_test (`eventobject`, `eventtype`, `eventtime`, `objectid`, `objectuid`, `uid`, `cid`, `json_data`) VALUES %s',
            'delete_keys_idx' => [
                4
            ],
            'name' => 'events_test',
            'autoinc_key_idx' => 5,
            '_cols_by_shards' => {}
        },
    },
);

for my $tc (@test_cases){
    copy_table(PPC(shard => 'all'), $tc->{table}, engine => "InnoDB");
    my $tbl = Direct::ReShard::Table->new_from_rule($tc->{table}, $tc->{rule}, 1);
    is_deeply($tbl, $tc->{result}, $tc->{title} || $tc->{table});
}


done_testing;
