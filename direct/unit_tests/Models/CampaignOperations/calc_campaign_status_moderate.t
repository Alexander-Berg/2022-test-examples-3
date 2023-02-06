#!/usr/bin/perl

use strict;
use warnings;

use utf8;

use Yandex::Test::UTF8Builder;
use Test::More;

use Models::CampaignOperations;
*csm = *Models::CampaignOperations::calc_campaign_status_moderate;

is(csm({statusModerate => 'Ready', cnt => 10, cnt_accept => 1, cnt_new => 9, cnt_wait => 0, cnt_mod => 0, type => 'text'}), undef, "campaign is awaiting moderation");
is(csm({statusModerate => 'Sent', cnt => 10, cnt_accept => 1, cnt_new => 9, cnt_wait => 0, cnt_mod => 1, type => 'text'}), 'Yes', "1 banenr accepted");
is(csm({statusModerate => 'Sent', cnt => 10, cnt_accept => 0, cnt_new => 0, cnt_wait => 0, cnt_mod => 0, type => 'text'}), undef, "All banners are awaiting moderation");
is(csm({statusModerate => 'Yes', cnt => 10, cnt_accept => 0, cnt_new => 9, cnt_wait => 0, cnt_mod => 1, cnt_decline => 1, cnt_preliminarily => 0, type => 'text'}), 'No', "Banner rejected");
is(csm({statusModerate => 'No', cnt => 10, cnt_accept => 1, cnt_new => 9, cnt_wait => 0, cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'text'}), 'Yes', "Banner accepted");

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 1, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_banner',
         group_cnt_accepted => 1}),
    'Yes', 
    "Accept cpm campaigns with accepted ad groups and banners"
);

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 1, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_yndx_frontpage',
         group_cnt_accepted => 1}),
    'Yes',
    "Accept cpm_yndx_frontpage campaign with accepted ad groups and banners"
);

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_banner',
         group_cnt_accepted => 1}),
    undef, 
    "Decline cpm campaigns with accepted ad groups but without accepted banners"
);

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_yndx_frontpage',
         group_cnt_accepted => 1}),
    undef,
    "Decline cpm_yndx_frontpage campaign with accepted ad groups but without accepted banners"
);

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_banner',
         group_cnt_accepted => 0}),
    undef, 
    "Decline campaign without accepted ad groups"
);

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_yndx_frontpage',
         group_cnt_accepted => 0}),
    undef,
    "Decline cpm_yndx_frontpage campaign without accepted ad groups"
);

is(
    csm({statusModerate => 'Sent', cnt => 2, cnt_accept => 1, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 1, cnt_decline => 0, cnt_preliminarily => 0, type => 'cpm_banner',
         group_cnt_accepted => 0}),
    'Yes', 
    "Accept campaigns with accepted banners"
);

is(
    csm({statusModerate => 'No', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 2, cnt_decline => 2, cnt_preliminarily => 0, type => 'internal_free',
         group_cnt_accepted => 0}),
    'Yes',
    "StatusModerate always Yes for INTERNAL_FREE campaigns with declined banners"
);
is(
    csm({statusModerate => 'No', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 1,
         cnt_mod => 2, cnt_decline => 1, cnt_preliminarily => 0, type => 'internal_distrib',
         group_cnt_accepted => 0}),
    'Yes',
    "StatusModerate always Yes for INTERNAL_DISTRIB campaigns with declined banners"
);
is(
    csm({statusModerate => 'Yes', cnt => 2, cnt_accept => 0, cnt_new => 0, cnt_wait => 0,
         cnt_mod => 2, cnt_decline => 2, cnt_preliminarily => 0, type => 'internal_autobudget',
         group_cnt_accepted => 0}),
    'Yes',
    "StatusModerate always Yes for INTERNAL_AUTOBUDGET campaigns with declined banners"
);

#is(csm({statusModerate => 'New', cnt => 10, cnt_accept => 0, cnt_new => 9, cnt_wait => 0, cnt_mod => 1, cnt_decline => 1, cnt_preliminarily => 0}), xxxx, "aaa bbb ccc");

done_testing();
