#!/usr/bin/perl

use warnings;
use strict;

use File::Slurp;
use File::Basename qw/dirname/;
use Cwd qw/realpath/;
use Test::More;

use Test::ListFiles;

use Settings;

my $JS_ROOT = "$Settings::ROOT/data";

my %FILES;
for my $file (grep {-f && /\.js$/} Test::ListFiles->list_repository($JS_ROOT, externals => 1)) {
    my @includes = grep {!/\/lego\// && !/\/const\.js/} scalar(read_file($file)) =~ /^\s*include\(\s*['"]([^'"]+)/mg;
    my $dir = dirname($file);
    $FILES{realpath($file)} = \@includes;
}

for my $file (sort keys %FILES) {
    my $dir = dirname($file);
    next unless $dir =~ /$JS_ROOT\/pages/;
    for my $include (@{$FILES{$file}}) {
        ok(!!$FILES{realpath("$dir/$include")}, "$file includes $include ".realpath("$dir/$include"));
    }
}

done_testing();
