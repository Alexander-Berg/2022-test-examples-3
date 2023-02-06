#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;
use Mediaplan;

use utf8;

my %db = (
    mediaplan_banners => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {mbid => 1, cid => 328},
                
                {mbid => 2, cid => 3201034, title => '', body => 'asdsd', geo => '882,92923,192'},
                {mbid => 3, cid => 3201034, title => 'kdiie', body => 'asdsd', geo => '882,92923,192'},
                {mbid => 4, cid => 3201034, title => 'kdiie', body => undef, geo => '882,92923,192'},
                
                {mbid => 5, cid => 629, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', vcard_id => 78},
                
                {mbid => 6, cid => 700, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', vcard_id => 56},
                
                {mbid => 112, cid => 900, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', vcard_id => 56},
                {mbid => 113, cid => 900, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', href => "ya.ru"},
                {mbid => 114, cid => 900, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', href => "ya.ru"},
                
                {mbid => 506, cid => 1008, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', vcard_id => 56},
                {mbid => 507, cid => 1008, title => 'kdiie', body => 'kdkieie', geo => '882,92923,192', href => "ya.ru"},
            ],
        }
    },
    vcards => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {vcard_id => 56, phone => "7782123"}
            ],
        }
    },
    mediaplan_bids => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {mbid => 112, id => 78},
                {mbid => 112, id => 79},
                {mbid => 112, id => 80},
                {mbid => 113, id => 89},
                
                {mbid => 507, id => 912},
            ],
        }
    },
    mediaplan_bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {mbid => 112, ret_id => 90},
                {mbid => 112, ret_id => 91},
                
                {mbid => 506, ret_id => 128},
            ],
        }
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 1580, ClientID => 1},
            {cid => 328, ClientID => 1},
            {cid => 3201034, ClientID => 1},
            {cid => 629, ClientID => 1},
            {cid => 700, ClientID => 1},
            {cid => 900, ClientID => 1},
            {cid => 1008, ClientID => 1},
        ]
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1, shard => 1 },
        ],
    },
);

init_test_dataset(\%db);

check_result(
    Mediaplan::check_mediaplan(328), 
    1, 
    1
);
check_result(
    Mediaplan::check_mediaplan(1580), 
    undef, 
    undef
);
check_result(
    Mediaplan::check_mediaplan(3201034), 
    1, 
    2
);
check_result(
    Mediaplan::check_mediaplan(629), 
    3, 
    1
);
check_result(
    Mediaplan::check_mediaplan(700), 
    2, 
    1
);
check_result(
    Mediaplan::check_mediaplan(900), 
    2, 
    1
);
check_result(
    Mediaplan::check_mediaplan(1008), 
    undef, 
    undef
);

done_testing;

sub check_result {
    
    my ($got_code, $got_text, $exp_code, $banners) = @_;
    
    is($got_code, $exp_code);
    if (defined $got_text && defined $banners) {
        like($got_text, qr/\s+${banners}\s+/);
    }
}
