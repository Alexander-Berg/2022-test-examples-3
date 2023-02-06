#!/usr/bin/perl

use Direct::Modern;

use Carp qw/croak/;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;

use Campaign::Types qw/get_camp_type/;
require my_inc;

my $data_file = my_inc::path("_test_db");
my $db = eval {require $data_file};
if (!defined $db) {
    croak "Can't get test data from '$data_file': $@";
}

init_test_dataset($db);

is(get_camp_type(cid => 1), 'text');
is(get_camp_type(cid => 1), 'text');

is(get_camp_type(OrderID => 0), undef);

is(get_camp_type(cid => 4), 'geo');

done_testing;


