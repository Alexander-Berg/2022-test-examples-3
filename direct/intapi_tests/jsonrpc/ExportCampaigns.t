#!/usr/bin/perl

# $Id $

use strict;
use warnings;

our $VERSION = 1.0;

use Readonly;
use Test::Intapi;
use Test::More;

use JSON;
use utf8;
use open ':std' => ':utf8';

Readonly my $RETVAL_TEST_COUNT => 3;

my @test_data = (
    # пользователь, который есть в Паспорте, но не в Директе
    {
        login                       => 'volodya',
        role_is_supported           => 0,
        has_access_to_all_campaigns => undef,
        result_has_campaigns        => 0,
        min_campaigns_count         => 0,
    },

    # просто клиент
    {
        login                       => 'andy-ilyin-client',
        role_is_supported           => 0,
        has_access_to_all_campaigns => undef,
        result_has_campaigns        => 0,
        min_campaigns_count         => 0,
    },

    # супер
    {
        login                       => 'yndx-a-balakina-super',
        role_is_supported           => 1,
        has_access_to_all_campaigns => 1,
        result_has_campaigns        => 0,
        min_campaigns_count         => 0,
    },

    # менеджер
    {
        login                       => 'alnasonow',
        role_is_supported           => 1,
        has_access_to_all_campaigns => 0,
        result_has_campaigns        => 1,
        min_campaigns_count         => 1000,
    },

    # руководитель группы
    {
        login                       => 'yndx-ekleimenova',
        role_is_supported           => 1,
        has_access_to_all_campaigns => 0,
        result_has_campaigns        => 1,
        min_campaigns_count         => 20_000,
    },

    # руководитель отдела
    {
        login                       => 'yndx-askerova-teamlead',
        role_is_supported           => 1,
        has_access_to_all_campaigns => 0,
        result_has_campaigns        => 1,
        min_campaigns_count         => 100_000,
    },

);

my $base_test_data = {
    name       => 'ExportCampaigns',
    read_only  => 1,
    url        => base_url() . '/jsonrpc/ExportCampaigns',
    method     => 'GET',
    preprocess => sub {
        my ($login) = @_;
        my $params = $login;
        return {
            method => 'get_managed_campaigns',
            params => encode_json( { login => $login } ),
        };
    },
};

my @tests;

foreach my $params (@test_data) {
    my %test = %$base_test_data;

    $test{data} = [ $params->{login} ];

    $test{check_num} = $params->{result_has_campaigns} ? 6 : 5;
    $test{check} = sub {
        my ( $login, $resp, $name ) = @_;

        my $display  = "get_managed_campaigns($login)";
        my $status   = $resp->status_line;
        my $skip_msg = "$display response tests useless: "
            . "invalid status $status";

        my $result;

    SKIP: {
            if ( !$resp->is_success ) {
                skip $skip_msg, $RETVAL_TEST_COUNT;
            }

            lives_ok { $result = decode_json( $resp->content ); } "$display: decode json";

            ok(
                ref $result eq 'HASH' && ref $result->{result} eq 'HASH',
                "$display: RPC result is well-formed"
            );

            my $result_payload = $result->{result};

            is(
                $result_payload->{role_is_supported},
                $params->{role_is_supported},
                "$display: role_is_supported"
            );

            is(
                $result_payload->{has_access_to_all_campaigns},
                $params->{has_access_to_all_campaigns},
                "$display: has_access_to_all_campaigns"
            );

            if ( $params->{result_has_campaigns} ) {
                ok(
                    $result_payload->{campaigns} && ref $result_payload->{campaigns} eq 'ARRAY',
                    "$display: result_has_campaigns"
                );

                ok(
                    @{ $result_payload->{campaigns} } > $params->{min_campaigns_count},
                    "$display: min_campaigns_count"
                );
            } else {
                ok(
                    ! defined $result_payload->{campaigns},
                    "$display: result_has_campaigns = false"
                );
            }
        }
    };

    push @tests, \%test;
};

$Test::Intapi::lwp_opt->{'timeout'} = 300;
run_tests( \@tests );
