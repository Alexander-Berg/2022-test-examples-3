#!/usr/bin/perl

use warnings;
use strict;

use Test::More tests => 24;
use Test::Deep;

use APICommon qw/:subs/;

use utf8;

sub f
{
    my $uri = shift;
    my ($version, $live_flag, $fullversion, $wsdl_subversion) = APICommon::get_api_version_by_uri($uri);
    return [$version, $live_flag, $fullversion, $wsdl_subversion];
}

# new url formating
cmp_deeply(f("/api/v1"), [1, undef, 1, undef], "Version 1");
cmp_deeply(f("/api/v1/"), [1, undef, 1, undef], "Version 1 (with slash)");

cmp_deeply(f("/api/v2/"), [2, undef, 2, undef], "Version 2");
cmp_deeply(f("/api/v22/"), [22, undef, 22, undef], "Version 22");

cmp_deeply(f("/api/v4/live"), [4,1, 4.5, undef], "Version api4live-1");
cmp_deeply(f("/api/v4/live/"), [4,1, 4.5, undef], "Version api4live-2");
cmp_deeply(f("/live/v4/soap/"), [4,1, 4.5, undef], "Version live4soap-1 new format");
cmp_deeply(f("/live/v4/soap"), [4,1, 4.5, undef], "Version live4soap-2 new format");
cmp_deeply(f("/v4/soap"), [4, undef, 4, undef], "Version 4soap-1 new format");
cmp_deeply(f("/v4/soap/"), [4, undef, 4, undef], "Version 4soap-2 new format");

cmp_deeply(f("/json-api/v4/live"), [4, 1, 4.5, undef], "Version json-api4live-1");
cmp_deeply(f("/json-api/v4/live/"), [4, 1, 4.5, undef], "Version json-api4live-2");
cmp_deeply(f("/live/v4/json/"), [4,1, 4.5, undef], "Version live4json-1 new format");
cmp_deeply(f("/live/v4/json"), [4,1, 4.5, undef], "Version live4json-2 new format");
cmp_deeply(f("/v4/json"), [4, undef, 4, undef], "Version 4json-1 new format");
cmp_deeply(f("/v4/json/"), [4, undef, 4, undef], "Version 4json-2 new format");

cmp_deeply(f("/live/v4/soap/1/"),   [4,1, 4.5, 1], "Version live4soap-1 new format with wsdl_subversion=1");
cmp_deeply(f("/live/v4/soap/1"),    [4,1, 4.5, 1], "Version live4soap-2 new format with wsdl_subversion=1");
cmp_deeply(f("/v4/soap/1/"),        [4, undef, 4, 1], "Version 4soap-1 new format with wsdl_subversion=1");
cmp_deeply(f("/v4/soap/1"),         [4, undef, 4, 1], "Version 4soap-2 new format with wsdl_subversion=1");

cmp_deeply(f("/live/v4/soap/2/"),   [4, 1, 4.5, 2], "Version live4soap-2 new format with wsdl_subversion=2");
cmp_deeply(f("/live/v4/soap/2"),    [4, 1, 4.5, 2], "Version live4soap-2 new format with wsdl_subversion=2");
cmp_deeply(f("/v4/soap/2/"),        [4, undef, 4, 2], "Version 4soap-2 new format with wsdl_subversion=2");
cmp_deeply(f("/v4/soap/2"),         [4, undef, 4, 2], "Version 4soap-2 new format with wsdl_subversion=2");
