#!/usr/bin/perl

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

my $ETC_ROOT = "$Settings::ROOT/etc";

# ищем все перловые файлы
my @files = sort grep {-f && /mysql.cnf$/} Test::ListFiles->list_repository($ETC_ROOT);

Test::More::plan(tests => scalar(@files));

for my $file (@files) {
    my $cnf = read_file($file);
    ok($cnf =~ / \n\s*\[mysqld\] .* \n\s*(default-character-set|character-set-server)\s*=\s*\S+\n/xs, "no default-character-set in $file");
}
