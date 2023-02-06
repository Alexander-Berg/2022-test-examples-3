#!/usr/bin/perl

# $Id$
# на самом деле, здесь еще проверяется работа calculateBannerStatus в Легком

use warnings;
use strict;
use Test::More;
use Test::Deep;

use Settings;

BEGIN { use_ok('Models::Banner'); }

use utf8;
use open ':std' => ':utf8';
binmode(STDERR, ":utf8");
binmode(STDOUT, ":utf8");
use Yandex::Test::UTF8Builder;
use Yandex::HashUtils qw/hash_merge/;

*gbsi = \&Models::Banner::get_banner_status_info;
*cbs  = \&Models::Banner::calculateBannerStatus;

my @tests = ( 
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            show_active => 1,
        },
        [
            "Идут показы.",
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
                creative_layout_id => 45,
                creative_statusModerate => 'Yes'
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            show_active => 1,
        },
        [
            "Идут показы.",
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'No',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'No',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_declined         => 1,
        },
        [
            'Отклонено модератором.',
        ],
        undef,
    ],
    # performance banners
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
                creative_layout_id => 45,
                creative_statusModerate => 'No',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_declined         => 1,
        },
        [
            'Отклонено модератором.',
        ],
        undef,
    ],
    # performance banners with legacy creatives
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
                creative_layout_id => 33,
                creative_statusModerate => 'No',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            show_active => 1,
        },
        [
            'Идут показы.'
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'No',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
                creative_layout_id => 33,
                creative_statusModerate => 'No',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            stopped => 1,
        },
        [
            'Остановлено.'
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'No',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_declined         => 1,
        },
        [
            'Отклонено модератором.',
        ],
        "Идут показы предыдущей версии объявления!",
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 1,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_phrases_declined => 1,
        },
        [
            'Часть фраз отклонена.',
        ],
        "Идут показы по принятым фразам!",
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'No',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_contact_declined => 1,
        },
        [
            'Контактная информация отклонена.',
        ],
        'Идут показы без ссылки «Адрес и телефон»!',
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'No',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 1,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_contact_declined => 1,
            moderate_phrases_declined => 1,
        },
        [
            'Отклонены: контактная информация, часть фраз.',
        ],
        'Идут показы по принятым фразам без ссылки «Адрес и телефон»!',
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Sent',
                href                 => 1,
                statusPostModerate   => 'Sent',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'No',
                BannerID             => undef,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Sent',
                statusPostModerate => 'Sent',
                statusBsSynced     => 'No',
            },
        },
        {
            moderate_wait             => 1,
        },
        [
            'Ожидает модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'No',
                BannerID             => undef,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
                creative_layout_id => 45,
                creative_statusModerate => 'Sent',
            },
            phrases => {
                statusModerate     => 'Sent',
                statusPostModerate => 'Sent',
                statusBsSynced     => 'No',
            },
        },
        {
            moderate_wait             => 1,
        },
        [
            'Ожидает модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Sent',
                href                 => 1,
                statusPostModerate   => 'Sent',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => undef,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Sent',
                statusPostModerate => 'Sent',
                statusBsSynced     => 'No',
            },
        },
        {
            moderate_wait             => 1,
        },
        [
            'Ожидает модерации.',
        ],
        "Идут показы предыдущей версии объявления!",
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'Sent',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_banner_accepted  => 1,
            moderate_contact_wait     => 1,
        },
        [
            'Объявление принято.',
            'Контактная информация ожидает модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'Sent',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_banner_accepted  => 1,
            moderate_sitelinks_wait   => 1,
        },
        [
            'Объявление принято.',
            'Быстрые ссылки ожидают модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'Yes',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'No',
                BannerID             => 1,
                statusSitelinksModerate => 'Ready',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'No',
            },
        },
        {
            moderate_banner_accepted  => 1,
            moderate_sitelinks_wait   => 1,
        },
        [
            'Объявление принято.',
            'Быстрые ссылки ожидают модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'Yes',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'No',
                BannerID             => 1,
                statusSitelinksModerate => 'Yes',
                image_statusModerate => 'Yes',
                display_href_statusModerate => 'Sent',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'No',
            },
        },
        {
            activation => 1,
            moderate_display_href_wait   => 1,
        },
        [
            'Отображаемая ссылка ожидает модерации.',
            'Идет активизация.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'No',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            stopped                   => 1,
        },
        [
            'Остановлено.'
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'Yes',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
        },
        [
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'Yes',
                statusModerate       => 'No',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_declined         => 1,
        },
        [
            'Отклонено модератором.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'Yes',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 1,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_phrases_declined => 1,
        },
        [
            'Часть фраз отклонена.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 10,

            },
            banner => {
                phoneflag            => 'No',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => undef,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_contact_declined => 1,
        },
        [
            'Контактная информация отклонена.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 1,

            },
            banner => {
                phoneflag            => 'No',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 1,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_contact_declined => 1,
            moderate_phrases_declined => 1,
        },
        [
            'Отклонены: контактная информация, часть фраз.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 1,

            },
            banner => {
                phoneflag            => 'No',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 1,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'No',
                image_statusModerate => 'Yes',
                display_href_statusModerate => 'No',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_contact_declined   => 1,
            moderate_phrases_declined   => 1,
            moderate_sitelinks_declined => 1,
            moderate_display_href_declined => 1,
        },
        [
            'Отклонены: контактная информация, часть фраз, быстрые ссылки, отображаемая ссылка.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 1,

            },
            banner => {
                phoneflag            => 'Yes',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'No',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_sitelinks_declined => 1,
        },
        [
            'Быстрые ссылки отклонены.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 0,

            },
            banner => {
                phoneflag            => 'No',
                statusModerate       => 'Sent',
                href                 => 1,
                statusPostModerate   => 'Sent',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 1,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_wait             => 1,
        },
        [
            'Ожидает модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 0,

            },
            banner => {
                phoneflag            => 'Sent',
                statusModerate       => 'Yes',
                href                 => undef,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_banner_accepted  => 1,
            moderate_contact_wait     => 1,
        },
        [
            'Объявление принято.',
            'Контактная информация ожидает модерации.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'No',
                statusModerate => 'Yes',
                sum_total      => 0,

            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'No',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            stopped                   => 1,
        },
        [
            'Остановлено.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 0,
                sum_to_pay     => 1,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 0,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            show_accepted             => 1,
        },
        [
            'Принято к показам.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 0,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 0,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            show_accepted             => 1,
        },
        [
            'Принято к показам.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'No',
                sum_total      => 0,
                sum_to_pay     => 0,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_declined => 1,
        },
        [
            'Отклонено модератором.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,
                sum_to_pay     => 0,
            },
            banner => {
                phoneflag            => 'Sent',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'No',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'No',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            moderate_banner_accepted => 1,
            moderate_contact_wait    => 1,
            stopped                  => 1,
            activation               => 1,
        },
        [
            'Остановлено.',
            'Контактная информация ожидает модерации.',
            'Идет активизация.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 10,
                sum_to_pay     => 0,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Yes',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'No',
                statusActive         => 'Yes',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'No',
                BannerID             => 1,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            stopped                  => 1,
            activation               => 1,
        },
        [
            'Остановлено.',
            'Идет активизация.',
        ],
        undef,
    ],
    [
        {
            camp => {
                statusShow     => 'Yes',
                statusModerate => 'Yes',
                sum_total      => 0,
                sum_to_pay     => 0,
            },
            banner => {
                phoneflag            => 'New',
                statusModerate       => 'Sent',
                href                 => 1,
                statusPostModerate   => 'Yes',
                statusArch           => 'No',
                statusShow           => 'Yes',
                statusActive         => 'No',
                countDeclinedPhrases => 0,
                statusBsSynced       => 'Yes',
                BannerID             => 0,
                statusSitelinksModerate => 'New',
                image_statusModerate => 'Yes',
            },
            phrases => {
                statusModerate     => 'Yes',
                statusPostModerate => 'Yes',
                statusBsSynced     => 'Yes',
            },
        },
        {
            show_accepted            => 1,
            moderate_wait            => 1,
        },
        [
            'Принято к показам.',
            'Ожидает модерации.',
        ],
        undef,
    ],
);

