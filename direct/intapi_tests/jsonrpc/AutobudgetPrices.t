#!/usr/bin/perl 

use strict;
use warnings;
use utf8;

use my_inc '../..';

use Settings;
use Test::Intapi;

use Yandex::DBTools;
use Yandex::Test::UTF8Builder;

use JSON;

my $base_url = base_url();
my $url = "$base_url/jsonrpc/AutobudgetPrices";

my @tests = (
    {
        name => 'simple write request',
        read_only => 0,
        url => $url,
        method => 'GET',
        data => [
            [
                {
                    GroupExportID => 305692,
                    PhraseID => 360439304,
                    ContextType => 1,
                    price => 0.66,
                    context_price => 0.33,
                    currency => 0,
                },
            ],
        ],
        preprocess => sub {
            return {
                method => 'set',
                params => to_json($_[0]),
            }
        },
        check_num => 3,
        check => sub {
            my ($data, $resp, $name) = @_;

            my $result_container = decode_json($resp->content);
            my $result = $result_container->{result};
            is_deeply($result, [], "$name: ответ без ошибки");
            my ($price, $price_context) = get_one_line_array_sql(PPC(pid => 305692), 'SELECT price, price_context FROM bids WHERE pid = ? AND PhraseID = ? LIMIT 1', 305692, 360439304);
            is($price, 0.66, "$name: верный price");
            is($price_context, 0.33, "$name: верный price_context");
        },
    },
    {
        name => 'ошибка при выключенном автобюджете',
        read_only => 0,
        url => $url,
        method => 'GET',
        data => [
            [
                {
                    GroupExportID => 1578,
                    PhraseID => 154847,
                    ContextType => 1,
                    price => 0.66,
                    context_price => 0.33,
                    currency => 0,
                },
            ],
        ],
        preprocess => sub {
            return {
                method => 'set',
                params => to_json($_[0]),
            }
        },
        check_num => 1,
        check => sub {
            my ($data, $resp, $name) = @_;

            my $result_container = decode_json($resp->content);
            my $result = $result_container->{result};
            my $expected_result = [
                {
                    GroupExportID => 1578,
                    PhraseID => 154847,
                    code => 1,
                    msg => 'autobudget disabled',
                },
            ];
            is_deeply($result, $expected_result, "$name: ошибка про автобюджет в ответе");
        },
    },
);

run_tests(\@tests);
