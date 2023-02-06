#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use List::MoreUtils qw/any/;

use Direct::Test::DBObjects qw/sandbox ok_func_is_called cmp_model_with/;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::RetargetingCondition');
    use_ok('Direct::RetargetingConditions');
}

sub mk_ret_cond { Direct::Test::DBObjects->new_ret_cond_model(@_) }
sub last_change__dont_quote { shift->get_db_column_value(retargeting_conditions => 'modtime', extended => 1)->{val__dont_quote} }

subtest 'Get retargeting conditions with camp info' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_obj = Direct::Test::DBObjects->new()->with_user();
        my $client_id = $db_obj->user->client_id;
        my $ret_bid = $db_obj->create_ret_bid();

        my $ret_cond = Retargeting::get_retargeting_conditions(ClientID => $client_id);
        ok !exists $ret_cond->{$ret_bid->ret_cond_id}->{campaigns};

        my $ret_cond_with_camp = Retargeting::get_retargeting_conditions(ClientID => $client_id, with_campaigns => 1);
        ok exists $ret_cond_with_camp->{$ret_bid->ret_cond_id}->{campaigns};
    };

subtest 'Prepare to create retargeting conditions' => sub {
    my $ret_cond = mk_ret_cond();

    ok !$ret_cond->has_last_change;
    Direct::RetargetingConditions->new([$ret_cond])->prepare_create();
    like last_change__dont_quote($ret_cond), qr/^now/i;
};

subtest 'Prepare to update retargeting conditions' => sub {
    my $orig_ret_cond = mk_ret_cond(is_accessible => 1);

    ok !$orig_ret_cond->has_last_change;

    # Изменение condition_name
    sandbox $orig_ret_cond => sub {
        $_->condition_name(mk_ret_cond()->condition_name);
        Direct::RetargetingConditions->new([$_])->prepare_update();
        is_deeply $_->get_state_hash, {
            changes => {condition_name => 1, last_change => 1},
            flags => {update_adgroups_last_change => 1},
        };
        like last_change__dont_quote($_), qr/^now/i;
    };

    # Изменение condition_desc
    sandbox $orig_ret_cond => sub {
        $_->condition_desc(mk_ret_cond()->condition_desc);
        Direct::RetargetingConditions->new([$_])->prepare_update();
        is_deeply $_->get_state_hash, {
            changes => {condition_desc => 1, last_change => 1},
            flags => {update_adgroups_last_change => 1},
        };
        like last_change__dont_quote($_), qr/^now/i;
    };

    # Изменение condition
    sandbox $orig_ret_cond => sub {
        $_->condition(mk_ret_cond()->condition);
        Direct::RetargetingConditions->new([$_])->prepare_update();
        is_deeply $_->get_state_hash, {
            changes => {_condition_json => 1, last_change => 1},
            flags => {bs_sync_adgroups => 1, bs_sync_multipliers => 1, update_adgroups_last_change => 1},
        };
        like last_change__dont_quote($_), qr/^now/i;
    };

    # Изменение is_deleted
    sandbox $orig_ret_cond => sub {
        $_->is_deleted(1);
        Direct::RetargetingConditions->new([$_])->prepare_update();
        is_deeply $_->get_state_hash, {
            changes => {is_deleted => 1, last_change => 1},
            flags => {},
        };
        like last_change__dont_quote($_), qr/^now/i;
    };

    # is_accessible == 0
    sandbox $orig_ret_cond => sub {
        $_->is_accessible(0); # no track changes
        Direct::RetargetingConditions->new([$_])->prepare_update();
        is_deeply $_->get_state_hash, {
            changes => {_condition_json => 1, last_change => 1},
            flags => {bs_sync_adgroups => 1, bs_sync_multipliers => 1, update_adgroups_last_change => 1},
        };
        like last_change__dont_quote($_), qr/^now/i;
        ok $_->_condition_json eq $orig_ret_cond->_condition_json;
    };
};

subtest 'Prepare to delete retargeting conditions' => sub {
    my $ret_cond = mk_ret_cond();

    Direct::RetargetingConditions->new([$ret_cond])->prepare_delete();
    is_deeply $ret_cond->get_state_hash, {
        changes => {is_deleted => 1, last_change => 1},
        flags => {clear_goals => 1},
    };
    like last_change__dont_quote($ret_cond), qr/^now/i;
};

subtest 'General' => sub {
    ok %{Direct::RetargetingConditions->WEB_FIELD_NAMES};
};

