#!/usr/bin/perl

=pod

     $Id$
     Проверка, что при создании таблиц с использованием create_db_by_schema, подключается DBSchema

=cut

use warnings;
use strict;

use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

use utf8;
use open ':std' => ':utf8';

my $ROOT = "$Settings::ROOT/protected";

# составляем списки файлов
my @files_to_check;
for my $file (grep {/\.(pm|pl)$/ && -f} Test::ListFiles->list_repository($ROOT)) {
    # пропускаем сам DBSchema.pm
    if ($file !~ /^$ROOT\/DBSchema.pm$/) {
        push @files_to_check, $file;
    }
}

@files_to_check = sort @files_to_check;

Test::More::plan(tests => scalar(@files_to_check));

# проверяем все по списку @files_to_check
for my $file (@files_to_check) {
    my $t = read_file($file);
    ok($t !~ /\bcreate_table_by_schema\b/sm || $t =~ /^use *Yandex::DBSchema\b/m, "No 'use Yandex::DBSchema;' in $file: $@");
}
