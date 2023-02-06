#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Settings;
use Test::ListFiles;
use Yandex::Shell qw/yash_quote/;

# ищем все перловые файлы
my @files = grep {-f && /\.py$/} Test::ListFiles->list_repository("$Settings::ROOT/python");
@files = map {s/^$Settings::ROOT\///; $_} sort @files;

Test::More::plan(tests => 2 * scalar(@files));

for my $file (@files) {
    my $cmd = qq{cd $Settings::ROOT; python -c 'import sys; compile(open(sys.argv[1], "U").read(), sys.argv[1], "exec")' } . yash_quote($file) . qq{ 2>&1};
    my @out = `$cmd`;
    ok($? == 0, "compiling $file");
    ok(@out == 0, "warnings $file:\n".join("\n", map {chomp; "    $_"} @out));
}
