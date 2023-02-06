use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;

use Yandex::DBUnitTest qw/:all/;

use Direct::Model::Banner::Constants;

use Settings qw/
    $DISALLOW_BANNER_LETTER_RE
    $MAX_TITLE_LENGTH
    $MAX_TITLE_UNINTERRUPTED_LENGTH
    $MAX_BODY_LENGTH
    $MAX_BODY_UNINTERRUPTED_LENGTH
    PPCDICT
/;
use Yandex::Test::Tools qw/generate_string/;

BEGIN {
    use_ok('Direct::Model::Banner');
    use_ok('Direct::Model::BannerText');
    use_ok('Direct::Model::AdGroup');
    use_ok('Direct::Model::SitelinksSet');
    use_ok('Direct::Model::Sitelink');
    use_ok('Direct::Validation::Banners', qw/
        validate_banners
    /);
}

package Test::Direct::Model::Campaign {
    use Mouse;
    extends 'Direct::Model::Campaign';
    with 'Direct::Model::Campaign::Role::BsQueue';
    1;
};

copy_table(PPCDICT, 'ppc_properties');

my %banner_hash = (
    title => 'Заголовок объявления',
    body => 'Тело объявления',
    href => 'http://ya.ru/',
    client_id => 1,
);

subtest 'validate_banners' => sub {
    local *vr = sub {
        my %banner = (%banner_hash, @_);
        validate_banners([Direct::Model::BannerText->new(
            banner_type => 'text',
            sitelinks_set_id => undef,
            vcard_id => undef,
            language => $Direct::Model::Banner::Constants::BANNER_NO_LANGUAGE,
            $banner{sitelinks} ? (
                sitelinks_set => Direct::Model::SitelinksSet->new(links => [map { Direct::Model::Sitelink->new(%$_) } @{delete($banner{sitelinks})}]),
            ) : (),
            title_extension => undef,
            %banner,
        )], Direct::Model::AdGroup->new(geo => "0", campaign=>Test::Direct::Model::Campaign->new(content_lang => '')));
    };

    #
    # title
    #
    cmp_validation_result(vr(title => $_), [{title => vr_errors('ReqField')}]) for ('', ' ', " \n\t  ");
    ok_validation_result(vr(title => "yyy #aaa# xxx"));
    ok_validation_result(vr(title => 'simple mark©'));
    cmp_validation_result(vr(title => "#aaa# #bbb#"), [{title => vr_errors('BadTemplate')}], 'double template labels');
    cmp_validation_result(vr(title => $_), [{title => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => $MAX_TITLE_LENGTH);
    cmp_validation_result(vr(title => 'a' x ($MAX_TITLE_LENGTH + 1)), [{title => vr_errors('MaxLength')}]);
    ok_validation_result(vr(title => generate_string(re => qr/[^ \#\-]/, not_re => $DISALLOW_BANNER_LETTER_RE, len => $MAX_TITLE_UNINTERRUPTED_LENGTH)));
    cmp_validation_result(vr(title => generate_string(re => qr/[^ \#\-]/, not_re => $DISALLOW_BANNER_LETTER_RE, len => $MAX_TITLE_UNINTERRUPTED_LENGTH + 1)), [{title => vr_errors('BadUsage')}]);
    cmp_validation_result(vr(title => " \x{00a0},rr"), [{title => vr_errors('BadUsage')}]);
    ok_validation_result(vr(title => "Заголовок баннера"));

    #
    # body
    #
    cmp_validation_result(vr(body => $_), [{body => vr_errors('ReqField')}]) for ('', ' ', " \n\t  ");
    ok_validation_result(vr(body => "yyy #aaa# xxx"));
    ok_validation_result(vr(body => 'simple mark©'));
    cmp_validation_result(vr(body => "#aaa# #bbb#"), [{body => vr_errors('BadTemplate')}], 'double template labels');
    cmp_validation_result(vr(body => $_), [{body => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => $MAX_BODY_LENGTH);
    cmp_validation_result(vr(body => 'a' x ($MAX_BODY_LENGTH + 1)), [{body => vr_errors('MaxLength')}]);
    ok_validation_result(vr(body => generate_string(re => qr/[^ \#\-]/, not_re => $DISALLOW_BANNER_LETTER_RE, len => $MAX_BODY_UNINTERRUPTED_LENGTH)));
    cmp_validation_result(vr(body => generate_string(re => qr/[^ \#\-]/, not_re => $DISALLOW_BANNER_LETTER_RE, len => $MAX_BODY_UNINTERRUPTED_LENGTH + 1)), [{body => vr_errors('BadUsage')}]);
    cmp_validation_result(vr(body => " \x{00a0},rr"), [{body => vr_errors('BadUsage')}]);
    ok_validation_result(vr(body => "Заголовок баннера"));

    #
    # templates
    #
    cmp_validation_result(vr($_ => "тест *шаблон*"), [{$_ => vr_errors('BadTemplate')}], 'deprecated template') for qw/title body/;
    cmp_validation_result(vr($_ => "##"), [{$_ => vr_errors('BadTemplate')}], 'empty template') for qw/title body/;
    cmp_validation_result(vr($_ => "баннер #шаблон#"), [{$_ => vr_errors('BadTemplate')}]) for qw/title body/;

    #
    # href
    #
    ok_validation_result(vr(href => $_)) for (
        'http://www.rbc.ru', 'http://www.rbc.ru/sadf/sdf.html?asdfasdf&sdaf=asdf#asdfsad',
        'http://www.rbc.ru#sdf.html', 'http://www.rbc.jhlkjhhru',
        'http://правительство.рф', 'http://xn--d1abbgf6aiiy.xn--p1ai',
    );

    # ссылки с разными необычными символами: () [] {} | 
    ok_validation_result(vr(href => "http://www.intel.com/ru_ru/business/itcenter/index.htm?cid=emea:yan|vprodtop_ru_desktop|ru891DE|s"));
    ok_validation_result(vr(href => "http://ad.doubleclick.net/clk;228044917;38592682;t?http://www.klm.com/travel/ru_ru/index.htm?popup=no&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=RU%20KLM%20Branding&WT.vr.rac_cr=KLM&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Yandex&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1"));
    ok_validation_result(vr(href => "http://ad.doubleclick.net/clk;228044917;38592682;t?http://www.klm.com/travel/ru_ru/index.htm?popup=no&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=RU%20KLM%20Branding&WT.vr.rac_cr=KLM&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Yandex&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1"));
    ok_validation_result(vr(href => "http://www.adtraction.com/t/t?a=102378751&as=50941721&t=2&tk=1&epi=(11!0!вытяжки!0)&url=http://electrolux.ru/node36.aspx?categoryid=9105"));
    ok_validation_result(vr(href => "http://www.adtraction.com/t/t?a=102378751&as=50941721&t=2&tk=1&epi=(11!0!вытяжки!0)&url=http://electrolux.ru/node36.aspx?categoryid=9105"));
    ok_validation_result(vr(href => "http://www.klm.com/travel/ru_ru/apps/ebt/ebt_home.htm?c[0].ds=ZRH&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=Yandex&WT.vr.rac_cr=Zurich&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Google&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1"));
    ok_validation_result(vr(href => "http://www.vsedlyauborki.ru/catalog/5/25#Derjateli_shubok_dlya_myt'ya_okon"));

    ok_validation_result(vr(href => "http://landing.company/"));
    ok_validation_result(vr(href => "http://landing.international/"));
    # домен первого уровня до 15 [a-z] символов
    ok_validation_result(vr(href => "http://landing.qweasdzxcrqweas/"));

    cmp_validation_result(vr(href => "httpU://www.rbc.ru"), [{href => vr_errors('InvalidField')}]);
    cmp_validation_result(vr(href => "127.0.0.1:80/2313"), [{href => vr_errors('InvalidField')}]);
    cmp_validation_result(vr(href => "12345"), [{href => vr_errors('InvalidField')}]);
    cmp_validation_result(vr(href => "http://landing.qweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcroooz/"), [{href => vr_errors('InvalidField')}]);

    #
    # href + sitelinks + vcard
    #
    cmp_validation_result(vr(href => undef, sitelinks_set_id => 1, vcard_id => 1), [vr_errors('InconsistentState')], 'sitelinks_set without href');
    # Основная ссылка баннера теперь может дублировать быстрые ссылки
    ok_validation_result(vr(href => 'http://ya.ru', sitelinks_set_id => 1, sitelinks => [{title => '1', href => 'http://ya.ru', description => undef}]));
    # такой баннер (без визитки и href) не разрешен для текстовых баннеров, а для базового баннера допустимо
    ok_validation_result(vr(href => undef, vcard_id => undef));


    # display_href
    ok_validation_result(vr(display_href => $_)) for (
        undef,
        'зелёная-ссылка',
        'у-попа/была-#шаблон#',
        'яНеВерблюд',
        ('ж' x $Direct::Validation::Banners::MAX_DISPLAY_HREF_LENGTH),
        '#' . ('ж' x $Direct::Validation::Banners::MAX_DISPLAY_HREF_LENGTH) . '#',
    );

    cmp_validation_result(
        vr(display_href => ''),
        [{display_href => vr_errors('InvalidFormat')}],
        'empty display_href'
    );

    cmp_validation_result(
        vr(display_href => 'ж' x ($Direct::Validation::Banners::MAX_DISPLAY_HREF_LENGTH+1)),
        [{display_href => vr_errors('MaxLength')}],
        'too long display_href'
    );

    cmp_validation_result(
        vr(display_href => "ммм\\жжж"),
        [{display_href => vr_errors('InvalidChars')}],
        'display_href invalid chars'
    );

    cmp_validation_result(
        vr(display_href => "#ммм#/#жжж#"),
        [{display_href => vr_errors('BadTemplate')}],
        'display_href double template'
    );

    cmp_validation_result(
        vr(display_href => "зелёная--ссылка"),
        [{display_href => vr_errors('InvalidFormat')}],
        'display_href double char'
    );

    cmp_validation_result(
        vr(href => undef, display_href => 'зелёная-ссылка'),
        [vr_errors('InconsistentState')],
        'display_href without href'
    );

    #
    # old tests
    #

    ok_validation_result(vr(
        title => "фраза\x{00a0}«не\x{00a0}перенесётся»",
        body => "и\x{00a0}тело\x{00a0}«не\x{00a0}перенесётся»",
    )); # баннер с неразрывным пробелом и кавычками-ёлочками в заголовке и теле

    ok_validation_result(vr(
        title => 'ы' x $Settings::MAX_TITLE_UNINTERRUPTED_LENGTH,
        body => 'ы' x $Settings::MAX_BODY_UNINTERRUPTED_LENGTH,
    )); # баннер с неразрывной строкой предельной длины в заголовке и теле

    cmp_validation_result(vr(
        title => 'ы' x ($Settings::MAX_TITLE_UNINTERRUPTED_LENGTH + 1),
    ), [{title => vr_errors('BadUsage')}]); # баннер с неразрывной строкой больше предельной длины в заголовке

    cmp_validation_result(vr(
        body => 'ы' x ($Settings::MAX_BODY_UNINTERRUPTED_LENGTH + 1),
    ), [{body => vr_errors('BadUsage')}]); # баннер с неразрывной строкой больше предельной длины в теле

    cmp_validation_result(vr(
        title => "заголовок\nбаннера\n",
        body => "текст\nбаннера\n",
    ), [{title => vr_errors('InvalidChars'), body => vr_errors('InvalidChars')}]); # баннер с переводом строки в середине заголовка/текста (smartstrip не используем)

    cmp_validation_result(vr(title => 'арақ 1', sitelinks => [{title => 'арақ 2', href => 'http://ya.ru', description => undef}], client_id => 1), [{text_lang => vr_errors('BadLang')}], 'bad lang in banner and sitelinks');
};

done_testing;
