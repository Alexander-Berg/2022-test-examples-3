use Direct::Modern;

use Test::More;

use Yandex::ListUtils qw/xminus/;

use Campaign::Types;

my $all_camp_types = get_camp_kind_types('all');
my $camp_types_to_check = xminus($all_camp_types, get_camp_kind_types('without_billing_aggregates'));
$camp_types_to_check = xminus($camp_types_to_check, ['internal_autobudget']); # TODO удалить эту строку, когда будет сделана поддержка internal_autobudget

for my $type (@$camp_types_to_check) {
    my $prod_type = Campaign::Types::default_product_type_by_camp_type($type);
    is(defined($prod_type), 1);
}

done_testing();