subtest 'Tests with database access' => sub {
    Direct::Test::DBObjects->create_tables;

    subtest 'Select performance filters' => sub {
        my $db_obj = Direct::Test::DBObjects->new()->with_user();
        local *_get_by = sub { Direct::RetargetingConditions->get_by(@_) };

        my (@ret_conds, @ret_conds_del);
        push @ret_conds, $db_obj->create_ret_cond() for 0..3;
        push @ret_conds, $db_obj->create_ret_cond({condition_name => "findbyname"});
        push @ret_conds_del, $db_obj->create_ret_cond({is_deleted => 1});

        dies_ok { _get_by(unknown => "key") };

        subtest 'Simple select' => sub {
            ok @{_get_by(client_id => $db_obj->user->client_id)->items} == @ret_conds, 'Select by client_id';
            ok _get_by(id => $ret_conds[0]->id)->items->[0]->id == $ret_conds[0]->id, 'Select by id (ret_cond_id)';
        };

        subtest 'Select with limit/offset/total_count' => sub {
            my $x = _get_by(client_id => $db_obj->user->client_id, limit => 2, offset => 1, total_count => 1);
            ok $x->total == @ret_conds;
            ok @{$x->items} == 2;
            ok $x->items->[0]->id == $ret_conds[1]->id;
        };

        subtest 'Default sort is by id asc' => sub {
            my @items = @{_get_by(client_id => $db_obj->user->client_id)->items};
            is join(':', map { $_->id } @items), join(':', sort { $a <=> $b } map { $_->id } @items);
        };

        subtest 'Select with_deleted' => sub {
            my @items = @{_get_by(client_id => $db_obj->user->client_id, with_deleted => 1)->items};
            ok @items == (@ret_conds + @ret_conds_del);
            ok any { $_->is_deleted } @items;
        };

        subtest 'Select by filter' => sub {
            ok @{_get_by(client_id => $db_obj->user->client_id, filter => {condition_name__contains => 'nothing'})->items} == 0;
            ok @{_get_by(client_id => $db_obj->user->client_id, filter => {condition_name__contains => 'findbyname'})->items} == 1;
        };

        subtest 'Select using fields' => sub {
            my $ret_cond = _get_by(client_id => $db_obj->user->client_id, fields => [qw/id condition_name/])->items->[0];
            ok keys(%{$ret_cond->to_hash}) == 2;
            dies_ok { $ret_cond->condition };
        };

        subtest 'Calculate is_accessible' => sub {
            my $ret_cond = _get_by(client_id => $db_obj->user->client_id, limit => 1)->items->[0];
            ok $ret_cond->is_accessible;
            $db_obj->sql("UPDATE retargeting_goals SET is_accessible = 0 WHERE goal_id = ?", $ret_cond->condition->[0]->goals->[0]->goal_id);
            ok ! _get_by(client_id => $db_obj->user->client_id, limit => 1)->items->[0]->is_accessible;
        };

        subtest 'Select with is_used' => sub {
            my $db_obj = Direct::Test::DBObjects->new()->with_adgroup('text');

            my $ret_cond_id = $db_obj->create_ret_cond()->id;
            ok ! _get_by(id => $ret_cond_id, with_is_used => 1)->items->[0]->is_used;
            $db_obj->create_ret_bid({ret_cond_id => $ret_cond_id});
            ok _get_by(id => $ret_cond_id, with_is_used => 1)->items->[0]->is_used;

            # TODO: used in multiplier
        };

        subtest 'items_by' => sub {
            my $logic = _get_by(client_id => $db_obj->user->client_id);

            ok keys(%{$logic->items_by}) == @ret_conds;
            ok keys(%{$logic->items_by('id')}) == @ret_conds;
            ok keys(%{$logic->items_by('client_id')}) == 1;
        };
    };

    subtest 'Create retargeting conditions' => sub {
        my $db_obj = Direct::Test::DBObjects->new()->with_user();

        my @ret_conds;
        push @ret_conds, $db_obj->new_ret_cond_model() for 0..2;

        my $logic = Direct::RetargetingConditions->new(\@ret_conds);
        ok_func_is_called 'Direct::RetargetingConditions::prepare_create' => sub { $logic->create() };

        cmp_model_with $_, $db_obj->get_ret_cond($_->id), exclude => [qw/last_change/] for @ret_conds;
    };

    subtest 'Update retargeting conditions' => sub {
        my $db_obj = Direct::Test::DBObjects->new()->with_user();

        my $ret_cond = $db_obj->create_ret_cond();

        $ret_cond->merge_with($db_obj->new_ret_cond_model(), exclude => ['id']);

        my $logic = Direct::RetargetingConditions->new([$ret_cond]);
        ok_func_is_called 'Direct::RetargetingConditions::prepare_update' => sub { $logic->update() };

        ok !$ret_cond->is_changed;
        cmp_model_with $ret_cond, $db_obj->get_ret_cond($ret_cond->id), exclude => [qw/last_change old/];
    };

    subtest 'Delete retargeting conditions' => sub {
        my $db_obj = Direct::Test::DBObjects->new()->with_user();

        my $ret_cond = $db_obj->create_ret_cond();

        my $logic = Direct::RetargetingConditions->new([$ret_cond]);
        ok_func_is_called 'Direct::RetargetingConditions::prepare_delete' => sub { $logic->delete() };

        ok !defined $db_obj->get_ret_cond($ret_cond->id);
        ok $db_obj->get_ret_cond($ret_cond->id, with_deleted => 1)->is_deleted;
    };
};

done_testing;
