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

sub p($) {
   return [split /,/, Rbac::get_perminfo(ClientID => $_[0])->{perms}]; 
}

cmp_deeply(p(1), bag($PERM_SUPER_SUBCLIENT));

Rbac::set_client_perm(1, $PERM_SUPER_SUBCLIENT => 0);
cmp_deeply(p(1), bag());

Rbac::set_client_perm(1, $PERM_MONEY_TRANSFER => 1);
cmp_deeply(p(1), bag($PERM_MONEY_TRANSFER));

Rbac::set_client_perm(1, $PERM_XLS_IMPORT => 1);
cmp_deeply(p(1), bag($PERM_MONEY_TRANSFER,$PERM_XLS_IMPORT));


done_testing;
