#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Test::More tests => 9;
use Test::Deep;
use Test::Exception;

#use Currencies;
use Currency::Pseudo;

use utf8;
use open ':std' => ':utf8';

*gc = sub { get_pseudo_currency(@_); };

my $pseudo_currency =  {
    id   => re('^.+$'),
    name => re('^.+$'),
    rate => re('^\d+(:?\.\d*)?$'),
    rate_without_nds => re('^\d+(:?\.\d*)?$'),
};

for my $id (qw/rub grivna tenge/){
    local $pseudo_currency->{id} = $id;
    cmp_deeply( gc(id => $id), $pseudo_currency , "pseudo_currency for id $id");
}

dies_ok { gc(id => 'ru') } 'incorrect pseudo_currency id, expecting to die';

for my $domain (qw/ru com ua kz/){
    my $hostname = "direct.yandex.$domain";
    cmp_deeply( gc(hostname => $hostname), $pseudo_currency , "pseudo_currency for hostname $hostname");
}

{
    local $pseudo_currency->{id} = "rub";
    cmp_deeply( gc(hostname => "direct.yandex.asdf"), $pseudo_currency , "pseudo_currency for unknown domain should bu 'rub'");
}


