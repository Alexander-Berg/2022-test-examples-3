#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use JSON;
use Test::Intapi;
use Test::More;
use Test::Deep;

my $json_obj = JSON->new->utf8(1)->allow_nonref(1);

my %domain2filter_domain = (
    'list.ru' => 'list.mail.ru',
    'xn----8sbcoaqejqobh4a0ae.xn--80adxhks' => 'beeline-internet.com',
    'yandex.ru' => 'www.yandex.ru',
);

my %domain2bs_filter_domain = (
    'list.ru' => 'list.mail.ru',
    'интернет-билайн.москва' => 'beeline-internet.com',
    'xn----8sbcoaqejqobh4a0ae.xn--80adxhks' => 'beeline-internet.com',
);

my $bad_result = {
    id => ignore(),
    jsonrpc => ignore(),
    error => {
        code => ignore(),
        message => code(sub {
            my $scalar = shift;
            return (defined $scalar && ref $scalar eq '' ? 1 : 0);
        }),
    },
};

my @tests = (
    {
        name => 'get_domain_filter',
        read_only  => 1,
        url        => base_url() . '/jsonrpc/Mirrors',
        method     => 'POST',
        data => [
            [],
            [undef],
            ['mail.ru'],
            ['list.ru'],
            ['yandex.ru'],  # -> www.yandex.ru
            ['direct.yandex.ru'],
            ['интернет-билайн.москва'], # не определяется, так как не в punycode
            ['xn----8sbcoaqejqobh4a0ae.xn--80adxhks'],  # а так определяется
            ['mail.ru', 'yandex.ru', 'google.com'],
        ],
        preprocess => sub {
            my ($params) = @_;
            return $json_obj->encode({
                method => 'get_domain_filter',
                params => $params,
            });
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = $json_obj->decode($resp->content)} "$name: decode JSON";
            my @expected_result;
            for my $d (@$data) {
                if ($d && exists $domain2filter_domain{$d}) {
                    push @expected_result, $domain2filter_domain{$d};
                } else {
                    push @expected_result, $d;
                }
            }
            cmp_deeply($json,
                       {
                            id => ignore(),
                            jsonrpc => ignore(),
                            result => \@expected_result,
                       },
                       "$name: good answer"
                       );
        },
    }, {
        name => 'get_domain_filter - bad request',
        read_only  => 1,
        url        => base_url() . '/jsonrpc/Mirrors',
        method     => 'POST',
        data => [
            undef,
            {},
            'string',
        ],
        preprocess => sub {
            my ($params) = @_;
            return $json_obj->encode({
                method => 'get_domain_filter',
                params => $params,
            });
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = $json_obj->decode($resp->content)} "$name: decode JSON";
            cmp_deeply($json, $bad_result, "$name: error in answer");
        },
    }, {
        name => 'get_domain_filter_for_bs',
        read_only  => 1,
        url        => base_url() . '/jsonrpc/Mirrors',
        method     => 'POST',
        data => [
            [],
            [undef],
            ['mail.ru'],
            ['list.ru'],
            ['yandex.ru'],  # главный домен - без www, в отличие от первой ручки
            ['direct.yandex.ru'],
            ['интернет-билайн.москва'], # ручка сама конвертирует в punycode, поэтому знает фильтр-домен
            ['xn----8sbcoaqejqobh4a0ae.xn--80adxhks'],  # тот же домен, но сразу в punycode
            ['mail.ru', 'yandex.ru', 'google.com'],
        ],
        preprocess => sub {
            my ($params) = @_;
            return $json_obj->encode({
                method => 'get_domain_filter_for_bs',
                params => $params,
            });
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = $json_obj->decode($resp->content)} "$name: decode JSON";
            my @expected_result;
            for my $d (@$data) {
                if ($d && exists $domain2bs_filter_domain{$d}) {
                    push @expected_result, $domain2bs_filter_domain{$d};
                } else {
                    push @expected_result, $d;
                }
            }
            cmp_deeply($json,
                       {
                            id => ignore(),
                            jsonrpc => ignore(),
                            result => \@expected_result,
                       },
                       "$name: good answer"
                       );
        },
    }, {
        name => 'get_domain_filter_for_bs - bad request',
        read_only  => 1,
        url        => base_url() . '/jsonrpc/Mirrors',
        method     => 'POST',
        data => [
            undef,
            {},
            'string',
        ],
        preprocess => sub {
            my ($params) = @_;
            return $json_obj->encode({
                method => 'get_domain_filter_for_bs',
                params => $params,
            });
        },
        check_num => 2,
        check => sub {
            my ($data, $resp, $name) = @_;
            my $json;
            lives_ok {$json = $json_obj->decode($resp->content)} "$name: decode JSON";
            cmp_deeply($json, $bad_result, "$name: error in answer");
        },
    }
);

run_tests( \@tests );
