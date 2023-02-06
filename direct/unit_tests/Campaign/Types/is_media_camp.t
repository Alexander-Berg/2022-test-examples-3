#!/usr/bin/perl

use Direct::Modern;

use Carp qw/croak/;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/init_test_dataset/;

use Campaign::Types qw/is_media_camp/;
require my_inc;

my $data_file = my_inc::path("_test_db");
my $db = eval {require $data_file};
if (!defined $db) {
    croak "Can't get test data from '$data_file': $@";
}

init_test_dataset($db);

ok( is_media_camp(type => 'mcb'));
ok(!is_media_camp(type => 'text'));
ok(!is_media_camp(type => undef));

ok( is_media_camp(cid => 3));
ok(!is_media_camp(cid => 1));
ok(!is_media_camp(cid => 100));

ok( is_media_camp(OrderID => 13));
ok(!is_media_camp(OrderID => 12));
ok(!is_media_camp(OrderID => 100));


done_testing;


