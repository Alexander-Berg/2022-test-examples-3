#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;

use utf8;
use open ':std' => ':utf8';


my $url = base_url()."/alive";
$url =~ s!intapi/!!;

my @tests = (
    {
        name => 'simple query',
        read_only => 1,
        url => $url, 
        method => 'GET', 
        data => [{}],
        check_num => 1, 
        check => sub {
            my ($data, $resp, $name) = @_;
            is($resp->content, 'ok', "$name: status");
        },
    },
);

run_tests(\@tests);

