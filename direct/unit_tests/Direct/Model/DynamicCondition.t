#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Yandex::DBShards;

BEGIN {
    use_ok('Direct::Model::DynamicCondition');
    use_ok('Direct::Model::DynamicCondition::Rule');
    use_ok('Direct::Model::DynamicCondition::Manager');
    use_ok('Direct::Model::AdGroupDynamic');
    use_ok('Direct::Model::AdGroupDynamic::Manager');
    use_ok('Direct::Model::BannerDynamic');
    use_ok('Direct::Model::BannerDynamic::Manager');

    use_ok('Direct::DynamicConditions');
}

sub sandbox($&) { my ($model, $code) = @_; local $_ = $model->clone; $code->(); }

sub _rule { Direct::Model::DynamicCondition::Rule->new(@_) }
sub newDynamicCondition {
    my %hash = @_;
    $hash{condition} = [map { _rule(%$_) } @{$hash{condition}}] if exists $hash{condition};
    return Direct::Model::DynamicCondition->new(%hash);
}

subtest 'DynamicCondition Model' => sub {
    lives_ok { Direct::Model::DynamicCondition->new() };
    lives_ok { Direct::Model::DynamicCondition->new({}) };
    dies_ok { Direct::Model::DynamicCondition->new("unknown" => "args") };
    dies_ok { Direct::Model::DynamicCondition->new({"unknown" => "args"}) };

    # AdGroup constraint
    lives_ok { Direct::Model::DynamicCondition->new(adgroup => Direct::Model::AdGroupDynamic->new()) };
    dies_ok { Direct::Model::DynamicCondition->new(adgroup => bless({}, 'Direct::Model::AdGroup')) };

    # Condition rules: parse json
    for (undef, '', "invalid json") {
        dies_ok { Direct::Model::DynamicCondition->new(_condition_json => $_) };
    }
    for (
        '[{"type":"any"}]',
        '[{"type":"title", "kind":"exact", "value":["ok"]}]',
        '[{"type":"domain", "kind":"equals", "value":["ya.ru"]}]',
    ) {
        lives_ok { Direct::Model::DynamicCondition->new(_condition_json => $_) };
    }
    for (
        '[{"type": "invalid"}]',
        '[{"type": "title", kind: "invalid"}]',
        '[{"type": "title", "kind": "exact", "value":{"invalid"}}]',
        '[{"type": "title", "kind": "exact", "value":[]}]',
    ) {
        dies_ok { Direct::Model::DynamicCondition->new(_condition_json => $_) };
    }
    is_deeply(
        Direct::Model::DynamicCondition->new(_condition_json => '[{"type":"any"}]')->to_hash,
        {condition => [{type => 'any'}], available => 0},
    );
    is_deeply(
        Direct::Model::DynamicCondition->new(_condition_json => '[{"type": "title", "kind": "exact", "value": ["телефоны"]}]')->to_hash,
        {condition => [{type => "title", kind => "exact", value => ["телефоны"]}], available => 0},
    );
    ok(Direct::Model::DynamicCondition->new(_condition_json => '[{"type":"any"}]')->_condition_hash > 0);
    is(Direct::Model::DynamicCondition->new(_condition_json => '[{"type":"any"}]', _condition_hash => 123)->_condition_hash, 123);

    # Condition rules: serialization
    subtest 'Serialize condition' => sub {
        my $dyn_cond = newDynamicCondition(condition => [
            {type => "title", kind => "equals", value => ["айфоны6"]},
            {type => "content", kind => "not_exact", value => ["бесплатно"]},
        ]);

        is(
            $dyn_cond->_condition_json,
            '[{"kind":"equals","type":"title","value":["айфоны6"]},{"kind":"not_exact","type":"content","value":["бесплатно"]}]',
        );
        is($dyn_cond->_condition_hash, $dyn_cond->_calc_condition_hash);

        sandbox $dyn_cond => sub {
            my $cond_tmp = $_->condition;
            $cond_tmp->[0]->value->[0] = "айфоны6s";
            $_->condition($cond_tmp);
            is(
                $_->_condition_json,
                '[{"kind":"equals","type":"title","value":["айфоны6s"]},{"kind":"not_exact","type":"content","value":["бесплатно"]}]',
            );
            ok($_->is_condition_changed && $_->_is_condition_hash_changed);
            ok($_->_condition_hash != $dyn_cond->_condition_hash);
        };
    };

    # Condition uhash
    subtest 'Condition uhash' => sub {
        my @rules = (
            {type => "title", kind => "equals", value => ["айфоны6"]},
            {type => "content", kind => "not_exact", value => ["бесплатно"]},
        );
        my $dyn_cond = newDynamicCondition(condition => [@rules]);
        my $dyn_cond2 = newDynamicCondition(condition => [reverse @rules]);
        isnt($dyn_cond->_condition_hash, $dyn_cond2->_condition_hash);
        is($dyn_cond->get_condition_uhash, $dyn_cond2->get_condition_uhash);
    };

    # is_suspended
    ok(Direct::Model::DynamicCondition->new(_opts => 'suspended')->is_suspended);
    ok(!Direct::Model::DynamicCondition->new(_opts => '')->is_suspended);
    is(Direct::Model::DynamicCondition->new(is_suspended => 1)->_opts, 'suspended');
    sandbox newDynamicCondition(_opts => 'suspended') => sub {
        $_->is_suspended(0);
        is($_->_opts, '');
    };

    # is_deleted
    ok(!Direct::Model::DynamicCondition->new(id => 1, condition_id => 1)->is_deleted);
    ok(Direct::Model::DynamicCondition->new(condition_id => 1)->is_deleted);
};