my @prof_tests = (
    [
        {
            'banner' => {
                'BannerID' => '0',
                statusSitelinksModerate => 'New',
                #image_statusModerate => 'Yes',
                'bid' => '12451949',
                'countDeclinedPhrases' => '91',
                'href' => 'kapmah.ru/koja/galantereia/',
                'phoneflag' => undef,
                'statusActive' => 'No',
                'statusArch' => undef,
                'statusBsSynced' => 'Sending',
                'statusModerate' => 'Sent',
                'statusPostModerate' => 'Yes',
                'statusShow' => 'Yes'
            },
            'camp' => {
                'statusModerate' => 'Yes',
                'statusShow' => 'Yes',
                'sum_to_pay' => '0.000000',
                'sum_total' => '0'
            },
            'phrases' => {
                'statusBsSynced' => 'Sending',
                'statusModerate' => 'Sent',
                'statusPostModerate' => 'Yes'
            }
        },
        {
            show_accepted => 1,
            moderate_wait => 1,
        },
        [
            'Принято к показам.',
        ],
        undef,
    ],
    [
        {
            'banner' => {
                'BannerID' => '10052241',
                statusSitelinksModerate => 'New',
                #image_statusModerate => 'Yes',
                'bid' => '11670958',
                'countDeclinedPhrases' => '1',
                'href' => 'info-dvd.ru/bbm/go/wok/p/oborona',
                'phoneflag' => undef,
                'statusActive' => 'Yes',
                'statusArch' => undef,
                'statusBsSynced' => 'Yes',
                'statusModerate' => 'Sent',
                'statusPostModerate' => 'Yes',
                'statusShow' => 'Yes'
            },
            'camp' => {
                'statusModerate' => 'Yes',
                'statusShow' => 'Yes',
                'sum_to_pay' => '0.000000',
                'sum_total' => '12.17'
            },
            'phrases' => {
                'statusBsSynced' => 'Yes',
                'statusModerate' => 'Yes',
                'statusPostModerate' => 'Yes'
            }
        },
        {
            show_active => 1,
        },
        [
            'Идут показы.'
        ],
        undef,
    ],
    [
        {
            'banner' => {
                'BannerID' => '10052241',
                statusSitelinksModerate => 'New',
                #image_statusModerate => 'Yes',
                'bid' => '11670958',
                'countDeclinedPhrases' => '0',
                'href' => 'info-dvd.ru/bbm/go/wok/p/oborona',
                'phoneflag' => undef,
                'statusActive' => 'Yes',
                'statusArch' => undef,
                'statusBsSynced' => 'Yes',
                'statusModerate' => 'Yes',
                'statusPostModerate' => 'Yes',
                'statusShow' => 'Yes',
                video_addition_statusModerate => 'No',
            },
            'camp' => {
                'statusModerate' => 'Yes',
                'statusShow' => 'Yes',
                'sum_to_pay' => '0.000000',
                'sum_total' => '12.17'
            },
            'phrases' => {
                'statusBsSynced' => 'Yes',
                'statusModerate' => 'Yes',
                'statusPostModerate' => 'Yes'
            }
        },
        {
            moderate_video_addition_declined => 1,
        },
        [
            'Видеодополнение отклонено.'
        ],
        undef,
    ],
);

