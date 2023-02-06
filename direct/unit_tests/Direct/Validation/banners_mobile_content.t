use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;
use Settings;

use Yandex::DBUnitTest qw/:all/;
use KnownDomains;

use Direct::Model::Banner::Constants;

use Settings qw/
    $MAX_BODY_LENGTH
    $MAX_BODY_LENGTH_MOBILE
    $MAX_NUMBER_OF_NARROW_CHARACTERS
    PPCDICT
    /;

BEGIN {
    use_ok('Direct::Model::BannerMobileContent');
    use_ok('Direct::Model::AdGroupMobileContent');
    use_ok('Direct::Model::Campaign');
    use_ok('Direct::Validation::BannersMobileContent', qw/
        validate_add_mobile_banners
        validate_update_mobile_banners
    /);
    use_ok('Direct::Validation::AdGroupsMobileContent', qw/
        validate_add_banners_mobile_adgroup
    /);
}

# здесь не хватает проверок на общие атрибуты баннера (title, body, geo etc.)

copy_table(PPCDICT, 'trusted_redirects');
copy_table(PPCDICT, 'ppc_properties');
KnownDomains::add_domain(mobile_app_counter => 'measurementapi.com');
KnownDomains::add_domain(mobile_app_counter => 'appmetrica.yandex.com');
KnownDomains::add_domain(mobile_app_counter => 'onelink.me');

# support custom link
KnownDomains::add_domain(mobile_app_counter => 'target.zorkamobi.com');
KnownDomains::add_domain(mobile_app_counter => 'target_https.zorkamobi.com', { https_only => 1 });
KnownDomains::add_domain(mobile_app_counter => 'target_wildcard.zorkamobi.com', { allow_wildcard => 1});
KnownDomains::add_domain(mobile_app_counter => 'target_https_and_wildcard.zorkamobi.com', { https_only => 1, allow_wildcard => 1 });

my $camp = Direct::Model::Campaign->new(content_lang=>'');
my $banners_1 = [
    Direct::Model::BannerMobileContent->new(
        href => undef,
        title => 'Игры на Google Play',
        body => 'Огромная коллекция бесплатных игр для смартфонов или планшетов Андроид!',
        reflected_attrs => ['icon'], 
        primary_action => 'download', 
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),    
    Direct::Model::BannerMobileContent->new(
        href => undef,
        title => 'Игры для Android на Google Play!',
        body => 'Сотни увлекательных игр на Google Play. Скачивай на свой Android и играй!',
        reflected_attrs => [], 
        primary_action => 'download',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'https://87340.measurementapi.com/ОАР1626#12',
        title => 'Играйте онлайн в игру-приключение',
        body => 'Строй и селись, торгуй и завоевывай. Создай королевство у себя в браузере!',
        # информации по приложению ещё нет (принимаем авансом)
        reflected_attrs => ['price', 'icon'], 
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
];

my $adgroup_1 = new Direct::Model::AdGroupMobileContent(
    geo => '1',
    store_content_href => 'https://play.google.com/store/apps/details?id=com.miniclip.agar.io',
    device_type_targeting => ['phone', 'tablet'],
    network_targeting => ['cell'],
    banners_count => 4,
    campaign => $camp,
);

ok_validation_result(
    validate_add_mobile_banners($banners_1, $adgroup_1)    
);

my $banners_2 = [
    Direct::Model::BannerMobileContent->new(
        href => undef,
        title => 'Candy Crush Saga',
        body => 'Присоединяйтесь к Тиффи и господину Тоффи в их увлекательном путешествии по...',
        reflected_attrs => ['icon', 'rating'], 
        primary_action => 'play', 
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),    
    Direct::Model::BannerMobileContent->new(
        href => 'http://clck.ru/1DHY01',
        title => 'Candy Crush Saga',
        body => 'Candy Crush Saga, от создателей Pet Rescue Saga и Farm Heroes Saga!',
        reflected_attrs => ['icon'], 
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
];

my $adgroup_2 = new Direct::Model::AdGroupMobileContent(
    geo => '225',
    store_content_href => 'https://play.google.com/store/apps/details?id=com.king.candycrushsaga&hl=ru',
    device_type_targeting => ['phone'],
    network_targeting => ['wifi'],
    banners_count => $Settings::DEFAULT_CREATIVE_COUNT_LIMIT - 1,
    campaign => $camp,
);

cmp_validation_result(
    validate_add_banners_mobile_adgroup($banners_2, $adgroup_2),
    {
        generic_errors => vr_errors('LimitExceeded'),
        objects_results => [{body => vr_errors('MaxLength')}, {href => vr_errors('BadUsage') }]
    }
);

my $banners_3 = [
    Direct::Model::BannerMobileContent->new(
        href => 'https://appmetrica.yandex.com/serve/7438548069196542965/?click_id={LOGID}',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'], 
        primary_action => 'play',        
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
];

ok_validation_result(validate_add_mobile_banners($banners_3, $adgroup_2));

my $banners_4 = [
    Direct::Model::BannerMobileContent->new(
        href => 'http://go.onelink.me/1966161688?pid=yandex&c=morda&af_dp=yandexmusic%3A%2F%2F&af_web_dp=http%3A%2F%2Fmusic.yandex.ru',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'], 
        primary_action => 'play',        
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'kjk329392quoejda',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'], 
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => '       ',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'], 
        primary_action => 'play',        
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'http://m.onelink.me/1fe662e4.',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'], 
        primary_action => 'play',        
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
];

cmp_validation_result(
    validate_update_mobile_banners($banners_4, $adgroup_2),
    [
        { },
        { href => vr_errors('InvalidField') },
        { href => vr_errors('EmptyField') },
        { },
    ]
);


my $banners_5 = [
    Direct::Model::BannerMobileContent->new(
        href => 'http://target.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'], 
        primary_action => 'play',        
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'http://target_https.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'],
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'https://target_https.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'],
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'http://b.a.target_wildcard.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'],
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'http://a.target_wildcard.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'],
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'http://a.target_https_and_wildcard.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'],
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerMobileContent->new(
        href => 'https://a.target_https_and_wildcard.zorkamobi.com/some/open/link',
        title => 'Subway Surfers',
        body => 'УСКОРЯЙСЯ так быстро, как только сможешь!',
        reflected_attrs => ['icon', 'rating_votes', 'rating'],
        primary_action => 'play',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
];

cmp_validation_result(
    validate_update_mobile_banners($banners_5, $adgroup_2),
    [
        { },
        { href => vr_errors('BadUsage') },
        { },
        { href => vr_errors('BadUsage') },
        { },
        { href => vr_errors('BadUsage') },
        { },
    ]
);



done_testing;
