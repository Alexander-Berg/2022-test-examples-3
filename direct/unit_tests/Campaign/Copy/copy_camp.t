use my_inc "../../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;
use Test::Exception;

use Settings;
use Test::CreateDBObjects;

use PrimitivesIds;
use Campaign;
use Yandex::DBTools;
use Yandex::DBShards;
use Common qw/:subs/;
use Models::Campaign;
use HierarchicalMultipliers qw/save_hierarchical_multipliers/;
use Retargeting;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';
use Test::JavaIntapiMocks::GenerateObjectIds;

sub load_modules: Tests(startup => 1) {
    use_ok 'Campaign::Copy';
}

sub got_ids_mapping: Test(1) {
    my $old_cid = create('campaign', shard => 1);
    my $old_group = create('group', cid => $old_cid);
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $new_user = create('user', shard => 2);

    no warnings qw/redefine once/;
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        return $new_user->{uid};
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };
    local *Campaign::get_perminfo = sub { return { role => 'client' } };
    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };

    my $ids_mapping = {};
    my $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $new_user->{uid}, undef, undef, flags => {copy_stopped => 1, copy_archived => 1}, ids_mapping => $ids_mapping);

    my $expected_ids_mapping = {
        bid_old2new => { 4 => 5 },
        new_groups_phrases => { 4 => [3] },
        old_groups_banners => { 3 => [4] },
        phrase_id_old2new => { 3 => 4 },
        pid_old2new => { 3 => 4 },
        ret_cond_ids_old2new => {},
        old_pid2ret_cond_ids => {},
        dyn_cond_ids_old2new => {},
        old_gid2dyn_cond_ids => {},
        old_gid2perf_filter_ids => {},
        perf_filter_ids_old2new => {},
    };

    is_deeply($ids_mapping, $expected_ids_mapping, 'got correct ids mapping');
}

sub hierarchical_multipliers_are_copied: Test(4) {
    my $old_cid = create('campaign', shard => 1, hierarchical_multipliers => {mobile_multiplier => {multiplier_pct => 77}});
    my $old_group = create('group', hierarchical_multipliers => {mobile_multiplier => {multiplier_pct => 88}}, cid => $old_cid);
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $new_user = create('user', shard => 2);

    no warnings qw/redefine once/;
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        return $new_user->{uid};
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };
    local *Campaign::get_perminfo = sub { return { role => 'client' } };
    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
    local *BSAuction::get_show_conditions_statistic = sub { return {} };

    my $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $new_user->{uid}, undef, undef, flags => {copy_stopped => 1, copy_archived => 1});

    is get_shard(cid => $new_cid), 2;
    eq_hierarchical_multipliers($new_cid, undef, {mobile_multiplier => {multiplier_pct => 77}});
    my $new_camp = Models::Campaign::get_user_camp_gr($new_user->{uid}, $new_cid, undef, {get_auction => 0});

    ok $new_camp->{groups}[0]{pid};
    eq_hierarchical_multipliers($new_cid, $new_camp->{groups}[0]{pid}, {mobile_multiplier => {multiplier_pct => 88}});
}

sub demography_multipliers_are_copied: Test(4) {
    my $old_cid = create('campaign', shard => 1, hierarchical_multipliers => {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '0-17',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ]
        },
    });
    my $old_group = create('group', cid => $old_cid, hierarchical_multipliers => {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 178,
                },
            ]
        },
    });
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $new_user = create('user', shard => 2);

    no warnings qw/redefine once/;
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        return $new_user->{uid};
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };

    local *Campaign::get_perminfo = sub { return { role => 'client' } };

    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };

    local *BSAuction::get_show_conditions_statistic = sub { return {} };

    my $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $new_user->{uid}, undef, undef, flags => {copy_stopped => 1, copy_archived => 1});

    is get_shard(cid => $new_cid), 2;
    eq_hierarchical_multipliers($new_cid, undef, {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '0-17',
                    gender => 'male',
                    multiplier_pct => 177,
                },
            ]
        },
    });
    my $new_camp = Models::Campaign::get_user_camp_gr($new_user->{uid}, $new_cid, undef, {get_auction => 0});

    ok $new_camp->{groups}[0]{pid};
    eq_hierarchical_multipliers($new_cid, $new_camp->{groups}[0]{pid}, {
        demography_multiplier => {
            is_enabled => 1,
            conditions => [
                {
                    age => '25-34',
                    gender => 'male',
                    multiplier_pct => 178,
                },
            ]
        },
    });
}