my $base_result = {
    archived                    => undef,
    show_active                 => undef,
    show_accepted               => undef,
    moderate_wait               => undef,
    moderate_banner_accepted    => undef,
    moderate_contact_wait       => undef,
    moderate_sitelinks_wait     => undef,
    moderate_declined           => undef,
    moderate_contact_declined   => undef,
    moderate_phrases_declined   => undef,
    moderate_sitelinks_declined => undef,
    moderate_callout_declined   => undef,
    moderate_image_declined     => undef,
    moderate_image_wait         => undef,
    moderate_display_href_declined => undef,
    moderate_display_href_wait  => undef,
    moderate_video_addition_declined => undef,
    moderate_video_addition_wait => undef,
    moderate_turbolanding_declined => undef,
    moderate_turbolanding_wait  => undef,
    moderate_placement_pages_declined => undef,
    moderate_placement_pages_wait  => undef,
    moderate_placement_pages_absent => undef,
    moderate_placement_pages_activization => undef,
    draft                       => undef,
    activation                  => undef,
    stopped                     => undef,
};
for my $t (@tests) {
    my %base = %$base_result;
    cmp_deeply(gbsi($t->[0], easy_user => 1), hash_merge(\%base, $t->[1]));
    cmp_deeply(cbs($t->[0], easy_user => 1)->{text}, $t->[2], join(' ', @{$t->[2]}));
    cmp_deeply(cbs($t->[0], easy_user => 1)->{warning}, $t->[3], $t->[3]);
}

for my $t (@prof_tests) {
    my %base = %$base_result;
    cmp_deeply(gbsi($t->[0]), hash_merge(\%base, $t->[1]));
    cmp_deeply(cbs($t->[0])->{text}, $t->[2], join(' ', @{$t->[2]}));
    cmp_deeply(cbs($t->[0])->{warning}, $t->[3], $t->[3]);
}

done_testing;
