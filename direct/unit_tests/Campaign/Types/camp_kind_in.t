#!/usr/bin/perl

use Direct::Modern;

use Carp qw/croak/;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/copy_table init_test_dataset check_test_dataset UT/;

use Campaign::Types;
require my_inc;

my $data_file = my_inc::path("_test_db");
my $db = eval {require $data_file};
if (!defined $db) {
    croak "Can't get test data from '$data_file': $@";
}

init_test_dataset($db);

ok(  camp_kind_in(type => 'text', 'all') );
ok(  camp_kind_in(type => 'text', 'xls') );
ok( !camp_kind_in(type => 'text', 'media' ) );


ok(  camp_kind_in(cid => 1, 'xls') );
ok( !camp_kind_in(cid => 1, 'media') );

ok( !camp_kind_in(OrderID => 11, 'all') );

ok(  camp_kind_in(OrderID => [12,13], 'web_edit') );

done_testing;


