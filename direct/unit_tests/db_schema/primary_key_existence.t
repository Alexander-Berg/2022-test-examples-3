#!/usr/bin/perl

use warnings;
use strict;

use File::Slurp;
use Test::More;

use Yandex::DBSchema;

use Test::ListFiles;
use Settings;

use utf8;

for my $schema_file (grep {-f && /\.schema.sql$/} Test::ListFiles->list_repository($Yandex::DBSchema::DB_SCHEMA_ROOT)) {
    my ($db, $table) = $schema_file =~ / ([^\/]+) \/+ ([^\/]+)\.schema\.sql $ /x;

    my $create_table_sql = read_file($schema_file);

    ok($create_table_sql =~ /PRIMARY KEY/, "$db.$table");
}

done_testing;
