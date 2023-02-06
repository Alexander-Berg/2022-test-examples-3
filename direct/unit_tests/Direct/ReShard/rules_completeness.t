#!/usr/bin/perl 

use strict;
use warnings;

use Test::More;
use Test::Exception;

use Yandex::ListUtils;
use Yandex::DBUnitTest;

use Direct::ReShard;
use Direct::ReShard::Rules qw/%RULES_IDS %IGNORED_TABLES/;

# таблички нужно альтерить, удалять auto_increment и после этого удалять из списка
# не должно быть причин для пополнения таблицы
my %AUTOINC_EXCEPTIONS = map {$_ => 1} qw/

/;

my $resharder;
lives_ok { $resharder = Direct::ReShard->create(); };

my @schema_tables = Yandex::DBSchema::get_tables('ppc');
my %schema = map {$_ => 1} @schema_tables;
my %resharder_tables;

for my $table ($resharder->tables_list) {
    push @{ $resharder_tables{$table->name} }, $table;
    ok(exists $schema{$table->name}, "table ".$table->name." from rule exists in schema");
    ok($table->key_type eq 'ClientID' || exists $RULES_IDS{$table->key_type}, "Correct key for ".$table->name);
}

for my $table_name (sort @schema_tables) {
    # может быть несколько правил для таблицы
    for my $reshard_table (xflatten($resharder_tables{$table_name})) {
        ok(defined $reshard_table || exists $Direct::ReShard::Rules::IGNORED_TABLES{$table_name}, "rule for table $table_name");
        next if !defined $reshard_table;

        my $create_table_sql = Yandex::DBSchema::get_create_table_sql(db=>'ppc', table => $table_name);
        my $reshard_autoinc = defined $reshard_table->autoinc_key_idx || exists $AUTOINC_EXCEPTIONS{$table_name};
        ok(!(defined $reshard_table->autoinc_key_idx && exists $AUTOINC_EXCEPTIONS{$table_name}), "excess exception for $table_name");
        if ($create_table_sql =~ /AUTO_INCREMENT/i) {
            ok($reshard_autoinc, "auto_increment in $table_name");
        } else {
            ok(!$reshard_autoinc, "no auto_increment for $table_name");
        }
        if ($reshard_table->delete_keys_idx) {
            for my $delete_key_id (@{ $reshard_table->delete_keys_idx }) {
                my $delete_key_name = $reshard_table->_cols_schema->[$delete_key_id];
                my $key_not_null = ($create_table_sql =~ m/`(?i:$delete_key_name)`.*NOT NULL/);
                ok($key_not_null, "delete_key ".$delete_key_name." for table ".$reshard_table->name." should be NOT NULL");
            }
        }
    }
}



done_testing;

