#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Readonly;
use Test::Intapi;
use Test::More;
use JSON;
use List::MoreUtils qw();

my @test_data = (
    # одиночная кампания
    {
        cid => 263,
    },
    # несколько кампаний [включая несуществующую]
    {
        cid => [263,8346816,666,9999999999],
    },
    # нет ключа cid
    {
    },
    # хеш в качестве номеров кампаний
    {
        cid => {263 => 666},
    },
    # undef вместо номеров кампаний
    {
        cid => undef,
    },
);

my $base_test_data = {
    name       => 'CampaignsSumsForBS',
    read_only  => 1,
    url        => base_url() . '/jsonrpc/CampaignsSumsForBS',
    method     => 'GET',
    preprocess => sub {
        my ($params) = @_;
        return {
            method => 'get',
            params => encode_json( $params ),
        };
    },
};

my @tests;

foreach my $params (@test_data) {
    my %test = %$base_test_data;

    $test{data} = [ $params ];

    $test{check_num} = 4;
    $test{check} = sub {
        my ( $cids, $resp, $name ) = @_;

        my $cids_text = to_json($cids, {allow_nonref => 1});
        my $display  = "get($cids_text)";

        SKIP: {
            if ( !$resp->is_success ) {
                my $status   = $resp->status_line;
                my $skip_msg = "$display response tests useless: invalid status $status";
                skip $skip_msg, 4;
            }

            my $result_full;
            lives_ok { $result_full = decode_json( $resp->content ) } "$display: decode json";
            is( ref $result_full, 'HASH', "$display: full result is hash" );
            my $result = $result_full->{result};
            if (!$result) {
                skip "Got message " . ($result_full->{message} // '') . " nothing to check", 2;
            } else {
                is( ref $result, 'ARRAY', "$display: result is an array" );
                ok( List::MoreUtils::all(sub {exists $_->{cid} && exists $_->{sum}}, @$result), "$display: call result has cid and sum keys" );
            }
        }
    };

    push @tests, \%test;
};

run_tests( \@tests );
