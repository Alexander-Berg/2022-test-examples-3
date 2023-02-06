#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;

use API::Authorize;
use APICommon qw//;

use Settings;
use Yandex::DBUnitTest;

use API::Settings qw//;
use HashingTools qw/hmac_sha256_hex/;

my %db = (
    shard_login => {
        original_db => PPCDICT,
        rows => [
            { login => 'l1', uid => 11 },
            { login => 'l2', uid => 22 },
            { login => 'l3', uid => 33 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 11, ClientID => 111 },
            { uid => 22, ClientID => 222 },
            { uid => 33, ClientID => 333 },
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 111, shard => 1 },
            { ClientID => 222, shard => 2 },
            { ClientID => 333, shard => 3 },
        ],
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 11, ClientID => 111 },
            ],
            2 => [
                { uid => 22, ClientID => 222 },
            ],
            3 => [
                { uid => 33, ClientID => 333 },
            ],
        },
    },
);
Yandex::DBUnitTest::init_test_dataset( \%db );

my $ptoken_key = $API::Settings::API_PERSISTENT_TOKENS_KEY = 'key';
sub ptoken_hmac {
    return hmac_sha256_hex($_[0], $ptoken_key);
}

my $ptokens = $API::Settings::API_PERSISTENT_TOKENS = {
    ptoken_hmac('token1') => {
        login => 'l1',
        application_id => 'a1',
        allow_to => [qr/bm.*.yandex.ru/],
    },
    ptoken_hmac('token2') => {
        login => 'l2',
        application_id => 'a2',
        allow_to => [],
    },
    ptoken_hmac('token3') => {
        login => 'l2',
        application_id => 'a2',
        allow_to => ['dns.google.com',
                     "networks:private.txt"],
    },
};

*f = \&API::Authorize::authorize_user_by_persistent_token;

my %dns = (
    '12.12.12.12' => 'bmprod01d.yandex.ru',
    '13.13.13.13' => 'bmprod01d.yandex.ru.mail.ru',
    '8.8.8.8' => 'dns.google.com',
    );
{ 
    no warnings 'redefine';
    *IpTools::get_hostname_fcrdns = sub ($) {$dns{$_[0]}};
    *APICommon::get_uid = sub ($$) {return 1};
};

my $LAST_WARN = '';
$SIG{__WARN__} = sub { $LAST_WARN = $_[0] };

# token1
is(f(persistent_token => '', login => '', remote_addr => '12.12.12.12'), undef, 'no token');
is(f(persistent_token => 'token13', login => 'l1', remote_addr => '12.12.12.12'), undef, 'incorrect token');
is(f(persistent_token => 'token1', login => 'l2', remote_addr => '12.12.12.12'), undef, 'incorrect login');
is(f(persistent_token => 'token1', login => 'l1', remote_addr => '1.1.1.1'), undef, 'incorrect addr');
is(f(persistent_token => 'token1', login => 'l1', remote_addr => '13.13.13.13'), undef, 'incorrect addr');
ok(f(persistent_token => 'token1', login => 'l1', remote_addr => '12.12.12.12'), 'correct l1');


# token2
is(f(persistent_token => 'token2', login => 'l2', remote_addr => '12.12.12.12'), undef, 'no acl');
is(f(persistent_token => 'token2', login => 'l2', remote_addr => '12.12.12.14'), undef, 'no acl');


# token3
is(f(persistent_token => 'token3', login => 'l2', remote_addr => '12.12.12.14'), undef, 'incorrect hostname');
ok(got_last_warn() =~ m/^token for login l2 application a2 authorized, but not from allowed network\. IP: 12\.12\.12\.14 at /, 'got warning about access from restricted network');

is(f(persistent_token => 'token3', login => 'l2', remote_addr => '12.12.12.12'), undef, 'incorrect hostname');
ok(f(persistent_token => 'token3', login => 'l2', remote_addr => '8.8.8.8'), 'good hostname');
ok(f(persistent_token => 'token3', login => 'l2', remote_addr => '127.0.0.1'), 'good network');
ok(f(persistent_token => 'token3', login => 'l2', remote_addr => '10.0.0.7'), 'good network');


done_testing;

sub got_last_warn {
    my $ret = $LAST_WARN;
    $LAST_WARN = '';
    return $ret;
}
