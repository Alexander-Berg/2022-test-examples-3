#!/usr/bin/perl

# Юнит тесты на MobileContent::icon_hash2url

use strict;
use warnings;
use utf8;

use Test::More;

use MobileContent qw/icon_hash2url/;


my @tests = (
    # [$input, $expected_out]
    # Google Play icons
    [['Android', '21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e'], '//avatars.mds.yandex.net/get-google-play-app-icon/21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e/icon'],
    [['Android', '21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e', 'icon-xl'], '//avatars.mds.yandex.net/get-google-play-app-icon/21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e/icon-xl'],
    [['Android', '21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e', 'icon-xld-retina'], '//avatars.mds.yandex.net/get-google-play-app-icon/21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e/icon-xld-retina'],
    [['Android', '', 'icon-xld-retina'], undef],
    [['Android', undef], undef],
    [['', '21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e', 'icon-xl'], undef],
    [[undef, '21534/com.starshipstudio.thesurvivor__83c6c809263bf4b5c381b9a1e078e89e'], undef],

    # Apple App Store icons
    [['iOS', '21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e'], '//avatars.mds.yandex.net/get-itunes-icon/21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e/icon'],
    [['iOS', '21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e', 'icon-xl'], '//avatars.mds.yandex.net/get-itunes-icon/21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e/icon-xl'],
    [['iOS', '21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e', 'icon-xld-retina'], '//avatars.mds.yandex.net/get-itunes-icon/21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e/icon-xld-retina'],
    [['iOS', '', 'icon-xld-retina'], undef],
    [['iOS', undef], undef],
    [['', '21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e', 'icon-xl'], undef],
    [[undef, '21534/id408709785__83c6c809263bf4b5c381b9a1e078e89e'], undef],

    [[undef, undef, undef], undef],
);

Test::More::plan(tests => scalar(@tests));

for my $test (@tests) {
    my ($input, $expected_out, $test_name) = @$test;
    my ($os_type, $icon_hash, $size) = @$input;
    $test_name ||= 'icon_hash2url(' . (join (', ', map { defined $_? $_:'' } @$input)) . ") = ".( $expected_out // 'undef');
    is_deeply(MobileContent::icon_hash2url($os_type, $icon_hash, $size), $expected_out, $test_name);
}
