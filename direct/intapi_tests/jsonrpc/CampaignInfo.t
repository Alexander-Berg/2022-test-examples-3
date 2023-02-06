#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;

use utf8;
use open ':std' => ':utf8';

my @tests = (
    {
        name => 'check valid format',
        read_only => 1,
        url => "/jsonrpc/CampaignInfo", 
        method => 'POST', 
        data => [{ cids => [1, 2, 1873212 ] }],
        preprocess => sub {
            return to_json {
                method => "getCampaignManager",
                params => $_[0]
            };
        },
        check_num => 5,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            my $result = $v->{result};
            is(ref $result, "ARRAY", "$name: result is an array");
            for my $subresult (@$result) {
                ok($subresult->{cid}, "$name: subresult has cid");
            }
        },
    },
);

run_tests(\@tests);
