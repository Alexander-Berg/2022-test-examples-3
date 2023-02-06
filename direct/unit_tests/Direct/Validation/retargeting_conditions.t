#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;

use Settings;

use Storable qw/dclone/;

use Direct::Test::DBObjects qw/sandbox ok_func_is_called cmp_model_with ok_vr err_vr/;

BEGIN {
    use_ok 'Direct::Validation::RetargetingConditions';
}

sub mk_ret_cond { Direct::Test::DBObjects->new_ret_cond_model(@_) }
sub vld_ret_conds { my $x = shift; Direct::Validation::RetargetingConditions::validate_retargeting_conditions(ref($x) eq 'ARRAY' ? $x : [$x], @_) }
sub vld_ret_conds_cl { my $x = shift; Direct::Validation::RetargetingConditions::validate_retargeting_conditions_for_client(ref($x) eq 'ARRAY' ? $x : [$x], @_) }

my $MAX_CONDITION_NAME_LENGTH = $Direct::Validation::RetargetingConditions::MAX_CONDITION_NAME_LENGTH;
my $MAX_RULES_IN_RET_COND = $Direct::Validation::RetargetingConditions::MAX_RULES_IN_RET_COND;
my $MAX_GOAL_REACH_TIME_DAYS = $Direct::Validation::RetargetingConditions::MAX_GOAL_REACH_TIME_DAYS;
my $MAX_RET_CONDS_ON_CLIENT = $Direct::Validation::RetargetingConditions::MAX_RET_CONDS_ON_CLIENT;

subtest 'validate_retargeting_conditions' => sub {
    ok_vr vld_ret_conds(mk_ret_cond());

    subtest 'condition_name' => sub {
        err_vr vld_ret_conds(mk_ret_cond(condition_name => "")), 'EmptyField';
        ok_vr vld_ret_conds(mk_ret_cond(condition_name => join("", 'z' x $MAX_CONDITION_NAME_LENGTH)));
        err_vr vld_ret_conds(mk_ret_cond(condition_name => join("", 'z' x ($MAX_CONDITION_NAME_LENGTH + 1)))), 'MaxLength';
    };

    subtest 'Cannot change negative property' => sub {
        my $ret_cond = mk_ret_cond(properties => []);
        $ret_cond->alter(old => $ret_cond->clone, properties => ['negative']);
        err_vr vld_ret_conds($ret_cond), 'BadUsage';
    };

    subtest 'validate_ret_cond_rules' => sub {
        my $rule = {type => 'all', goals => [{goal_id => 1, time => 1}]};

        err_vr vld_ret_conds(mk_ret_cond(condition => [])), 'LimitExceeded';
        ok_vr vld_ret_conds(mk_ret_cond(condition => [($rule) x $MAX_RULES_IN_RET_COND]));
        err_vr vld_ret_conds(mk_ret_cond(condition => [($rule) x ($MAX_RULES_IN_RET_COND + 1)])), 'LimitExceeded';

        subtest 'goal existence' => sub {
            my $goal_id = $rule->{goals}->[0]->{goal_id};
            ok_vr vld_ret_conds(mk_ret_cond(condition => [$rule]), {$goal_id => 1});
            err_vr vld_ret_conds(mk_ret_cond(condition => [$rule]), {$goal_id => 0}), 'NotFound_Goal';
        };

        subtest 'goal_time' => sub {
            my $rule = dclone($rule);

            $rule->{goals}->[0]->{time} = 0;
            err_vr vld_ret_conds(mk_ret_cond(condition => [$rule])), 'InvalidField';

            $rule->{goals}->[0]->{time} = $MAX_GOAL_REACH_TIME_DAYS;
            ok_vr vld_ret_conds(mk_ret_cond(condition => [$rule]));

            $rule->{goals}->[0]->{time} = $MAX_GOAL_REACH_TIME_DAYS + 1;
            err_vr vld_ret_conds(mk_ret_cond(condition => [$rule])), 'InvalidField';
        };

        subtest 'negative and goals-only' => sub {
            my $rule = {type => 'not', goals => [{goal_id => 1_000_000_000 + 1, time => 1}]};
            my $ret_cond = mk_ret_cond(condition => [$rule]);

            ok $ret_cond->is_negative;
            ok $ret_cond->condition->[0]->goals->[0]->goal_type eq 'segment';
            err_vr vld_ret_conds(mk_ret_cond(condition => [$rule])), 'InconsistentState';
        };
    };
};

subtest 'validate_retargeting_conditions_for_client' => sub {
    ok_vr vld_ret_conds_cl(mk_ret_cond());

    subtest 'max ret_conds on client' => sub {
        my @ret_conds = map { mk_ret_cond() } 0..$MAX_RET_CONDS_ON_CLIENT;
        ok vld_ret_conds_cl(\@ret_conds);

        push @ret_conds, mk_ret_cond();
        err_vr vld_ret_conds_cl(\@ret_conds), 'ReachLimit';
    };

    subtest 'uniq condition_name' => sub {
        err_vr vld_ret_conds_cl([mk_ret_cond(condition_name => 'rc1'), mk_ret_cond(condition_name => 'rc1')]), ['Duplicated', 'Duplicated'];
        err_vr vld_ret_conds_cl([mk_ret_cond(condition_name => 'rc1')], [mk_ret_cond(condition_name => 'rc1')]), 'InconsistentState';
    };

    subtest 'uniq condition' => sub {
        my $ret_cond1 = mk_ret_cond();
        my $ret_cond2 = mk_ret_cond(condition => $ret_cond1->condition);

        err_vr vld_ret_conds_cl([$ret_cond1, $ret_cond2]), ['Duplicated', 'Duplicated'];
        err_vr vld_ret_conds_cl([$ret_cond1], [$ret_cond2]), 'InconsistentState';
    };
};

done_testing;
