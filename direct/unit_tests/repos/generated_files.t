#!/usr/bin/perl

# проверка на то, что автоматически-генерируемые файлы никто не закоммитит

use warnings;
use strict;

use Test::More;

use Test::ListFiles;
use Settings;

my @REGEXPS = (
    'data/(js|css|pages)(/.*)?/_(?!(ppc-advanced|ppc|yandex-global)\.css)[^\/]*$',
    'etc/settings.yaml',
    '.*\.pyc',
    'data/t/auto/',
    'data/t/news(_en|_ukr)?\.html',
    'data/t/archive(_en|_ukr)?\.html',
    );

# собираем файлы, генерируемые скриптом export_consts_to_js.pl
require 'maintenance/export_consts_to_js.pl';
our %to_export;
push @REGEXPS, map {"data/js/jq/$_.js"} keys %to_export;

my $DIR = $Settings::ROOT;

my @files = 
    map {$_ => 1}
    map {s/^$DIR\/+//; $_}
    grep { -f }
    Test::ListFiles->list_repository($DIR);

Test::More::plan(tests => scalar(@REGEXPS));

for my $re (@REGEXPS) {
    my @generated_files = grep {/^($re)$/} @files;
    ok(!@generated_files, "generated_files $re: ".join(", ", @generated_files));
}

