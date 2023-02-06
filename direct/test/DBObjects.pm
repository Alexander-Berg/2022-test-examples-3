package Direct::Test::DBObjects;

use Direct::Modern;

use Mouse;
use Mouse::Exporter;

use Test::Builder qw//;
use Test::Deep::NoTest qw/cmp_details deep_diag/;

use Settings;

use List::Util qw/shuffle/;
use List::MoreUtils qw/all/;
use POSIX qw/strftime/;
use Readonly;

use Yandex::DBTools;
use Yandex::DBShards;
use Yandex::DBSchema;
use Yandex::DBUnitTest qw/:all/;

use Yandex::HashUtils qw/hash_cut/;
use Yandex::TimeCommon qw/mysql_round_day tomorrow/;

use Direct::Model::User;
#use Direct::Model::CampaignText;
#use Direct::Model::CampaignDynamic;
#use Direct::Model::CampaignPerformance;
use Direct::Model::AdGroupText;
use Direct::Model::AdGroupDynamic;
use Direct::Model::AdGroupPerformance;
use Direct::Model::BannerText;
use Direct::Model::BannerCpmBanner;
use Direct::Model::BannerDynamic;
use Direct::Model::BannerMobileContent;
use Direct::Model::BannerPerformance;
use Direct::Model::CanvasCreative::Manager;
use Direct::Model::Creative::Manager;
use Direct::Model::Creative;
use Direct::Model::Creative::Manager;
use Direct::Model::Feed;
use Direct::Model::Feed::Manager;
use Direct::Model::PerformanceFilter;
use Direct::Model::PerformanceFilter::Manager;
use Direct::Model::Keyword;
use Direct::Model::Keyword::Manager;
use Direct::Model::BidRelevanceMatch;
use Direct::Model::Sitelink;
use Direct::Model::SitelinksSet;
use Direct::Model::SitelinksSet::Manager;
use Direct::Model::Retargeting;
use Direct::Model::Retargeting::Manager;
use Direct::Model::RetargetingCondition;
use Direct::Model::RetargetingCondition::Manager;
use Direct::Model::MediaResource;
use Direct::Model::TargetInterest;
use Direct::Model::MobileContent;

use Direct::Campaigns;
use Direct::Clients;
use Direct::AdGroups2;
use Direct::AdGroups2::Smart;
use Direct::CanvasCreatives;
use Direct::Creatives;
use Direct::DynamicConditions;
use Direct::Feeds;
use Direct::PerformanceFilters;
use Direct::Keywords;
use Direct::Banners;
use Direct::Wallets;
use Direct::Retargetings;
use Direct::RetargetingConditions;

use User qw//;
use Campaign qw//;
use PhraseText qw//;
use Sitelinks qw//;
use ShardingTools;

# для некоторых таблиц engine важен
our %TABLE_ENGINE = (
    bs_resync_queue   => 'InnoDB',
    banner_permalinks => 'InnoDB',
    client_phones     => 'InnoDB',
    mod_resync_queue  => 'InnoDB',
    );

Mouse::Exporter->setup_import_methods(
    as_is => [qw/sandbox ok_func_is_called cmp_model_with ok_vr err_vr/],
);

has 'shard' => (is => 'ro', isa => 'Int', builder => sub { (shuffle ppc_shards())[0] });

has 'user' => (
    is => 'rw', isa => 'Direct::Model::User',
    lazy => 1,
    builder => sub { confess "no user!" },
    predicate => 'has_user',
);

has 'wallet' => (
    is => 'rw', isa => 'Direct::Model::Wallet',
    lazy => 1,
    builder => sub { confess "no wallet!" },
    predicate => 'has_wallet',
);

has 'campaign' => (
    is => 'rw', isa => 'Direct::Model::Campaign',
    lazy => 1,
    builder => sub { confess "no campaign!" },
    predicate => 'has_campaign',
);

has 'adgroup' => (
    is => 'rw', isa => 'Direct::Model::AdGroup',
    lazy => 1,
    builder => sub { confess "no adgroup!" },
    predicate => 'has_adgroup',
);

sub clone {
    my ($self) = @_;
    return $self->meta->clone_object($self);
}

Readonly our $PRODUCT_TEXT_RUB => 503162;
Readonly our $PRODUCT_CPM_BANNER_RUB => 508570;
Readonly our $PRODUCT_CPM_VIDEO_RUB => 518570;


#############
#   Users   #
#############

sub get_user {
    my ($self, $user_id) = @_;
    my $row = get_one_line_sql(PPC(uid => $user_id),  "SELECT uo.*, u.* FROM users u LEFT JOIN users_options uo ON (uo.uid = u.uid) WHERE u.uid = ?", $user_id);
    return $row ? Direct::Model::User->from_db_hash($row, \{}) : undef;
}

