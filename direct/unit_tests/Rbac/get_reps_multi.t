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
    
cmp_deeply(Rbac::get_reps_multi(ClientID => [1]), {1 => bag(6, 8)});
cmp_deeply(Rbac::get_reps_multi(uid => [6]), {6 => bag(6, 8)});
cmp_deeply(Rbac::get_reps_multi(uid => [8]), {8 => bag(6, 8)});

cmp_deeply(Rbac::get_reps_multi(uid => [8], role => 'client'), {8 => bag(6, 8)});
cmp_deeply(Rbac::get_reps_multi(uid => [8], role => 'agency'), {});

cmp_deeply(Rbac::get_reps_multi(uid => [8], rep_type => 'chief'), {8 => bag(6)});
cmp_deeply(Rbac::get_reps_multi(uid => [8], rep_type => 'main'), {8 => bag(8)});

cmp_deeply(Rbac::get_reps_multi(uid => [8], rep_type => ['chief', 'main']), {8 => bag(6, 8)});

cmp_deeply(Rbac::get_reps_multi(ClientID => [3], rep_type => 'limited'), 
           {3 => bag(12,13)}
);

done_testing;
