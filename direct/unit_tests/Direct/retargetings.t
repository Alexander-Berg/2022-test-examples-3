#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Exception;

use Settings;

use List::MoreUtils qw/uniq/;

use Direct::Test::DBObjects qw/sandbox ok_func_is_called cmp_model_with/;

use Test::JavaIntapiMocks::GenerateObjectIds;

BEGIN {
    use_ok('Direct::Model::Retargeting');
    use_ok('Direct::Model::AdGroupText');

    use_ok('Direct::Retargetings');
}

sub mk_ret_bid { Direct::Test::DBObjects->new_ret_bid_model(@_) }
sub last_change__dont_quote { shift->get_db_column_value(bids_retargeting => 'modtime', extended => 1)->{val__dont_quote} }

subtest 'Prepare to create retargeting bids' => sub {
    my $adgroup = Direct::Model::AdGroupText->new(id => 1);
    my $orig_ret_bid = mk_ret_bid(adgroup_id => $adgroup->id, adgroup => $adgroup);

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        sandbox $orig_ret_bid => sub {
            Direct::Retargetings->new([$_])->prepare_create();
            is_deeply($_->get_state_hash, {
                changes => {last_change => 1, status_bs_synced => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1) : ()),
                },
            });
            is $_->status_bs_synced, 'No';
            like last_change__dont_quote($_), qr/^now/i;
        };
    }
};

subtest 'Prepare to update retargeting bids' => sub {
    my $adgroup = Direct::Model::AdGroupText->new(id => 1);
    my $orig_ret_bid = mk_ret_bid(adgroup_id => $adgroup->id, adgroup => $adgroup, ret_cond_id => 1);

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        # Изменение условия ретаргетинга
        sandbox $orig_ret_bid => sub {
            $_->ret_cond_id(2);
            Direct::Retargetings->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {ret_cond_id => 1, last_change => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1) : ()),
                },
            });
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение ставок: price_context
        sandbox $orig_ret_bid => sub {
            $_->price_context($_->price_context + 1);
            Direct::Retargetings->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {price_context => 1, last_change => 1, status_bs_synced => 1},
                flags => {},
            });
            is $_->status_bs_synced, 'No';
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение приоритета автобюджета
        sandbox $orig_ret_bid => sub {
            $_->autobudget_priority(5);
            Direct::Retargetings->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {autobudget_priority => 1, last_change => 1, status_bs_synced => 1},
                flags => {},
            });
            is $_->status_bs_synced, 'No';
            like last_change__dont_quote($_), qr/^now/i;
        };

        # Изменение is_suspended
        sandbox $orig_ret_bid => sub {
            $_->is_suspended(1);
            Direct::Retargetings->new([$_])->prepare_update();
            is_deeply($_->get_state_hash, {
                changes => {is_suspended => 1, last_change => 1},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1) : ()),
                },
            });
            like last_change__dont_quote($_), qr/^now/i;
        };
    }
};

subtest 'Prepare to delete retargeting bids' => sub {
    my $adgroup = Direct::Model::AdGroupText->new(id => 1);
    my $orig_ret_bid = mk_ret_bid(adgroup_id => $adgroup->id, adgroup => $adgroup, ret_cond_id => 1);

    for my $g_status_moderate (qw/No Yes/) {
        $adgroup->status_moderate($g_status_moderate);

        sandbox $orig_ret_bid => sub {
            Direct::Retargetings->new([$_])->prepare_delete();
            is_deeply($_->get_state_hash, {
                changes => {},
                flags => {
                    update_adgroup_last_change => 1,
                    ($g_status_moderate ne 'New' ? (bs_sync_adgroup => 1) : ()),
                },
            });
        };
    }
};

subtest 'General' => sub {
    ok %{Direct::Retargetings->WEB_FIELD_NAMES};
};

