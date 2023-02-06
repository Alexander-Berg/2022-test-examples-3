#!/usr/bin/perl

use Direct::Modern;
use Test::More;
use Test::Deep;

use Direct::Model::Campaign;
use Yandex::DBUnitTest qw/:all/;
use Yandex::ListUtils qw/xsort/;
use Settings;

BEGIN {
    use_ok('Direct::Creatives', qw/search_performance_creatives/);
    use_ok('Direct::CanvasCreatives');
}

package Test::Direct::Model::Creative {
    use Mouse;
    extends 'Direct::Model::Creative';
    with 'Direct::Model::Creative::Role::UsedInCamps';
    1;
}

init_test_dataset(construct_test_dataset());

my $uid = 1;

#отфильтровываем canvas креативы
my $creatives_1 = Direct::Creatives->get_by(creative_id => [1, 508], $uid)->items;
my $ex_creatives_1 = to_model_creatives(
    {creative_id => 1, client_id => 1, name => 'Общий список', width => 240, height => 400,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
        preview_url => '//avatars.mds.yandex.net/HFJF7373JUE',
        statusModerate => 'New', template_id => 302}
);
cmp_creatives($creatives_1, $ex_creatives_1);

my $canvas_creatives = Direct::CanvasCreatives->get_by(creative_id => [1, 508], $uid)->items;
is(scalar @$canvas_creatives, 1);
if (@$canvas_creatives > 0) {
    is(ref $canvas_creatives->[0], 'Direct::Model::CanvasCreative');
    is($canvas_creatives->[0]->id, 508);
}

# креативы не принадлежат пользователю
my $creatives_2 = Direct::Creatives->get_by(creative_id => [5, 6, 7], $uid)->items;
cmp_deeply($creatives_2, []);

my ($search_cr3) = search_performance_creatives(12, [], page => 1, per_page => 1, filter => [ { filter => 'name', value => 'Много товаров' } ] );
my $ex_creatives_3 = to_model_creatives(
    {creative_id => 6, client_id => 99, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
        preview_url => '//avatars.mds.yandex.net/HYY712E',
        statusModerate => 'Yes', template_id => 901},
);
is($search_cr3->{total_count}, 2);
cmp_creatives($search_cr3->{creatives}, $ex_creatives_3);

my ($search_cr4) = search_performance_creatives(12, [], page => 2, per_page => 1, filter => [ { filter => 'name', value => 'Много товаров' } ] );
my $ex_creatives_4 = to_model_creatives(
    {creative_id => 9, client_id => 99, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
        preview_url => '//avatars.mds.yandex.net/HYY712E',
        statusModerate => 'No', template_id => 302},
);
is($search_cr4->{total_count}, 2);
cmp_creatives($search_cr4->{creatives}, $ex_creatives_4);

{
    my $search = search_performance_creatives(12, [], page => 1, per_page => 5, filter => [
        { filter => 'name', value => 5 }
    ]);
    is(scalar @{$search->{creatives}}, 1);
    is($search->{creatives}->[0]->id, 5);
}

{
    my $search = search_performance_creatives(5, [], page => 1, per_page => 5, filter => [
        { filter => 'status_moderate', value => 'Wait' }
    ]);
    is(scalar @{$search->{creatives}}, 1);
    is($search->{creatives}->[0]->status_moderate, 'Sending');
}

{
    my $search = search_performance_creatives(5, [], page => 1, per_page => 5, filter => [
        { filter => 'size', value => '666x777' }
    ]);
    is(scalar @{$search->{creatives}}, 1);
    is($search->{creatives}->[0]->width, 666);
    is($search->{creatives}->[0]->height, 777);
}

{
    my $search = search_performance_creatives(12, [], page => 1, per_page => 5, filter => [
        { filter => 'campaigns', value => 1 }
    ]);
    is(scalar @{$search->{creatives}}, 2);
    is($search->{creatives}->[0]->id, 6);
    is($search->{creatives}->[1]->id, 9);
}

