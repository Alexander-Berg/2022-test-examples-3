use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;

use Direct::Model::Banner::Constants;

use Yandex::DBUnitTest qw/:all/;

use Settings qw/
    $MAX_BODY_LENGTH
    $MAX_BODY_LENGTH_MOBILE
    $MAX_TITLE_LENGTH
    $MAX_TITLE_EXTENSION_LENGTH
    $MAX_NUMBER_OF_NARROW_CHARACTERS
    PPCDICT
    /;

my %banner_hash = (
    title => 'Заголовок объявления',
    body => 'Тело объявления',
    href => 'http://ya.ru/',
    client_id => 1,
);

copy_table(PPCDICT, 'ppc_properties');

BEGIN {
    use_ok('Direct::Model::BannerText');
    use_ok('Direct::Model::AdGroup');
    use_ok('Direct::Model::SitelinksSet');
    use_ok('Direct::Model::Sitelink');
    use_ok('Direct::Model::Campaign');
    use_ok('Direct::Model::VideoAddition');
    use_ok('Direct::Model::BannerCreative');
    use_ok('Direct::Validation::BannersText', qw/
        validate_add_text_banners
        validate_update_text_banners
    /);
}
my $campaign = Direct::Model::Campaign->new(content_lang => '');

cmp_validation_result(vr(href => undef, vcard_id => undef), [vr_errors('InconsistentState')]);
cmp_validation_result(vr(href => 'jfjf39393_12', vcard_id => undef), [{href => vr_errors('InvalidField')}]);
ok_validation_result(vr(href => 'http://direct.yandex.com.tr?utm_camp=_38312_', vcard_id => 7891));
ok_validation_result(vr(href => undef, vcard_id => 7891));
ok_validation_result(vr(href => 'http://maps.yandex.ru', vcard_id => undef));

cmp_validation_result(vr(body => 'a' x ($MAX_BODY_LENGTH)), [{body => vr_errors('BadUsage')}]);
cmp_validation_result(vr(body => 'a' x ($MAX_BODY_LENGTH + 1)), [{body => vr_errors('MaxLength')}]);
cmp_validation_result(vr(body => 'a' x ($MAX_BODY_LENGTH_MOBILE + 1)), [{body => vr_errors('BadUsage')}]);
cmp_validation_result(vr(body => '.' x ($MAX_NUMBER_OF_NARROW_CHARACTERS + 1)), [{body => vr_errors('BadUsage')}]);
ok_validation_result(vr(body => '.' x $MAX_NUMBER_OF_NARROW_CHARACTERS));
cmp_validation_result(vr(body => ('a' x ($MAX_BODY_LENGTH)).','), [{body => vr_errors('BadUsage')}]);

cmp_validation_result(vr(title => 'a' x ($MAX_TITLE_LENGTH)), [{title => vr_errors('BadUsage')}]);
cmp_validation_result(vr(title => 'a' x ($MAX_TITLE_LENGTH + 1)), [{title => vr_errors('MaxLength')}]);
cmp_validation_result(vr(title => 'a' x ($MAX_TITLE_LENGTH - 1)), [{title => vr_errors('BadUsage')}]);
cmp_validation_result(vr(title => '.' x ($MAX_NUMBER_OF_NARROW_CHARACTERS + 1)), [{title => vr_errors('BadUsage')}]);
ok_validation_result(vr(title => '.' x $MAX_NUMBER_OF_NARROW_CHARACTERS));
cmp_validation_result(vr(title => ('a' x ($MAX_TITLE_LENGTH)).','), [{title => vr_errors('BadUsage')}]);

cmp_validation_result(vr(title_extension => 'a' x ($MAX_TITLE_EXTENSION_LENGTH)), [{title_extension => vr_errors('BadUsage')}]);
cmp_validation_result(vr(title_extension => 'a' x ($MAX_TITLE_EXTENSION_LENGTH + 1)), [{title_extension => vr_errors('MaxLength')}]);
cmp_validation_result(vr(title_extension => '.' x ($MAX_NUMBER_OF_NARROW_CHARACTERS + 1)), [{title_extension => vr_errors('BadUsage')}]);
ok_validation_result(vr(title_extension => '.' x $MAX_NUMBER_OF_NARROW_CHARACTERS));
cmp_validation_result(vr(title_extension => ('a' x ($MAX_TITLE_EXTENSION_LENGTH)).','), [{title_extension => vr_errors('BadUsage')}]);

