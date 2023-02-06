#!/usr/bin/perl

# $Id$

use strict;
use warnings;
use utf8;

use Test::Exception;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;
use Yandex::Test::UTF8Builder;

BEGIN { 
    require_ok('Models::CampaignOperations');
}

my %db = (
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1, ClientID => 1, },
            { uid => 2, ClientID => 2, },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
            { ClientID => 2, shard => 2 },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            { cid => 1, ClientID => 1 },
            { cid => 2, ClientID => 1 },
            { cid => 3, ClientID => 2 },
            { cid => 4, ClientID => 2 },
            { cid => 5, ClientID => 2 },
        ],
    },
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 1, type => 'wallet', uid => 1, sum => 1, sum_to_pay => 0, statusModerate => 'Sent', statusEmpty => 'No', },
                { cid => 2, type => 'text', wallet_cid => 1, uid => 1, statusModerate => 'No', statusEmpty => 'No', },
            ],
            2 => [
                { cid => 3, type => 'wallet', uid => 2, statusModerate => 'Sent', statusEmpty => 'No', },
                { cid => 4, type => 'text', wallet_cid => 3, uid => 2, sum => 1, sum_to_pay => 1, statusModerate => 'Yes', statusEmpty => 'No', },
                { cid => 5, type => 'text', wallet_cid => 3, uid => 2, sum => 0, sum_to_pay => 0, statusModerate => 'Ready', statusEmpty => 'No', },
            ],
        },
    },
    camp_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { cid => 1, statusPostModerate => 'Yes', },
                { cid => 2, statusPostModerate => 'Yes', },
            ],
            2 => [
                { cid => 3, statusPostModerate => 'Yes', },
                { cid => 4, statusPostModerate => 'No', },
                { cid => 5, statusPostModerate => 'No', },
            ],
        },
    },
);

init_test_dataset(\%db);

*mass_check_block_money_camps = *Models::CampaignOperations::mass_check_block_money_camps;

throws_ok {
    mass_check_block_money_camps( [ { cid => 3, } ] );
} qr/campaign type not defined/, 'proper error when camp without type or mediaType given';

lives_and {
    # && ! $camp_info->{ManagerUID}
    # && ! $camp_info->{AgencyUID}
    # && (($camp_info->{statusPostModerate} || '' ) ne 'Accepted')
    # && ( $camp_info->{statusPostModerate} && $camp_info->{statusPostModerate} eq "Yes"          enum('new', 'yes', 'no', 'accepted')
    #     || $camp_info->{statusModerate} && $camp_info->{statusModerate} =~ /^(No|Ready|Sent)$/) enum('yes', 'no', 'sent', 'ready', 'new', 'mediaplan')
    my $args = [
        { cid => 1, type => 'text', statusPostModerate => 'Yes', sum => 1, sum_to_pay => 0, },
        { cid => 2, type => 'text', statusPostModerate => 'Yes', sum => 0, sum_to_pay => 0, },
        { cid => 3, type => 'text', statusPostModerate => 'No', sum => 1, sum_to_pay => 0, },
        { cid => 4, type => 'mcb', statusPostModerate => 'Yes', sum => 0, sum_to_pay => 1, },
        { cid => 5, type => 'mcb', statusPostModerate => 'Yes', sum => 0, sum_to_pay => 0, },
        { cid => 6, type => 'mcb', statusPostModerate => 'No', sum => 0, sum_to_pay => 1, },
        { cid => 7, type => 'text', ManagerUID => 1, },
        { cid => 8, type => 'text', AgencyUID => 2, },
        { cid => 9, type => 'text', statusPostModerate => 'Accepted', }, # fake tables join
        { cid => 10, type => 'text', statusPostModerate => 'No', statusModerate => 'yes', }, # fake tables join
    ];
    my $results = { 1 => 1, 2 => 0, 3 => 0, 4 => 1, 5 => 0, 6 => 0, 7 => 0, 8 => 0, 9 => 0, 10 => 0, };
    is_deeply( mass_check_block_money_camps( $args ), $results );
} 'campaigns without wallets - succesfull call with proper results';

lives_and {
    my $args = [
        { cid => 1, type => 'wallet', uid => 1, },
        { cid => 2, type => 'text', wallet_cid => 1, uid => 1, },
        { cid => 3, type => 'wallet', uid => 2, },
        { cid => 4, type => 'text', wallet_cid => 3, uid => 2, },
        { cid => 5, type => 'text', wallet_cid => 3, uid => 2, },
    ];
    my $results = { 1 => 1, 2 => 1, 3 => 0, 4 => 0, 5 => 0, };
    is_deeply( mass_check_block_money_camps( $args ), $results );
} 'campaigns with wallets - succesfull call with proper results';

done_testing();