subtest 'Tests with database access' => sub {
    Direct::Test::DBObjects->create_tables;

    my $db_obj_c = Direct::Test::DBObjects->new()->with_campaign('text');

    local $LogTools::context{uid} = $db_obj_c->user->id;

    no warnings 'redefine';
    local *Direct::Retargetings::do_logging = sub {};

    subtest 'Select retargeting bids' => sub {
        my $db_obj_g1 = $db_obj_c->clone()->with_adgroup();
        my $db_obj_g2 = $db_obj_c->clone()->with_adgroup();

        local *_get_by = sub { Direct::Retargetings->get_by(@_) };

        my @ret_bids;
        push @ret_bids, $db_obj_g1->create_ret_bid() for 0..3;
        push @ret_bids, $db_obj_g2->create_ret_bid() for 0..2;
        push @ret_bids, $db_obj_g2->create_ret_bid({autobudget_priority => 5});

        dies_ok { _get_by(unknown => "key") };

        subtest 'Simple select' => sub {
            # Select by campaign_id
            ok @{_get_by(campaign_id => $db_obj_c->campaign->id)->items} == @ret_bids;

            # Select by adgroup_id
            for my $adgroup ($db_obj_g1->adgroup, $db_obj_g2->adgroup) {
                my @g_ret_bids = grep { $_->adgroup_id == $adgroup->id } @ret_bids;
                ok @{_get_by(adgroup_id => $adgroup->id)->items} == @g_ret_bids;
            }

            # Select by ret_cond_id
            ok uniq(map { $_->ret_cond_id } @ret_bids) == @ret_bids;
            ok @{_get_by(ret_cond_id => $ret_bids[0]->ret_cond_id)->items} == 1;

            # Select by id
            dies_ok { _get_by(ret_id => $ret_bids[0]->id) };
            ok _get_by(ret_id => $ret_bids[0]->id, ClientID => $db_obj_c->user->client_id)->items->[0]->id == $ret_bids[0]->id;
        };

        subtest 'Select with limit/offset/total_count' => sub {
            my $x = _get_by(campaign_id => $db_obj_c->campaign->id, limit => 2, offset => 1, total_count => 1);
            ok $x->total == @ret_bids;
            ok @{$x->items} == 2;
            ok $x->items->[0]->id == $ret_bids[1]->id;
        };

        subtest 'Default sort is by id asc' => sub {
            my @items = @{_get_by(campaign_id => $db_obj_c->campaign->id)->items};
            is join(':', map { $_->id } @items), join(':', sort { $a <=> $b } map { $_->id } @items);
        };

        subtest 'Select by filter' => sub {
            ok @{_get_by(campaign_id => $db_obj_c->campaign->id, filter => {autobudgetPriority => 5})->items} == 1;
        };

        subtest 'items_by' => sub {
            my $logic = _get_by(campaign_id => $db_obj_c->campaign->id);

            ok keys(%{$logic->items_by}) == @ret_bids;
            ok keys(%{$logic->items_by('id')}) == @ret_bids;

            ok keys(%{$logic->items_by('adgroup_id')}) == 2;
            ok @{$logic->items_by('adgroup_id')->{$db_obj_g1->adgroup->id}} == grep { $_->adgroup_id == $db_obj_g1->adgroup->id } @ret_bids;
            ok @{$logic->items_by('adgroup_id')->{$db_obj_g2->adgroup->id}} == grep { $_->adgroup_id == $db_obj_g2->adgroup->id } @ret_bids;
        };
    };

    subtest 'Create retargeting bids' => sub {
        my $db_obj = $db_obj_c->clone->with_adgroup();
        my $banner = $db_obj->create_banner();

        my $ret_bid = $db_obj->create_ret_bid()->alter(adgroup => $db_obj->adgroup);

        my $logic = Direct::Retargetings->new([$ret_bid]);
        ok_func_is_called 'Direct::Retargetings::prepare_create' => sub { $logic->create() };

        cmp_model_with $ret_bid, $db_obj->get_ret_bid($ret_bid->id), exclude => [qw/last_change adgroup/];
        is_deeply $logic->data->{log_price}, [{
            cid => $db_obj->campaign->id,
            pid => $db_obj->adgroup->id,
            id => $ret_bid->id,
            type => 'ret_add',
            price => 0,
            price_ctx => $ret_bid->price_context,
            currency => $db_obj->campaign->currency,
        }];
        is_deeply $logic->data->{notifications}, [{
            object     => 'adgroup',
            event_type => 'b_retargeting',
            object_id  => $db_obj->adgroup->id,
            old_text   => '',
            new_text   => $ret_bid->ret_cond_id,
            uid        => $db_obj->user->id,
        }];
    };

    subtest 'Update retargeting bids' => sub {
        my $db_obj = $db_obj_c->clone->with_adgroup();
        my $banner = $db_obj->create_banner();

        my ($ret_bid1, $ret_bid2) = ($db_obj->create_ret_bid(), $db_obj->create_ret_bid());
        $_->alter(old => $_->clone, adgroup => $db_obj->adgroup) for ($ret_bid1, $ret_bid2);

        $ret_bid1->ret_cond_id($db_obj->create_ret_cond()->id);
        $ret_bid2->price_context('5.67');

        my $logic = Direct::Retargetings->new([$ret_bid1, $ret_bid2]);
        ok_func_is_called 'Direct::Retargetings::prepare_update' => sub { $logic->update() };

        ok !$_->is_changed for ($ret_bid1, $ret_bid2);
        cmp_model_with $_, $db_obj->get_ret_bid($_->id), exclude => [qw/last_change old adgroup/] for ($ret_bid1, $ret_bid2);

        is_deeply $logic->data->{log_price}, [{
            cid => $db_obj->campaign->id,
            pid => $db_obj->adgroup->id,
            id => $ret_bid2->id,
            type => 'ret_update',
            price => 0,
            price_ctx => $ret_bid2->price_context,
            currency => $db_obj->campaign->currency,
        }];
        is_deeply $logic->data->{notifications}, [{
            object     => 'adgroup',
            event_type => 'b_retargeting',
            object_id  => $db_obj->adgroup->id,
            old_text   => $ret_bid1->old->ret_cond_id,
            new_text   => $ret_bid1->ret_cond_id,
            uid        => $db_obj->user->id,
        }, {
            object     => 'phrase',
            event_type => 'ph_price_ctx',
            object_id  => $banner->id,
            old_text   => $ret_bid2->old->price_context,
            new_text   => $ret_bid2->price_context,
            uid        => $db_obj->user->id,
        }];
    };

    subtest 'Delete retargeting bids' => sub {
        my $db_obj = $db_obj_c->clone->with_adgroup();
        my $banner = $db_obj->create_banner();

        my $ret_bid = $db_obj->create_ret_bid()->alter(adgroup => $db_obj->adgroup);

        my $logic = Direct::Retargetings->new([$ret_bid]);
        ok_func_is_called 'Direct::Retargetings::prepare_delete' => sub { $logic->delete() };

        ok !defined $db_obj->get_ret_bid($ret_bid->id);
        is_deeply $logic->data->{log_price}, [{
            cid => $db_obj->campaign->id,
            pid => $db_obj->adgroup->id,
            id => $ret_bid->id,
            type => 'delete2',
            price => 0,
            price_ctx => $ret_bid->price_context,
            currency => $db_obj->campaign->currency,
        }];
        is_deeply $logic->data->{notifications}, [{
            object     => 'adgroup',
            event_type => 'ret_delete',
            object_id  => $db_obj->adgroup->id,
            old_text   => $ret_bid->ret_cond_id,
            new_text   => '',
            uid        => $db_obj->user->id,
        }];
    };
};

done_testing;