my ($seach_cr5) = Direct::Creatives::_get_creatives(12, {}, per_page => 100, page => 1, with_campaigns => 1, mask_obsolete_creatives => 1);
my $ex_creatives_5 = to_model_creatives(
    {creative_id => 5, ClientID => 99, name => 'Один товар крупно с описанием и каруселью', width => 240, height => 400,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/",
        preview_url => '//avatars.mds.yandex.net/5555GHE',
        statusModerate => 'Yes', 
        used_in_camps => [], template_id => 800},
    {creative_id => 6, ClientID => 99, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
        preview_url => '//avatars.mds.yandex.net/HYY712E',
        statusModerate => 'Yes',
        used_in_camps => [
            {cid => 2, name => 'окна rehau'},
            {cid => 4, name => 'Системы публикации'}
        ],
        template_id => 901
    },
    {creative_id => 7, ClientID => 99, name => 'Адаптив, смарт-плитка 1x2', width => 300, height => 300,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
        preview_url => '//avatars.mds.yandex.net/HYY712E',
        statusModerate => 'No',
        used_in_camps => [], template_id => 740
    },
    {creative_id => 8, ClientID => 99, name => 'Адаптив, смарт-плитка 1x3', width => 160, height => 600,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
        preview_url => '//avatars.mds.yandex.net/HYY712E',
        statusModerate => 'Yes',
        used_in_camps => [], template_id => 750
    },
    {creative_id => 9, ClientID => 99, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
        preview_url => '//avatars.mds.yandex.net/HYY712E',
        statusModerate => 'Yes',
        used_in_camps => [
            {cid => 2, name => 'окна rehau'},
            {cid => 4, name => 'Системы публикации'},
            {cid => 3, name => 'НТЦ  "Техноком АС"'}
        ],
        template_id => 302
    },
);
is($seach_cr5->{total_count}, 5);

cmp_creatives($seach_cr5->{creatives}, $ex_creatives_5);

{
    my $creatives = Direct::Creatives::_get_creatives(1, { creative_group_id => 1 }, per_page => 100, page => 1, group => 1);
    is(@{$creatives->{groups}}, 1, '1 group found');
    is($creatives->{groups}->[0]->group_creatives_count, 2, '2 creatives in group');
    cmp_deeply($creatives->{groups}->[0]->creatives_data, [ { id => 101, business_type => 'retail', theme_id => undef }, { id => 102, business_type => 'auto', theme_id => undef } ]);
}

{
    my $creatives = Direct::Creatives::_get_creatives(12, {}, per_page => 100, page => 1, get_status_moderate_stats => 1, mask_obsolete_creatives => 1);
    is($creatives->{status}->{accepted}, 4);
    is($creatives->{status}->{rejected}, 1);
    is($creatives->{status}->{all}, 5);
}

{
    my $creatives = Direct::Creatives::_get_creatives(5, { creative_group_id => 2 }, per_page => 5, page => 1);
    ok(scalar @{$creatives->{creatives}}. 5);
    cmp_deeply([map { $_->{id} } @{$creatives->{other_ids}}], [206 .. 209]);
    $creatives = Direct::Creatives::_get_creatives(5, { creative_group_id => 2 }, per_page => 5, page => 2);
    ok(scalar @{$creatives->{creatives}}. 4);
    cmp_deeply($creatives->{other_ids}, []);
}

done_testing;

