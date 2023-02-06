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
    
cmp_deeply(Rbac::get_agency_uid_multi(ClientID => [1]), {1 => { old_schema => [13], new_schema => [12] }});
cmp_deeply(Rbac::get_agency_uid_multi(ClientID => [2]), {});

cmp_deeply(Rbac::get_agency_uid_multi(uid => [7]), {});
cmp_deeply(Rbac::get_agency_uid_multi(uid => [8]), {8 => { old_schema => [13], new_schema => [12] }});

done_testing;
