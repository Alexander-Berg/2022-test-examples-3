use Direct::Modern;

use Test::More;
use List::MoreUtils qw/uniq/;

use Yandex::ListUtils qw/xminus/;

use Settings;

use Direct::BillingAggregates;
use Campaign::Types;

my $all_bill_agg_prod_types = get_all_possible_bill_agg_product_types();

for my $prod_type (@$all_bill_agg_prod_types) {
    is(exists($Direct::BillingAggregates::AGGREGATE_NAME_BY_PRODUCT_TYPE{$prod_type}), 1, "Billing Aggregate name for product type $prod_type exists");
}

done_testing();

sub get_all_possible_bill_agg_product_types {
    my $all_camp_types = get_camp_kind_types('all');
    my $camp_types_to_check = xminus($all_camp_types, get_camp_kind_types('without_billing_aggregates'));
    # TODO удалить internal_autobudget, когда будет поддержка. Для content_promotion поддержка не планируется
    $camp_types_to_check = xminus($camp_types_to_check, ['internal_autobudget', 'content_promotion']);

    return [
        uniq map {
            Campaign::Types::default_product_type_by_camp_type($_),
            @{Direct::BillingAggregates::get_special_product_types_by_camp_type($_)}
        } @$camp_types_to_check
    ];
}
