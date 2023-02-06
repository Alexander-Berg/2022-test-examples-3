#!/usr/bin/perl

use strict;
use warnings;

use utf8;

use my_inc '../../../../';

use Settings;

use Test::More; # tests => 2;


use Yandex::Test::UTF8Builder;

use_ok('API::Methods::Retargeting');

my $manager = {
    rbac_login_rights => {
        role => 'manager'
    },
    user_info => {
        login => 'holodilnikru'
    },
    uid => 2
};

my $client = {
    rbac_login_rights => {
        role => 'client'
    },
    user_info => {
        login => 'holodilnikru'
    },
    uid => 3
};

my $media = {
    rbac_login_rights => {
        role => 'media'
    },
};

my $superreader = {
    rbac_login_rights => {
        role => 'superreader'
    },
};

*pre_validate = \&API::Methods::Retargeting::pre_validate_retargeting;

subtest 'API::Methods::Retargeting::pre_validate_retargeting - forbiddent actions for media and superreader' => sub {
    plan tests => 10;

    is_deeply( [ pre_validate( $media, { Action => 'Add', Login => 'holodilnikru' } ) ], [ 'NoRights', ], 'media can\'t add retargeting' );
    is_deeply( [ pre_validate( $media, { Action => 'Update', Login => 'holodilnikru' } ) ], [ 'NoRights', ], 'media can\'t update retargeting' );
    is_deeply( [ pre_validate( $media, { Action => 'Delete', Login => 'holodilnikru' } ) ], [ 'NoRights', ], 'media can\'t delete retargeting' );

    my $tmp = { %$media };
    ok( ! pre_validate( $tmp, { Action => 'Get', Login => 'holodilnikru' } ), 'media can get retargeting' );
    is_deeply( $tmp, $media, 'media can get retargeting' );

    is_deeply( [ pre_validate( $superreader, { Action => 'Add', Login => 'holodilnikru' } ) ], [ 'NoRights', ], 'superreader can\'t add retargeting' );
    is_deeply( [ pre_validate( $superreader, { Action => 'Update', Login => 'holodilnikru' } ) ], [ 'NoRights', ], 'superreader can\'t update retargeting' );
    is_deeply( [ pre_validate( $superreader, { Action => 'Delete', Login => 'holodilnikru' } ) ], [ 'NoRights', ], 'superreader can\'t delete retargeting' );

    $tmp = { %$superreader };

    ok( ! pre_validate( $tmp, { Action => 'Get', Login => 'holodilnikru' } ), 'superreader can get retargeting' );
    is_deeply( $tmp, $superreader, 'superreader can get retargeting' );
};

subtest 'API::Methods::Retargeting::pre_validate_retargeting - Login param' => sub {
    plan tests => 4;

    is_deeply( [ pre_validate( $manager, {} ) ], [ 'BadParams', 'Поле Login должно быть указано' ], 'manager must give login' );
    is_deeply( [ pre_validate( $manager, { Login => undef } ) ], [ 'BadParams', 'Поле Login должно быть указано' ], 'manager must give defined login' );
    is_deeply( [ pre_validate( $manager, { Login => ' a(' } ) ], [ 'BadLogin', 'Поле Login' ], 'manager must give login matched /^[a-zA-Z0-9\-\.\@]+$/' );
    # validate_login already check for not_empty
    # is_deeply( [ pre_validate( $manager, { Login => '' } ) ], [ 'BadParams', 'Поле Login должно быть указано' ], 'manager must give not empty login' );
    ok( !pre_validate( $client, {Action => 'Add'} ), 'client shouldn\'t give login' );
};

subtest 'API::Methods::Retargeting::pre_validate_retargeting - Login param' => sub {
    plan tests => 4;

    is_deeply( [ pre_validate( $manager, {} ) ], [ 'BadParams', 'Поле Login должно быть указано' ], 'manager must give login' );
    is_deeply( [ pre_validate( $manager, { Login => undef } ) ], [ 'BadParams', 'Поле Login должно быть указано' ], 'manager must give defined login' );
    is_deeply( [ pre_validate( $manager, { Login => ' a(' } ) ], [ 'BadLogin', 'Поле Login' ], 'manager must give login matched /^[a-zA-Z0-9\-\.\@]+$/' );
    # validate_login already check for not_empty
    # is_deeply( [ pre_validate( $manager, { Login => '' } ) ], [ 'BadParams', 'Поле Login должно быть указано' ], 'manager must give not empty login' );
    ok( !pre_validate( $client, {Action => 'Add'} ), 'client shouldn\'t give login' );
};

subtest 'API::Methods::Retargeting::pre_validate_retargeting - ContextPrice param' => sub {
    plan tests => 22;

    ok( !pre_validate( $client, { Action => 'Add', Retargetings => [] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'no ContextPrice - no errors' );

    ok( !pre_validate( $client, { Action => 'Update', Retargetings => [ {} ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'no ContextPrice - no errors' );

    ok( !pre_validate( $client, {Action => 'Add', Retargetings => [ { ContextPrice => undef } ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'undef ContextPrice - no errors' );

    ok( !pre_validate( $client, { Action => 'Update', Retargetings => [ { ContextPrice => '' } ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'empty ContextPrice - no errors' );

    ok( !pre_validate( $client, { Action => 'Update', Retargetings => [ { ContextPrice => 5, Fields => [qw/ContextPrice/] } ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'Update ContextPrice - no errors' );

    ok( !pre_validate( $client, { Action => 'Add', Retargetings => [ { ContextPrice => 'abc' } ] } ), 'no login errors' );
    is_deeply(
        delete $client->{ret},
        [
            {
                Errors => [
                    { FaultCode => 242, FaultString => 'Неверно указана цена', FaultDetail => "Некорректная цена: 'abc'", },
                ],
            },
        ],
        'Add with bad ContextPrice - BadPrice error'
    );

    ok( !pre_validate( $client, { Action => 'Add', Retargetings => [ { ContextPrice => '0.005' } ] } ), 'no login errors' );
    is_deeply(
        delete $client->{ret},
        [
            {
                Errors => [
                    { FaultCode => 242, FaultString => 'Неверно указана цена', FaultDetail => "Цена не может быть меньше 0.01 у.е.", },
                ],
            },
        ],
        'Add with too small ContextPrice in default currency - BadPrice error'
    );

    ok( !pre_validate( $client, { Action => 'Update', Retargetings => [ { ContextPrice => '25001', Currency => 'RUB', } ] } ), 'no login errors' );
    is_deeply(
        delete $client->{ret},
        [
            {
                Errors => [
                    { FaultCode => 242, FaultString => 'Неверно указана цена', FaultDetail => "Цена не может быть больше 25\x{00A0}000.00 руб.", }, # символ неразрывного пробела
                ],
            },
        ],
        'Update with too big ContextPrice in roubles - BadPrice error'
    );

    ok( !pre_validate( $client, { Action => 'Add', Retargetings => [ { ContextPrice => '1.5' } ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'Add with too small ContextPrice in default currency' );

    ok( !pre_validate( $client, { Action => 'Update', Retargetings => [ { ContextPrice => '1500', Currency => 'RUB', } ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'Update with ContextPrice in roubles' );

    ok( !pre_validate( $client, { Action => 'Get', Retargetings => [ { ContextPrice => 'abc' } ] } ), 'no login errors' );
    is_deeply( delete $client->{ret}, undef, 'check ContextPrice for Add or Update' );
};

done_testing();