sub create_user {
    my ($self, $params, %options) = @_;
    $params //= {};

    my $client_id = 2 + (get_one_field_sql(PPCDICT, "SELECT MAX(ClientID) FROM shard_client_id") // 0);

    save_shard(ClientID => $client_id, shard => $self->shard);

    my $user_id = 1 + (get_one_field_sql(PPCDICT, "SELECT MAX(uid) FROM shard_uid") // 0);
    my $user = Direct::Model::User->new(
        id => $user_id,
        client_id => $client_id,
        login => "user_${user_id}",
        email => "user_${user_id}\@domain.com",
        fio => "User${user_id} User${user_id}ovich",
        tags_allowed => 'Yes', # to insert record to users_options
        %$params,
    );

    do {
        no warnings qw/redefine once/;
        local *User::get_info_by_uid_passport = sub ($) {
            my ($uid) = @_;
            return {
                %{hash_cut $user->to_db_hash, qw/login email fio/},
                karma => 55,
                reg_date => '2015-01-01'
            };
        };
        my %user_data = (
            %{$user->to_db_hash},
            UID => 1,
            initial_currency => 'RUB',
            initial_country => 225,
        );
        User::create_update_user($user->id, \%user_data);
    };

    return $self->get_user($user->id);
}

sub with_user {
    my ($self, $params, %options) = @_;
    $params //= {};
    $self->user($params->{id} ? $self->get_user($params->{id}) : $self->create_user($params, %options));
    return $self;
}

#################
#   Campaigns   #
#################

sub get_campaign {
    my ($self, $campaign_id, %options) = @_;
    return Direct::Campaigns->get_by(campaign_id => $campaign_id, %options)->items->[0];
}

sub create_campaign {
    my ($self, $type, $params, %options) = @_;
    $params //= {};

    my $user = $self->has_user ? $self->user : undef;
    $user //= $params->{user_id} ? $self->get_user($params->{user_id}) : $self->create_user();
    my $client = Direct::Clients->get_by(id => $user->client_id)->items->[0];

    my $campaign_class = $Direct::Campaigns::MODEL_CLASS_BY_TYPE{$type};
    my $campaign = $campaign_class->new(
        currency => $client->work_currency,
        user_id => $user->id,
        %$params,
    );

    no warnings qw/redefine/;
    local *Client::ClientFeatures::has_get_strategy_id_from_shard_inc_strategy_id_enabled = sub { return 0 };
    my $campaign_id = Campaign::create_empty_camp(
        client_chief_uid => $user->id,
        ClientID => $user->client_id,
        client_fio => $user->fio,
        client_email => $user->email,
        ($campaign->has_agency_user_id ? (agency_uid => $campaign->agency_user_id) : ()),
        ($campaign->has_manager_user_id ? (manager_uid => $campaign->manager_user_id) : ()),
        %{$campaign->to_db_hash},
    );
    if ($campaign->has_wallet_id && $campaign->wallet_id) {
        do_sql(PPC(cid => $campaign_id), ["update campaigns set wallet_cid = ".int($campaign->wallet_id), where => {cid => $campaign_id}]);
    }
    do_sql(PPC(cid => $campaign_id), ["update campaigns set statusEmpty = 'No'", where => {cid => $campaign_id}]);

    return Direct::Campaigns->get_by(campaign_id => $campaign_id)->items->[0];
}

sub with_campaign {
    my ($self, $type, $params, %options) = @_;
    $params //= {};

    my $campaign = $params->{id} ? $self->get_campaign($params->{id}) : $self->create_campaign($type, $params, %options);

    $self->with_user({id => $campaign->user_id}) if !$self->has_user;
    $self->campaign($campaign);

    return $self;
}

#################
#    Wallets    #
#################

sub get_wallets {
    my ($self, $campaign_id) = @_;
    return Direct::Wallets->get_by(campaign_id => $campaign_id)->items;
}

sub create_wallet {
    my ($self, $params) = @_;
    $params //= {};

    my $user = $self->has_user ? $self->user : undef;
    $user //= $params->{user_id} ? $self->get_user($params->{user_id}) : $self->create_user();

    no warnings qw/redefine/;
    local *Client::ClientFeatures::has_get_strategy_id_from_shard_inc_strategy_id_enabled = sub { return 0 };
    my $campaign_id = Campaign::create_empty_camp(
        client_chief_uid => $user->id,
        ClientID => $user->client_id,
        client_fio => $user->fio,
        client_email => $user->email,
        currency => 'RUB',
        type => 'wallet',
        statusModerate     => 'Yes',
        statusPostModerate => 'Accepted',
        client_fio         => $user->fio,
        client_email       => $user->email,
        statusEmpty        => 'No',
        %$params,
    );    

    $params->{autopay_mode} //= 'none';
    $params->{paymethod_id} //= 1;

    my $wallet_campaigns_params = {
        wallet_cid              => $campaign_id,
        autopay_mode            => $params->{autopay_mode},
        onoff_date__dont_quote  => 'NOW()', 
    };

    do_insert_into_table(PPC(cid => $campaign_id), 'wallet_campaigns', $wallet_campaigns_params,
                                    on_duplicate_key_update => 1,
                                    key => ['wallet_cid'],
                                );

    my $wallet = Direct::Model::Wallet->new(id => $campaign_id, autopay_mode => $params->{autopay_mode});
    if ($params->{autopay_mode} ne 'none') {
        $wallet->autopay(
            Direct::Model::Autopay->new(id=>$campaign_id, user_id=>$user->id, %{hash_cut $params, qw/paymethod_type paymethod_id remaining_sum payment_sum person_id/})
        );
    }

    Direct::Model::Wallet::Manager->new(items => [$wallet])->update();

    return Direct::Wallets->get_by(campaign_id => $campaign_id)->items->[0];
}

sub with_wallet {
    my ($self, $params) = @_;
    $params //= {};

    my $wallet = $params->{id} ? $self->get_wallets($params->{id})->[0] : $self->create_wallet($params);

    $self->with_user({id => $wallet->user_id}) if !$self->has_user;
    $self->wallet($wallet);

    return $self;
}

################
#   AdGroups   #
################

sub get_adgroup {
    my ($self, $adgroup_id, %options) = @_;
    return $self->get_adgroups(adgroup_id => $adgroup_id, %options)->[0];
}

sub get_adgroups {
    my ($self, $key, $vals, %options) = @_;
    return Direct::AdGroups2->get_by($key, $vals, %options)->items;
}

sub create_adgroup {
    my ($self, $type, $params, %options) = @_;
    $params //= {};

    state $adgroup_type2class = {
        text => 'Direct::Model::AdGroupText',
        dynamic => 'Direct::Model::AdGroupDynamic',
        performance => 'Direct::Model::AdGroupPerformance',
        mobile_content => 'Direct::Model::AdGroupMobileContent',
        cpm_banner => 'Direct::Model::AdGroupCpmBanner',
    };

    my $campaign = $self->has_campaign ? $self->campaign : undef;
    $campaign //= $params->{campaign_id} ? $self->get_campaign($params->{campaign_id}) : $self->create_campaign($type);

    $type //= $campaign->campaign_type;
    $type = 'text' if $type eq 'base';

    if ($type eq 'performance') {
        if (!defined $params->{feed_id}) {
            # Feed required: user
            my $self_feed = $self->has_user ? $self : $self->new(shard => $self->shard, user => $self->get_user($campaign->user_id));
            $params->{feed_id} = $self_feed->create_perf_feed()->id;
        }
        $params->{status_bl_generated} //= 'No';
    } elsif ($type eq 'mobile_content') {
        $params->{mobile_content} = Direct::Model::MobileContent->new(
            client_id => $self->user->client_id,
            store_content_id => 'app1',
            store_country => 'XX',
            os_type => 'iOS',
            content_type => 'app',
        );
        $params->{store_content_href} = 'http://yandex.ru';
        $params->{device_type_targeting} = ['phone', 'tablet'];
        $params->{network_targeting} = ['wifi', 'cell'];
        $params->{min_os_version} = '1.0';
    } elsif ($type eq 'dynamic') {
        $params->{client_id} = $self->user->client_id();
        $params->{main_domain} //= 'ya.ru';
    }

    my $last_change_in_past;
    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $last_change_in_past = 1;
        delete $params->{last_change};
    }

    my $adgroup_id = get_new_id('pid', cid => $campaign->id);
    my $adgroup = $adgroup_type2class->{$type}->new(
        id => $adgroup_id,
        campaign_id => $campaign->id,
        adgroup_name => "AdGroup #${adgroup_id}",
        geo => $params->{geo} // "0",
        %$params,
    );

    if ($last_change_in_past) {
        $adgroup->set_db_column_value('phrases', 'LastChange', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
    }

    $adgroup->manager_class->new(items => [$adgroup])->create();

    return $self->get_adgroup($adgroup->id);
}

sub with_adgroup {
    my ($self, $type, $params, %options) = @_;
    $params //= {};

    my $adgroup = $params->{id} ? $self->get_adgroup($params->{id}) : $self->create_adgroup($type, $params, %options);

    $self->with_campaign($type, {id => $adgroup->campaign_id}) if !$self->has_campaign;
    $self->adgroup($adgroup);

    return $self;
}

sub update_adgroup {
    my ($self, $adgroup, $params) = @_;
    $params //= {};

    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $adgroup->set_db_column_value('phrases', 'LastChange', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
        $adgroup->is_last_change_changed(1);
        delete $params->{last_change};
    }

    $adgroup->$_($params->{$_}) for keys %$params;
    $adgroup->manager_class->new(items => [$adgroup])->update();

    return $self->get_adgroup($adgroup->id);
}

#############################
#   Performance Creatives   #
#############################

sub get_perf_creative {
    my ($self, $creative_id, $uid) = @_;
    return $self->get_perf_creatives($creative_id, $uid)->[0];
}

sub get_perf_creatives {
    my ($self, $creative_ids, $uid) = @_;

    $uid //= $self->user->id;
    return Direct::Creatives->get_by(creative_id => $creative_ids, $uid, with_rejection_reasons => 1)->items;
}

sub create_perf_creative {
    my ($self, $type, $params, %options) = @_;
    $params //= {};

    my $user = $self->has_user ? $self->user : undef;
    if ($params->{user_id}) {
        my $user_id = delete $params->{user_id};
        $user = $self->get_user($user_id);
    }
    $user //= $self->create_user();

    my $creative_id = 1 + (get_one_field_sql(PPCDICT, "SELECT MAX(creative_id) FROM shard_creative_id") // 0);
    my $creative = Direct::Model::Creative->new(
        id => $creative_id,
        stock_creative_id => $creative_id,
        client_id => $user->client_id,
        name => "Perf creative #${creative_id}",
        width => '640',
        height => '480',
        href => 'http://ya.ru',
        alt_text => undef,
        preview_url => 'https://yastatic.net/morda-logo/i/bender/logo.png',
        status_moderate => 'New',
        template_id => 320,
        creative_type => 'performance',
        version => 1,
        _mod_reason_yaml => undef,
        business_type => 'retail',
        ( map { $_ => undef } qw/group_create_time theme_id layout_id creative_group_id group_name live_preview_url moderation_comment duration is_adaptive has_packshot is_bannerstorage_predeployed/),
        %$params,
    );

    Direct::Model::Creative::Manager->new(items => [$creative])->create_or_update();

    return $self->get_perf_creative($creative_id, $user->id);
}

sub update_perf_creative {
    my ($self, $creative, $params) = @_;
    $params //= {};

    if (defined $params->{moderate_send_time} && $params->{moderate_send_time} eq '_in_past') {
        my @time = localtime();
        $time[5]--; # Previous year
        $creative->moderate_send_time(strftime('%Y-%m-%d %H:%M:%S', @time));
        delete $params->{moderate_send_time};
    }

    $creative->$_($params->{$_}) for keys %$params;
    Direct::Model::Creative::Manager->new(items => [$creative])->create_or_update();

    return $self->get_perf_creative($creative->id);
}

sub create_canvas_creative {
    my ($self, $params, %options) = @_;
    $params //= {};

    my $user = $self->has_user ? $self->user : undef;
    $user //= $params->{user_id} ? $self->get_user($params->{user_id}) : $self->create_user();

    my $creative_id = 1 + (get_one_field_sql(PPCDICT, "SELECT MAX(creative_id) FROM shard_creative_id") // 0);
    my $creative = Direct::Model::CanvasCreative->new(
        id => $creative_id,
        stock_creative_id => $creative_id,
        client_id => $user->client_id,
        name => "Canvas creative #${creative_id}",
        width => '300',
        height => '250',
        preview_url => 'https://avatars.mds.yandex.net/get-media-adv-screenshooter/41244/f670932e-49e2-4717-abec-85bab87d9cba/orig',
        status_moderate => 'New',
        business_type => 'retail',
        ( map { $_ => undef } qw/href alt_text template_id version _mod_reason_yaml group_create_time theme_id layout_id creative_group_id group_name live_preview_url moderation_comment duration is_adaptive has_packshot is_bannerstorage_predeployed/),
        %$params,
    );

    Direct::Model::CanvasCreative::Manager->new(items => [$creative])->create_or_update();

    return (@{$self->get_canvas_creatives($creative_id)})[0];
}

sub get_canvas_creatives {
    my ($self, $creative_ids) = @_;
    return Direct::CanvasCreatives->get_by(creative_id => $creative_ids, $self->user->id)->items;
}

#########################
#   Performance Feeds   #
#########################

sub get_perf_feed {
    my ($self, $client_id, $feed_id) = @_;
    return $self->get_perf_feeds($client_id, id => $feed_id)->[0];
}

sub get_perf_feeds {
    my ($self, $client_id, %options) = @_;
    return Direct::Feeds->get_by($client_id, %options)->items;
}

sub create_perf_feed {
    my ($self, $params, %options) = @_;
    $params //= {};

    if (!defined $params->{client_id}) {
        my $user = $self->has_user ? $self->user : undef;
        $user //= $params->{user_id} ? $self->get_user($params->{user_id}) : $self->create_user();
        $params->{client_id} = $user->client_id;
    }

    #my $feed_id = get_new_id('feed_id');
    my $feed = Direct::Model::Feed->new(
        source => 'url',
        name => "Test Feed",
        url => undef,
        refresh_interval => 86400,
        last_change => strftime('%Y-%m-%d %H:%M:%S', localtime()),
        %$params,
    );

    Direct::Model::Feed::Manager->new(items => [$feed])->create();

    return $self->get_perf_feed($params->{client_id}, $feed->id);
}

###########################
#   Performance Filters   #
###########################

sub get_perf_filter {
    my ($self, $perf_filter_id, %options) = @_;
    return $self->get_perf_filters(perf_filter_id => $perf_filter_id, %options, shard => $self->shard)->[0];
}

sub get_perf_filters { shift->get_perf_filters_obj(@_)->items }
sub get_perf_filters_obj {
    my ($self, $key, $vals, %options) = @_;
    return Direct::PerformanceFilters->get_by($key, $vals, %options);
}

sub create_perf_filter {
    my ($self, $params, %options) = @_;
    $params //= {};

    my $adgroup = $self->has_adgroup ? $self->adgroup : undef;
    $adgroup //= $params->{adgroup_id} ? $self->get_adgroup($params->{adgroup_id}) : $self->create_adgroup('performance');

    my $last_change_in_past;
    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $last_change_in_past = 1;
        delete $params->{last_change};
    }

    my $perf_filter_id = get_new_id('perf_filter_id');
    my $perf_filter = Direct::Model::PerformanceFilter->new(
        id => $perf_filter_id,
        adgroup_id => $adgroup->id,
        campaign_id => $adgroup->campaign_id,
        filter_name => "Test filter #${perf_filter_id}",
        price_cpc => 0 + sprintf("%.2f", rand(5)),
        price_cpa => 0 + sprintf("%.2f", rand(3)),
        target_funnel => 'same_products',
        condition => [
            Direct::Model::PerformanceFilter::Rule->new(filter_type => 'performance', 
                field => 'name', relation => '==', value => ["product#${perf_filter_id}"]),
            Direct::Model::PerformanceFilter::Rule->new(filter_type => 'performance',
                field => 'price', relation => '>', value => [0 + sprintf("%.2f", rand(10))]),
        ],
        ret_cond_id => undef,
        %$params,
        filter_type => 'performance',
    );

    if ($last_change_in_past) {
        $perf_filter->set_db_column_value('bids_performance', 'LastChange', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
    } else {
        $perf_filter->last_change('now');
    }

    Direct::Model::PerformanceFilter::Manager->new(items => [$perf_filter])->create();

    return $self->get_perf_filter($perf_filter->id, with_additional => 1, with_deleted => 1);
}

################
#   Keywords   #
################

sub get_keyword {
    my ($self, $keyword_id, %options) = @_;
    return $self->get_keywords(keyword_id => $keyword_id, %options, shard => $self->shard)->[0];
}

sub get_keywords { shift->get_keywords_logic(@_)->items }
sub get_keywords_logic {
    my ($self, $key, $vals, %options) = @_;
    return Direct::Keywords->get_by($key, $vals, %options);
}

sub create_keyword {
    my ($self, $params, %options) = @_;
    $params //= {};

    my $adgroup = $self->has_adgroup ? $self->adgroup : undef;
    $adgroup //= $params->{adgroup_id} ? $self->get_adgroup($params->{adgroup_id}) : $self->create_adgroup('performance');

    my $last_change_in_past;
    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $last_change_in_past = 1;
        delete $params->{last_change};
    }

    my $keyword_id = get_new_id('phid');
    my $keyword = Direct::Model::Keyword->new(
        id => $keyword_id,
        adgroup_id => $adgroup->id,
        campaign_id => $adgroup->campaign_id,
        text => "Тестовая фраза ${keyword_id}",
        price => 0 + sprintf("%.2f", rand(5)),
        price_context => 0 + sprintf("%.2f", rand(3)),
        %$params,
    );

    my $props = PhraseText::get_phrase_props($keyword->text);
    $keyword->normalized_text($props->{norm_phrase});
    $keyword->words_count($props->{numword});

    if ($last_change_in_past) {
        $keyword->set_db_column_value('bids', 'modtime', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
    } else {
        $keyword->last_change('now');
    }

    Direct::Model::Keyword::Manager->new(items => [$keyword])->create();

    return $self->get_keyword($keyword->id);
}

##############################
#   Retargeting Conditions   #
##############################

sub get_ret_cond {
    my ($self, $ret_cond_id, %options) = @_;
    return $self->get_ret_conds(ret_cond_id => $ret_cond_id, %options, shard => $self->shard)->[0];
}

sub get_ret_conds { shift->get_ret_conds_logic(@_)->items }
sub get_ret_conds_logic {
    my ($self, $key, $vals, %options) = @_;
    return Direct::RetargetingConditions->get_by($key, $vals, %options);
}

sub get_ret_cond_goals {
    my ($self, $ret_cond_id) = @_;
    return get_all_sql(PPC(shard => $self->shard), [
        "SELECT goal_id, goal_type, is_accessible FROM retargeting_goals", WHERE => {ret_cond_id => $ret_cond_id},
    ]);
}

sub new_ret_cond_model {
    my ($self, %params) = @_;
    my $ret_cond_id = delete($params{id}) || 0;
    state $ver = 0; $ver++;
    return Direct::Model::RetargetingCondition->new(
        id => $ret_cond_id,
        (ref($self) && $self->has_user ? (client_id => $self->user->client_id) : ()),
        condition_name => "ret_cond_${ret_cond_id} v${ver}",
        condition_desc => "Test retargeting condition ${ret_cond_id} v${ver}",
        condition => [{
            type => 'all',
            goals => [
                map {
                    +{goal_id => 1000 * $_ + 1 + int(rand(999)), time => 1 + int(rand(30))}
                } 0 .. (int(rand(9)) + 1)
            ],
        }],
        is_deleted => 0,
        is_accessible => 1,
        %params,
    );
}

sub create_ret_cond {
    my ($self, $params, %options) = @_;
    $params //= {};

    if (!defined $params->{client_id}) {
        my $user = $self->has_user ? $self->user : undef;
        $user //= $params->{user_id} ? $self->get_user($params->{user_id}) : $self->create_user();
        $params->{client_id} = $user->client_id;
    }

    my $last_change_in_past;
    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $last_change_in_past = 1;
        delete $params->{last_change};
    }

    my $ret_cond_id = get_new_id('ret_cond_id', ClientID => $params->{client_id});
    my $ret_cond = $self->new_ret_cond_model(id => $ret_cond_id, %$params);

    if ($last_change_in_past) {
        $ret_cond->set_db_column_value('retargeting_conditions', 'modtime', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
    } else {
        $ret_cond->last_change('now') if !$ret_cond->has_last_change;
    }

    Direct::Model::RetargetingCondition::Manager->new(items => [$ret_cond])->create();

    return $self->get_ret_cond($ret_cond->id, with_deleted => 1);
}

########################
#         Bids         #
########################


sub new_bid_relevance_match_model {
    my ($self, %params) = @_;
    return Direct::Model::BidRelevanceMatch->new(
        (ref($self) && $self->has_adgroup ? (adgroup_id => $self->adgroup->id) : ()),
        (ref($self) && $self->has_campaign ? (campaign_id => $self->campaign->id) : ()),
        price => '1.23',
        autobudget_priority => undef,
        is_suspended => 0,
        %params,
    );
}

sub get_bid {
    my ($self, $bid_id, %params) = @_;
    return Direct::Bids->get_by(bid_id => $bid_id, shard => $self->shard, %params)->items->[0];
}

sub create_bid {
    my ($self, $bid_type, $params) = @_;
    $params //= {};

    my $adgroup = $self->has_adgroup ? $self->adgroup : undef;
    $adgroup //= $params->{adgroup_id} ? $self->get_adgroup($params->{adgroup_id}) : $self->create_adgroup('text');

    my $bid;
    if ($bid_type eq 'relevance_match') {
        $bid = $self->new_bid_relevance_match_model(
            adgroup_id => $adgroup->id, campaign_id => $adgroup->campaign_id, %$params
        );
    } else {
        die "unsupport bid_type: $bid_type";
    }
    $bid->last_change('now');

    ($bid->manager_class)->new(items => [$bid])->create();
    return $self->get_bid($bid->id, bid_type => $params->{bid_type});
}

########################
#   Retargeting Bids   #
########################

sub get_ret_bid {
    my ($self, $ret_id, %options) = @_;
    return $self->get_ret_bids(ret_id => $ret_id, %options, shard => $self->shard)->[0];
}

sub get_ret_bids { shift->get_ret_bids_logic(@_)->items }
sub get_ret_bids_logic {
    my ($self, $key, $vals, %options) = @_;
    return Direct::Retargetings->get_by($key, $vals, %options);
}

sub new_ret_bid_model {
    my ($self, %params) = @_;
    my $ret_id = delete($params{id}) || 0;
    return Direct::Model::Retargeting->new(
        id => $ret_id,
        (ref($self) && $self->has_adgroup ? (adgroup_id => $self->adgroup->id) : ()),
        (ref($self) && $self->has_campaign ? (campaign_id => $self->campaign->id) : ()),
        price_context => '1.23',
        autobudget_priority => undef,
        is_suspended => 0,
        %params,
    );
}

sub create_ret_bid {
    my ($self, $params, %options) = @_;
    $params //= {};

    my $adgroup = $self->has_adgroup ? $self->adgroup : undef;
    $adgroup //= $params->{adgroup_id} ? $self->get_adgroup($params->{adgroup_id}) : $self->create_adgroup('text');

    my $last_change_in_past;
    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $last_change_in_past = 1;
        delete $params->{last_change};
    }

    my $ret_id = get_new_id('ret_id');
    my $ret_cond_id = $params->{ret_cond_id} || $self->create_ret_cond()->id;
    my $ret = $self->new_ret_bid_model(
        id => $ret_id, ret_cond_id => $ret_cond_id,
        adgroup_id => $adgroup->id, campaign_id => $adgroup->campaign_id, %$params,
    );

    if ($last_change_in_past) {
        $ret->set_db_column_value('bids_retargeting', 'modtime', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
    } else {
        $ret->last_change('now') if !$ret->has_last_change;
    }

    Direct::Model::Retargeting::Manager->new(items => [$ret])->create();

    return $self->get_ret_bid($ret->id);
}

##########################
#   Dynamic Conditions   #
##########################

sub create_dyn_condition {
    my ($self, %params) = @_;

    my $user = $self->user();
    my $adgroup = $self->has_adgroup ? $self->adgroup : undef;
    $adgroup //= $params{adgroup_id} ? $self->get_adgroup($params{adgroup_id}) : $self->create_adgroup('dynamic');

    my $campaign_hash = Models::Campaign::get_user_camp_gr($user->id(), $adgroup->campaign_id(), {no_groups => 1, without_multipliers => 1});
    my $smart = Direct::AdGroups2::Smart->new(campaign => $campaign_hash,);
    my $user_data = {
        adgroup_id => 0,
        condition => [{type => "any"}],
        condition_name => "Все страницы",
        dyn_id => "",
        price => 3,
        price_context => 3,
        %params,
    };
    my $dynamic_conditions = $smart->prepare_dyn_conds_from_user_data($adgroup, [$user_data]);
    Direct::DynamicConditions->new($dynamic_conditions)->create();

    return $dynamic_conditions->[0];
}


########################
#   Target Interests   #
########################


sub new_target_interest_model {
    my ($self, %params) = @_;
    my $ret_id = delete($params{id}) || 0;
    return Direct::Model::TargetInterest->new(
        id => $ret_id,
        (ref($self) && $self->has_adgroup ? (adgroup_id => $self->adgroup->id) : ()),
        (ref($self) && $self->has_campaign ? (campaign_id => $self->campaign->id) : ()),
        price_context => '1.23',
        autobudget_priority => 1,
        is_suspended => 0,
        adgroup => $self->has_adgroup ? $self->adgroup : undef,
        %params,
    );
}


###############
#   Banners   #
###############

sub get_banner {
    my ($self, $banner_id, %options) = @_;
    return $self->get_banners(banner_id => $banner_id, %options)->[0];
}

sub get_banners {
    my ($self, $key, $vals, %options) = @_;
    return Direct::Banners->get_by($key, $vals, %options)->items;
}

sub create_banner {
    my ($self, $type, $params, %options) = @_;
    $params //= {};

    state $banner_type2class = {
        text => 'Direct::Model::BannerText',
        dynamic => 'Direct::Model::BannerDynamic',
        performance => 'Direct::Model::BannerPerformance',
        cpm_banner => 'Direct::Model::BannerCpmBanner',
        mobile_content => 'Direct::Model::BannerMobileContent',
    };

    my $adgroup;
    $adgroup = $self->has_adgroup ? $self->adgroup : undef unless $options{with_new_adgroup};
    $adgroup //= $params->{adgroup_id} ? $self->get_adgroup($params->{adgroup_id}) : $self->create_adgroup($type // $options{with_new_adgroup});

    $type //= $adgroup->adgroup_type;
    $type = 'text' if $type eq 'base';

    if ($type eq 'performance') {
        if (!defined $params->{creative_id}) {
            # PerfCreative required: user
            my $self_creative = $self->has_user
                ? $self
                : $self->new(shard => $self->shard, user => $self->get_user($self->get_campaign($adgroup->campaign_id)->user_id));
            $params->{creative_id} = $self_creative->create_perf_creative()->id;
        }
    }

    my $last_change_in_past;
    if (defined $params->{last_change} && $params->{last_change} eq '_in_past') {
        $last_change_in_past = 1;
        delete $params->{last_change};
    }

    my $banner_id = get_new_id('bid', cid => $adgroup->campaign_id);
    if ($type eq 'cpm_banner') {
        if (!defined $params->{creative_id}) {
            my $self_creative = $self->has_user
                ? $self
                : $self->new(shard => $self->shard, user => $self->get_user($self->get_campaign($adgroup->campaign_id)->user_id));
            $params->{creative_id} = $self_creative->create_canvas_creative()->id;
        }

        $params->{creative} = Direct::Model::BannerCreative->new(
            creative_id => delete $params->{creative_id},
            campaign_id => $adgroup->campaign_id,
            adgroup_id => $adgroup->id,
            banner_id => $banner_id,
        ),
    }

    my $measurers = delete $params->{measurers};

    my $banner_class = $banner_type2class->{$type};
    my $banner = $banner_class->new(
        id => $banner_id,
        adgroup_id => $adgroup->id,
        campaign_id => $adgroup->campaign_id,
        ($banner_class->is_title_supported ? (title => "Banner ${banner_id} title") : ()),
        ($banner_class->is_body_supported ? (body => "Banner ${banner_id} body") : ()),
        ($banner_class->is_href_supported ? (href => "https://ya.ru/banner${banner_id}", domain => "ya.ru") : ()),
        ($type eq 'mobile_content' ? (reflected_attrs => 'icon') : ()),
        %$params,
    );

    if ($last_change_in_past) {
        $banner->set_db_column_value('banners', 'LastChange', "DATE_SUB(now(), interval 1 year)", dont_quote => 1);
    }

    $banner->manager_class->new(items => [$banner])->create();

    if ($measurers) {
        do_mass_insert_sql(
            PPC(bid => $banner_id),
	    'INSERT INTO banner_measurers(bid, measurer_system, params) VALUES %s',
            [map { [$banner_id, $_->measurer_system, $_->params] } @$measurers]);
    }

    return $self->get_banner($banner->id, $type eq 'cpm_banner' ? (with_pixels => 1) : ());
}

#################
#   Sitelinks   #
#################

sub get_sitelinks_set {
    my ($self, $sitelinks_set_id) = @_;

    my @select_columns = (
        Direct::Model::SitelinksSet->get_db_columns(sitelinks_sets => 'sls', prefix => ''),
        Direct::Model::Sitelink->get_db_columns(sitelinks_links => 'sl', prefix => ''),
        "sls2l.order_num"
    );

    my @tables = (
        "sitelinks_sets sls",
        "JOIN sitelinks_set_to_link sls2l ON (sls2l.sitelinks_set_id = sls.sitelinks_set_id)",
        "JOIN sitelinks_links sl ON (sl.sl_id = sls2l.sl_id)"
    );

    my $rows = get_all_sql(PPC(shard => $self->shard), [
        sprintf("SELECT %s FROM %s", join(', ', @select_columns), join(' ', @tables)),
        where => {
            'sls.sitelinks_set_id' => $sitelinks_set_id,
        },
        "ORDER BY sls.sitelinks_set_id, sls2l.order_num",
    ]);
    return if !@$rows;

    my $sitelinks_set = Direct::Model::SitelinksSet->from_db_hash($rows->[0], \{});
    $sitelinks_set->links(Direct::Model::Sitelink->from_db_hash_multi($rows));

    return $sitelinks_set;
}

sub create_sitelinks_set {
    my ($self, $params, %options) = @_;
    $params //= {};

    if (!defined $params->{client_id}) {
        my $user = $self->has_user ? $self->user : undef;
        $user //= $params->{user_id} ? $self->get_user($params->{user_id}) : $self->create_user();
        $params->{client_id} = $user->client_id;
    }

    my $sitelinks_set = Direct::Model::SitelinksSet->new(
        links => [
            Direct::Model::Sitelink->new(title => "Sitelink 1", description => undef, href => "http://ya.ru/sitelink/1"),
            Direct::Model::Sitelink->new(title => "Sitelink 2", description => undef, href => "http://ya.ru/sitelink/2"),
            Direct::Model::Sitelink->new(title => "Sitelink 3", description => undef, href => "http://ya.ru/sitelink/3"),
        ],
        %$params,
    );

    Direct::Model::SitelinksSet::Manager->new(items => [$sitelinks_set])->save();

    return $sitelinks_set;
};

sub create_video_resource {
    my ($self, $params) = @_;
    $params //= {};
    return Direct::Model::MediaResource->new(
        resource_type => $params->{resource_type} // 'video',
        yacontextCategories => $params->{yacontextCategories},
    );

}

#####################
#   Turbolandings   #
#####################

sub get_turbolanding {
    my ($self, $client_id, $tl_id, %options) = @_;
    return Direct::TurboLandings->get_by(client_id => $client_id, id => $tl_id, %options)->items->[0];
}

sub create_turbolanding {
    my ($self, $params, %options) = @_;
    $params //= {};

    my $client_id = $params->{client_id};
    unless ($client_id){
        my $user = $params->{user_id} ? $self->get_user($params->{user_id}) :
            $self->has_user ? $self->user : $self->create_user();
        $client_id = $user->client_id;
    }
    
    my $tl_id = $params->{tl_id} // $params->{id} // int(1+rand(1000000));
    do_insert_into_table(PPC(ClientID => $client_id), 'turbolandings',
        {
            tl_id    => $tl_id,
            ClientID => $client_id,
            name     => $params->{name}  // 'тестовая турбо-страница',
            href     => $params->{href}  // 'https://yandex.ru/turbo?aaaa=bbbb',
            metrika_counters_json => $params->{metrika_counters_json} // '[]',
        }
    );

    return $self->get_turbolanding($client_id, $tl_id);
}

######################################################

sub create_tables {
    my ($class) = @_;

    $Settings::SHARDS_NUM = $Yandex::DBUnitTest::SHARDS_NUM;

    # Tables were created somewhere outside ?
    if (@{get_all_sql(PPCDICT, "show tables like 'domains_dict'")} > 0) {
        return if get_one_field_sql(PPCDICT, "SELECT 1 FROM domains_dict WHERE domain = 'direct_dbojects.tables'");
    }

    my @ppcdict_tables = Yandex::DBSchema::get_tables('ppcdict');
    @ppcdict_tables = grep {!/^QRTZ/} @ppcdict_tables;
    my @ppc_tables = grep { !/^(?:users_notifications.*)$/ } Yandex::DBSchema::get_tables('ppc');

    init_test_dataset({
        (map { $_, { original_db => PPCDICT, like => $_ } } @ppcdict_tables),
        (map { $_, { 
            original_db => PPC(shard => 'all'), 
            like => $_, 
            ($TABLE_ENGINE{$_} ? (engine => $TABLE_ENGINE{$_}) : ()) 
               } 
         } @ppc_tables),
    });

    do_sql(PPCDICT, qq{
        INSERT IGNORE INTO `products` VALUES
            (1475,'Директ',0,'text',NULL,1.000000,'YND_FIXED',1,'Bucks',7,1,NULL,NULL,'','','clicks',1,'cpc'),
            ($PRODUCT_TEXT_RUB,'Рублевый Директ',0,'text',NULL,1.000000,'RUB',0,'Bucks',7,1,NULL,NULL,'','','clicks',1,'cpc'),
            (503163,'Долларовый Директ',0,'text',NULL,1.000000,'USD',0,'Bucks',7,1,NULL,NULL,'','','clicks',1,'cpc'),
            (508569,'Директ, cpm_banner',0,'cpm_banner',NULL,1.000000,'YND_FIXED',1,'Bucks',7, 1, NULL, NULL,'','','shows',1,'cpm'),
            ($PRODUCT_CPM_BANNER_RUB,'Директ, cpm_banner',0,'cpm_banner',NULL,1.000000,'RUB',1,'Bucks',7, 1, NULL, NULL,'','','shows',1,'cpm'),
            ($PRODUCT_CPM_VIDEO_RUB,'Директ, cpm_video',0,'cpm_video',NULL,1.000000,'RUB',1,'Bucks',7, 1, NULL, NULL,'','','shows',1,'cpm')
    });

    my $logtime = strftime("%Y%m%d%H%M%S", localtime(int(time())));
    my $today = mysql_round_day($logtime);
    my $tomorrow = tomorrow($today);

    do_sql(PPCDICT, "INSERT INTO domains_dict (domain) VALUES ('direct_dbojects.tables')");

    return;
}

sub sql {
    my $self = shift;
    return do_sql(PPC(shard => $self->shard), @_);
}

######################################################

sub sandbox($&) { my ($model, $code) = @_; local $_ = $model->clone; $code->(); }

sub ok_func_is_called($&) {
    my ($func_path, $code) = @_;

    my $is_called;
    my $orig_func = \&$func_path;
    no warnings 'redefine';
    no strict 'refs';
    local *$func_path = sub { $is_called = 1; $orig_func->(@_) };
    use strict 'refs';
    use warnings 'redefine';

    my $builder = Test::Builder->new();
    $code->();
    $builder->ok($is_called, "Is `$func_path` called");
}

sub cmp_model_with($$;%) {
    my ($expected_model, $got_model, %options) = @_;

    my $expected = $expected_model->to_hash;
    delete $expected->{$_} for @{$options{exclude} // []};

    my $got = hash_cut $got_model->to_hash, keys %$expected;

    my $builder = Test::Builder->new();
    my ($ok, $stack) = cmp_details($got, $expected);
    if (!$builder->ok($ok)) {
        my $diag = deep_diag($stack);
        $builder->diag($diag);
    }
}

sub ok_vr($;$) {
    my ($vr) = @_;
    my $builder = Test::Builder->new();
    $builder->ok($vr->is_valid);
}

sub err_vr {
    my ($vr, $errors) = @_;
    my $builder = Test::Builder->new();
    if ($builder->ok(!$vr->is_valid)) {
        my $vr_errors = $vr->get_errors;
        $errors = [$errors] if ref($errors) ne 'ARRAY';
        $builder->ok(
            @$errors == @$vr_errors &&
            (all { my $e = $vr_errors->[$_]; $e->name.($e->suffix ? '_'.$e->suffix : '') eq $errors->[$_]  } 0..$#$errors)
        );
    }
}

1;
