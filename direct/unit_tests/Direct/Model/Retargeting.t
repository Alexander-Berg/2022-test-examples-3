#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use Yandex::DBShards;

use Direct::Test::DBObjects;

BEGIN {
    use_ok('Direct::Model::Retargeting');
    use_ok('Direct::Model::Retargeting::Manager');
}

sub mk_ret_bid { Direct::Model::Retargeting->new(@_) }

subtest 'Retargeting Model' => sub {
    lives_ok { mk_ret_bid() };
    lives_ok { mk_ret_bid({}) };
    dies_ok { mk_ret_bid("unknown" => "args") };

    subtest 'last_change' => sub {
        my $ret_bid = mk_ret_bid();
        $ret_bid->last_change('now');
        ok $ret_bid->has_last_change;
        ok $ret_bid->get_db_column_value(bids_retargeting => 'modtime', extended => 1)->{val__dont_quote} =~ /^now/i;
    };

    subtest 'to_db_hash' => sub {
        my $ret_bid = mk_ret_bid(id => 5, campaign_id => 1);
        is_deeply $ret_bid->to_db_hash, {ret_id => 5, cid => 1};
    };
};

subtest 'Retargeting Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_campaign('text')->with_adgroup();

    subtest 'Create retargeting bid' => sub {
        my $ret_cond = $db_obj->create_ret_cond();
        my $ret_bid = mk_ret_bid(
            id => get_new_id('ret_id'),
            campaign_id => $db_obj->campaign->id,
            adgroup_id => $db_obj->adgroup->id,
            ret_cond_id => $ret_cond->id,
            price_context => 5.01,
        );
        $ret_bid->last_change('now');
        Direct::Model::Retargeting::Manager->new(items => [$ret_bid])->create();
        cmp_model_with $ret_bid, $db_obj->get_ret_bid($ret_bid->id, shard => $db_obj->shard), exclude => [qw/last_change/];
        is $ret_bid->is_changed, 0, 'Test resetting model state';
    };

    subtest 'Update retargeting bid' => sub {
        my $ret_bid = $db_obj->create_ret_bid();

        $ret_bid->ret_cond_id($db_obj->create_ret_cond()->id);
        $ret_bid->price_context("12.10");

        Direct::Model::Retargeting::Manager->new(items => [$ret_bid])->update();
        cmp_model_with $ret_bid, $db_obj->get_ret_bid($ret_bid->id, shard => $db_obj->shard), exclude => [qw/last_change/];
        is $ret_bid->is_changed, 0, 'Test resetting model state';
    };

    subtest 'Delete retargeting bid' => sub {
        my $ret_bid = $db_obj->create_ret_bid();
        Direct::Model::Retargeting::Manager->new(items => [$ret_bid])->delete();
        is $db_obj->get_ret_bid($ret_bid->id, shard => $db_obj->shard), undef;
    };

    subtest 'Flag: bs_sync_adgroup' => sub {
        my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, {status_bs_synced => 'Yes', last_change => '_in_past'});

        my $ret_bid = $db_obj->create_ret_bid();
        $ret_bid->do_bs_sync_adgroup(1);
        Direct::Model::Retargeting::Manager->new(items => [$ret_bid])->update();

        my $adgroup2 = $db_obj->get_adgroup($adgroup->id);
        is $adgroup2->status_bs_synced, 'No';
        is $adgroup2->last_change, $adgroup->last_change;
    };

    subtest 'Flag: update_adgroup_last_change' => sub {
        my $adgroup = $db_obj->update_adgroup($db_obj->adgroup, {last_change => '_in_past'});

        my $ret_bid = $db_obj->create_ret_bid();
        $ret_bid->do_update_adgroup_last_change(1);
        Direct::Model::Retargeting::Manager->new(items => [$ret_bid])->update();

        ok $db_obj->get_adgroup($adgroup->id)->last_change gt $adgroup->last_change;
    };
};

done_testing;