sub retargeting_multipliers_are_copied: Test(8) {
    ##################
    # Setup old data #
    ##################
    no warnings qw/redefine once/;
    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
    local *BSAuction::get_show_conditions_statistic = sub { return {} };

    my $old_group = create('group', shard => 1);
    is get_shard(cid => $old_group->{cid}), 1; # Just to be sure
    my $ret_cond_id_for_camp = create('retargeting_condition', uid => get_uid(cid => $old_group->{cid}));
    my $ret_cond_id_for_group = create('retargeting_condition', uid => get_uid(cid => $old_group->{cid}));
    my $old_cid = $old_group->{cid};
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $old_camp_data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id_for_camp => {
                    multiplier_pct => 177,
                },
            },
        },
    };

    save_hierarchical_multipliers($old_cid, undef, $old_camp_data);
    my $old_group_data = {
        retargeting_multiplier => {
            is_enabled => 1,
            conditions => {
                $ret_cond_id_for_group => {
                    multiplier_pct => 178,
                },
            },
        },
    };
    save_hierarchical_multipliers($old_cid, $old_group->{pid}, $old_group_data);

    #########################################
    # Create new user and do actual copying #
    #########################################
    my $new_user = create('user', shard => 2);
    my $new_ClientID = $new_user->{ClientID};
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        return $new_user->{uid};
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };
    local *Campaign::get_perminfo = sub { return { role => 'client' } };

    my $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $new_user->{uid}, undef, undef, flags => {copy_stopped => 1, copy_archived => 1});

    ############################
    # Validate copying results #
    ############################
    is get_shard(cid => $new_cid), 2;

    my $old_retargeting_conditions = Retargeting::get_retargeting_conditions(ClientID => $old_ClientID);
    my $new_retargeting_conditions = Retargeting::get_retargeting_conditions(ClientID => $new_ClientID);

    is scalar(keys %$new_retargeting_conditions), 2;

    my $old_group_goal = $old_retargeting_conditions->{$ret_cond_id_for_group}{condition}[0]{goals}[0]{goal_id};
    my $old_camp_goal = $old_retargeting_conditions->{$ret_cond_id_for_camp}{condition}[0]{goals}[0]{goal_id};

    my ($new_ret_cond_id_for_group, $new_ret_cond_id_for_camp);
    for my $ret_cond (values %$new_retargeting_conditions) {
        if ($ret_cond->{condition}[0]{goals}[0]{goal_id} == $old_group_goal) {
            $new_ret_cond_id_for_group = $ret_cond->{ret_cond_id};
        }
        if ($ret_cond->{condition}[0]{goals}[0]{goal_id} == $old_camp_goal) {
            $new_ret_cond_id_for_camp = $ret_cond->{ret_cond_id};
        }
    }
    ok $new_ret_cond_id_for_camp > 0;
    ok $new_ret_cond_id_for_group > 0;

    my $new_camp = Models::Campaign::get_user_camp_gr($new_user->{uid}, $new_cid, undef, {get_auction => 0});
    ok $new_camp->{groups}[0]{pid};

    $old_camp_data->{retargeting_multiplier}{conditions}{$new_ret_cond_id_for_camp}
        = delete $old_camp_data->{retargeting_multiplier}{conditions}{$ret_cond_id_for_camp};
    eq_hierarchical_multipliers($new_cid, undef, $old_camp_data);

    $old_group_data->{retargeting_multiplier}{conditions}{$new_ret_cond_id_for_group}
        = delete $old_group_data->{retargeting_multiplier}{conditions}{$ret_cond_id_for_group};
    eq_hierarchical_multipliers($new_cid, $new_camp->{groups}[0]{pid}, $old_group_data);
}

sub test_copy_banners_minus_geo: Test {
    my $old_cid = create('campaign', shard => 1);
    my $old_group = create('group', cid => $old_cid);
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $new_user = create('user', shard => 2);

    no warnings qw/redefine once/;
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        return $old_uid;
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };
    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
    local *Campaign::Copy::get_user_data = sub {return {email => '', fio => ''}};
    local *Campaign::Copy::send_banners_to_moderate = sub {};
    local *Campaign::Copy::rbac_who_is = sub {return 'client'};


    my $bid = $old_group->{banners}->[0]->{bid};

    do_insert_into_table(PPC(cid => $old_cid), 'banners_minus_geo', { bid => $bid, minus_geo => '225' });

    my $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $old_uid, undef, undef, flags => { copy_moderate_status => 1 });

    my $new_geo = get_one_field_sql(PPC(cid => $new_cid), [
        'select bmg.minus_geo from banners_minus_geo bmg join banners b on b.bid = bmg.bid',
        where => {
            'b.cid' => $new_cid,
        }
    ]);
    is($new_geo, '225', "copied minus geo");

}

