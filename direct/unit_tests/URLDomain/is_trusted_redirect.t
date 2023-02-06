#!/usr/bin/perl

#    $Id$

use warnings;
use strict;
use utf8;
use open ':std' => ':utf8';
use Test::More tests => 19;

use Settings;
use Yandex::DBTools;
use Yandex::DBUnitTest qw/:all/;

copy_table(PPCDICT,'trusted_redirects');
do_insert_into_table(PPCDICT, 'trusted_redirects', {
    domain => 'tinyurl.com',
    redirect_type => 'short',
});
do_insert_into_table(PPCDICT, 'trusted_redirects', {
    domain => 'click01.begun.ru',
    redirect_type => 'counter',
});

do_insert_into_table(PPCDICT, 'trusted_redirects', {
    domain => 'myragon.ru',
    redirect_type => 'counter',
});

use URLDomain;

*itr = \&URLDomain::is_trusted_redirect;


ok(!itr(), 'invalid input');
ok(!itr('',''), 'invalid input');
ok(!itr(undef,undef), 'invalid input');
ok(!itr('invalid domain','invalid_domain2'), 'invalid input');

ok(!itr('example.com', 'unknown-redirect-service.com'), 'unknown redirect');

ok(itr('example.com', 'example.com'), 'not a redirect is also ok');

ok(itr('example.com', 'www.example.com'), 'www redirect');
ok(itr('www.example.com', 'example.com'), 'www redirect');
ok(itr('example.com','market.example.com'), 'redirect to subdomain');
ok(itr('market.example.com','example.com'), 'redirect to subdomain');
ok(itr('market.example.com','maps.example.com'), 'redirect to subdomain');

ok(!itr('good.ya.ru','bad.ya.ru'), 'subdomain on known hostings');

ok(!itr('example.com','tinyurl.com'), 'unknown redirectors');
ok(itr('example.com','click01.begun.ru'), 'known redirectors');

ok(itr('example.com','example.com?ref=123'), 'href with params');
ok(itr('example.com','example.com?ref=123&id=321'), 'href with params');
ok(itr('example.com','example.com/user/name'), 'href with params');
ok(itr('example.com','example.com#anchor'), 'href with params');

ok(itr('example.com','a72.myragon.ru'), 'redirect with subdomain in counter');

