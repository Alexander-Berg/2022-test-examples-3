#! /usr/bin/perl 

use strict;
use warnings;

use Test::Intapi;

use JSON;

use utf8;
use open ':std' => ':utf8';

my @tests = (
    {
        name => 'simple POST',
        read_only => 0,
        url => "/jsonrpc/ResyncCampaigns", 
        method => 'POST', 
        data => [ [], [123], [123,456], [[]], [{}], [1582801,[]] ],
        preprocess => sub {
            return to_json {
                method => "add", 
                params => $_[0]
            };
        },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{result}->{success}, 1, "$name: POST success");
        },
    },
    {
        name => 'simple GET',
        read_only => 0,
        url => "/jsonrpc/ResyncCampaigns", 
        method => 'GET', 
        data => [ [], [123], [123,456], [[]], [{}], [1582801,[]] ],
        preprocess => sub {
            return {
                method => "add", 
                params => to_json($_[0]),
            };
        },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            is($v->{result}->{success}, 1, "$name: GET success");
        },
    },
    {
        name => 'incorrect parameters, GET',
        read_only => 1,
        url => "/jsonrpc/ResyncCampaigns", 
        method => 'GET', 
        data => [ {}, "asdf", 12345],
        preprocess => sub {
            return {
                method => "add", 
                params => to_json($_[0], { allow_nonref => 1 }),
            };
        },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            isnt($v->{error}->{message} || '', '', "$name: error message");
        },
    },
    {
        name => 'incorrect parameters, POST',
        read_only => 1,
        url => "/jsonrpc/ResyncCampaigns", 
        method => 'POST', 
        data => [ {}, "asdf", 12345 ],
        preprocess => sub {
            return to_json {
                method => "add", 
                params => $_[0]
            };
        },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            isnt($v->{error}->{message} || '', '', "$name: error message");
        },
    },
    {
        name => 'wrong method, GET',
        read_only => 1,
        url => "/jsonrpc/ResyncCampaigns", 
        method => 'GET', 
        data => [ [], [123,456], [[12436942,[1,2,6]],[3710842,[9,13,113]]], {}, 0, 12345 ],
        preprocess => sub {
            return {
                method => "wrongMethod", 
                params => to_json($_[0], {allow_nonref => 1}),
            };
        },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            isnt($v->{error}->{message} || '', '', "$name: error message");
        },
    },
    {
        name => 'wrong method, POST',
        read_only => 1,
        url => "/jsonrpc/ResyncCampaigns", 
        method => 'POST', 
        data => [ [], [123,456], [[12436942,[1,2,6]],[3710842,[9,13,113]]], {}, 0, 12345 ],
        preprocess => sub {
            return to_json {
                method => "wrongMethod", 
                params => $_[0]
            };
        },
        check_num => 3, 
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, "HASH", "$name: result is a hash");
            isnt($v->{error}->{message} || '', '', "$name: error message");
        },
    },
);

run_tests(\@tests);