sub test_reset_status_moderate: Test(2) {
    my $old_cid = create('campaign', shard => 1);
    my $old_group = create('group', cid => $old_cid);
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $new_user = create('user', shard => 2);

    no warnings qw/redefine once/;
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        return $old_uid;
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };
    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub {return 0};
    local *Campaign::Copy::get_user_data = sub {return {email => '', fio => ''}};
    local *Campaign::Copy::send_banners_to_moderate = sub {};
    local *Campaign::Copy::rbac_who_is = sub {return 'client'};

    my $bid = $old_group->{banners}->[0]->{bid};

    do_update_table(PPC(cid => $old_cid), "phrases", {statusModerate => 'Yes'}, where => {cid => $old_cid});
    my $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $old_uid, undef, undef, flags => {copy_moderate_status => 1});
    my $copied_status = get_one_field_sql(PPC(cid => $old_cid), ["SELECT statusModerate FROM phrases", where => {cid => $new_cid}]);
    is($copied_status, 'Ready', 'should reset statusModerate');

    do_update_table(PPC(cid => $old_cid), "bids", {statusModerate => 'Yes'}, where => {cid => $old_cid});
    $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $old_uid, undef, undef, flags => {copy_moderate_status => 1});
    $copied_status = get_one_field_sql(PPC(cid => $old_cid), ["SELECT statusModerate FROM phrases", where => {cid => $new_cid}]);
    is($copied_status, 'Yes', 'should not reset statusModerate');
}

sub test_copy_video_additions_on_different_users: Test(2) {
    my $old_cid = create('campaign', shard => 1);
    my $old_group = create('group', cid => $old_cid);
    my $old_uid = get_uid(cid => $old_cid);
    my $old_ClientID = get_clientid(uid => $old_uid);
    my $new_user = create('user', shard => 2);

    no warnings qw/redefine once/;
    local *Campaign::Copy::rbac_get_chief_rep_of_client_rep = sub {
        my ($uid) = @_;
        return $uid;
    };
    local *Campaign::Copy::rbac_get_client_clientids_by_uids = sub {
        return {
            $new_user->{uid} => $new_user->{ClientID},
            $old_uid => $old_ClientID,
        };
    };
    local *Campaign::get_perminfo = sub { return { role => 'client' } };
    local *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };

    my $bid = $old_group->{banners}->[0]->{bid};
    my $pid = $old_group->{pid};
    do_sql(PPC(shard => 'all'), 'delete from banners_performance');
    do_sql(PPC(shard => 'all'), 'delete from perf_creatives');
    do_sql(PPC(shard => 'all'), "alter table banners_performance Engine = InnoDB");
    do_sql(PPC(shard => 'all'), "alter table perf_creatives Engine = InnoDB");
    do_sql(PPC(shard => 'all'), "alter table banners_performance
        ADD CONSTRAINT `banners_performance_ibfk_2`
        FOREIGN KEY (`creative_id`)
        REFERENCES `perf_creatives` (`creative_id`) "
    );

    do_delete_from_table(PPC(uid => $new_user->{uid}), 'perf_creatives', where => { creative_id => 1 });
    do_insert_into_table(PPC(cid => $old_cid), 'perf_creatives', { creative_id => 1, creative_type => 'video_addition' });
    do_insert_into_table(PPC(cid => $old_cid), 'banners_performance', { banner_creative_id => 1, creative_id => 1, cid => $old_cid, bid => $bid, pid => $pid });
    
    my $new_cid;
    lives_ok {
        local *Campaign::Copy::get_user_data = sub {return {email => '', fio => ''}};
        local *Campaign::Copy::send_banners_to_moderate = sub {};
        local *Campaign::Copy::rbac_who_is = sub {return 'client'};
        $new_cid = Campaign::Copy::copy_camp(undef, $old_cid, $new_user->{uid}, undef, undef, flags => { copy_moderate_status => 1 });
    };
    isnt(get_shard(cid => $new_cid), get_shard(cid => $old_cid), "new camp created in another shard");
    do_sql(PPC(shard => 'all'), "alter table banners_performance DROP FOREIGN KEY banners_performance_ibfk_2");
    do_sql(PPC(shard => 'all'), "alter table banners_performance Engine = MyISAM");
    do_sql(PPC(shard => 'all'), "alter table perf_creatives Engine = MyISAM");
}

local $BS::TrafaretAuction::LOG_REQUESTS = undef;
create_tables();
__PACKAGE__->runtests;
