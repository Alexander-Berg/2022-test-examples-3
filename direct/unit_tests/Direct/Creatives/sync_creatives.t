#!/usr/bin/perl

use Direct::Modern;
use Test::More;
use Test::Deep;

use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;
use Settings;
use BannerStorage;
use List::MoreUtils;

BEGIN {
    use_ok('Direct::Creatives');
}

init_test_dataset(construct_test_dataset());

my $sql = "SELECT creative_id, ClientID, stock_creative_id, name, width, height, alt_text, href, preview_url, statusModerate, template_id, version, duration FROM perf_creatives";

{
no warnings 'redefine';

*BannerStorage::Dict::get_dict = sub {
    my $dict = shift;
    return {} if $dict ne 'template';

    return {
        320 => {
            layouts => {
                23 => { themeId => 19, layoutId => 30, },
            },
            name => 'test template 1',
            type => undef,
            value => 320,
        },
        334 => {
            layouts => {
                21 => { themeId => 19, layoutId => 30, },
                22 => { themeId => 19, layoutId => 30, },
            },
            name => 'test template 1',
            type => undef,
            value => 334,
        },
    };
};

*BannerStorage::banner_storage_call = sub {
        return _fake_receive_creatives(
            original => \@_,
            dataset => 
                [
                    {
                        id => 9, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
                        thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                        status => _status_moderate2status('No'),
                        templateId => 334, version => 9, layoutCodeId => 21,
                        group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                        duration => 15,
                    },
                    {
                        id => 5, name => 'Крупная бытовая тех', width => 200, height => 250,
                        alt_text => "Холодильник.ру", href => "https://holodilnik.ru",
                        thumbnailUrl => '//avatars.mds.yandex.net/HF271HYe',
                        status => _status_moderate2status('Yes'),
                        templateId => 334, version => 2, layoutCodeId => 22,
                        group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                        duration => 15,
                    },
                    {
                        id => 7, name => 'Много товаров с одним описанием и мозаикой', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/900",
                        thumbnailUrl => '//avatars.mds.yandex.net/THUBNAIL',
                        screenshotUrl => '//avatars.mds.yandex.net/SCREENSHOT',
                        status => _status_moderate2status('Sent'),
                        templateId => 320, version => 3, layoutCodeId => 23,
                        group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                        duration => 15,
                    },
                    {
                        id => 1, name => 'Общий список', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
                        thumbnailUrl => '//avatars.mds.yandex.net/HFJF7373JUE',
                        status => _status_moderate2status('No'),
                        templateId => 320, version => 4, layoutCodeId => 23,
                        group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                        duration => 15,
                    },
                ],
        );
};
}

# update exists creatives
my $synced = Direct::Creatives::sync_creatives([{creative_id => 5, client_id => 99}, {creative_id => 7, client_id => 99}, {creative_id => 1, client_id => 1}, {creative_id => 9, client_id => 99}]);
my $got_creatives = get_all_sql(PPC(ClientID => 99), [$sql, WHERE => {creative_id => [5, 9]}]);
push @$got_creatives, @{get_all_sql(PPC(ClientID => 1), [$sql, WHERE => {creative_id => [1,7]}])};
is(get_one_field_sql(PPCDICT, "SELECT count(*) FROM shard_creative_id"), 5, 'Total creatives: not changed');

cmp_deeply(
    [sort { $a->{creative_id} <=> $b->{creative_id} } @$got_creatives],
    [
        {
            creative_id => 1, name => 'Общий список', width => 240, height => 400,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
            preview_url => '//avatars.mds.yandex.net/HFJF7373JUE',
            statusModerate => 'No', ClientID => 1,
            template_id => 320, version => 4,
            stock_creative_id => 1,
            duration => 15,
        },
        {
            creative_id => 5, name => 'Крупная бытовая тех', width => 200, height => 250,
            alt_text => "Холодильник.ру", href => "https://holodilnik.ru",
            preview_url => '//avatars.mds.yandex.net/HF271HYe',
            statusModerate => 'Yes', ClientID => 99,
            template_id => 334, version => 2,
            stock_creative_id => 5,
            duration => 15,
        },
        {
            creative_id => 7, name => 'Много товаров с одним описанием и мозаикой', width => 240, height => 400,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/900",
            preview_url => '//avatars.mds.yandex.net/SCREENSHOT',
            statusModerate => 'Sent', ClientID => 99,
            template_id => 320, version => 3,
            stock_creative_id => 7,
            duration => 15,
        },
        {
            creative_id => 9, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
            preview_url => '//avatars.mds.yandex.net/HYY712E',
            statusModerate => 'No', ClientID => 99,
            template_id => 334, version => 9,
            stock_creative_id => 9,
            duration => 15,
        },
    ],
    'Sync_creatives 1'
);

