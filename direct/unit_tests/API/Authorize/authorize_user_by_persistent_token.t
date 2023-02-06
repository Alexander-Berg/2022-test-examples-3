#!/usr/bin/perl

use Direct::Modern;

use Test::More tests => 3;

use Yandex::I18n;
use Settings;

use API::Authorize;

my $ptokens = {
    token1 => {
        login => 'login1',
    },
};

{
    no warnings 'redefine';
    *API::Authorize::PersistentToken::check_token = sub { $ptokens->{ shift() } }; # ($$)
};

*f = \&API::Authorize::authorize_user_by_persistent_token;

is_deeply(
    f(token => 'token1', persistent_token => 'token1'),
    {
        error => 'UserInvalid',
        error_detail => iget('Неверное использование persistent_token')
    },
    'error - both token and persistent token given'
);

is(
    f(persistent_token => 'token2', login => 'login1', remote_addr => '127.0.0.1'),
    undef,
    'bad persistent token given'
);

is(
    f(persistent_token => 'token1', login => 'login2', remote_addr => '127.0.0.1'),
    undef,
    'auth login not equal to persistent token`s login'
);