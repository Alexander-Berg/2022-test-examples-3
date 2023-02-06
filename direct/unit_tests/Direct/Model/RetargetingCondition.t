#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;
use Test::Deep qw/cmp_bag/;

use Settings;

use Yandex::DBShards;

use Direct::Test::DBObjects;
use Primitives qw//;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::RetargetingCondition');
    use_ok('Direct::Model::RetargetingCondition::Manager');
}

sub goal_type { Primitives::get_goal_type_by_goal_id(@_) }
sub mk_ret_cond { Direct::Model::RetargetingCondition->new(@_) }

subtest 'RetargetingCondition Model' => sub {
    lives_ok { mk_ret_cond() };
    lives_ok { mk_ret_cond({}) };
    dies_ok { mk_ret_cond("unknown" => "args") };

    # Condition rules: parse json
    dies_ok { mk_ret_cond(_condition_json => $_) } for (undef, '', '{}', "invalid json");
    dies_ok { mk_ret_cond(_condition_json => '[{"type":"or","goals":[{"goal_id":123,"time":"invalid"}]}]') };
    lives_ok { mk_ret_cond(_condition_json => $_) } for ('[]', '[{"type":"or","goals":[]}]');
    lives_ok { mk_ret_cond(_condition_json => '[{"type":"or","goals":[{"goal_id":123,"time":1}]}]') };
    is_deeply(
        mk_ret_cond(_condition_json => '[{"type":"or","goals":[{"goal_id":123,"time":1}]}]')->to_hash,
        {condition => [{type => 'or', goals => [{goal_id => 123, goal_type => goal_type(123), time => 1}]}], properties => []},
    );
    is_deeply(
        mk_ret_cond(_condition_json => '[{"type":"not","goals":[{"goal_id":123,"time":1}]}]')->to_hash,
        {condition => [{type => 'not', goals => [{goal_id => 123, goal_type => goal_type(123), time => 1}]}], properties => ['negative']},
    );

    # Condition rules: serialization
    dies_ok { mk_ret_cond(condition => undef) };
    dies_ok { mk_ret_cond(condition => [bless({}, 'Direct::Model::NonExistent')]) };
    is
        mk_ret_cond(condition => [{type => 'or', goals => []}])->_condition_json,
        '[{"goals":[],"type":"or"}]';
    is
        mk_ret_cond(condition => [{type => 'or', goals => [{goal_id => 123, time => 1}]}])->_condition_json,
        '[{"goals":[{"goal_id":123,"time":1}],"type":"or"}]';
    is
        mk_ret_cond(condition => [{type => 'or', goals => [{goal_id => "123", time => "1"}]}])->_condition_json,
        '[{"goals":[{"goal_id":123,"time":1}],"type":"or"}]',
        'Serialize strings as integer in goals';
    is_deeply
        mk_ret_cond(condition => [{type => 'not', goals => [{goal_id => 123, time => 1}]}])->to_hash,
        {
            condition => [{type => 'not', goals => [{goal_id => 123, goal_type => goal_type(123), time => 1}]}],
            properties => ['negative'],
        };
    subtest 'Serialize when change condition' => sub {
        my $ret_cond = mk_ret_cond(condition => [{type => 'all', goals => []}]);
        $ret_cond->condition([{type => 'all', goals => [{goal_id => 55555555, time => 10}]}]);
        ok $ret_cond->is_condition_changed;
        is $ret_cond->_condition_json, '[{"goals":[{"goal_id":55555555,"time":10}],"type":"all"}]';
    };

    subtest 'Change negative property' => sub {
        my $ret_cond = mk_ret_cond(condition => [{type => 'all', goals => []}]);
        ok !$ret_cond->is_negative;
        $ret_cond->condition([{type => 'not', goals => []}]);
        ok $ret_cond->is_negative;
    };

    subtest 'last_change' => sub {
        my $ret_cond = mk_ret_cond();
        $ret_cond->last_change('now');
        ok $ret_cond->has_last_change;
        ok $ret_cond->get_db_column_value(retargeting_conditions => 'modtime', extended => 1)->{val__dont_quote} =~ /^now/i;
    };

    subtest 'get_using_goal_ids' => sub {
        my $empty = mk_ret_cond(condition => []);
        cmp_bag($empty->get_using_goal_ids, []);

        my $ret_1 = mk_ret_cond(condition => [{type => 'all', goals => [{goal_id => 1, time => 0}, {goal_id => 3, time => 0}]}]);
        cmp_bag($ret_1->get_using_goal_ids, [1,3]);

        my $ret_2 = mk_ret_cond(condition => [{goals => [{goal_id => 4417469, time => 30}], type => 'or'},{goals => [{goal_id => 2149378, time => 30}], type => 'not'}]);
        cmp_bag($ret_2->get_using_goal_ids, [4417469, 2149378]);

        my $ret_3 = mk_ret_cond(condition => [{"goals" => [{"goal_id" => 1000066666,"time" => 30},{"goal_id" => 1000067451,"time" => 30}],"type" =>"all"},
                                              {"goals" => [{"goal_id" => 11294127,"time" => 30},{"goal_id" => 11273182,"time" => 30},{"goal_id" => 11294132,"time" => 30}],"type" => "not"}]);
        cmp_bag($ret_3->get_using_goal_ids, [1000066666, 1000067451, 11294127, 11273182, 11294132]);
    };
};