#проверяем, что создались задания на синхронизацию
is(get_one_field_sql(PPC(shard => 'all'), "SELECT count(*) FROM creative_banner_storage_sync WHERE sync_status='New'"), 4, 'Sync tasks for new and modifed creatives');
# Проверяем количество синхронизированных и пропущенных креативов
is(scalar(@$synced), 4, "Sync_creatives for waiting moderation: moderate, synced amount");

# update exists creatives
$synced = Direct::Creatives::sync_creatives([{creative_id => 5, client_id => 99}, {creative_id => 7, client_id => 99}, {creative_id => 1, client_id => 1}, {creative_id => 9, client_id => 99}]);
$got_creatives = get_all_sql(PPC(ClientID => 99), [$sql, WHERE => {creative_id => [5, 9]}]);
push @$got_creatives, @{get_all_sql(PPC(ClientID => 1), [$sql, WHERE => {creative_id => [1,7]}])};
is(get_one_field_sql(PPCDICT, "SELECT count(*) FROM shard_creative_id"), 5, 'Total creatives: not changed');

cmp_deeply(
    [sort { $a->{creative_id} <=> $b->{creative_id} } @$got_creatives],
    [
        {
            creative_id => 1, name => 'Общий список', width => 240, height => 400,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
            preview_url => '//avatars.mds.yandex.net/HFJF7373JUE',
            statusModerate => 'No', ClientID => 1,
            template_id => 320, version => 4,
            stock_creative_id => 1,
            duration => 15,
        },
        {
            creative_id => 5, name => 'Крупная бытовая тех', width => 200, height => 250,
            alt_text => "Холодильник.ру", href => "https://holodilnik.ru",
            preview_url => '//avatars.mds.yandex.net/HF271HYe',
            statusModerate => 'Yes', ClientID => 99,
            template_id => 334, version => 2,
            stock_creative_id => 5,
            duration => 15,
        },
        {
            creative_id => 7, name => 'Много товаров с одним описанием и мозаикой', width => 240, height => 400,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/900",
            preview_url => '//avatars.mds.yandex.net/SCREENSHOT',
            statusModerate => 'Sent', ClientID => 99,
            template_id => 320, version => 3,
            stock_creative_id => 7,
            duration => 15,
        },
        {
            creative_id => 9, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
            preview_url => '//avatars.mds.yandex.net/HYY712E',
            statusModerate => 'No', ClientID => 99,
            template_id => 334, version => 9,
            stock_creative_id => 9,
            duration => 15,
        },
    ],
    'Sync_creatives: moderated for all'
);
# Проверяем количество синхронизированных и пропущенных креативов
is(scalar(@$synced), 4, "Sync_creatives for all: moderate, synced amount");

# проверяем, что создались задания на синхронизацию
is(get_one_field_sql(PPC(shard => 'all'), "SELECT count(*) FROM creative_banner_storage_sync WHERE sync_status='New'"), 4, 'Sync tasks for new and modifed creatives');

# update and create creative

