#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use Yandex::ListUtils qw/xminus/;

use utf8;

use Campaign::Types;


# кампании, для которых не нужно подставлять параметр type:
# mcb    - отправляется в отдельном экспорте
# wallet - не бывает ссылок
my $no_param_type = [qw/
    mcb
    wallet
    billing_aggregate
/];

my $all_camp_types = get_camp_kind_types('all');
my $camp_types_to_check = xminus($all_camp_types, $no_param_type);

plan tests => scalar @$camp_types_to_check;

foreach my $camp_type (@$camp_types_to_check) {
    my $type_param = Campaign::Types::camp_type_to_param($camp_type);
    like($type_param, qr/^type[0-9]+$/, ($type_param? $type_param: 'undef') ." is $camp_type");
}
