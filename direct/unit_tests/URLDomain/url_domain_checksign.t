#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;
use Test::More tests => 5;
use Yandex::HashUtils;

use_ok('URLDomain');

*c = \&url_domain_checksign;

my $href         = 'clck.ru/LBV4';    # что ввел пользователь
my $domain       = 'h.yandex.net';    # что будет показано в объявлении
my $domain_redir = 'fotki.yandex.ru'; # куда на самом деле ведет редирект

my $domain_sign       = url_domain_sign({ href => $href, domain => $domain });
my $domain_redir_sign = url_domain_sign({ href => $href, domain => $domain_redir });

my $vars = {
    href        => $href,
    domain      => $domain,
    domain_sign => $domain_sign,
};

ok(c($vars), 'checksign for domain only');

hash_merge $vars, {
    domain_redir      => $domain_redir,
    domain_redir_sign => $domain_redir_sign,
};
ok(c($vars), 'checksign with domain_redir');


$vars = {
    href        => $href,
    domain      => $domain,
    domain_sign => 'invalid sign!',
};
ok(!c($vars), 'domain only - invalid sign');

hash_merge $vars, {
    domain_redir      => $domain_redir,
    domain_redir_sign => 'invalid sign!',
};
ok(!c($vars), 'domain_redir - invalid sign');