subtest 'RetargetingCondition Manager' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj = Direct::Test::DBObjects->new()->with_user();

    subtest 'Create retargeting condition' => sub {
        my $ret_cond = mk_ret_cond(
            id => get_new_id('ret_cond_id', ClientID => $db_obj->user->client_id),
            client_id => $db_obj->user->client_id,
            condition_name => "test condition",
            condition => [{type => 'or', goals => [{goal_id => 123, time => 1}]}],
        );
        $ret_cond->last_change('now');

        Direct::Model::RetargetingCondition::Manager->new(items => [$ret_cond])->create();

        my $ret_cond_db = $db_obj->get_ret_cond($ret_cond->id, with_additional => 1);
        cmp_model_with $ret_cond, $ret_cond_db, exclude => [qw/last_change/];
        ok $ret_cond_db->is_accessible;

        is $ret_cond->is_changed, 0, 'Test resetting model state';
    };

    subtest 'Update retargeting condition' => sub {
        my $ret_cond = $db_obj->create_ret_cond();

        $ret_cond->condition_name("Test ret_cond updated");
        $ret_cond->condition([{type => 'or', goals => [{goal_id => 999, time => 1}]}]);

        Direct::Model::RetargetingCondition::Manager->new(items => [$ret_cond])->update();
        cmp_model_with $ret_cond, $db_obj->get_ret_cond($ret_cond->id, with_additional => 1), exclude => [qw/last_change/];
        is $ret_cond->is_changed, 0, 'Test resetting model state';
    };

    subtest 'Flag: bs_sync_adgroups' => sub {
        my $ret_cond = $db_obj->create_ret_cond();

        # Text adgroup with retargeting
        my $adgroup_text = $db_obj->create_adgroup('text', {status_bs_synced => 'Yes', last_change => '_in_past'});
        my $ret_bid = $db_obj->create_ret_bid({adgroup_id => $adgroup_text->id, ret_cond_id => $ret_cond->id});

        # Performance adgroup with retargeting
        my $adgroup_perf = $db_obj->create_adgroup('performance', {status_bs_synced => 'Yes', last_change => '_in_past'});
        my $perf_filter = $db_obj->create_perf_filter({adgroup_id => $adgroup_perf->id, ret_cond_id => $ret_cond->id});
        my $banner_perf = $db_obj->create_banner('performance', {adgroup_id => $adgroup_perf->id, status_bs_synced => 'Yes', last_change => '_in_past'});

        $ret_cond->do_bs_sync_adgroups(1);
        Direct::Model::RetargetingCondition::Manager->new(items => [$ret_cond])->update();

        # Test text adgroup
        my $adgroup_text_db = $db_obj->get_adgroup($adgroup_text->id);
        is $adgroup_text_db->status_bs_synced, 'No';
        cmp_model_with $adgroup_text, $adgroup_text_db, exclude => [qw/status_bs_synced/];

        # Test performance adgroup with banners
        my $adgroup_perf_db = $db_obj->get_adgroup($adgroup_perf->id);
        is $adgroup_perf_db->status_bs_synced, 'No';
        cmp_model_with $adgroup_perf, $adgroup_perf_db, exclude => [qw/status_bs_synced/];

        my $banner_perf_db = $db_obj->get_banner($banner_perf->id);
        is $banner_perf_db->status_bs_synced, 'No';
        cmp_model_with $banner_perf, $banner_perf_db, exclude => [qw/status_bs_synced/];
    };

    subtest 'Flag: bs_sync_multipliers' => sub {
        # Need to implement multipliers in DBObjects
        ok 1;
    };

    subtest 'Flag: update_adgroups_last_change' => sub {
        my $ret_cond = $db_obj->create_ret_cond();

        # Text adgroup with retargeting
        my $adgroup_text = $db_obj->create_adgroup('text', {last_change => '_in_past'});
        my $ret_bid = $db_obj->create_ret_bid({adgroup_id => $adgroup_text->id, ret_cond_id => $ret_cond->id});

        $ret_cond->do_update_adgroups_last_change(1);
        Direct::Model::RetargetingCondition::Manager->new(items => [$ret_cond])->update();

        # Test text adgroup
        my $adgroup_text_db = $db_obj->get_adgroup($adgroup_text->id);
        ok $adgroup_text_db->last_change gt $adgroup_text->last_change;
        cmp_model_with $adgroup_text, $adgroup_text_db, exclude => [qw/last_change/];
    };

    subtest 'Flag: clear_goals' => sub {
        my $ret_cond = $db_obj->create_ret_cond();

        ok @{$db_obj->get_ret_cond_goals($ret_cond->id)} > 0;
        $ret_cond->do_clear_goals(1);
        Direct::Model::RetargetingCondition::Manager->new(items => [$ret_cond])->update();
        ok @{$db_obj->get_ret_cond_goals($ret_cond->id)} == 0;
    };
};

done_testing;
