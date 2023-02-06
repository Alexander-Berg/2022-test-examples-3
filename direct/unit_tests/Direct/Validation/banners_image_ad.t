use Direct::Modern;

use Test::More;
use Test::Exception;
use Yandex::Test::ValidationResult;
use Direct::Model::BannerImageAd;
use Direct::Model::AdGroupText;
use Direct::Model::AdGroupMobileContent;
use Direct::Model::ImageFormat;
use Direct::Model::Image;
use Direct::Model::CanvasCreative;
use Direct::Model::BannerCreative;
use Direct::Model::Campaign;
use JSON;
use Yandex::DBUnitTest qw/:all/;
use KnownDomains;
use geo_regions;

use Settings;


BEGIN {
    use_ok('Direct::Validation::BannersImageAd', qw/
        validate_update_imagead_banners
        validate_add_imagead_banners
    /);
}

my %db = (
    clients => {
        original_db => PPC(shard => 1),
        like => 'clients',
        rows => {1 => [
            {ClientID => 1812, country_region_id =>  $geo_regions::RUS},
            {ClientID => 9827, country_region_id =>  $geo_regions::UKR},
        ]},
    }, 
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 1812, shard => 1 },
            { ClientID => 9827, shard => 1 },
        ],
    },
);
init_test_dataset(\%db);

copy_table(PPCDICT, 'trusted_redirects');
copy_table(PPCDICT, 'ppc_properties');
KnownDomains::add_domain(mobile_app_counter => 'measurementapi.com');
KnownDomains::add_domain(mobile_app_counter => 'appmetrica.yandex.com');
KnownDomains::add_domain(mobile_app_counter => 'onelink.me');

package Test::Direct::Model::BannerImageAd {
    use Mouse;
    extends 'Direct::Model::BannerImageAd';
    with 'Direct::Model::BannerImage::Role::Url';
    1;
};

my $ru_camp = Direct::Model::Campaign->new(content_lang => 'ru');

subtest 'validate add text imagead on picture' => sub {
    local *vr_add = sub {
        my %banner = @_;

        my $format = Direct::Model::ImageFormat->new(width => $banner{width}, height => $banner{height});
        my $image = Direct::Model::Image->new(
           hash => $banner{hash},
           format => $format,
        );

        validate_add_imagead_banners(
            [Direct::Model::BannerImageAd->new(
                image_ad => $image,
                (exists $banner{href} ? (href => $banner{href}) : ()),
                client_id => 1,
             )],
            Direct::Model::AdGroupText->new(geo => 255, campaign => $ru_camp, banners_count => 2)
        );
    };

    ok_validation_result(vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => 'https://ya.ru'));

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "\t    "),
        [{href => vr_errors('EmptyField')}]
    );

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "asdasdasdasd"),
        [{href => vr_errors('InvalidField')}]
    );

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "http://jfjjdjddj/kdkdk/jdjd.html"),
        [{href => vr_errors('InvalidField')}]
    );

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "ftp://ya.com/secret/key"),
        [{href => vr_errors('InvalidField')}]
    );

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "https://" . join("", "r" x 256) . ".com"),
        [{href => vr_errors('InvalidField')}]
    );
};

subtest 'text imagead with image url' => sub {

    my $vr1 = validate_add_imagead_banners(
       [Test::Direct::Model::BannerImageAd->new(
           href => 'http://ya.ru',
           url => 'http://www.subarenda.ru/enter.shtml#t2',
           client_id => 1
        )],
       Direct::Model::AdGroupText->new(geo => 255, campaign => $ru_camp, banners_count => 0));

    ok_validation_result($vr1);

    my $vr2 = validate_add_imagead_banners(
       [Test::Direct::Model::BannerImageAd->new(
           href => 'http://ya.ru',
           url => 'hpp://kjdak',
           client_id => 1
        )],
       Direct::Model::AdGroupText->new(geo => 255, campaign => $ru_camp, banners_count => 1));

    cmp_validation_result($vr2, [{image => vr_errors(qr/некорректная ссылка/)}]);
};

subtest 'validate update text imagead on picture' => sub {
    local *vr_update = sub {
        my %banner = @_;

        my $format_old = Direct::Model::ImageFormat->new(width => $banner{old_width}, height => $banner{old_height});
        my $image_old = Direct::Model::Image->new(
           hash => $banner{old_hash},
           format => $format_old,
        );
        my $banner_old = Direct::Model::BannerImageAd->new(image_ad => $image_old);

        my $format = Direct::Model::ImageFormat->new(width => $banner{width}, height => $banner{height});
        my $image = Direct::Model::Image->new(
           hash => $banner{hash},
           format => $format,
        );
        validate_update_imagead_banners(
            [Direct::Model::BannerImageAd->new(
                image_ad => $image, (exists $banner{href} ? (href => $banner{href}) : ()),
                old => $banner_old,
                client_id => 1
             )],
            Direct::Model::AdGroupText->new(geo => 255, campaign => $ru_camp)
        );
    };

    ok_validation_result(
       vr_update(
          hash => "-2II_29wA7UTZNXhkXk1RA",
          width => 800, height => 600,
          href => 'https://ya.ru',
          old_width => 800, old_height => 600,
          old_hash => '-KSG4xrZtpfbKNlpiOHdFg'
   ));

    cmp_validation_result(
       vr_update(
          hash => "-2II_29wA7UTZNXhkXk1RA",
          width => 800, height => 600,
          href => 'https://ya.ru',
          old_width => 100, old_height => 150,
          old_hash => '-KSG4xrZtpfbKNlpiOHdFg'),
      [vr_errors(qr/Размеры.+должны быть одинаковыми/)]   
   );
};