my $banners = [
    Direct::Model::BannerText->new(
        banner_type => 'text',
        adgroup => Direct::Model::AdGroup->new(geo => "0", campaign => $campaign),
        sitelinks_set_id => undef,
        vcard_id => 23101,        
        href => undef,
        title => 'Проекты домов для Юга России',
        body => 'Проектирование домов, коттеджей. Доставка проектов почтой или курьером',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),    
    Direct::Model::BannerText->new(
        banner_type => 'text',
        adgroup => Direct::Model::AdGroup->new(geo => "0", campaign => $campaign),
        sitelinks_set_id => undef,
        vcard_id => undef,        
        href => undef,
        title => 'ПРОЕКТЫ',
        body => 'Проектирование и строительство. Дома, коттеджи, магазины, офисы',
        client_id => 1,
	language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
    Direct::Model::BannerText->new(
        banner_type => 'text',
        adgroup => Direct::Model::AdGroup->new(geo => "0", campaign => $campaign),
        sitelinks_set_id => undef,
        vcard_id => undef,        
        href => 'http://www.projects.com.ru',
        title => 'ПРОЕКТЫ',
        body => 'Проектирование и строительство. Дома, коттеджи, магазины, офисы',
        client_id => 1,
        language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
    ),
];

cmp_validation_result(
    Direct::Validation::BannersText::validate_text_banners($banners, Direct::Model::AdGroup->new(geo => "1", campaign => $campaign)),
    [{}, vr_errors('InconsistentState'), {}]
);

{
    my $banner = Direct::Model::BannerText->new(
        banner_type => 'text',
        adgroup => Direct::Model::AdGroup->new(geo => "0", campaign => $campaign),
        sitelinks_set_id => undef,
        vcard_id => undef,        
        href => 'http://www.projects.com.ru',
        title => 'ПРОЕКТЫ',
        body => 'Проектирование и строительство. Дома, коттеджи, магазины, офисы',
        client_id => 1,
        language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
        creative => Direct::Model::BannerCreative->new(
            creative => Direct::Model::VideoAddition->new(
                duration => 15,
                layout_id => 2,
            )
        ),
    );
    my $vr = Direct::Validation::BannersText::validate_text_banners([$banner], Direct::Model::AdGroup->new(geo => "1", campaign => $campaign));
    ok_validation_result($vr);
}

{
    my $banner = Direct::Model::BannerText->new(
        banner_type => 'text',
        adgroup => Direct::Model::AdGroup->new(geo => "0", campaign => $campaign),
        sitelinks_set_id => undef,
        vcard_id => undef,        
        href => 'http://www.projects.com.ru',
        title => 'ПРОЕКТЫ',
        body => 'Проектирование и строительство. Дома, коттеджи, магазины, офисы',
        language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
        client_id => 1,
        creative => Direct::Model::BannerCreative->new(
            creative => Direct::Model::VideoAddition->new(
                duration => 42,
                layout_id => 2,
            )
        )
    );
    my $vr = Direct::Validation::BannersText::validate_text_banners([$banner], Direct::Model::AdGroup->new(geo => "1", campaign => $campaign));
    cmp_validation_result($vr, [{creative => vr_errors('BadUsage')}]);
}

done_testing;


sub vr {
    my %banner = (%banner_hash, @_);    
    Direct::Validation::BannersText::validate_text_banners([Direct::Model::BannerText->new(
        banner_type => 'text',
        sitelinks_set_id => undef,
        vcard_id => undef,
        language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
        $banner{sitelinks} ? (
            sitelinks_set => Direct::Model::SitelinksSet->new(links => [map { Direct::Model::Sitelink->new(%$_) } @{delete($banner{sitelinks})}]),
        ) : (),
        %banner,
    )], Direct::Model::AdGroup->new(geo => "0", campaign => $campaign));
}
