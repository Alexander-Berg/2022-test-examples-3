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
    
cmp_deeply(Rbac::get_rep2client([666]), {});
cmp_deeply(Rbac::get_rep2client([6]), {6=>1});
cmp_deeply(Rbac::get_rep2client([7]), {7=>2});
cmp_deeply(Rbac::get_rep2client([8]), {8=>1});
cmp_deeply(Rbac::get_rep2client([8, 10, 11]), {8=>1, 10=>3, 11=>3});

cmp_deeply(Rbac::get_rep2client([8, 10, 11], role => 'agency'), {10=>3, 11=>3});
cmp_deeply(Rbac::get_rep2client([8, 10, 11], role => 'client'), {8=>1});
cmp_deeply(Rbac::get_rep2client([8, 10, 11], role => ['agency', 'client']), {8=>1, 10=>3, 11=>3});
cmp_deeply(Rbac::get_rep2client([8, 10, 11], role => 'manager'), {});


done_testing;
