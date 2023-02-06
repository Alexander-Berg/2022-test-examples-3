#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use ModerateChecks;

use utf8;
use open ':std' => ':utf8';

use Data::Dumper;
$Data::Dumper::Sortkeys = 1;

*cma = sub { return check_moderate_contactinfo(@_) };

my $bo = get_banner();


#nothing changed
is(cma($bo,$bo), 0, 'nothing changed');

#contactinfo deleted
is(cma(get_banner(delete_contactinfo=>1), $bo), 1, 'contactinfo deleted');

#change phone
is(cma(get_banner(change_phone=>1), $bo), 1, "change phone");

#change_ci_slightly
is(cma(get_banner(change_ci_slightly=>1), $bo), 0, "change_ci_slightly");

# declined vcard
# is(cma($bo, get_banner(declined => 1)), 1, "declined vcard");

# geo_camp_vcard
is(cma(get_banner(change_phone => 1), $bo, geo_camp_vcard => 1), 0, "ignore phone change for geo camp vcard");
is(cma(get_banner(change_extra_message => 1), $bo, geo_camp_vcard => 1), 1, "moderate extra_message change for geo camp vcard");
is(cma(get_banner(change_extra_message => 1), $bo), 1, "moderate extra_message change for any vcard");

my $empty_ci = get_banner(delete_contactinfo => 1);
$empty_ci->{phoneflag} = 'New';
is(cma($empty_ci, $empty_ci), 0, 'empty contact info');

done_testing;

sub get_banner
{
    my %OPT = @_;

    my %BASE_BANNER = (
        'apart' => undef,
        'bid' => '4949585',
        'build' => undef,
        'city' => "Москва",
        'city_code' => '495',
        'contact_email' => undef,
        'contactperson' => undef,
        'country' => "Россия",
        'country_code' => '+7',
        'ext' => undef,
        'extra_message' => undef,
        'house' => undef,
        'im_client' => undef,
        'im_login' => undef,
        'name' => 'Name of company',
        'phone' => '6666666',
        'phoneflag' => 'Yes',
        'street' => undef,
        'worktime' => '0#4#10#00#18#00',
    );

    my %b = %BASE_BANNER;
    
    if ($OPT{delete_contactinfo}) {
        foreach (qw/phone house city name country street apart build city_code country_code ext extra_message contactperson contact_email im_login im_client worktime/) {
            delete $b{$_};
        }
    }

    if ($OPT{change_phone}) {
        $b{phone} .= '123';
    }

    if ($OPT{change_extra_message}) {
        $b{extra_message} .= 'abc123';
    }

    if ($OPT{change_ci_slightly}) {
        @b{qw/contact_email im_client im_login/}=('ya@ya.ru', 'Jabber', 'jabber');
    }

    if ($OPT{declined}) {
        $b{phoneflag} = 'No';
    }

    return \%b;
}
