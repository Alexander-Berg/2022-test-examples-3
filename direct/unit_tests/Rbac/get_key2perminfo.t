#!/usr/bin/perl

use strict;
use warnings;

use Test::More;
use Test::Deep;

use Yandex::DBUnitTest qw/:all/;

use my_inc '../..';

use Settings;
use Rbac ':const';

use lib::abs '.';
use RbacTestData;

use utf8;

init_test_dataset($RbacTestData::DATASET);

cmp_deeply(Rbac::get_key2perminfo(ClientID => [ 1, 3 ]), {
                1 => {
                        ClientID         => 1,
                        role             => 'client',
                        subrole          => undef,
                        agency_client_id => 3,
                        agency_uid       => 13,
                        agency_uids      => [12],
                        chief_uid        => 6,
                        perms            => 'super_subclient',
                        primary_manager_set_by_idm => 1,
                        primary_manager_uid => 77,
                },
                3 => {
                        ClientID         => 3,
                        role             => 'agency',
                        subrole          => undef,
                        agency_client_id => undef,
                        agency_uid       => undef,
                        chief_uid        => 10,
                        perms            => '',
                        primary_manager_set_by_idm => 0,
                        primary_manager_uid => undef,
                }
        });

cmp_deeply(Rbac::get_key2perminfo(uid => [ 8, 11 ]), {
                8 => {
                        ClientID         => 1,
                        role             => 'client',
                        rep_type         => 'main',
                        subrole          => undef,
                        agency_client_id => 3,
                        agency_uid       => 13,
                        agency_uids      => [12],
                        chief_uid        => 6,
                        perms            => 'super_subclient',
                        primary_manager_set_by_idm => 1,
                        primary_manager_uid => 77,
                        mcc_client_ids   => [2],
                },
                11 => {
                        ClientID         => 3,
                        role             => 'agency',
                        rep_type         => 'main',
                        subrole          => undef,
                        agency_client_id => undef,
                        agency_uid       => undef,
                        chief_uid        => 10,
                        perms            => '',
                        primary_manager_set_by_idm => 0,
                        primary_manager_uid => undef,
                }
        });

done_testing;
