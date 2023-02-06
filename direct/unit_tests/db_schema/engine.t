#!/usr/bin/perl

use warnings;
use strict;

use File::Slurp;
use Test::More;

use Yandex::DBSchema;

use Test::ListFiles;
use Settings;

use utf8;

my %expected_engine = (
    );


for my $schema_file (grep {-f && /\.schema.sql$/} Test::ListFiles->list_repository($Yandex::DBSchema::DB_SCHEMA_ROOT)) {
    my ($db, $table) = $schema_file =~ / ([^\/]+) \/+ ([^\/]+)\.schema\.sql $ /x;
    my ($engine) = scalar(read_file($schema_file)) =~ /ENGINE=(\w+)/;
    my $expected_engine = $expected_engine{"$db.$table"} || $expected_engine{$db} || 'InnoDB';
    is($engine, $expected_engine, "$db.$table");
}

done_testing;
