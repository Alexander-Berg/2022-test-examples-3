#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More tests => 1;

use VCards;

use utf8;
use open ':std' => ':utf8';

*gc = sub { get_contacts_string(@_); };

my $ci = {
    phone         => "+7#812#2128506",
    name          => "company", 
    contactperson => "mr. Smith",
    worktime      => "1#3#10#15#18#30;4#6#10#30#20#25",
    country       => "Russia", 
    city          => "Moscow",
    extra_message => "I know it's hard to believe, but I haven't been warm for a week..."
};

like(gc($ci), qr/^.+$/s, "stupid test for suspicious subroutine");

