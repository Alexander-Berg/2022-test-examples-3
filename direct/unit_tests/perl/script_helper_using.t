#!/usr/bin/perl

=pod

    $Id$
    Проверка на то, что во всех скриптах есть ScriptHelper

=cut

use warnings;
use strict;
use File::Slurp;
use Test::More;
use File::Basename;

use Test::ListFiles;
use Settings;

my %IGNORES = map {$_ => 1} qw/
    get_yacatalog.pl

    startup2.pl
    warmup_geotools.pl

    ppcOtherSwitchmanScriptsCrontabs.pl
    ppcOtherScriptsMonruns.pl
/;

# ищем все перловые файлы
my @files = grep {
    -f 
    && /\.pl$/
    && !/\/(maintenance|one-shot|API\/Samples|nginx|prebuild|tools)\//
} Test::ListFiles->list_repository("$Settings::ROOT/protected");

Test::More::plan(tests => scalar(@files) + scalar(keys %IGNORES));

for my $file (@files) {
    my $has_sh = (scalar(read_file($file)) =~ /^\s*use\s*ScriptHelper/m);
    my $base_file = basename($file);
    if ($IGNORES{ $base_file }) {
        ok(!$has_sh, "IGNORE for $file is needed");
        $IGNORES{ $base_file }--;
    } else {
        ok($has_sh, "ScriptHelper in $file");
    }
}

for my $file (keys %IGNORES) {
    is($IGNORES{$file}, 0, "IGNORE for $file is ever used");
}
