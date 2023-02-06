#!/usr/bin/perl

use strict;
use warnings;

use utf8;

use Test::More;

use Models::CampaignOperations;
*csm = *Models::CampaignOperations::calc_campaign_status_post_moderate;

is(csm({ cid => 1, statusPostModerate => "Accepted", type => "text" }, StatusModerate => "No"), "Accepted", "Reject accepted campaign");
is(csm({ cid => 1, statusPostModerate => "No", type => "text" }, StatusModerate => "No"), "No", "Reject not accepted campaign");
is(csm({ cid => 1, statusPostModerate => "Yes", type => "text", sum => 111 }, StatusModerate => "No"), undef, "Reject post moderated campaign");
is(csm({ cid => 1, statusPostModerate => "Yes", type => "text", sum => 0, sum_to_pay => 0 }, StatusModerate => "No"), "No", "Reject post moderated campaign");
is(csm({ cid => 1, statusPostModerate => "No", type => "text" }, StatusModerate => "Yes"), "Accepted", "Accept not accepted campaign");
is(csm({ cid => 1, statusPostModerate => "Yes", type => "text", sum => 111 }, StatusModerate => "Yes"), "Accepted", "Accept post moderated campaign");

done_testing();
