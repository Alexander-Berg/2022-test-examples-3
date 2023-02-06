#!/usr/bin/perl

use Direct::Modern;

use Test::More tests => 1;

use my_inc '../..';

use Settings;

use Rbac;


my $actual = {
    1 => {
        some_other_field       => 1,
        chiefs_client_id       => undef,
        chiefs_uid             => '[]',
        subordinates_client_id => '[2,3]',
        subordinates_uid       => '[4,5]',
    },
    2 => {
        some_another_field     => 'blah-blah-blah',
        chiefs_client_id       => '[6,7,8]',
        chiefs_uid             => '[9,10,11]',
        subordinates_client_id => undef,
        subordinates_uid       => '[]',
    },
};

Rbac::_unpack_json_fields( $actual );

my $expected = {
    1 => {
        some_other_field       => 1,
        chiefs_client_id       => [],
        chiefs_uid             => [],
        subordinates_client_id => [2, 3],
        subordinates_uid       => [4, 5],
    },
    2 => {
        some_another_field     => 'blah-blah-blah',
        chiefs_client_id       => [qw/6 7 8/],
        chiefs_uid             => [qw/ 9 10 11 /],
        subordinates_client_id => [],
        subordinates_uid       => [],
    },
};

is_deeply( $actual, $expected, 'test unpacking json fields' );