#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;
use Direct::Test::DBObjects qw/ok_func_is_called/;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBShards;
use Scalar::Util qw/blessed/;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::DynamicCondition');
    use_ok('Direct::Model::DynamicCondition::Rule');
    use_ok('Direct::Model::AdGroupDynamic');
    use_ok('Direct::Model::AdGroupDynamic::Manager');

    use_ok('Direct::DynamicConditions');
}

sub sandbox($&) { my ($dyn_cond, $code) = @_; local $_ = $dyn_cond->clone; $code->(); }

sub _rule { Direct::Model::DynamicCondition::Rule->new(@_) }
sub newDynamicCondition {
    my %hash = @_;
    $hash{condition} = [map { _rule(%$_) } @{$hash{condition}}] if exists $hash{condition};
    Direct::Model::DynamicCondition->new(%hash);
}

subtest 'Prepare to create dynamic conditions' => sub {
    my $adgroup = Direct::Model::AdGroupDynamic->new(id => 1);
    my $dyn_cond = newDynamicCondition(
        adgroup_id => $adgroup->id, adgroup => $adgroup, price => '0.1', price_context => '0.1',
        condition_name => "cond1", condition => [{type => 'any'}],
    );

    for my $g_status_moderate (qw/No Yes/) {
    for my $g_status_bl_generated(qw/No Yes Processing/) {
        $adgroup->status_moderate($g_status_moderate);
        $adgroup->status_bl_generated($g_status_bl_generated);

        # Добавление активного/выключенного условия нацеливания
        for my $is_suspended (0, 1) {
            sandbox $dyn_cond => sub {
                $_->is_suspended($is_suspended);
                Direct::DynamicConditions->new([$_])->prepare_create();
                is_deeply($_->get_state_hash, {
                    changes => {is_suspended => 1, _opts => 1},
                    flags => {
                        update_adgroup_last_change => 1,
                        ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1, bs_sync_banners => 1, freeze_autobudget_alert => 1) : ()),
                        (!$is_suspended && $g_status_bl_generated eq 'No' ? (set_adgroup_bl_status => 'Processing') : ()),
                    },
                });
            };
        }
    }}
};

subtest 'Prepare to update dynamic conditions' => sub {
    my $adgroup = Direct::Model::AdGroupDynamic->new(id => 1);
    my $dyn_cond = newDynamicCondition(
        id => 1, adgroup_id => $adgroup->id, adgroup => $adgroup, price => '0.1', price_context => '0.1',
        condition_name => "cond2", condition => [{type => 'any'}], is_suspended => 0, autobudget_priority => 0,
    );

    for my $g_status_moderate (qw/No Yes/) {
    for my $g_status_bl_generated(qw/No Yes Processing/) {
        $adgroup->status_moderate($g_status_moderate);
        $adgroup->status_bl_generated($g_status_bl_generated);

        # Изменение имени
        sandbox $dyn_cond => sub {
            $_->condition_name('cond2 (new)');
            Direct::DynamicConditions->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {condition_name => 1},
                flags => {update_adgroup_last_change => 1},
            });
        };

        # Изменение is_suspended
        for my $is_suspended (0, 1) {
            sandbox $dyn_cond => sub {
                $_->is_suspended(!$is_suspended);
                $_->reset_state();
                $_->is_suspended($is_suspended);
                Direct::DynamicConditions->new([$_])->prepare_update();
                is_deeply($_->get_state_hash, {
                    changes => {is_suspended => 1, _opts => 1},
                    flags => {
                        update_adgroup_last_change => 1,
                        ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1, bs_sync_banners => 1) : ()),
                        (!$is_suspended && $g_status_bl_generated eq 'No' ? (set_adgroup_bl_status => 'Processing') : ()),
                    },
                });
            };
        }

        # Изменение целей в условии нацеливания
        sandbox $dyn_cond => sub {
            $_->condition([_rule(type => "title", kind => "exact", value => ["телефоны"])]);
            Direct::DynamicConditions->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {_condition_hash => 1, _condition_json => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1, bs_sync_banners => 1) : ()),
                    ($g_status_bl_generated eq 'No' ? (set_adgroup_bl_status => 'Processing') : ()),
                },
            });
        };

        # Изменение ставок
        sandbox $dyn_cond => sub {
            $_->price(0.2);
            $_->price_context(0.2);
            Direct::DynamicConditions->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {price => 1, price_context => 1, status_bs_synced => 1},
                flags => {},
            });
        };

        # Изменение приоритета автобюджета
        sandbox $dyn_cond => sub {
            $_->autobudget_priority(1);
            Direct::DynamicConditions->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {autobudget_priority => 1, status_bs_synced => 1},
                flags => {},
            });
        };
    }}
};