subtest 'DynamicCondition Manager' => sub {
    init_test_dataset(&get_test_dataset);

    local *get_dynamic_conditions = sub { Direct::DynamicConditions->get_by(adgroup_id => [$_[0]])->items };

    # Создадим группу, куда будем добавлять условия нацеливания
    my $adgroup = Direct::Model::AdGroupDynamic->new(
        id => get_new_id('pid', ClientID => 1), client_id => 1,
        campaign_id => 1, adgroup_name => "dyn_adgroup1", main_domain => "ya.ru",
        status_bs_synced => 'Yes', status_moderate => 'New', status_post_moderate => 'New',
    );
    Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup])->create();

    # Создание условия нацеливания
    subtest 'Create dynamic condition' => sub {
        my $dyn_cond = newDynamicCondition(
            id => get_new_id('dyn_id'), adgroup_id => $adgroup->id, price => '1.95',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        $dyn_cond->price_context('1.33');

        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->create();

        compare_model_with($dyn_cond, get_dynamic_conditions($adgroup->id)->[0]);
        is($dyn_cond->is_changed, 0, 'Test resetting model state');
    };

    # Обновление данных без затрагивания `ppc.dynamic_conditions`
    subtest 'Simple update dynamic condition' => sub {
        my $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];
        my $orig_condition_id = $dyn_cond->condition_id;

        $dyn_cond->price('1.77');
        $dyn_cond->autobudget_priority(5);

        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();

        my $dyn_cond2 = get_dynamic_conditions($adgroup->id)->[0];
        compare_model_with($dyn_cond, $dyn_cond2);
        is($orig_condition_id, $dyn_cond2->condition_id);
    };

    # Обновление условия нацеливания и проверка иммутабельности
    subtest 'Update dynamic condition' => sub {
        my $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];
        my $orig_condition_id = $dyn_cond->condition_id;

        $dyn_cond->condition_name("Телефоны");
        $dyn_cond->condition([_rule(type => "title", kind => "exact", value => ["телефоны"])]);
        $dyn_cond->price('0.97');
        $dyn_cond->price_context('0.71');
        $dyn_cond->autobudget_priority(3);
        $dyn_cond->is_suspended(1);

        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();

        my $dyn_cond2 = get_dynamic_conditions($adgroup->id)->[0];
        compare_model_with($dyn_cond, $dyn_cond2);
        isnt($orig_condition_id, $dyn_cond2->condition_id);

        # Вернём предыдущее условие, но с новым именем
        $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];
        $dyn_cond->condition([_rule(type => "any")]);
        $dyn_cond->condition_name("Все страницы (2)");
        $dyn_cond->is_suspended(!!0);

        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();

        my $dyn_cond3 = get_dynamic_conditions($adgroup->id)->[0];
        compare_model_with($dyn_cond, $dyn_cond3);
        is($orig_condition_id, $dyn_cond3->condition_id);

        is($dyn_cond->is_changed, 0, 'Test resetting model state');
    };

    # Удаление условия нацеливания
    subtest 'Delete dynamic condition and reuse after' => sub {
        # Подготовка данных
        my $adgroup2 = Direct::Model::AdGroupDynamic->new(
            id => get_new_id('pid', ClientID => 1), campaign_id => 1, adgroup_name => "dyn_adgroup2", main_domain => "ya.ru"
        );
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup2])->create();

        my $dyn_cond2 = newDynamicCondition(
            id => get_new_id('dyn_id'), adgroup_id => $adgroup2->id, price => '0.01',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond2])->create();

        # Удаление
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond2])->delete();

        # Проверка
        is(scalar @{get_dynamic_conditions($adgroup2->id)}, 0, 'no record in `bids_dynamic` table');
        is(
            $dyn_cond2->condition_id,
            get_one_field_sql(PPC(pid => $adgroup2->id), ["SELECT dyn_cond_id FROM dynamic_conditions", where => {pid => SHARD_IDS}]),
            'keep record in` dynamic_condition` table',
        );

        # Переиспользование json условия
        my $dyn_cond3 = newDynamicCondition(
            id => get_new_id('dyn_id'), adgroup_id => $adgroup2->id, price => '1.31',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond3])->create();
        is($dyn_cond3->condition_id, $dyn_cond2->condition_id);
    };

    local *get_adgroup_data = sub {
        get_one_line_sql(PPC(pid => $_[0]), [q{
            SELECT g.statusBsSynced, g.LastChange, g.statusModerate, g.statusPostModerate, gd.statusBlGenerated
            FROM phrases g JOIN adgroups_dynamic gd ON (gd.pid = g.pid)
        }, where => {'g.pid' => SHARD_IDS}])
    };

    local *get_banner_data = sub {
        get_one_line_sql(PPC(bid => $_[0]), ['SELECT statusBsSynced, LastChange, statusModerate, statusPostModerate, phoneflag FROM banners', where => {bid => SHARD_IDS}])
    };

    # Флаг: bs_sync_adgroup
    subtest 'Flag: bs_sync_adgroup' => sub {
        my $adgroup4 = Direct::Model::AdGroupDynamic->new(
            id => get_new_id('pid', ClientID => 1), campaign_id => 1, adgroup_name => "dyn_adgroup4", main_domain => "ya.ru", status_bs_synced => 'Yes'
        );
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup4])->create();

        my $dyn_cond4 = newDynamicCondition(
            id => get_new_id('dyn_id'), adgroup_id => $adgroup4->id, price => '0.01',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond4])->create();

        my $adgroup_last_change_orig = get_adgroup_data($adgroup4->id)->{LastChange};

        $dyn_cond4->do_bs_sync_adgroup(1);

        is(get_adgroup_data($adgroup4->id)->{statusBsSynced}, 'Yes');
        sleep(1); # Чтобы перещелкнул LastChange
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond4])->update();
        is_deeply([@{get_adgroup_data($adgroup4->id)}{qw/statusBsSynced LastChange/}], ['No', $adgroup_last_change_orig]);
    };

    # Флаг: bs_sync_banners
    subtest 'Flag: bs_sync_banners' => sub {
        # Добавим несколько баннеров в группу
        my $banner_tmpl = sub {(
            id => get_new_id('bid', ClientID => $adgroup->client_id), adgroup_id => $adgroup->id, campaign_id => $adgroup->campaign_id,
            status_bs_synced => 'Yes', last_change => get_adgroup_data($adgroup->id)->{LastChange},
        )};
        my $banner1 = Direct::Model::BannerDynamic->new(&$banner_tmpl, body => 'banner (1) text');
        my $banner2 = Direct::Model::BannerDynamic->new(&$banner_tmpl, body => 'banner (2) text');
        Direct::Model::BannerDynamic::Manager->new(items => [$banner1, $banner2])->create();

        my $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];

        $dyn_cond->do_bs_sync_banners(1);
        is(get_banner_data($_->id)->{statusBsSynced}, 'Yes') for ($banner1, $banner2);
        sleep(1); # Чтобы перещелкнул LastChange
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        is_deeply([@{get_banner_data($_->id)}{qw/statusBsSynced LastChange/}], ['No', $_->last_change]) for ($banner1, $banner2);
    };

    # Флаг: update_adgroup_last_change
    subtest 'Flag: update_adgroup_last_change' => sub {
        my $adgroup_last_change_orig = get_adgroup_data($adgroup->id)->{LastChange};
        my $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];

        $dyn_cond->do_update_adgroup_last_change(0);
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        is(get_adgroup_data($adgroup->id)->{LastChange}, $adgroup_last_change_orig, 'force keep adgroup last_change');

        $dyn_cond->do_update_adgroup_last_change(1);
        sleep(1);
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        isnt(get_adgroup_data($adgroup->id)->{LastChange}, $adgroup_last_change_orig, 'force update adgroup last_change');

        # update_adgroup_last_change(1) + bs_sync_adgroup
        do_update_table(PPC(pid => $adgroup->id), 'phrases', {statusBsSynced => 'Yes'}, where => {pid => SHARD_IDS});
        $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];
        $adgroup_last_change_orig = get_adgroup_data($adgroup->id)->{LastChange};
        $dyn_cond->do_bs_sync_adgroup(1);
        $dyn_cond->do_update_adgroup_last_change(1);
        sleep(1);
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        isnt(get_adgroup_data($adgroup->id)->{LastChange}, $adgroup_last_change_orig);
        is(get_adgroup_data($adgroup->id)->{statusBsSynced}, 'No');
    };

    # Флаг: moderate_adgroup
    subtest 'Flag: moderate_adgroup' => sub {
        my $adgroup4 = Direct::Model::AdGroupDynamic->new(
            id => get_new_id('pid', ClientID => 1), campaign_id => 1, adgroup_name => "dyn_adgroup4", main_domain => "ya.ru",
            status_moderate => 'New', status_post_moderate => 'New',            
        );
        Direct::Model::AdGroupDynamic::Manager->new(items => [$adgroup4])->create();

        my $dyn_cond = newDynamicCondition(
            id => get_new_id('dyn_id'), adgroup_id => $adgroup4->id, price => '0.01',
            condition_name => "Все страницы", condition => [{type => "any"}],
        );
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->create();

        # my $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];

        is_deeply([@{get_adgroup_data($adgroup4->id)}{qw/statusModerate statusPostModerate/}], [qw/New New/]);
        $dyn_cond->do_moderate_adgroup(1);
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        is_deeply([@{get_adgroup_data($adgroup4->id)}{qw/statusModerate statusPostModerate/}], [qw/Ready No/]);

        # Сохранение statusPostModerate = 'Rejected'
        do_update_table(PPC(pid => $adgroup4->id), 'phrases', {statusModerate => 'No', statusPostModerate => 'Rejected'}, where => {pid => SHARD_IDS});
        is_deeply([@{get_adgroup_data($adgroup4->id)}{qw/statusModerate statusPostModerate/}], [qw/No Rejected/]);
        $dyn_cond->do_moderate_adgroup(1);
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        is_deeply([@{get_adgroup_data($adgroup4->id)}{qw/statusModerate statusPostModerate/}], [qw/Ready Rejected/]);
    };

    # Флаг: set_adgroup_bl_status
    subtest 'Flag: set_adgroup_bl_status' => sub {
        my $dyn_cond = get_dynamic_conditions($adgroup->id)->[0];

        is_deeply(get_adgroup_data($adgroup->id)->{statusBlGenerated}, 'No');
        $dyn_cond->do_set_adgroup_bl_status('Processing');
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        is_deeply(get_adgroup_data($adgroup->id)->{statusBlGenerated}, 'Processing');

        $dyn_cond->do_set_adgroup_bl_status('Yes');
        Direct::Model::DynamicCondition::Manager->new(items => [$dyn_cond])->update();
        is_deeply(get_adgroup_data($adgroup->id)->{statusBlGenerated}, 'Yes');
    };

};

sub compare_model_with {
    my ($model1, $model2) = @_;
    my @attrs = grep { $_->has_value($model1) } $model1->get_public_attributes;
    is_deeply({map { $_->name => $_->get_value($model1) } @attrs}, {map { $_->name => $_->get_value($model2) } @attrs});
}

sub get_test_dataset { +{
    shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}]},
    shard_inc_cid   => {original_db => PPCDICT, rows => [{cid => 1, ClientID => 1}]},
    (map { $_ => {original_db => PPCDICT} } qw/
        shard_inc_pid inc_dyn_id inc_dyn_cond_id shard_inc_bid domains_dict
    /),

    (map { $_ => {original_db => PPC(shard => 'all')} } qw/
        bids_dynamic dynamic_conditions phrases adgroups_dynamic banners domains feeds banners_minus_geo banners_to_fill_language_queue
    /),
    campaigns       => {original_db => PPC(shard => 'all'), rows => {
        1 => [{cid => 1, uid => 1, type => 'dynamic', currency => 'RUB'}],
    }},
} }

done_testing;
