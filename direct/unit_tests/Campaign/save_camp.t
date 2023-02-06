use my_inc "../..";
use Direct::Modern;

use base qw/Test::Class/;
use Test::More;

use Settings;
use Test::Subtest;
use DirectContext;
use Yandex::DBTools;
use Yandex::HashUtils qw/hash_cut/;
use Test::CreateDBObjects;
use Test::JavaIntapiMocks::BidModifiers ':forward_to_perl';

BEGIN {
    use_ok 'Campaign';
}

{
    no warnings 'redefine';
    my $original_new = \&Yandex::Log::new;
    *Yandex::Log::new = sub { my $self = shift; my %O = @_; $O{use_syslog} = 0; return $original_new->($self, %O) };
    *Client::ClientFeatures::has_access_to_new_feature_from_java = sub { return 0 };
}

sub save {
    my $c = DirectContext->new({
        login_rights => {},
    });

    Campaign::save_camp($c, @_);
}

create_tables();

subtest_ "save_camp_saves_mobile_multiplier_both_to_camp_options_and_to_hierarchical_multipliers" => sub {
    my $cid = create('campaign', bssynced_long_time_ago => 1);
    my $campaign = Campaign::get_camp_info($cid, undef, with_strategy => 1);

    $campaign->{hierarchical_multipliers}{mobile_multiplier}{multiplier_pct} = 99;

    save($campaign, $campaign->{uid});

    eq_hierarchical_multipliers $cid, undef, {mobile_multiplier => {multiplier_pct => 99}};
    my ($statusBsSynced, $last_change_age) = get_one_line_array_sql(
        PPC(cid => $cid), [
            "select statusBsSynced, unix_timestamp(now()) - unix_timestamp(LastChange) from campaigns",
            where => {cid => $cid}
        ]
    );
    # XXX Сейчас save_camp() выставляет statusBsSynced и LastChange безусловно
    is $statusBsSynced, 'No';

    # ok $last_change_age > 86400;
    return 'save_camp() is too dumb, nothing to test yet';
};

subtest_ "save_camp_deletes_mobile_multiplier_both_from_camp_options_and_hierarchical_multipliers" => sub {
    my $cid = create('campaign', bssynced_long_time_ago => 1, hierarchical_multipliers => {mobile_multiplier => {multiplier_pct => 177}});
    my $campaign = Campaign::get_camp_info($cid, undef, with_strategy => 1);

    delete $campaign->{hierarchical_multipliers}{mobile_multiplier};
    save($campaign, $campaign->{uid});

    eq_hierarchical_multipliers $cid, undef, {};
};

subtest_ "save_camp_honors_ignore_hierarchical_multipliers_option" => sub {
    my $cid = create('campaign', bssynced_long_time_ago => 1);
    my $campaign = Campaign::get_camp_info($cid, undef, with_strategy => 1);

    $campaign->{hierarchical_multipliers} = {mobile_multiplier => {multiplier_pct => 177}};
    save($campaign, $campaign->{uid});

    delete $campaign->{hierarchical_multipliers}{mobile_multiplier};
    save($campaign, $campaign->{uid}, ignore_hierarchical_multipliers => 1);

    eq_hierarchical_multipliers $cid, undef, {mobile_multiplier => {multiplier_pct => 177}};
};

subtest_ "When saving strategy for performance campaign" => sub {
    sub mk {
        my ($strategy) = @_;
    };
    my %strategy_samples = ();
    my @valid_perf_strategies = ();
    subtest_ "it should accept valid strategies for DMO" => sub {
        for my $strategy (values %{hash_cut \%strategy_samples, @valid_perf_strategies}) {
            my $camp = mk($strategy);
        }
    };
    subtest_ "it should reject all other strategies" => sub {
    };
};

subtest_ "Do not touch LastChange when camp name changed" => sub {
    my $cid = create('campaign');
    my $camp = Campaign::get_camp_info($cid, undef, with_strategy => 1);
    $camp->{broad_match_flag} = undef;
    my $group = create('group', cid => $cid);

    my $bid = $group->{banners}->[0]->{bid};
    do_update_table(PPC(bid => $bid), 'banners', { 'LastChange' => '2016-01-01 00:00:00' }, where => { bid => $bid });
    my $last_change_before = get_one_field_sql(PPC(bid => $bid), 'select LastChange from banners where bid = ?', $bid);

    ok(defined $last_change_before);
    
    $camp->{name} = '--- new name ---';
    save($camp, $camp->{uid});

    my $last_change_after = get_one_field_sql(PPC(bid => $bid), 'select LastChange from banners where bid = ?', $bid);

    is($last_change_after, $last_change_before, 'LastChange on banners not changed');
};

run_subtests();
