#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use Direct::Test::DBObjects;

BEGIN {
    use_ok('Direct::Model::BidRelevanceMatch');
    use_ok('Direct::Model::BidRelevanceMatch::Manager');
    use_ok('Direct::Model::BidRelevanceMatch::Helper');
}

sub mk_bid_relevance_match { Direct::Model::BidRelevanceMatch->new(@_) }

subtest 'Bid Model' => sub {
        lives_ok { mk_bid_relevance_match() };
        lives_ok { mk_bid_relevance_match({ }) };
        dies_ok { mk_bid_relevance_match("unknown" => "args") };

        subtest 'last_change' => sub {
                my $bid = mk_bid_relevance_match();
                $bid->last_change('now');
                ok $bid->has_last_change;
                ok $bid->get_db_column_value(bids_base => 'LastChange', extended => 1)->{val__dont_quote} =~ /^now/i;
            };

        subtest 'to_template_hash' => sub {
                my $bid = mk_bid_relevance_match(id => 5, campaign_id => 1);
                is_deeply $bid->to_template_hash, { bid_id => 5, campaign_id => 1, bid_type => 'relevance_match' };
            };
    };

subtest 'Bid Manager (for relevance_match)' => sub {
        Direct::Test::DBObjects->create_tables;

        my $db_obj = Direct::Test::DBObjects->new()->with_campaign('text')->with_adgroup();

        subtest 'Create bid' => sub {
                my $bid = mk_bid_relevance_match(
                    bid_type    => 'relevance_match',
                    adgroup_id  => $db_obj->adgroup->id,
                    campaign_id => $db_obj->campaign->id,
                    price       => 5.01,
                    href_param1 => 'param01'
                );
                $bid->last_change('now');
                Direct::Model::BidRelevanceMatch::Manager->new(items => [ $bid ])->create();
                cmp_model_with $bid, $db_obj->get_bid($bid->id, shard => $db_obj->shard), exclude => [ qw/last_change/ ];
                is $bid->is_changed, 0, 'Test resetting model state';
            };

        subtest 'Update bid' => sub {
                my $bid = $db_obj->create_bid('relevance_match');

                $bid->price("12.10");
                $bid->href_param1('p1');
                $bid->href_param2('p2');
                Direct::Model::BidRelevanceMatch::Manager->new(items => [ $bid ])->update();
                cmp_model_with $bid, $db_obj->get_bid($bid->id, shard => $db_obj->shard), exclude => [ qw/last_change/ ];
                is $bid->is_changed, 0, 'Test resetting model state';
            };

        subtest 'Delete bid' => sub {
                my $bid = $db_obj->create_bid('relevance_match', { price => 12.33, href_param1 => 'param1' });
                $bid->adgroup($db_obj->adgroup);

                Direct::Bids::BidRelevanceMatch->new(items => [ $bid ])->delete(uid => $db_obj->user->id);
                ok $db_obj->get_bid($bid->id, shard => $db_obj->shard, with_deleted => 1)->is_deleted;

                my $new_bid = mk_bid_relevance_match(
                    bid_type    => 'relevance_match',
                    adgroup_id  => $db_obj->adgroup->id,
                    campaign_id => $db_obj->campaign->id,
                );
                Direct::Model::BidRelevanceMatch::Manager->new(items => [ $new_bid ])->create();
                ok $new_bid->id == $bid->id;

                my $new_bid2 = $db_obj->get_bid($new_bid->id, shard => $db_obj->shard);
                ok !$new_bid2->is_deleted;
                ok !$new_bid2->href_param1;
            };

        subtest 'Flag: bs_sync_adgroup' => sub {
                my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, { status_bs_synced => 'Yes', last_change => '_in_past' });

                my $bid = $db_obj->create_bid('relevance_match');
                $bid->do_bs_sync_adgroup(1);
                Direct::Model::BidRelevanceMatch::Manager->new(items => [ $bid ])->update();

                my $adgroup2 = $db_obj->get_adgroup($adgroup->id);
                is $adgroup2->status_bs_synced, 'No';
                is $adgroup2->last_change, $adgroup->last_change;
            };

        subtest 'Flag: update_adgroup_last_change' => sub {
                my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, { last_change => '_in_past' });

                my $bid = $db_obj->create_bid('relevance_match');
                $bid->do_update_adgroup_last_change(1);
                Direct::Model::BidRelevanceMatch::Manager->new(items => [ $bid ])->update();

                ok $db_obj->get_adgroup($adgroup->id)->last_change gt $adgroup->last_change;
            };

        subtest 'Flag: moderate_adgroup' => sub {
                my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, { status_moderate => 'No' });

                my $bid = $db_obj->create_bid('relevance_match');
                $bid->do_moderate_adgroup(1);
                Direct::Model::BidRelevanceMatch::Manager->new(items => [ $bid ])->update();

                my $adgroup2 = $db_obj->get_adgroup($adgroup->id);
                is $adgroup2->status_moderate, 'Ready';
            };
    };

subtest 'Bid Helper' => sub {
        subtest 'XLS is relevance match' => sub {
                ok is_relevance_match(q/---autotargeting/);
                ok is_relevance_match(q/'---autotargeting/);
                ok is_relevance_match(q/---AUTOTARGETING/);
                ok is_relevance_match(q/'---AutoTargeting/);
                ok !is_relevance_match(q/autotargeting/);
                ok !is_relevance_match(q/'autotargeting/);
                ok !is_relevance_match("");
            };
    };

done_testing;
