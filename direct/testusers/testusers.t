#!/usr/bin/perl

# $Id$

use strict;
use warnings;

use Readonly;
use Test::More tests => 5;

use Settings;
use Yandex::DBUnitTest qw( :all );

use TestUsers;

Readonly my $DOMAIN_LOGIN => 'alice';
Readonly my $ROLE         => 'super';
Readonly my $LOGIN        => 'andy-ilyin-super';
Readonly my $CREATOR_UID  => '225634549';

copy_table( PPCDICT, 'testusers' );

sub uid_present {
    my ($uid) = @_;

    my $rows = TestUsers::get_all();

    my ($row) = grep { $_->{uid} == $uid } @{$rows};

    return $row;
}

my $uid = $CREATOR_UID;

my $initial_rows = TestUsers::get_all();
ok( $initial_rows && ref $initial_rows eq 'ARRAY', 'get_all returns an array' );

ok( !uid_present($uid), "$LOGIN not initially present" );

TestUsers::create_or_replace(
    uid          => $uid,
    domain_login => $DOMAIN_LOGIN,
    role         => $ROLE,
    UID          => $CREATOR_UID,
);

my $inserted_row = uid_present($uid);
ok( $inserted_row, "$LOGIN present after it's inserted" );

my $data_ok
    = $inserted_row->{uid} == $uid
    && $inserted_row->{domain_login} eq $DOMAIN_LOGIN
    && $inserted_row->{role} eq $ROLE;

ok( $data_ok, 'inserted row has the data as requested' );

TestUsers::remove( uid => $uid, UID => $CREATOR_UID );

ok( !uid_present($uid), "$LOGIN not present after it's removed" );
