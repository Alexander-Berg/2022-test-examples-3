#!/usr/bin/perl

#    $Id$

use strict;
use utf8;
use warnings;
use Test::More;
use Test::Deep;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

use URLDomain;

my %db = (
    trusted_redirects => {
        original_db => PPCDICT,
        like => 'trusted_redirects',
        rows => [
            {
                domain => 'tinyurl.com',
                redirect_type => 'short',
            },

            {
                domain => 'click01.begun.ru',
                redirect_type => 'counter',
            },
            
            {
                domain => 'begun.ru',
                redirect_type => 'counter',
            },

            {
                domain => 'myragon.ru',
                redirect_type => 'counter',
            },
        ],
    },
);

init_test_dataset(\%db);

my @test_data = (
    {
      input => undef,
      expect => 0,
      msg => 'invalid input',
      tester => \&is,
    },
    
    {
      input => '',
      expect => 0,
      msg => 'invalid input',
      tester => \&is,
    },

    {
      input => [],
      expect => {},
      msg => 'empty input',
      tester => \&cmp_deeply,
    },

    {
      input => 'https://tinyurl.com/a/b/c/?a=b&c=d',
      expect => 0,
      msg => 'unknown redirect',
      tester => \&is,
    },

    {
      input => 'http://this-domain.is-not-in-the-table.begun.ru/a/b/c/?a=b&c=d',
      expect => 1,
      msg => 'known redirect',
      tester => \&is,
    },

    {
      input => ['https://tinyurl.com/a/b/c/?a=b&c=d'],
      expect => {'tinyurl.com' => 0},
      msg => 'unknown redirect (array ref)',
      tester => \&cmp_deeply,
    },

    {
      input => ['https://click01.begun.ru/a/b/c/?a=b&c=d'],
      expect => {'click01.begun.ru' => 1},
      msg => 'known redirect (array ref)',
      tester => \&cmp_deeply,
    },

    {
      input => ['https://this-domain.is-not-in-the-table.begun.ru/a/b/c/?a=b&c=d'],
      expect => {'this-domain.is-not-in-the-table.begun.ru' => 1},
      msg => 'known redirect (array ref)',
      tester => \&cmp_deeply,
    },

    {
      input => ['unknown1.com/a/b/c/?a=b&c=d', 'unknown2.com/a/b/c/?a=b&c=d', 'unknown3.com/a/b/c/?a=b&c=d'],
      expect => {'unknown1.com' => 0, 'unknown2.com' => 0, 'unknown3.com' => 0},
      msg => 'all unknown redirects',
      tester => \&cmp_deeply,
    },

    {
      input => ['this-domain.is-not-in-the-table.begun.ru/a/b/c/?a=b&c=d', 'unknown1.com/a/b/c/?a=b&c=d', 'unknown2.com/a/b/c/?a=b&c=d', 'click01.begun.ru/a/b/c/?a=b&c=d'],
      expect => {'this-domain.is-not-in-the-table.begun.ru' => 1, 'unknown1.com' => 0, 'unknown2.com' => 0, 'click01.begun.ru' => 1},
      msg => '2 known redirects, 2 unknown redirects',
      tester => \&cmp_deeply,
    },
);

Test::More::plan(tests => scalar(@test_data));

*ikr = \&URLDomain::is_known_redirect;

foreach my $data (@test_data) {
    $data->{tester}->(ikr($data->{input}), $data->{expect}, $data->{msg});
}
