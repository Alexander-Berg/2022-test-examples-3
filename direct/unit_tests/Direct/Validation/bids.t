#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;

use Settings;

use Direct::Test::DBObjects;
use Currencies qw/get_currency_constant/;

BEGIN {
    use_ok 'Direct::Validation::Bids';
}

our $CLIENT_HAS_CONTEXT_RELEVANCE_MATCH_FEATURE = 0;

sub mk_bid_relevance_match { shift->new_bid_relevance_match_model( @_ ) }
sub vld_relevance_matches {
    my ($relevance_matches, $is_autobudget, $adgroup, $strategy_extra) = @_;
    $strategy_extra //= {};
    Direct::Validation::Bids::validate_relevance_matches_for_adgroup(
        ((ref($relevance_matches) eq 'ARRAY') ? $relevance_matches : [ $relevance_matches ]),
        $adgroup,
        {
            currency => 'RUB',
            strategy => { is_autobudget => $is_autobudget, name => 'different_places', %$strategy_extra },
            type => 'text', ClientID => 1
        }
    )
}

no warnings 'redefine';
local *Client::ClientFeatures::has_context_relevance_match_feature = sub($){$CLIENT_HAS_CONTEXT_RELEVANCE_MATCH_FEATURE};

subtest 'relevance_matches' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'mobile_content' );
        $db_objects->with_adgroup( 'mobile_content' );
        $db_objects->adgroup->active_keywords_count( 1 );
        $db_objects->adgroup->active_retargetings_count( 1 );
        $db_objects->adgroup->active_target_interests_count( 1 );

        ok_vr vld_relevance_matches(mk_bid_relevance_match($db_objects, autobudget_priority => 3), 1, $db_objects->adgroup);
        ok_vr vld_relevance_matches(mk_bid_relevance_match($db_objects, autobudget_priority => 3, href_param1 => 'param'), 1, $db_objects->adgroup);

        subtest 'relevance_matches_limit_exceeded' => sub {
                err_vr vld_relevance_matches(
                        [ mk_bid_relevance_match($db_objects), mk_bid_relevance_match($db_objects) ],
                        1,
                        $db_objects->adgroup
                    ), 'LimitExceeded';
            };

        subtest 'relevance_matches_incorrect_autobudget_priority' => sub {
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, autobudget_priority => 0),
                        1,
                        $db_objects->adgroup
                    ), 'InvalidField';
            };

        subtest 'relevance_matches_incorrect_autobudget_priority' => sub {
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, autobudget_priority => undef),
                        1,
                        $db_objects->adgroup
                    ), 'ReqField';
            };

        subtest 'relevance_matches_incorrect_price' => sub {
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, price => get_currency_constant('RUB', 'MIN_PRICE') - 1),
                        0,
                        $db_objects->adgroup,
                        {is_net_stop => 1}
                    ), 'InvalidField';
            };

        subtest 'relevance_matches_incorrect_price' => sub {
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, price => get_currency_constant('RUB', 'MAX_PRICE') + 1),
                        0,
                        $db_objects->adgroup,
                        {is_net_stop => 1}
                    ), 'InvalidField';
            };
        
        subtest 'relevance_matches_incorrect_price_context' => sub {
                local $CLIENT_HAS_CONTEXT_RELEVANCE_MATCH_FEATURE = 1;
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, price_context => get_currency_constant('RUB', 'MIN_PRICE') - 1),
                        0,
                        $db_objects->adgroup,
                        {is_search_stop => 1}
                    ), 'InvalidField';
            };
        
        subtest 'relevance_matches_incorrect_price_context' => sub {
                local $CLIENT_HAS_CONTEXT_RELEVANCE_MATCH_FEATURE = 1;
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, price_context => get_currency_constant('RUB', 'MAX_PRICE') + 1),
                        0,
                        $db_objects->adgroup,
                        {is_search_stop => 1}
                    ), 'InvalidField';
            };

        subtest 'relevance_matches_incorrect_price_context_without_extended_relevance_match_feature' => sub {
                local $CLIENT_HAS_CONTEXT_RELEVANCE_MATCH_FEATURE = 0;
                ok_vr vld_relevance_matches(
                    (mk_bid_relevance_match($db_objects, price_context => get_currency_constant('RUB', 'MAX_PRICE') + 1)),
                    0,
                    $db_objects->adgroup,
                    {is_search_stop => 1}
                );
        };

        subtest 'relevance_matches_bad_href_param' => sub {
                err_vr vld_relevance_matches(
                        mk_bid_relevance_match($db_objects, autobudget_priority => 3,  href_param1 => join('', map { 'a' } 1..$Settings::MAX_HREF_PARAM_LENGTH+1)),
                        1,
                        $db_objects->adgroup
                    ), 'MaxLength';
            };
    };

done_testing;