{
sub creative_to_hash {
    my ($model, $with_camp) = @_;
    my $hash = { map { ($_ => $model->$_) } qw/id name width height alt_text href preview_url status_moderate/};
    if ($with_camp) {
        $hash->{used_in_camps} = [xsort { $_->{cid} } map { +{cid => $_->id, name => $_->campaign_name} } @{$model->used_in_camps}];
    }
    return $hash; 
}

sub to_model_creatives {
    my @creatives;
    my $with_camp = 0;
    for my $creative (@_) {
        if (exists $creative->{used_in_camps}) {
            $creative->{used_in_camps} = [map { Direct::Model::Campaign->from_db_hash($_, \{}) } @{$creative->{used_in_camps}}];
            $with_camp = 1;  
        }
        push @creatives, $creative;
    }
    return $with_camp ? Test::Direct::Model::Creative->from_db_hash_multi(\@creatives) : Direct::Model::Creative->from_db_hash_multi(\@creatives);
}

sub cmp_creatives {
    my ($got, $expected, $test_name) = @_;
    my $with_camp = scalar grep { $_->does('Direct::Model::Creative::Role::UsedInCamps') } @$got, @$expected;
    cmp_deeply([map { creative_to_hash($_, $with_camp) } @$got], [map { creative_to_hash($_, $with_camp) } @$expected], $test_name);  
}
}