subtest 'validate update text imagead on canvas' => sub {
    local *vr_update = sub {
        my %banner = @_;

        my $creative_old = Direct::Model::CanvasCreative->new(
            id => $banner{old_creative_id},
            width => $banner{old_width}, height => $banner{old_height}
        );
        my $rel_old = Direct::Model::BannerCreative->new(creative => $creative_old);
        my $banner_old = Direct::Model::BannerImageAd->new(creative => $rel_old);

        my $creative = Direct::Model::CanvasCreative->new(
            id => $banner{creative_id},
            width => $banner{width}, height => $banner{height}
        );
        my $rel = Direct::Model::BannerCreative->new(creative => $creative);
        validate_update_imagead_banners(
            [Direct::Model::BannerImageAd->new(
                creative => $rel,
                (exists $banner{href} ? (href => $banner{href}) : ()),
                old => $banner_old,
                client_id => 1
             )],
            Direct::Model::AdGroupText->new(geo => 255, campaign => $ru_camp)
        );
    };

    ok_validation_result(
       vr_update(
          creative_id => 230,
          width => 800, height => 600,
          href => 'https://ya.ru',
          old_creative_id => 671,
          old_width => 800, old_height => 600,
   ));

    cmp_validation_result(
       vr_update(
          creative_id => 200,
          width => 800, height => 600,
          href => 'https://ya.ru',
          old_creative_id => 567,
          old_width => 100, old_height => 150),
      [vr_errors(qr/Размеры.+должны быть одинаковыми/)]    
   );
};

subtest 'validate add mobile app imagead on picture' => sub {
    local *vr_add = sub {
        my %banner = @_;

        my $format = Direct::Model::ImageFormat->new(width => $banner{width}, height => $banner{height});
        my $image = Direct::Model::Image->new(
           hash => $banner{hash},
           format => $format,
        );

        validate_add_imagead_banners(
            [Direct::Model::BannerImageAd->new(
                image_ad => $image,
                (exists $banner{href} ? (href => $banner{href}) : ()),
                client_id => 1
             )],
            Direct::Model::AdGroupMobileContent->new(geo => 255, campaign => $ru_camp, banners_count => 4)
        );
    };

    ok_validation_result(vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => 'https://onelink.me/hfj838jdjd'));
    ok_validation_result(vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600));

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "http://ya.ru/123"),
        [{href => vr_errors('BadUsage')}]
    );

    cmp_validation_result(
        vr_add(hash => "-2II_29wA7UTZNXhkXk1RA", width => 800, height => 600, href => "ht://onelink.me/kddke671Hd"),
        [{href => vr_errors('InvalidField')}]
    );
};

subtest 'imagead or creative' => sub {
    local *vr_add = sub {
        my %banner = @_;
        validate_add_imagead_banners(
            [Direct::Model::BannerImageAd->new(
                href => 'http://ya.ru',
                (exists $banner{image} ? (image => Direct::Model::Image->new()) : ()),
                (exists $banner{creative} ? (creative => Direct::Model::BannerCreative->new()) : ()),
                client_id => 1
             )],
            Direct::Model::AdGroupText->new(geo => 255, campaign => $ru_camp, banners_count => 0)
        );
    };

    throws_ok (sub {
       [Test::Direct::Model::BannerImageAd->new(
           href => 'http://ya.ru',
           creative => Direct::Model::BannerCreative->new(),
           image_ad => Direct::Model::Image->new(),
           client_id => 1
        )]
       },
       qr/Cannot apply `image_ad` and `creative` together/
    );
#    cmp_validation_result($vr1, [vr_errors(qr/одновременно задать креатив и/)]);
};

subtest 'banner lang accordance adgroup geo' => sub {
    my $kk_camp = Direct::Model::Campaign->new(content_lang => 'kk');
    my $blank_camp = Direct::Model::Campaign->new(content_lang => undef);

    local *vr_add = sub {
        my %banner = @_;

        my $creative = Direct::Model::CanvasCreative->new(
            _moderate_info => exists $banner{texts} ? to_json({texts => [map { {text => $_} } @{$banner{texts}}]}, {utf8 => 0}) : undef
        );
        my $rel = Direct::Model::BannerCreative->new(creative => $creative);
        validate_add_imagead_banners(
            [Direct::Model::BannerImageAd->new(
                creative => $rel,
                href => "http://yandex.ru/maps",
                client_id => 1812
             )],
            Direct::Model::AdGroupText->new(geo => $banner{geo}, campaign => $banner{camp}, banners_count => 7)
        );
    };

    ok_validation_result(vr_add(camp => $kk_camp, geo => 159, texts => ["Астана жаңалықтары"]));
    cmp_validation_result(
       vr_add(camp => $kk_camp, geo => 225, texts => ["Астана жаңалықтары"]),
       [{text_lang => vr_errors('BadLang')}]
    );

    cmp_validation_result(
       vr_add(camp => $blank_camp, geo => 983, texts => ["ІСТОРИЧНА ФОНОЛОГІЯ УКРАЇНСЬКОЇ МОВИ"]),
       [{text_lang => vr_errors('BadLang')}]
    );
};



done_testing;
