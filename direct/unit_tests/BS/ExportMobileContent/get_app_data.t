#!/usr/bin/perl
use Direct::Modern;

use Test::More;
use Test::Deep;

use BS::ExportMobileContent ();
use Settings;

sub test {
  my ($content, $app, $expect, $message) = @_;

  my $result = BS::ExportMobileContent::get_app_data(
    $content,
    $app
  );
  cmp_deeply($result, $expect, $message);
}

test(
  { os_type => 'bad os' },
  { store_href => '---bad-href---' },
  {
      BundleId => "",
      SourceID => 0,
      RegionName => "ru",
      LocaleName => "ru"
  },
  'bad_os_type'
);

test(
  { os_type => 'Android', store_content_id => 'store.content.id' },
  { store_href => '---bad-href---' },
  {
    BundleId => 'store.content.id',
    SourceID => 1,
    RegionName => "ru",
    LocaleName => "ru"
  },
  'android bundle id'
);

test(
  { os_type => 'Android', store_content_id => 'store.content.id' },
  { store_href => 'https://play.google.com/store/apps/details?id=ru.yandex.direct&hl=EN&gl=US' },
  {
    BundleId => 'store.content.id',
    SourceID => 1,
    RegionName => "us",
    LocaleName => "en"
  },
  'android region and language'
);

test(
  { os_type => 'iOS', bundle_id => 'yandex.direct' },
  { store_href => 'https://apps.apple.com/US/app/яндекс-директ/id583627331?ign-mpt=uo%3D4&l=EN' },
  {
    BundleId => "yandex.direct",
    SourceID => 2,
    RegionName => "us",
    LocaleName => "en"
  },
  'apple region and language'
);

sub test_region_and_language {
  my ($os, $href, $expect_lang, $expect_region) = @_;

  my $href_string = defined $href ? $href : "undef";

  test(
    { os_type => $os, bundle_id => 'yandex.direct', store_content_id => 'yandex.direct' },
    { store_href => $href },
    {
      BundleId => 'yandex.direct',
      SourceID => $os eq 'Android' ? 1 : 2,
      RegionName => $expect_region,
      LocaleName => $expect_lang
    },
    "Os = $os, href = $href_string, expect_lang = $expect_lang, expect_region = $expect_region" 
  ); 

}

test_region_and_language(
  'Android',
  undef,
  "ru",
  "ru"
);

test_region_and_language(
  'iOS',
  "",
  "ru",
  "ru"
);

test_region_and_language(
  'iOS',
  "https://apps.apple.com/XXX/app/id1561640111",
  "ru",
  "xxx"
);

test_region_and_language(
  'iOS',
  "https://apps.apple.com//app/id1561640111?l=dasdasd",
  "dasdasd",
  "ru"
);

test_region_and_language(
  'iOS',
  "https://apps.apple.com////yyy///app////id1346027678",
  "ru",
  "yyy"
);

test_region_and_language(
  'iOS',
  "https://apps.apple.com/en/app/id1561640111?l1=xx&l2=yy&l=zz",
  "zz",
  "en"
);

test_region_and_language(
  'iOS',
  "https://apps.apple.com/en/l=zz/id1561640111",
  "ru",
  "en"
);

test_region_and_language(
  'iOS',
  "!@#\$%^&*()",
  "ru",
  "ru"
);

test_region_and_language(
  'Android',
  "!@#\$%^&*()",
  "ru",
  "ru"
);

test_region_and_language(
  'iOS',
  "https://apps.apple.com/app/id1",
  "ru",
  "ru"
);

test_region_and_language(
  'iOS',
  "https://itunes.apple.com/app/a",
  "ru",
  "ru"
);

test_region_and_language(
  'Android',
  "https://play.google.com/store/apps?hl=xxx&gl=yyy",
  "xxx",
  "yyy"
);

test_region_and_language(
  'Android',
  "https://play.google.com/gl=xxx/store/apps?hl=US",
  "us",
  "ru"
);

test_region_and_language(
  'Android',
  "https://play.google.com/hl=xxx/store/apps?gl=US",
  "ru",
  "us"
);


done_testing();
