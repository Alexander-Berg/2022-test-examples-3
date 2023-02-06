#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use List::MoreUtils qw/any/;

use Direct::Test::DBObjects qw/sandbox ok_func_is_called cmp_model_with/;
use PrimitivesIds qw/get_clientid get_cid/;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::PerformanceFilter');
    use_ok('Direct::Model::PerformanceFilter::Rule');
    use_ok('Direct::Model::AdGroupPerformance');

    use_ok('Direct::PerformanceFilters');
}

sub mk_filter { Direct::Model::PerformanceFilter->new(@_, filter_type => 'performance') }
sub mk_rule { Direct::Model::PerformanceFilter::Rule->new(@_, filter_type => 'performance') }
sub last_change__dont_quote { shift->get_db_column_value(bids_performance => 'LastChange', extended => 1)->{val__dont_quote} }

subtest 'Prepare to create performance filters' => sub {
    my $adgroup = Direct::Model::AdGroupPerformance->new(id => 1);
    my $orig_perf_filter = mk_filter(
        adgroup_id => $adgroup->id, adgroup => $adgroup, filter_name => 'filter1', price_cpc => '0.1', price_cpa => '0.1',
        target_funnel => 'same_products', now_optimizing_by => 'CPC',
        condition => [mk_rule(field => 'categoryId', relation => '==', value => '1')],
        ret_cond_id => 600
    );

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        sandbox $orig_perf_filter => sub {
            Direct::PerformanceFilters->new([$_])->prepare_create();
            is_deeply($_->get_state_hash, {
                changes => {last_change => 1, status_bs_synced => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New'
                      ? (bs_sync_banners => 1, bs_sync_adgroup => 1, freeze_autobudget_alert => 1, set_adgroup_bl_status => 'Processing')
                      : ()),
                },
            });
            is $_->status_bs_synced, 'No';
            like last_change__dont_quote($_), qr/^now/i;
        };
    }
};

subtest 'Prepare to update performance filters' => sub {
    my $adgroup = Direct::Model::AdGroupPerformance->new(id => 1);
    my $orig_perf_filter = mk_filter(
        id => 1, adgroup_id => $adgroup->id, adgroup => $adgroup, filter_name => 'filter1', price_cpc => '0.1', price_cpa => '0.1',
        target_funnel => 'same_products', now_optimizing_by => 'CPC',
        condition => [mk_rule(field => 'categoryId', relation => '==', value => '1')],
        ret_cond_id => 600
    );

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        # Изменение имени
        sandbox $orig_perf_filter => sub {
            $_->filter_name('filter2');
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {filter_name => 1, last_change => 1},
                flags => {update_adgroup_last_change => 1},
            });
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение ставок: price_cpc/price_cpa
        sandbox $orig_perf_filter => sub {
            $_->price_cpc(0.25);
            $_->price_cpa(0.36);
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {price_cpc => 1, price_cpa => 1, last_change => 1, status_bs_synced => 1},
                flags => {},
            });
            is $_->status_bs_synced, 'No';
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение приоритета автобюджета
        sandbox $orig_perf_filter => sub {
            $_->autobudget_priority(5);
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {autobudget_priority => 1, last_change => 1, status_bs_synced => 1},
                flags => {},
            });
            is $_->status_bs_synced, 'No';
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение target_funnel
        sandbox $orig_perf_filter => sub {
            $_->target_funnel('product_page_visit');
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {target_funnel => 1, last_change => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_banners => 1, bs_sync_adgroup => 1) : ()),
                },
            });
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение now_optimizing_by
        sandbox $orig_perf_filter => sub {
            $_->now_optimizing_by('CPA');
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {now_optimizing_by => 1},
                flags => {},
            });
        };

        # Изменение is_suspended
        sandbox $orig_perf_filter => sub {
            $_->is_suspended(1);
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {is_suspended => 1, last_change => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_banners => 1, bs_sync_adgroup => 1) : ()),
                },
            });
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение is_deleted
        sandbox $orig_perf_filter => sub {
            $_->is_deleted(1);            
            Direct::PerformanceFilters->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {is_deleted => 1, last_change => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_banners => 1, bs_sync_adgroup => 1) : ()),
                },
            });
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение фильтра
        sandbox $orig_perf_filter => sub {
            $_->condition([mk_rule(field => 'price', relation => '>', value => '10')]);
            Direct::PerformanceFilters->new([$_])->prepare_update();

            # Changed filter
            is_deeply($_->get_state_hash, {
                changes => {_condition_json => 1, status_bs_synced => 1, last_change => 1},
                flags => {set_adgroup_bl_status => 'Processing', update_adgroup_last_change => 1, bs_sync_adgroup => 1, bs_sync_banners => 1},
            });
        };
    }
};

