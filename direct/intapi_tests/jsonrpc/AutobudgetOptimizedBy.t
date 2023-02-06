#! /usr/bin/perl

use strict;
use warnings;

use Test::Intapi;

use JSON;

use utf8;
use open ':std' => ':utf8';

my @tests = (
    {
        name => 'setOrder valid data',
        url => "/jsonrpc/AutobudgetOptimizedBy",
        method => 'POST',
        data => [
            [
                {"OrderID" => 123456,"OptimizedBy" => "CPA"},
                {"OrderID" => 654321,"OptimizedBy" => "CPC"},
            ],
        ],
        preprocess => sub {
            return to_json {
                method => "setOrder",
                params => $_[0]
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, 'HASH', "$name: result is a hash");
            is(ref $v->{result}, 'ARRAY', "$name: call result is an array");
            is(scalar @{$v->{result}}, 0, "$name: is successful");
        },
    },
    {
        name => 'setOrder bad data',
        url => "/jsonrpc/AutobudgetOptimizedBy",
        method => 'POST',
        data => [
            [
                {"OrderID" => 666,"OptimizedBy" => "CPC"},
                {"OrderID" => 667,"OptimizedBy" => "CPC"},
            ],
        ],
        preprocess => sub {
            return to_json {
                method => "setOrder",
                params => $_[0]
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, 'HASH', "$name: result is a hash");
            is(ref $v->{result}, 'ARRAY', "$name: call result is an array");
            is(scalar @{$v->{result}}, 2, "$name: is successful");
        },
    },
    {
        name => 'setFilter valid data',
        url => "/jsonrpc/AutobudgetOptimizedBy",
        method => 'POST',
        data => [
            [
                {"OrderID" => 42356,"GroupExportID" => 88287,"PhraseID" => 456789,"OptimizedBy" => "CPA"},
                {"OrderID" => 20063,"GroupExportID" => 350,"PhraseID" => 987654,"OptimizedBy" => "CPC"},
            ],
        ],
        preprocess => sub {
            return to_json {
                method => "setFilter",
                params => $_[0]
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, 'HASH', "$name: result is a hash");
            is(ref $v->{result}, 'ARRAY', "$name: call result is an array");
            is(scalar @{$v->{result}}, 0, "$name: is successful");
        },
    },
    {
        name => 'setFilter bad data',
        url => "/jsonrpc/AutobudgetOptimizedBy",
        method => 'POST',
        data => [
            [
                {"OrderID" => 20159,"GroupExportID" => 667,"PhraseID" => 987654,"OptimizedBy" => "CPC"},
                {"OrderID" => 666,"GroupExportID" => 666,"PhraseID" => 987654,"OptimizedBy" => "CPC"},
            ],
        ],
        preprocess => sub {
            return to_json {
                method => "setFilter",
                params => $_[0]
            };
        },
        check_num => 4,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $v;
            lives_ok { $v = decode_json($resp->content) } "$name: decode json";
            is(ref $v, 'HASH', "$name: result is a hash");
            is(ref $v->{result}, 'ARRAY', "$name: call result is an array");
            is(scalar @{$v->{result}}, 2, "$name: is successful");
        },
    },
);

run_tests(\@tests);