subtest 'Prepare to delete dynamic conditions' => sub {
    my $adgroup = Direct::Model::AdGroupDynamic->new(id => 1);
    my $dyn_cond = newDynamicCondition(
        id => 1, adgroup_id => $adgroup->id, adgroup => $adgroup, price => '0.1', price_context => '0.1',
        condition_name => "cond3", condition => [{type => 'any'}], is_suspended => 0,
    );

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        # Удаление условия нацеливания
        sandbox $dyn_cond => sub {
            Direct::DynamicConditions->new([$_])->prepare_delete();
            is_deeply($_->get_state_hash, {
                changes => {},
                flags => {
                    ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1, bs_sync_banners => 1) : ()),
                    update_adgroup_last_change => 1,
                },
            });
        };
    }
};

subtest 'General' => sub {
    ok(%{Direct::DynamicConditions->WEB_FIELD_NAMES});
};

subtest 'Tests with database access' => sub {
    init_test_dataset(&get_test_dataset);

    local *get_dynamic_conditions = sub { Direct::DynamicConditions->get_by(adgroup_id => [$_[0]]) };

    local *create_adgroup = sub {
        my %overwrite = @_;
        state $num = 0; $num++;
        my $client_id = $overwrite{client_id} || 1;
        my $adgroup = Direct::Model::AdGroupDynamic->new(
            id => get_new_id('pid', ClientID => $client_id), client_id => $client_id,
            campaign_id => 1, adgroup_name => "dyn_adgroup ($num)", main_domain => "ya.ru", status_bs_synced => 'Yes',
            status_moderate => 'New', status_post_moderate => 'New', %overwrite,
        );
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->create();

        return $adgroup;
    };

    # Создадим группу, куда будем добавлять условия нацеливания
    my $adgroup = create_adgroup();

    subtest 'Create dynamic conditions' => sub {
        my $dyn_cond = newDynamicCondition(
            adgroup_id => $adgroup->id, adgroup => $adgroup, price => '1.95',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        my $dyn_cond2 = newDynamicCondition(
            adgroup_id => $adgroup->id, adgroup => $adgroup, price_context => '1.11',
            condition_name => "Плееры", condition => [{type => "title", kind => "exact", value => ["плееры"]}],
        );

        my $dyn_conds_obj = Direct::DynamicConditions->new([$dyn_cond, $dyn_cond2]); $dyn_conds_obj->create();

        compare_model_with($dyn_cond, get_dynamic_conditions($adgroup->id)->items_by->{$dyn_cond->id});
        compare_model_with($dyn_cond2, get_dynamic_conditions($adgroup->id)->items_by->{$dyn_cond2->id});
        is_deeply($dyn_conds_obj->data->{log_price}, [
            {cid => 1, pid => $adgroup->id, id => $dyn_cond->id, type => 'dyn_condition_create', price => $dyn_cond->price, price_ctx => 0, currency => 'RUB'},
            {cid => 1, pid => $adgroup->id, id => $dyn_cond2->id, type => 'dyn_condition_create', price => 0, price_ctx => $dyn_cond2->price_context, currency => 'RUB'},
        ]);
    };

    subtest 'Dynamic conditions on moderated adgroup' => sub {
        my $adgroup = create_adgroup(status_moderate => 'Yes', status_bl_generated => 'Yes', campaign_id => 2);
        my $dyn_cond = newDynamicCondition(adgroup_id => $adgroup->id, adgroup => $adgroup, price => '1.11',
            condition_name => 'test', condition => [{ type => 'any' }],
            
        );
        my $dyn_conds_obj = Direct::DynamicConditions->new([$dyn_cond]);
        # убеждаемся что при добавлении условия замораживается автобюджетный алерт
        ok_func_is_called 'AutobudgetAlerts::update_on_new_phrases_add' => sub { $dyn_conds_obj->create() };
    };

    subtest 'Select dynamic conditions' => sub {
        ok(@{Direct::DynamicConditions->get_by(adgroup_id => undef)->items} == 0);
        ok(@{Direct::DynamicConditions->get_by(adgroup_id => [])->items} == 0);

        ok(@{Direct::DynamicConditions->get_by(campaign_id => 1)->items} == 2);
        dies_ok { Direct::DynamicConditions->get_by(dyn_cond_id => 1) };
        lives_ok { Direct::DynamicConditions->get_by(dyn_cond_id => 1, ClientID => 1) };

        # filter
        ok(@{Direct::DynamicConditions->get_by(campaign_id => 1, filter => {condition_name__rlike => 'Пле'})->items} == 1);

        # limit + offset
        ok(@{Direct::DynamicConditions->get_by(campaign_id => 1, limit => 1)->items} == 1);
        ok(@{Direct::DynamicConditions->get_by(campaign_id => 1, limit => 1, offset => 2)->items} == 0);

        subtest 'items_by' => sub {
            my ($dyn_cond, $dyn_cond2) = @{get_dynamic_conditions($adgroup->id)->items};
            ok(defined get_dynamic_conditions($adgroup->id)->items_by->{$dyn_cond->id});
            ok(defined get_dynamic_conditions($adgroup->id)->items_by('id')->{$dyn_cond2->id});
            ok(@{get_dynamic_conditions($adgroup->id)->items_by('adgroup_id')->{$adgroup->id}} == 2);
            ok(@{get_dynamic_conditions($adgroup->id)->items_by('gid')->{$adgroup->id}} == 2);
            dies_ok { get_dynamic_conditions($adgroup->id)->items_by('cid') };
        };
    };

    subtest 'Update dynamic conditions' => sub {
        my $dyn_cond = get_dynamic_conditions($adgroup->id)->items->[0];
        $dyn_cond->adgroup($adgroup);
        $dyn_cond->condition_name("Телефоны");
        $dyn_cond->condition([_rule(type => "title", kind => "exact", value => ["телефоны"])]);
        $dyn_cond->price_context('1.55');

        my $dyn_conds_obj = Direct::DynamicConditions->new([$dyn_cond]); $dyn_conds_obj->update();

        ok(!$dyn_cond->is_changed);
        compare_model_with($dyn_cond, get_dynamic_conditions($adgroup->id)->items_by->{$dyn_cond->id});
        is_deeply($dyn_conds_obj->data->{log_price}, [
            {cid => 1, pid => $adgroup->id, id => $dyn_cond->id, type => 'dyn_condition_update', price => $dyn_cond->price, price_ctx => $dyn_cond->price_context, currency => 'RUB'},
        ]);

        # Update price-only
        $dyn_cond->price('3.67');
        $dyn_conds_obj = Direct::DynamicConditions->new([$dyn_cond]); $dyn_conds_obj->update();
        is_deeply($dyn_conds_obj->data->{log_price}, [
            {cid => 1, pid => $adgroup->id, id => $dyn_cond->id, type => 'dyn_condition_update', price => $dyn_cond->price, price_ctx => $dyn_cond->price_context, currency => 'RUB'},
        ]);

        # Without price-updating
        $dyn_cond->autobudget_priority(5);
        $dyn_conds_obj = Direct::DynamicConditions->new([$dyn_cond]); $dyn_conds_obj->update();
        is_deeply($dyn_conds_obj->data, {});
    };

    subtest 'Copy dynamic conditions' => sub {
        my $from_clientid = 1;
        my $to_clientid = 2;
        # Создаём группы в разных шардах
        my $src_adgroup = create_adgroup(campaign_id => 1);
        my $dst_adgroup = create_adgroup(campaign_id => 2, client_id => $to_clientid);

        # Создание условий нацеливания
        my $dyn_cond1 = newDynamicCondition(
            adgroup_id => $src_adgroup->id, adgroup => $src_adgroup, price => '1.95',
            condition_name => "Все страницы", condition => [{type => "any"}], status_bs_synced => 'Yes',
        );
        my $dyn_cond2 = newDynamicCondition(
            adgroup_id => $src_adgroup->id, adgroup => $src_adgroup, price_context => '1.11',
            condition_name => "Телефоны", condition => [{type => "title", kind => "exact", value => ["телефоны"]}], status_bs_synced => 'Yes',
        );
        Direct::DynamicConditions->new([$dyn_cond1, $dyn_cond2])->create();

        # Копирование условий нацеливания из src_adgroup в dst_adgroup
        my $price_coef = 1.17;
        my ($id_src2dst, $copy_obj);
        ($id_src2dst, undef, $copy_obj) = Direct::DynamicConditions->copy(
            [adgroup_id => $src_adgroup->id],
            $from_clientid, $to_clientid,
            sub {
                $_ = shift;
                $_->adgroup_id($dst_adgroup->id);
                $_->price(0 + sprintf("%.2f", $_->price * $price_coef));
                $_->price_context(0 + sprintf("%.2f", $_->price_context * $price_coef));
                $_->status_bs_synced('No');
                return $_;
            }, primary_key => 'dyn_id'); 

        # Проверка результатов
        my $src_dyn_conds_obj = get_dynamic_conditions($src_adgroup->id);
        my $dst_dyn_conds_obj = get_dynamic_conditions($dst_adgroup->id);

        ok(@{$src_dyn_conds_obj->items} == 2);
        ok(@{$dst_dyn_conds_obj->items} == 2);

        for my $src_dyn_cond (@{$src_dyn_conds_obj->items}) {
            my $dst_dyn_cond = $dst_dyn_conds_obj->items_by->{ $id_src2dst->{$src_dyn_cond->id} };
            ok($src_dyn_cond->id != $dst_dyn_cond->id);
            ok($src_dyn_cond->condition_id != $dst_dyn_cond->condition_id);
            ok($src_dyn_cond->_condition_hash eq $dst_dyn_cond->_condition_hash);
            ok($src_dyn_cond->_condition_json eq $dst_dyn_cond->_condition_json);
            ok(sprintf("%.2f", $dst_dyn_cond->price) == sprintf("%.2f", $src_dyn_cond->price * $price_coef));
            ok(sprintf("%.2f", $dst_dyn_cond->price_context) == sprintf("%.2f", $src_dyn_cond->price_context * $price_coef));
            ok($dst_dyn_cond->status_bs_synced eq 'No');
        }

        ($dyn_cond1, $dyn_cond2) = @{$dst_dyn_conds_obj->items};
        is_deeply($copy_obj->data->{log_price}, [
            {cid => 2, pid => $dst_adgroup->id, id => $dyn_cond1->id, type => 'dyn_condition_copy', price => (0 + $dyn_cond1->price), price_ctx => 0, currency => 'RUB'},
            {cid => 2, pid => $dst_adgroup->id, id => $dyn_cond2->id, type => 'dyn_condition_copy', price => 0, price_ctx => (0 + $dyn_cond2->price_context), currency => 'RUB'},
        ]);
    };

    subtest 'Delete dynamic conditions' => sub {

        my $dyn_cond = get_dynamic_conditions($adgroup->id)->items->[0];
        $dyn_cond->adgroup($adgroup);

        Direct::DynamicConditions->new([$dyn_cond])->delete();

        is_deeply(
            Direct::DynamicConditions->get_by(dyn_cond_id => $dyn_cond->dyn_cond_id, ClientID => $adgroup->client_id)->items,
            [],
            'No condition selected'
        );

        my $dyn_cond2 = Direct::DynamicConditions->get_by(
            dyn_cond_id => $dyn_cond->dyn_cond_id, ClientID => $adgroup->client_id, with_deleted => 1,
        )->items_by('dyn_cond_id')->{$dyn_cond->dyn_cond_id};
        ok(!$dyn_cond2->has_id);
        ok(!$dyn_cond2->has_price);
        ok(!$dyn_cond2->has_price_context);
        ok(!$dyn_cond2->has_is_suspended);
        is($dyn_cond2->dyn_cond_id, $dyn_cond->dyn_cond_id);
        is_deeply($dyn_cond2->condition, $dyn_cond->condition);
    };

    subtest 'Select deleted & non-deleted dynamic conditions' => sub {
        my $adgroup2 = create_adgroup();

        my $dyn_cond2_1 = newDynamicCondition(
            adgroup_id => $adgroup2->id, adgroup => $adgroup2, price => '1.95',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        my $dyn_cond2_2 = newDynamicCondition(
            adgroup_id => $adgroup2->id, adgroup => $adgroup2, price_context => '1.11',
            condition_name => "Телефоны", condition => [{type => "title", kind => "exact", value => ["телефоны"]}],
        );

        Direct::DynamicConditions->new([$dyn_cond2_1, $dyn_cond2_2])->create();
        Direct::DynamicConditions->new([$dyn_cond2_1])->delete();

        my $dyn_conds = Direct::DynamicConditions->get_by(adgroup_id => $adgroup2->id, with_deleted => 1)->items;

        ok(@$dyn_conds == 2);
        ok(scalar(grep { $_->has_id } @$dyn_conds) == 1);
        ok(scalar(grep { !$_->has_id } @$dyn_conds) == 1);
    };
};

sub compare_model_with {
    my ($model1, $model2) = @_;
    my @attrs = grep { $_->has_value($model1) && !blessed($_->get_value($model1)) } $model1->get_public_attributes;
    is_deeply({map { $_->name => $_->get_value($model1) } @attrs}, {map { $_->name => $_->get_value($model2) } @attrs});
}

sub get_test_dataset { +{
    shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}, {ClientID => 2, shard => 2}]},
    shard_inc_cid   => {original_db => PPCDICT, rows => [{cid => 1, ClientID => 1}, {cid => 2, ClientID => 2}]},
    (map { $_ => {original_db => PPCDICT} } qw/
        shard_inc_pid inc_dyn_id inc_dyn_cond_id shard_inc_bid domains_dict
    /),

    (map { $_ => {original_db => PPC(shard => 'all')} } qw/
        bids_dynamic dynamic_conditions phrases adgroups_dynamic banners domains autobudget_alerts feeds
        minus_words group_params users filter_domain banner_display_hrefs
        camp_secondary_options
    /),
    campaigns       => {original_db => PPC(shard => 'all'), rows => {
        1 => [{cid => 1, uid => 1, type => 'dynamic', currency => 'RUB'}],
        2 => [{cid => 2, uid => 2, type => 'dynamic', currency => 'RUB'}],
    }},
} }

done_testing;
