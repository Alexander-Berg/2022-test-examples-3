#! /usr/bin/perl 

use strict;
use warnings;

use POSIX qw/strftime/;

use Test::Intapi;

use Yandex::HTTP;
use JSON;

use utf8;
use open ':std' => ':utf8';

my $url = base_url()."/ExportCmdLog";

my @tests = (
    {
        name => 'simple query',
        read_only => 1,
        url => $url,
        method => 'GET',
        data => [{from => strftime("%Y%m%d%H%M%S", localtime(time - 30 * 24 * 3600)), limit => 5}],
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my @lines = split "\n", $resp->content;
            is(scalar @lines, 5, 'number of lines in response');
            ok(!(grep {/^(\d+\t){3}\d+$/} @lines), 'correct lines format');
        }
    },
);

run_tests(\@tests);