{
no warnings 'redefine';
*BannerStorage::banner_storage_call = sub {
    return _fake_receive_creatives(
        original => \@_,
        dataset => 
            [
                {
                    id => 6, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('No'),
                    templateId => 320, version => 8, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 500, name => 'Шины Goodyear', width => 200, height => 250,
                    alt_text => "www.pard.ru", href => "http://www.pard.ru",
                    thumbnailUrl => '',
                    status => _status_moderate2status('New'),
                    templateId => 320, version => 6, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 609, name => 'А ты уже купил хостинг', width => 200, height => 250,
                    alt_text => "ХЦ РБК в разгаре Супер-Лето", href => "hosting.rbc.ru/counter/referrer?refgrp=yandexsummer",
                    thumbnailUrl => '//avatars.mds.yandex.net/MMJ67',
                    status => _status_moderate2status('Yes'),
                    templateId => 334, version => 7, layoutCodeId => 21,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
            ]
    );
};
}

Direct::Creatives::sync_creatives([{creative_id => 6, client_id => 99}, {creative_id => 609, client_id => 1}, {creative_id => 500, client_id => 99}]);
my $got_creatives_2 = get_all_sql(PPC(ClientID => 99), [$sql, WHERE => {creative_id => [6, 500]}]);
push @$got_creatives_2, @{get_all_sql(PPC(ClientID => 1), [$sql, WHERE => {creative_id => [609]}])};
# + 2 creative
is(get_one_field_sql(PPCDICT, "SELECT count(*) FROM shard_creative_id"), 7, 'Total creatives: +2');

cmp_deeply(
    [sort { $a->{creative_id} <=> $b->{creative_id} } @$got_creatives_2],
    [
        {
            creative_id => 6, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
            preview_url => '//avatars.mds.yandex.net/HYY712E',
            statusModerate => 'No', ClientID => 99,
            template_id => 320, version => 8,
            stock_creative_id => 6,
            duration => 15,
        },
        {
            creative_id => 500, name => 'Шины Goodyear', width => 200, height => 250,
            alt_text => "www.pard.ru", href => "http://www.pard.ru",
            preview_url => '',
            statusModerate => 'New', ClientID => 99,
            template_id => 320, version => 6,
            stock_creative_id => 500,
            duration => 15,
        },
        {
            creative_id => 609, name => 'А ты уже купил хостинг', width => 200, height => 250,
            alt_text => "ХЦ РБК в разгаре Супер-Лето", href => "hosting.rbc.ru/counter/referrer?refgrp=yandexsummer",
            preview_url => '//avatars.mds.yandex.net/MMJ67',
            statusModerate => 'Yes', ClientID => 1,
            template_id => 334, version => 7,
            stock_creative_id => 609,
            duration => 15,
        },
    ],
    'Sync_creatives'
);

#проверяем, что создались задания на синхронизацию
is(get_one_field_sql(PPC(shard => 'all'), "SELECT count(*) FROM creative_banner_storage_sync WHERE sync_status='New'"), 7, 'Sync tasks for new and modifed creatives');

#проверяем синхронизацию скриншотов
{
no warnings 'redefine';
*BannerStorage::banner_storage_call = sub {
    return _fake_receive_creatives(
        original => \@_,
        dataset => 
            [
                # ничего не изменилось
                {
                    id => 9, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('No'),
                    templateId => 334, version => 1, layoutCodeId => 21,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                #должен установиться скриншот, остальные поля остаться неизменными
                {
                    id => 5, name => 'Крупная бытовая тех', width => 200, height => 250,
                    alt_text => "Холодильник.ру++", href => "https://h-o-l-o-d-ilnik.ru",
                    thumbnailUrl => '//avatars.mds.yandex.net/HF271HYe',
                    screenshotUrl => '//avatars.mds.yandex.net/NEW_SCREENSHOT',
                    status => _status_moderate2status('Yes'),
                    templateId => 334, version => 2, layoutCodeId => 22,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                # изменился statusModerate, должен игнорироваться
                {
                    id => 6, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('No'),
                    templateId => 320, version => 5, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                # а скриншот должен обновиться
                {
                    id => 1, name => 'Общий список', width => 240, height => 400,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/111111",
                    thumbnailUrl => '//avatars.mds.yandex.net/HFJF7373JUE',
                    screenshotUrl => '//avatars.mds.yandex.net/E_SCREENSHOT',
                    status => _status_moderate2status('No'),
                    templateId => 320, version => 4, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
            ],
    );
};
}

Direct::Creatives::sync_creative_screenshots([{creative_id => 6, client_id => 99}, {creative_id => 1, client_id => 1}, {creative_id => 5, client_id => 99}, {creative_id => 9, client_id => 99}]);
my $got_creatives_3 = get_all_sql(PPC(ClientID => 99), [$sql, WHERE => {creative_id => [6,5,9]}]);
push @$got_creatives_3, @{get_all_sql(PPC(ClientID => 1), [$sql, WHERE => {creative_id => [1]}])};
cmp_deeply(
    [sort { $a->{creative_id} <=> $b->{creative_id} } @$got_creatives_3],
    [
        {
            creative_id => 1, name => 'Общий список', width => 240, height => 400,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
            preview_url => '//avatars.mds.yandex.net/E_SCREENSHOT',
            statusModerate => 'No', ClientID => 1,
            template_id => 320, version => 4,
            stock_creative_id => 1,
            duration => 15,
        },
        {
            creative_id => 5, name => 'Крупная бытовая тех', width => 200, height => 250,
            alt_text => "Холодильник.ру", href => "https://holodilnik.ru",
            preview_url => '//avatars.mds.yandex.net/NEW_SCREENSHOT',
            statusModerate => 'Yes', ClientID => 99,
            template_id => 334, version => 2,
            stock_creative_id => 5,
            duration => 15,
        },

        {
            creative_id => 6, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
            preview_url => '//avatars.mds.yandex.net/HYY712E',
            statusModerate => 'No', ClientID => 99,
            template_id => 320, version => 8,
            stock_creative_id => 6,
            duration => 15,
        },
        {
            creative_id => 9, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
            alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
            preview_url => '//avatars.mds.yandex.net/HYY712E',
            statusModerate => 'No', ClientID => 99,
            template_id => 334, version => 9,
            stock_creative_id => 9,
            duration => 15,
        },

    ],
    'Sync_creatives: sync_creative_screenshots'
);


# update creative check banners statusBsSynced

{
    no warnings 'redefine';
    *BannerStorage::banner_storage_call = sub {
        return _fake_receive_creatives(
            original => \@_,
            dataset =>
            [
                {
                    id => 6, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('Yes'),
                    templateId => 320, version => 8, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 5, name => 'Крупная бытовая тех', width => 200, height => 250,
                    alt_text => "Холодильник.ру++", href => "https://h-o-l-o-d-ilnik.ru",
                    thumbnailUrl => '//avatars.mds.yandex.net/HF271HYe',
                    screenshotUrl => '//avatars.mds.yandex.net/NEW_SCREENSHOT',
                    status => _status_moderate2status('No'),
                    templateId => 334, version => 2, layoutCodeId => 22,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 7, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('Sent'),
                    templateId => 320, version => 3, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 500, name => 'Шины Goodyear', width => 200, height => 250,
                    alt_text => "www.pard.ru", href => "http://www.pard.ru",
                    thumbnailUrl => '',
                    status => _status_moderate2status('New'),
                    templateId => 320, version => 7, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 8, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('Sent'),
                    templateId => 320, version => 5, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
                {
                    id => 10, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                    alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                    thumbnailUrl => '//avatars.mds.yandex.net/HYY712E',
                    status => _status_moderate2status('Sent'),
                    templateId => 320, version => 9, layoutCodeId => 23,
                    group => {dateCreate => '2017-04-24T16:30:24.120+03:00'},
                    duration => 15,
                },
            ]
        );
    };
}
do_sql(PPC(ClientID => 99), ['update perf_creatives set statusModerate = "Sent"', WHERE => {creative_id => [5, 6, 7, 10]}]);
do_sql(PPC(ClientID => 99), ['update banners set statusBsSynced = "Yes", statusModerate = "Yes"', WHERE => {bid => [1, 2, 4, 5, 666, 701, 704, 705, 706, 708]}]);
do_sql(PPC(ClientID => 99), ['update banners set statusArch = "Yes"', WHERE => {bid => [2,666]}]);
my $sql_banners = "SELECT bp.creative_id, b.bid, b.statusBsSynced FROM banners b join banners_performance bp using(bid)";
Direct::Creatives::sync_creatives([{creative_id => 6, client_id => 99}, {creative_id => 5, client_id => 99},
        {creative_id => 7, client_id => 99}, {creative_id => 500, client_id => 99}, {creative_id => 8, client_id => 99}, {creative_id => 10, client_id => 99}]);
my $got_banners = get_all_sql(PPC(ClientID => 99), [$sql_banners, WHERE => {'bp.creative_id' => [5, 6, 7, 8, 10, 500]}]);

# same creative
is(get_one_field_sql(PPCDICT, "SELECT count(*) FROM shard_creative_id"), 7, 'Total creatives: not changed');

cmp_deeply(
    [sort { $a->{bid} <=> $b->{bid} } @$got_banners],
    [
        #не сбрасываем - архивная кампания
        {
            creative_id => 6, bid => 1, statusBsSynced => 'Yes',
        },
        #не сбрасываем - архивная кампания и архивный баннер
        {
            creative_id => 6, bid => 2, statusBsSynced => 'Yes',
        },
        #сбрасываем - statusModerate изменился на Yes
        {
            creative_id => 6, bid => 4, statusBsSynced => 'No',
        },
        #сбрасываем - statusModerate изменился на Yes
        {
            creative_id => 6, bid => 5, statusBsSynced => 'No',
        },
        #не сбрасываем - архивный баннер
        {
            creative_id => 6, bid => 666, statusBsSynced => 'Yes',
        },
        #сбрасываем - statusModerate изменился на Yes
        {
            creative_id => 6, bid => 701, statusBsSynced => 'No',
        },
        #сбрасываем - statusModerate изменился на No
        {
            creative_id => 5, bid => 704, statusBsSynced => 'No',
        },
        #не сбрасываем - statusModerate изменился на Sent
        {
            creative_id => 7, bid => 705, statusBsSynced => 'Yes',
        },
        #сбрасываем - version изменился
        {
            creative_id => 8, bid => 706, statusBsSynced => 'No',
        },
        #не сбрасываем - баннер черновик
        {
            creative_id => 8, bid => 707, statusBsSynced => 'No',
        },
        #не сбрасываем - не было version
        {
            creative_id => 10, bid => 708, statusBsSynced => 'Yes',
        },
    ],
    'Sync_creatives banner statusBsSynced'
);


done_testing;

sub construct_test_dataset {
    {
        shard_client_id => {original_db => PPCDICT, rows => [{ClientID => 1, shard => 1}, {ClientID => 99, shard => 1}]},
        shard_uid => {original_db => PPCDICT, rows => [{uid => 1, ClientID => 1}, {uid => 12, ClientID => 99}]},
        shard_creative_id => {original_db => PPCDICT, rows => [
                {creative_id => 1, ClientID => 1},
                {creative_id => 5, ClientID => 99},
                {creative_id => 9, ClientID => 99},
                {creative_id => 6, ClientID => 99},
                {creative_id => 7, ClientID => 99},
            ]},

        perf_creatives => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {creative_id => 1, ClientID => 1, name => 'Общий список', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/54440",
                        preview_url => '//avatars.mds.yandex.net/HFJF7373JUE',
                        statusModerate => 'New', template_id => 320, stock_creative_id => 1, version => 4},
                    {creative_id => 5, ClientID => 99, name => 'Один товар крупно с описанием и каруселью', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/",
                        preview_url => '//avatars.mds.yandex.net/5555GHE',
                        statusModerate => 'Yes', template_id => 334, , stock_creative_id => 5,  version => 2},
                    {creative_id => 9, ClientID => 99, name => 'Много товаров крупно с описанием по наведению и каруселью', width => 200, height => 250,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/800",
                        preview_url => '//avatars.mds.yandex.net/HYY712E',
                        statusModerate => 'No', template_id => 509, , stock_creative_id => 9, version => 1},
                    {creative_id => 6, ClientID => 99, name => 'Много товаров с одним описанием и мозаикой', width => 729, height => 90,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/1023",
                        preview_url => '//avatars.mds.yandex.net/HYY712E',
                        statusModerate => 'Yes', template_id => 320, stock_creative_id => 6, version => 5},
                    {creative_id => 7, ClientID => 99, name => 'Много товаров с одним описанием и мозаикой', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/900",
                        preview_url => '//avatars.mds.yandex.net/DEFAULT',
                        statusModerate => 'Sent', template_id => 320, stock_creative_id => 7, version => 3},
                    {creative_id => 8, ClientID => 99, name => 'Много товаров с одним описанием и мозаикой', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/900",
                        preview_url => '//avatars.mds.yandex.net/DEFAULT',
                        statusModerate => 'Sent', template_id => 320, version => 3, stock_creative_id => 8},
                    {creative_id => 10, ClientID => 99, name => 'Много товаров с одним описанием и мозаикой', width => 240, height => 400,
                        alt_text => "marker.yandex.ru", href => "https://market.yandex.ru/catalog/900",
                        preview_url => '//avatars.mds.yandex.net/DEFAULT',
                        statusModerate => 'Sent', template_id => 320, stock_creative_id => 10},
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

                    {bid => 666, cid => 55},
                    {bid => 700, cid => 4},
                    {bid => 701, cid => 4},
                    {bid => 702, cid => 3},
                    {bid => 703, cid => 4},
                    {bid => 704, cid => 4},
                    {bid => 705, cid => 4},
                    {bid => 706, cid => 4},
                    {bid => 707, cid => 4},
                    {bid => 708, cid => 4},
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
                    {banner_creative_id => 11, creative_id => 5, cid => 4, pid => 8, bid => 704},
                    {banner_creative_id => 12, creative_id => 7, cid => 4, pid => 8, bid => 705},
                    {banner_creative_id => 13, creative_id => 8, cid => 4, pid => 8, bid => 706},
                    {banner_creative_id => 14, creative_id => 8, cid => 4, pid => 8, bid => 707},
                    {banner_creative_id => 15, creative_id => 10, cid => 4, pid => 8, bid => 708},
                ],
            },
        }, 
        users => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                    {uid => 1, ClientID => 1, login => 'unit-test'},
                    {uid => 12, ClientID => 99, login => 'creative-tests'}
                ],
            }
        },

        creative_banner_storage_sync => {
            original_db => PPC(shard => 'all'),
            rows => {
                1 => [
                     {creative_id => 001, sync_status => 'Processed', modifed => '2016-02-01 13:00'},
                ],
            },
        },

        mod_reasons => {
            original_db => PPC(shard => 'all'),
            rows => {
            }
        },

        banner_storage_dict => {
            original_db => PPCDICT,
            rows => [
                    {id => 4, type => 'theme', json_content => '{"value":4,"name":"Автомобили"}'},
                    {id => 4, type => 'layout', json_content => '{"value":4,"name":"Много товаров с описаниями","img_src":"http://storage.mds.yandex.net/get-bstor/15472/4_3.svg"}'},
                    {id => 23, type => 'template', json_content => '{"layouts":{},"value":23,"templateType":3,"type":null,"name":"DSP Simple banner"}'},
                    {id => 320, type => 'template', json_content => '{"layouts":{},"value":320,"templateType":9,"type":null,"name":"320"}'},
                    {id => 334, type => 'template', json_content => '{"layouts":{},"value":334,"templateType":9,"type":null,"name":"334"}'},
                    {id => 509, type => 'template', json_content => '{"layouts":{},"value":509,"type":null,"name":"509"}'},
                    ]
        }
    },
}

sub _status_moderate2status{
    my %sm2s = (undef => 0, New => 1, Sent => 2, No => 3, Yes => 4);
    return $sm2s{ $_[0] };
}

sub _fake_receive_creatives {
    my %in = @_;
    
    my ($method, $http_method, $args) = @{ $in{original} };
    my @ids = split /,/, $args->{ids};
    my $result = $in{dataset};

    # возвращаем данные только для тех id, которые у нас запрашивали   
    return { content => { items => [ grep { my $id = $_->{id}; List::MoreUtils::any {$id == $_} @ids } @$result ]  } };
}

1;

