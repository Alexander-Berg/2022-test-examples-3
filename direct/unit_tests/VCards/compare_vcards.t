#!/usr/bin/perl

use strict;
use warnings;
use utf8;

use Test::More;
use VCards qw/compare_vcards/;

is(compare_vcards(undef, undef), 0);
is(compare_vcards(undef, {}), 1);
is(compare_vcards({}, undef), 1);
is(compare_vcards({}, {}), '');

my $vcard_1 = {
	address_id => 11,
    geo_id => 2,
    phone => '+7#812#337-20-67#',
    country => 'Россия',
    city => 'Санкт-Петербург',
    street => 'улица Крупской',
    house => 32,
    build => 1,
    apart => 2,
    metro => 20330,
    name => 'HANSE',
    contactperson => 'somebody',
    contact_email => 'some@mail.local',
    worktime => '0#4#10#00#18#00',
    extra_message => 'Оригинальные запчасти для иномарок. Оптовые цены в розницу!',
    im_client => 'icq',
    im_login => '111111111',
    org_details_id => undef,
};
my $vcard_2 = {
	address_id => 11,
    geo_id => 2,
    phone => '+7#812#337-20-67#',
    country => 'Россия',
    city => 'Санкт-Петербург',
    street => 'улица Крупской',
    house => 38,
    build => 1,
    apart => 2,
    metro => 20330,
    name => 'HANSE',
    contactperson => 'somebody',
    contact_email => 'some@mail.local',
    worktime => '0#4#10#00#18#00',
    extra_message => 'Оригинальные запчасти для иномарок. Оптовые цены в розницу!',
    im_client => 'icq',
    im_login => '111111111',
    org_details_id => undef,
};
is(compare_vcards($vcard_1, $vcard_2), 1);


done_testing;
