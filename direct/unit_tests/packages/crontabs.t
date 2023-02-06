#!/usr/bin/perl

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

my $PKG_ROOT = "$Settings::ROOT/packages/yandex-direct/debian";

# ищем все кронтаб-файлы
# TODO включить в проверку и генерируемые из METADATA кронтабы
my @files = sort grep {-f && /\.cron\.d$/} Test::ListFiles->list_repository($PKG_ROOT);

Test::More::plan(tests => 3*scalar(@files));

for my $file (@files) {
    my $crontab = read_file($file);
    ok($crontab !~ /\r/, "windows line-endings in $file");
    ok($crontab =~ /\n$/, "crontab-end in $file");

    my $ELM_RE = qr/\*|\*\/\d+|\d+-\d+\/\d+|\d+|\d+-\d+/;
    my $FIELD_RE = qr/($ELM_RE(,$ELM_RE)*)/;
    my @bad_lines =
        grep {!/^ ( \@\w+ | $FIELD_RE(\s+$FIELD_RE){4} ) \s+ [\w\-]+ \s+ /xi}
        grep {!/^\s*\w+\s*=/i}
        grep {!/^\s*(#.*)?$/}
        split /\n/, $crontab;
    ok(@bad_lines == 0, "errors in $file:".join("", map {"\n  $_"} @bad_lines));
}
