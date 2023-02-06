#!/usr/bin/perl

use Direct::Modern;

use Carp qw/croak/;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/copy_table init_test_dataset check_test_dataset UT/;

use Campaign::Types qw/camp_type_in/;
require my_inc;

my $data_file = my_inc::path("_test_db");
my $db = eval {require $data_file};
if (!defined $db) {
    croak "Can't get test data from '$data_file': $@";
}

init_test_dataset($db);

ok(  camp_type_in(type => 'text', 'text') );
ok( !camp_type_in(type => 'text', 'mcb') );
ok(  camp_type_in(type => ['text', 'text'], 'mcb', 'text') );


ok(  camp_type_in(cid => 1, 'text') );
ok( !camp_type_in(cid => 1, 'mcb') );

ok(  camp_type_in(cid => [1,2], 'text') );
ok(  camp_type_in(cid => [1,2], 'mcb', 'text') );
ok( !camp_type_in(cid => [1,2], 'mcb') );

ok( !camp_type_in(cid => [1,2,4], 'text') );
ok(  camp_type_in(cid => [1,2,4], 'text', 'geo') );


ok( !camp_type_in(OrderID => 11, 'text') );
ok( !camp_type_in(OrderID => 11, 'mcb') );

ok( !camp_type_in(OrderID => [11,12], 'text') );
ok(  camp_type_in(OrderID => [12,13], 'mcb', 'text') );
ok( !camp_type_in(OrderID => [12,13], 'mcb') );

ok( !camp_type_in(OrderID => [11,12,14], 'text') );
ok(  camp_type_in(OrderID => [12,14], 'text', 'geo') );

done_testing;


