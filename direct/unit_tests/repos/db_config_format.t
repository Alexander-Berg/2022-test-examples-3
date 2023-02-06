#!/usr/bin/perl

use warnings;
use strict;

use File::Slurp;
use JSON;
use Test::More;
use Test::Exception;

use Test::ListFiles;
use Settings;

my @files = sort
    grep { /\/db-config\.[^\/]+\.json$/ }
    grep { -f }
    Test::ListFiles->list_repository("$Settings::ROOT/etc", depth => 0);

for my $file (@files) {
    lives_ok {from_json(scalar read_file($file))} "good json: $file";
}

done_testing();
