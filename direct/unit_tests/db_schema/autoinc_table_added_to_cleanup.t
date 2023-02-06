#!/usr/bin/perl

use warnings;
use strict;

use Test::Exception;
use Test::More;

use Yandex::DBShards;
use Yandex::DBUnitTest qw/init_test_dataset/;

use Test::ListFiles;
use Settings;

use utf8;


my %autoinc_tables;
my %test_db;
# copied from Yandex::DBShards::clear_all_autoinc_tables
for my $key (keys %Yandex::DBShards::SHARD_KEYS) {
    my $info = $Yandex::DBShards::SHARD_KEYS{$key};
    next if exists $info->{shard_key} || exists $info->{chain_key};
    $autoinc_tables{ $info->{table} } = $key;
    $test_db{ $info->{table} } = { original_db => PPCDICT, rows => [] };
}
init_test_dataset(\%test_db);

for my $schema_file (grep {-f && /\binc_.*\.schema.sql$/} Test::ListFiles->list_repository($Yandex::DBSchema::DB_SCHEMA_ROOT . "/ppcdict")) {
    SKIP: { 
        my ($db, $table) = $schema_file =~ / ([^\/]+) \/+ ([^\/]+)\.schema\.sql $ /x;

        skip("table $table key is not unique, cleanup implemented manually in script", 2) if $table =~ m/^inc_(?:(?:client|camp|adgroup)_additional_targetings_id|lal_segments)$/;

        # Все инкрементные таблицы, даже если не используются в PERL должны быть добавлены
        # это нужно, так как в перле работает скрипт их очистки
        ok(exists $autoinc_tables{$table}, "Table $table should specified in Yandex::DBShards::SHARD_KEYS via Settings.pm");
    
        # проверяем, что таблица корректно вписана в Settings
        # https://wiki.yandex-team.ru/Direct/CodeStyleGuide/DB/#tablicygenerirujushhieid
        # если тест падает, вероятно что ключ в табличке не такой, под каким она добавлена в Settings
        my $key = $autoinc_tables{$table} ;
        lives_ok { Yandex::DBShards::clear_autoinc_table($key) } "clear_autoinc_table works success for table $table (key $key)";
    }
}

done_testing;
