#!/usr/bin/perl

use Direct::Modern;

use Test::Intapi;

use JSON;
use Encode;


my @tests = (
    {
        name => 'simple query',
        read_only => 1,
        url => base_url().'/CampaignUnarc',
        method => 'POST',
        data => [
            {
                uid => 16040823,
                cid => 8890714
            },
        ],
        preprocess => sub { return encode 'utf8', to_json shift },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $response;
            lives_ok { $response = $resp->content } "$name: got response";
            my %expected_result;
            cmp_deeply({$response}, {'1'}, "$name: good answer");
        }
    },
    {
        name => 'forced query',
        read_only => 1,
        url => base_url().'/CampaignUnarc',
        method => 'POST',
        data => [
            {
                uid => 16040823,
                cid => 8890820,
                options => {'force' => '1'},
            },
        ],
        preprocess => sub { return encode 'utf8', to_json shift },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $response;
            lives_ok { $response = $resp->content } "$name: got response";
            my %expected_result;
            cmp_deeply({$response}, {'1'}, "$name: good answer");
        }
    }
);

run_tests(\@tests);

