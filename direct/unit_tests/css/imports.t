#!/usr/bin/perl

use warnings;
use strict;

use File::Slurp;
use File::Basename qw/dirname/;
use Cwd qw/realpath/;
use Test::More;
use List::MoreUtils qw/any/;

use Test::ListFiles;

use Settings;

my @DATA_ROOTS = ("$Settings::ROOT/data", "$Settings::ROOT/data3",);

my %FILES;
my $tests = 0;
for my $file (grep {-f && /\.css$/} map {Test::ListFiles->list_repository($_)} @DATA_ROOTS) {
    my @includes = grep {!/\/lego\//} scalar(read_file($file)) =~ /^\s*\@import\s+url\(\s*([^\)]+?)\s*\)/mg;
    my $dir = dirname($file);
    $tests += @includes if any {$dir =~ /$_\/pages/} @DATA_ROOTS;
    $FILES{realpath($file)} = \@includes;
}

Test::More::plan(tests => $tests);

for my $file (sort keys %FILES) {
    my $dir = dirname($file);
    next unless any {$dir =~ /$_\/pages/} @DATA_ROOTS;
    for my $include (@{$FILES{$file}}) {
        ok($FILES{realpath("$dir/$include")}, "$file includes $include ".realpath("$dir/$include"));
    }
}

