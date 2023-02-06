#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;

use utf8;
use open ':std' => ':utf8';

my @tests = (
    {
        name => 'send invalid data format',
        url => "/jsonrpc/AutobudgetAlerts", 
        method => 'POST', 
        data => [[ 1873212 => { problems => 11, overdraft => 11 } ]], # arrayref instead hashref
        preprocess => sub {
            return to_json {
                method => "ordersNotExceededBudget", 
                params => $_[0]
            };
        },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{result}->{success}, 0, "$name: receive error answer");
            is($v->{result}->{error}, 1, "$name: receive error answer");
        },
    },
    {
        name => 'test one valid order',
        url => "/jsonrpc/AutobudgetAlerts", 
        method => 'POST', 
        data => [{ 1873212 => { problems => 11, overdraft => 11 } }],
        preprocess => sub {
            return to_json {
                method => "ordersNotExceededBudget", 
                params => $_[0]
            };
        },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{result}->{success}, 1, "$name: receive successful answer");
            is($v->{result}->{error}, 0, "$name: receive successful answer");
        },
    },
    
    {
        name => 'CPA alerts',
        url => "/jsonrpc/AutobudgetAlerts", 
        method => 'POST', 
        data => [{ 1873212 => { cpa => 110000, apc => 110000 } }],
        preprocess => sub {
            return to_json {
                method => "ordersWithCpaWarnings", 
                params => $_[0]
            };
        },
        check_num => 4, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{result}->{success}, 1, "$name: receive successful answer");
            is($v->{result}->{error}, 0, "$name: receive successful answer");
        },
    },
);

run_tests(\@tests);