sub construct_test_dataset {
    {
        shard_client_id => {original_db => PPCDICT,
            rows => [
                {ClientID => 1, shard => 1},
                {ClientID => 99, shard => 1},
                {ClientID => 5, shard => 1},
            ]
        },
        shard_uid => {original_db => PPCDICT,
            rows => [
                {uid => 1, ClientID => 1},
                {uid => 12, ClientID => 99},
                {uid => 5, ClientID => 5},
            ]
        },
        
        perf_creatives => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {creative_id => 1, ClientID => 1, stock_creative_id => 1, name => 'Общий список', width => 240, height => 400,
                        creative_type => 'performance',
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
                        preview_url => '//avatars.mds.yandex.net/HFJF7373JUE',
                        statusModerate => 'New', template_id => 302},
                    {creative_id => 508, ClientID => 1, stock_creative_id => 508, name => 'Мойки ВД', width => 728, height => 90,
                        creative_type => 'canvas',
                        preview_url => 'https://canvas.yandex.ru/images/HHIEMMAPO8331SD.jpg',
                        statusModerate => 'Yes', template_id => undef},
                    {creative_id => 5, ClientID => 99, stock_creative_id => 5, name => 'Один товар крупно с описанием и каруселью', width => 240, height => 400,
                        creative_type => 'performance',
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/",
                        preview_url => '//avatars.mds.yandex.net/5555GHE',
                        statusModerate => 'Yes', template_id => 800, layout_id => 6},
                    {creative_id => 9, ClientID => 99, stock_creative_id => 9, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
                        creative_type => 'performance',
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
                        preview_url => '//avatars.mds.yandex.net/HYY712E',
                        statusModerate => 'No', template_id => 302, layout_id => 28},
                    {creative_id => 6, ClientID => 99, stock_creative_id => 6, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                        creative_type => 'performance',
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                        preview_url => '//avatars.mds.yandex.net/HYY712E',
                        statusModerate => 'Yes', template_id => 901, layout_id => 5},
                    {creative_id => 7, ClientID => 99, stock_creative_id => 7, name => 'Адаптив, смарт-плитка 1x2', width => 300, height => 300,
                        creative_type => 'performance',
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
                        preview_url => '//avatars.mds.yandex.net/HYY712E',
                        statusModerate => 'No', template_id => 740, layout_id => 46},
                    {creative_id => 8, ClientID => 99, stock_creative_id => 8, name => 'Адаптив, смарт-плитка 1x3', width => 160, height => 600,
                        creative_type => 'performance',
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                        preview_url => '//avatars.mds.yandex.net/HYY712E',
                        statusModerate => 'Yes', template_id => 750, layout_id => 47},
                    {
                        creative_id => 101, business_type => 'retail', ClientID => 1, stock_creative_id => 101, name => 'Creative group', width => 240, height => 400,
                        creative_type => 'performance', alt_text => '', preview_url => '', href => '',
                        statusModerate => 'New', template_id => 302, layout_id => 43, creative_group_id => 1, group_create_time => '2016-01-01 00:00:00',
                    },
                    {
                        creative_id => 102, business_type => 'auto', ClientID => 1, stock_creative_id => 102, name => 'Creative group', width => 240, height => 400,
                        creative_type => 'performance', alt_text => '', preview_url => '', href => '',
                        statusModerate => 'New', template_id => 302, layout_id => 44, creative_group_id => 1, group_create_time => '2016-01-01 00:00:00',
                    },
                    ( map { 
                        {
                            creative_id => 200 + $_, ClientID => 5, stock_creative_id => 200 + $_, name => 'Creative group', width => 240, height => (400 + $_*10),
                            creative_type => 'performance', alt_text => '', preview_url => '', href => '',
                            statusModerate => 'New', template_id => 302, layout_id => 43 + $_, creative_group_id => 2, group_create_time => '2016-01-01 00:00:00',
                            theme_id => $_*20,
                        },
                    } (1 .. 9) ),
                    {
                        creative_id => 103, business_type => 'auto', ClientID => 5, stock_creative_id => 103, name => 'on_mod', width => 666, height => 777,
                        creative_type => 'performance', alt_text => '', preview_url => '', href => '',
                        statusModerate => 'Sending', template_id => 302, layout_id => 45, creative_group_id => 3, group_create_time => '2016-01-01 00:00:00',
                    },
                ],
            }
        },
        campaigns => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {cid => 1, uid => 12, name => 'FENGON', archived => 'Yes', statusEmpty => 'No'},
                    {cid => 2, uid => 12, name => 'окна rehau', statusEmpty => 'No', archived => 'No'},
                    {cid => 3, uid => 12, name => 'НТЦ  "Техноком АС"', statusEmpty => 'No', archived => 'No'},
                    {cid => 4, uid => 12, name => 'Системы публикации', statusEmpty => 'No', archived => 'No'},
                    {cid => 55, uid => 12, name => 'Versant', statusEmpty => 'Yes', archived => 'No'},
                ],
            }
        },
        subcampaigns => {
            original_db => PPC(shard => 'all'),
        },
        banners => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {bid => 1, cid => 1},
                    {bid => 2, cid => 1},
                    {bid => 22, cid => 1},
                    {bid => 3, cid => 2},
                    {bid => 4, cid => 2},
                    {bid => 5, cid => 2},
                    {bid => 15, cid => 2},
                    
                    {bid => 666, cid => 55},
                    {bid => 700, cid => 4},
                    {bid => 701, cid => 4},
                    {bid => 702, cid => 3},
                    {bid => 703, cid => 4},
                ],
            },
        },
        banners_performance => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {banner_creative_id => 1, creative_id => 6, cid => 1, pid => 1, bid => 1},
                    {banner_creative_id => 2, creative_id => 6, cid => 1, pid => 2, bid => 2},
                    {banner_creative_id => 3, creative_id => 6, cid => 2, pid => 3, bid => 4},
                    {banner_creative_id => 4, creative_id => 6, cid => 2, pid => 4, bid => 5},
                    {banner_creative_id => 5, creative_id => 6, cid => 55, pid => 5, bid => 666},
                    {banner_creative_id => 6, creative_id => 6, cid => 4, pid => 6, bid => 701},

                    {banner_creative_id => 7, creative_id => 9, cid => 2, pid => 7, bid => 15},
                    {banner_creative_id => 8, creative_id => 9, cid => 4, pid => 8, bid => 703},
                    {banner_creative_id => 9, creative_id => 9, cid => 3, pid => 9, bid => 702},
                    {banner_creative_id => 10, creative_id => 9, cid => 1, pid => 10, bid => 22},
                ],
            },
        }, 
        users => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {uid => 1, ClientID => 1, login => 'unit-test'},
                    {uid => 12, ClientID => 99, login => 'creative-tests'},
                    {uid => 5, ClientID => 5, login => 'creatives-pages-test'},
                ],
            }
        },
    }
}