subtest 'Prepare to delete performance filters' => sub {
    my $adgroup = Direct::Model::AdGroupPerformance->new(id => 1);
    my $orig_perf_filter = mk_filter(
        id => 1, adgroup_id => $adgroup->id, adgroup => $adgroup, filter_name => 'filter1', price_cpc => '0.1', price_cpa => '0.1',
        target_funnel => 'same_products', now_optimizing_by => 'CPC',
        condition => [mk_rule(field => 'categoryId', relation => '==', value => '1')],
        ret_cond_id => undef,
    );

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        sandbox $orig_perf_filter => sub {
            $_->filter_name("Changed!");
            Direct::PerformanceFilters->new([$_])->prepare_delete();
            is_deeply($_->get_state_hash, {
                changes => {is_deleted => 1, last_change => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_banners => 1, bs_sync_adgroup => 1) : ()),
                },
            });
            like last_change__dont_quote($_), qr/^now/i;
        };
    }
};

subtest 'General' => sub {
    ok %{Direct::PerformanceFilters->WEB_FIELD_NAMES};
};

subtest 'Tests with database access' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj_c = Direct::Test::DBObjects->new()->with_campaign('performance');

    subtest 'Select performance filters' => sub {
        local *_get_by = sub { Direct::PerformanceFilters->get_by(@_) };

        my @perf_filters;

        my $db_obj_g1 = $db_obj_c->clone->with_adgroup();
        push @perf_filters, $db_obj_g1->create_perf_filter() for 0..3;
        push @perf_filters, $db_obj_g1->create_perf_filter({filter_name => "Mobile phones"});

        my $db_obj_g2 = $db_obj_c->clone->with_adgroup();
        push @perf_filters, $db_obj_g2->create_perf_filter() for 0..5;
        push @perf_filters, $db_obj_g2->create_perf_filter({is_deleted => 1});
        push @perf_filters, $db_obj_g2->create_perf_filter({ret_cond_id => $db_obj_g2->create_ret_cond()->id});

        dies_ok { _get_by(unknown => "key") };

        subtest 'Simple select' => sub {
            # Select by campaign_id
            ok @{_get_by(campaign_id => $db_obj_c->campaign->id)->items} == (@perf_filters - (grep { $_->is_deleted } @perf_filters));

            # Select by adgroup_id
            for my $adgroup ($db_obj_g1->adgroup, $db_obj_g2->adgroup) {
                my @g_perf_filters = grep { $_->adgroup_id == $adgroup->id } @perf_filters;
                ok @{_get_by(adgroup_id => $adgroup->id)->items} == (@g_perf_filters - (grep { $_->is_deleted } @g_perf_filters));
            }

            # Select by id
            dies_ok { _get_by(perf_filter_id => $perf_filters[0]->id) };
            ok _get_by(perf_filter_id => $perf_filters[0]->id, cid => $db_obj_c->campaign->id)->items->[0]->id == $perf_filters[0]->id;
        };

        subtest 'Select with limit/offset' => sub {
            my @items = @{_get_by(campaign_id => $db_obj_c->campaign->id, limit => 3)->items};
            ok @items == 3;
            ok _get_by(campaign_id => $db_obj_c->campaign->id, limit => 3, offset => 1)->items->[0]->id == $items[1]->id;
        };

        subtest 'Default sort is by id asc' => sub {
            my @items = @{_get_by(campaign_id => $db_obj_c->campaign->id)->items};
            is join(':', map { $_->id } @items), join(':', sort { $a <=> $b } map { $_->id } @items);
        };

        subtest 'Select with_additional' => sub {
            my $item = _get_by(perf_filter_id => $perf_filters[0]->id, shard => $db_obj_c->shard, with_additional => 1)->items->[0];
            ok $item->has_from_tab;
            ok $item->campaign_id == $db_obj_c->campaign->id;
        };

        subtest 'Select with_deleted' => sub {
            my @items = @{_get_by(campaign_id => $db_obj_c->campaign->id, with_deleted => 1)->items};
            ok @items == @perf_filters;
            ok any { $_->is_deleted } @items;
        };

        subtest 'Select by filter' => sub {
            ok @{_get_by(campaign_id => $db_obj_c->campaign->id, filter => {'bpf.name__contains' => 'nothing'})->items} == 0;
            ok @{_get_by(campaign_id => $db_obj_c->campaign->id, filter => {'bpf.name__contains' => 'phones'})->items} == 1;
        };

        subtest 'items_by' => sub {
            my $logic = _get_by(campaign_id => $db_obj_c->campaign->id, with_deleted => 1);

            ok keys(%{$logic->items_by}) == @perf_filters;
            ok keys(%{$logic->items_by('id')}) == @perf_filters;

            ok keys(%{$logic->items_by('adgroup_id')}) == 2;
            ok @{$logic->items_by('adgroup_id')->{$db_obj_g1->adgroup->id}} == grep { $_->adgroup_id == $db_obj_g1->adgroup->id } @perf_filters;
            ok @{$logic->items_by('adgroup_id')->{$db_obj_g2->adgroup->id}} == grep { $_->adgroup_id == $db_obj_g2->adgroup->id } @perf_filters;
        };
    };

    subtest 'Create performance filters' => sub {
        my $db_obj = $db_obj_c->clone->with_adgroup();
        my $adgroup = $db_obj->adgroup;

        my %hash = (
            adgroup_id => $db_obj->adgroup->id, adgroup => $db_obj->adgroup, campaign_id => $db_obj->campaign->id,
            filter_name => "Test filter", target_funnel => 'same_products', from_tab => 'condition',
        );
        my $perf_filter1 = mk_filter(
            %hash, price_cpc => '0.11', condition => [mk_rule(field => 'name', relation => '==', value => ['test'])],
        );
        my $perf_filter2 = mk_filter(
            %hash, price_cpa => '4.12', condition => [mk_rule(field => 'price', relation => '>', value => ['10'])],
            ret_cond_id => $db_obj->create_ret_cond()->id,
        );

        my $logic = Direct::PerformanceFilters->new([$perf_filter1, $perf_filter2]);
        ok_func_is_called 'Direct::PerformanceFilters::prepare_create' => sub { $logic->create() };

        # TODO: fix last_change here and below
        cmp_model_with $_, $db_obj->get_perf_filter($_->id, with_additional => 1), exclude => [qw/last_change adgroup/] for ($perf_filter1, $perf_filter2);
        is_deeply $logic->data->{log_price}, [map { +{
            cid => $db_obj->campaign->id,
            pid => $db_obj->adgroup->id,
            id => $_->id,
            type => 'perf_filter_create',
            price => $_->has_price_cpc ? $_->price_cpc : 0,
            price_ctx => $_->has_price_cpa ? $_->price_cpa : 0,
            currency => $db_obj->campaign->currency,
        } } ($perf_filter1, $perf_filter2)];
    };

    subtest 'Update performance filters' => sub {
        my $db_obj = $db_obj_c->clone->with_adgroup();

        my ($perf_filter1, $perf_filter2) = ($db_obj->create_perf_filter(), $db_obj->create_perf_filter());
        $_->adgroup($db_obj->adgroup) for ($perf_filter1, $perf_filter2);

        $perf_filter1->condition([mk_rule(field => 'name', relation => '==', value => ['name321'])]);
        $perf_filter1->ret_cond_id($db_obj->create_ret_cond()->id);
        $perf_filter2->price_cpc('5.67');

        my $logic = Direct::PerformanceFilters->new([$perf_filter1, $perf_filter2]);
        ok_func_is_called 'Direct::PerformanceFilters::prepare_update' => sub { $logic->update() };

        subtest 'Check perf_filter1 after update' => sub {
            cmp_model_with $perf_filter1, $db_obj->get_perf_filter($perf_filter1->id, with_additional => 1), exclude => [qw/last_change adgroup/];
        };
        subtest 'Check perf_filter2 after update' => sub {
            cmp_model_with $perf_filter2, $db_obj->get_perf_filter($perf_filter2->id, with_additional => 1), exclude => [qw/last_change adgroup/];
        };
        is_deeply $logic->data->{log_price}, [
            {
                cid => $db_obj->campaign->id,
                pid => $db_obj->adgroup->id,
                id => $perf_filter2->id,
                type => 'perf_filter_update',
                price => $perf_filter2->price_cpc,
                price_ctx => $perf_filter2->price_cpa,
                currency => $db_obj->campaign->currency,
            },
        ];
    };

    subtest 'Copy performance filters' => sub {
        # Создадим группы в разных шардах
        my $db_obj_from = Direct::Test::DBObjects->new(shard => 1)->with_adgroup('performance');
        my $db_obj_to = Direct::Test::DBObjects->new(shard => 2)->with_campaign('performance', {currency => 'RUB'})->with_adgroup();

        # Добавим фильтры на группу
        $db_obj_from->create_perf_filter({last_change => '_in_past'}) for 0..2;

        my $from_clientid = get_clientid(pid => $db_obj_from->adgroup->id);
        my $to_clientid = get_clientid(pid => $db_obj_to->adgroup->id); 
        my $price_coef = 1.17;
        my ($id_src2dst, $copy_obj);
        ($id_src2dst, undef, $copy_obj) = Direct::PerformanceFilters->copy(
            [adgroup_id => $db_obj_from->adgroup->id],
            $from_clientid, $to_clientid,
            sub {
                $_ = shift;
                $_->campaign_id(get_cid(pid => $db_obj_to->adgroup->id));
                $_->adgroup_id($db_obj_to->adgroup->id);
                $_->price_cpc(0 + sprintf("%.2f", $_->price_cpc * $price_coef));
                $_->price_cpa(0 + sprintf("%.2f", $_->price_cpa * $price_coef));
                $_->last_change('now');
                $_->status_bs_synced('No');
                return $_;
            }, primary_key => 'perf_filter_id'
        );

        my $src_filters = $db_obj_from->get_perf_filters_obj(adgroup_id => $db_obj_from->adgroup->id, with_additional => 1);
        my $dst_filters = $db_obj_to->get_perf_filters_obj(adgroup_id => $db_obj_to->adgroup->id, with_additional => 1);

        ok @{$src_filters->items} == 3;
        ok @{$dst_filters->items} == 3;

        for my $src_filter (@{$src_filters->items}) {
            my $dst_filter = $dst_filters->items_by->{$id_src2dst->{ $src_filter->id }};

            ok $src_filter->id != $dst_filter->id;
            for (qw/filter_name autobudget_priority target_funnel now_optimizing_by is_suspended is_deleted _condition_json from_tab available/) {
                is $src_filter->$_, $dst_filter->$_;
            }
            is sprintf("%.2f", $src_filter->price_cpc * $price_coef), sprintf("%.2f", $dst_filter->price_cpc);
            is sprintf("%.2f", $src_filter->price_cpa * $price_coef), sprintf("%.2f", $dst_filter->price_cpa);
            ok $src_filter->campaign_id != $dst_filter->campaign_id;
            isnt $src_filter->last_change, $dst_filter->last_change;
        }

        is_deeply($copy_obj->data->{log_price}, [map { +{
            cid => $db_obj_to->campaign->id,
            pid => $db_obj_to->adgroup->id,
            id => $_->id,
            type => 'perf_filter_copy',
            price => 0 + $_->price_cpc,
            price_ctx => 0 + $_->price_cpa,
            currency => $db_obj_to->campaign->currency,
        } } @{$dst_filters->items}]);
    };

    subtest 'Delete performance filters' => sub {
        my $db_obj = $db_obj_c->clone->with_adgroup();

        my $perf_filter = $db_obj->create_perf_filter();
        $perf_filter->adgroup($db_obj->adgroup);

        my $logic = Direct::PerformanceFilters->new([$perf_filter]);
        ok_func_is_called 'Direct::PerformanceFilters::prepare_delete' => sub { $logic->delete() };

        ok !defined $db_obj->get_perf_filter($perf_filter->id);
        ok $db_obj->get_perf_filter($perf_filter->id, with_deleted => 1)->is_deleted == 1;
    };
};

done_testing;
