#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::Intapi;
use Test::More;

use JSON;

my @test_data = (
    # [ { $args, $expected_result, $test_count, $check_sub }, {...}, ... ]
    # минимальный поиск кампании
    [
        {
            what => 'campaign',
            text_search => 263,
            short => 1,
            limit => 2,
        },
        undef,
        4,
        sub {
            my ($test_name, $result) = @_;

            is(scalar(@{$result->{banners}}), 2, "$test_name: 2 results with limit = 2");
            is($result->{banners}->[0]->{cid}, 263, "$test_name: first result has requested cid");
            is($result->{banners}->[1]->{cid}, 263, "$test_name: second result has requested cid");
            my @keys = keys %{$result->{banners}->[0]};
            is_deeply([sort @keys], [qw(BannerID bid cid)], "$test_name: only needed keys in result");
        },
    ],
    # запрос как из Модерации
    [
        {
            what => 'campaign',
            activeonly => 0,
            short => 1,
            include_currency_archived_campaigns => 1,
            sort => 'desc',
            exact_domain => 0,
            limit => 1,
            text_search => 263,
        },
        undef,
        2,
        sub {
            my ($test_name, $result) = @_;

            is(scalar(@{$result->{banners}}), 1, "$test_name: 1 results with limit = 1");
            is($result->{banners}->[0]->{cid}, 263, "$test_name: first result has requested cid");
        },
    ],
);

my $base_test_data = {
    name       => 'SearchBanners',
    read_only  => 1,
    url        => base_url() . '/jsonrpc/SearchBanners',
    method     => 'GET',
    preprocess => sub {
        my ($args) = @_;
        return {
            method => 'search',
            params => to_json($args->[0]),
        };
    },
};

my @tests;
foreach my $params (@test_data) {
    my ($args, $expected_result, $test_count, $check_sub) = @$params;
    my %test = %$base_test_data;
    $test{data} = [ $params ];
    $test{check_num} = 4 + $test_count;
    $test{check} = sub {
        my ($test, $resp, $test_name) = @_;
        my ($args, $expected_result, $test_count, $check_sub) = @$test;

        ok($resp->is_success, "$test_name: response success");
        my $result;
        lives_ok { $result = decode_json( $resp->content ); } "$test_name: decode json";
        is(ref($result), 'HASH', "$test_name: JSON-RPC result is a HASH");
        my $result_payload = $result->{result};
        is(ref($result_payload), 'HASH', "$test_name: result payload is HASH");

        if ($expected_result) {
            my $expected_result_text = to_json($expected_result);
            is_deeply($result_payload, $expected_result, "$test_name: result payload should be $expected_result_text");
        } else {
            $check_sub->($test_name, $result_payload);
        }
    };

    push @tests, \%test;
};

run_tests( \@tests );
