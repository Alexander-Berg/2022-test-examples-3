use Direct::Modern;

use Test::More;
use Test::Exception;
use Yandex::Test::ValidationResult;

use Settings qw/
    $ALLOW_LETTERS
    $DISALLOW_BANNER_LETTER_RE
/;

use Yandex::Test::Tools qw/generate_string/;

BEGIN {
    use_ok('Direct::Model::VCard');
    use_ok('Direct::Validation::VCards', qw/validate_vcards/);
}

{
    package Test::Model::VCard;
    use Mouse;
    extends qw(Direct::Model::VCard);
    has banners_count => (is => 'rw', isa => 'Int');
    has banners_count_without_href => (is => 'rw', isa => 'Int');
};

my %vcard_hash = (
    name => 'Название организации',
    country => 'Россия',
    city => 'Москва',
    work_time => '0#4#9#00#21#00',
    phone => '+7#495#123-45-47#',
    contact_person => 'Маша',
    street => 'Льва Толстого',
    house => '16',
    building => undef,
    apartment => undef,
    extra_message => undef,
    im_client => undef,
    im_login => undef,
    ogrn => undef,
    contact_email => 'test@ya.ru',
    metro => undef,
);

subtest 'validate_vcards' => sub {
    local *vr = sub {
        my %vcard = (%vcard_hash, @_);
        validate_vcards([Direct::Model::VCard->new(%vcard)]);
    };

    #
    # name
    #
    cmp_validation_result(vr(name => $_), [{name => vr_errors('ReqField')}]) for (undef, '', ' ', " \n\t  ");
    cmp_validation_result(vr(name => $_), [{name => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 200);
    ok_validation_result(vr(name => $_)) for generate_string(not_re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 200);
    ok_validation_result(vr(name => 'a' x 255));
    dies_ok { my $x = vr(name => 'a' x 256); cmp_validation_result($x, [{name => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of title';

    #
    # country
    #
    cmp_validation_result(vr(country => $_), [{country => vr_errors('ReqField')}]) for (undef, '', ' ', " \n\t  ");
    cmp_validation_result(vr(country => $_), [{country => vr_errors('InvalidChars')}]) for generate_string(not_re => qr/^[${ALLOW_LETTERS}\- \(\)]+$/i, chunk_len => 50);
    ok_validation_result(vr(country => $_)) for generate_string(re => qr/^[${ALLOW_LETTERS}\- \(\)]+$/i, chunk_len => 50);
    dies_ok { my $x = vr(country => 'a' x 51); cmp_validation_result($x, [{country => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of country';

    #
    # city
    #
    cmp_validation_result(vr(city => $_), [{city => vr_errors('ReqField')}]) for (undef, '', ' ', " \n\t  ");
    cmp_validation_result(vr(city => $_), [{city => vr_errors('InvalidChars')}]) for generate_string(not_re => qr/^[${ALLOW_LETTERS}\- \(\)]+$/i, chunk_len => 55);
    ok_validation_result(vr(city => $_)) for generate_string(re => qr/^[${ALLOW_LETTERS}\- \(\)]+$/i, chunk_len => 55);
    dies_ok { my $x = vr(city => 'a' x 56); cmp_validation_result($x, [{city => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of city';

    #
    # work_time
    #
    cmp_validation_result(vr(work_time => $_), [{work_time => vr_errors('ReqField')}]) for (undef, '', ' ', " \n\t  ");
    cmp_validation_result(vr(work_time => 'd#4#10#00#18#00'), [{work_time => vr_errors('InvalidFormat')}]);
    cmp_validation_result(vr(work_time => '3#e#10#00#18#00'), [{work_time => vr_errors('InvalidFormat')}]);
    cmp_validation_result(vr(work_time => '7#4#10#00#18#00'), [{work_time => vr_errors('InvalidFormat')}]);
    cmp_validation_result(vr(work_time => '1#9#10#00#18#00'), [{work_time => vr_errors('InvalidFormat')}]);
    cmp_validation_result(vr(work_time => '1#5#10#17#18#16'), [{work_time => vr_errors('InvalidFormat')}], 'minutes are not multiple 15');
    cmp_validation_result(vr(work_time => '1#5#24#15#25#15'), [{work_time => vr_errors('InvalidFormat')}], 'hours > 23');
    cmp_validation_result(vr(work_time => '1#5#10#60#25#61'), [{work_time => vr_errors('InvalidFormat')}], 'minutes > 59');
    cmp_validation_result(vr(work_time => '0#4#10#00#18#00;4#4#10#00#18#00'), [{work_time => vr_errors('InvalidFormat')}], 'multiple periods for the same day');
    cmp_validation_result(vr(work_time => '0#4#10#00#18#00;5#1#10#00#18#00'), [{work_time => vr_errors('InvalidFormat')}], 'multiple periods for the same day');
    ok_validation_result(vr(work_time => '1#4#10#00#18#00'));
    ok_validation_result(vr(work_time => '4#1#10#00#18#00'));
    ok_validation_result(vr(work_time => '1#4#10#00#18#00;5#6#13#00#16#00'));

    #
    # phone
    #
    cmp_validation_result(vr(phone => $_), [{phone => vr_errors('ReqField')}]) for (undef, '', ' ', " \n\t  ");
    cmp_validation_result(vr(phone => '#495#123-45-67#'), [{phone => {country_code => vr_errors('InvalidField')}}], 'no country code');
    cmp_validation_result(vr(phone => '+7##123-45-67#'), [{phone => {city_code => vr_errors('InvalidField')}}], 'no city code');
    cmp_validation_result(vr(phone => '+7#495##'), [{phone => {phone_short => vr_errors('InvalidField')}}], 'no short phone');
    cmp_validation_result(vr(phone => '+7#495##'), [{phone => {phone_short => vr_errors('InvalidField')}}], 'no short phone');
    ok_validation_result(vr(phone => '+7#495#123-45-67#'));
    cmp_validation_result(vr(phone => 's#495#123-45-67#'), [{phone => {country_code => vr_errors('InvalidFormat')}}], 'invalid country code');
    cmp_validation_result(vr(phone => '+7#0#123-45-67#'), [{phone => {city_code => vr_errors('InvalidFormat')}}], 'invalid city code');
    cmp_validation_result(vr(phone => '+7#s#123-45-67#'), [{phone => {city_code => vr_errors('InvalidFormat')}}], 'invalid city code');
    cmp_validation_result(vr(phone => '+7#123456#123-45-67#'), [{phone => {city_code => vr_errors('InvalidFormat')}}], 'invalid city code');
    cmp_validation_result(vr(phone => '+7#1234#abcde#'), [{phone => {phone_short => vr_errors('InvalidFormat')}}], 'invalid short phone');
    cmp_validation_result(vr(phone => '7#495#123-45-67#'), [{phone => {country_code => vr_errors('InvalidFormat')}}], 'no plus sign in country code');
    ok_validation_result(vr(phone => '8#800#123-45-67#'));
    cmp_validation_result(vr(phone => '+8#0800#123-45-67#'), [{phone => {country_code => vr_errors('InvalidFormat')}}], 'invalid plus sign in country code "8 800"');
    cmp_validation_result(vr(phone => '+8#0804#123-45-67#'), [{phone => {country_code => vr_errors('InvalidFormat')}}], 'invalid plus sign in country code "8 804"');
    cmp_validation_result(vr(phone => '+0#0800#123-45-67#'), [{phone => {country_code => vr_errors('InvalidFormat')}}], 'invalid plus sign in country code "0 800"');
    ok_validation_result(vr(phone => '+90##444-45-67#'), '+90 _ 444');
    cmp_validation_result(vr(phone => '8#0800#123-45-67#qqw'), [{phone => {phone_ext => vr_errors('InvalidFormat')}}], 'invalid ext phone');

    #
    # contact_person
    #
    ok_validation_result(vr(contact_person => undef));
    cmp_validation_result(vr(contact_person => $_), [{contact_person => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 155);
    ok_validation_result(vr(contact_person => $_)) for generate_string(not_re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 155);
    dies_ok { my $x = vr(contact_person => 'a' x 156); cmp_validation_result($x, [{contact_person => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of contact_person';


    #
    # street
    #
    ok_validation_result(vr(street => undef));
    cmp_validation_result(vr(street => $_), [{street => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 55);
    ok_validation_result(vr(street => $_)) for generate_string(not_re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 55);
    dies_ok { my $x = vr(street => 'a' x 56); cmp_validation_result($x, [{street => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of street';

    #
    # house
    #
    ok_validation_result(vr(house => undef));
    cmp_validation_result(vr(house => $_), [{house => vr_errors('InvalidChars')}]) for generate_string(re => qr/[^\Q$ALLOW_LETTERS\E\\\/\-, ]/, chunk_len => 30);
    ok_validation_result(vr(house => $_)) for generate_string(not_re => qr/[^\Q$ALLOW_LETTERS\E\\\/\-, ]/, chunk_len => 30);
    dies_ok { my $x = vr(house => 'a' x 31); cmp_validation_result($x, [{house => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of house';

    #
    # building
    #
    ok_validation_result(vr(building => undef));
    cmp_validation_result(vr(building => $_), [{building => vr_errors('InvalidChars')}]) for generate_string(re => qr/[^\Q$ALLOW_LETTERS\E\\\/\-, ]/, chunk_len => 10);
    ok_validation_result(vr(building => $_)) for generate_string(not_re => qr/[^\Q$ALLOW_LETTERS\E\\\/\-, ]/, chunk_len => 10);
    dies_ok { my $x = vr(building => 'a' x 11); cmp_validation_result($x, [{building => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of building';

    #
    # apartment
    #
    ok_validation_result(vr(apartment => undef));
    cmp_validation_result(vr(apartment => $_), [{apartment => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 100);
    ok_validation_result(vr(apartment => $_)) for generate_string(not_re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 100);
    dies_ok { my $x = vr(apartment => 'a' x 101); cmp_validation_result($x, [{apartment => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of apartment';

    #
    # extra_message
    #
    ok_validation_result(vr(extra_message => undef));
    cmp_validation_result(vr(extra_message => $_), [{extra_message => vr_errors('InvalidChars')}]) for generate_string(re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 200);
    ok_validation_result(vr(extra_message => $_)) for generate_string(not_re => $DISALLOW_BANNER_LETTER_RE, chunk_len => 200);
    dies_ok { my $x = vr(extra_message => 'a' x 201); cmp_validation_result($x, [{extra_message => vr_errors('MaxLength')}]); die unless $x->is_valid; } 'max length of extra_message';

    #
    # im_client & im_login
    #
    ok_validation_result(vr(im_client => undef, im_login => undef));
    cmp_validation_result(vr(im_client => "icq"), [{im_login => vr_errors('ReqField')}]);
    cmp_validation_result(vr(im_login => "12345"), [{im_client => vr_errors('ReqField')}]);
    cmp_validation_result(vr(im_client => "unknown"), [{im_client => vr_errors('InvalidField')}]);

    ok_validation_result(vr(im_client => "icq", im_login => "1234567"));
    cmp_validation_result(vr(im_client => "icq", im_login => "asdref"), [{im_login => vr_errors('InvalidFormat')}]);
    ok_validation_result(vr(im_client => "mail_agent", im_login => 'ya@'.$_.'.ru')) for qw/mail inbox bk list/;
    cmp_validation_result(vr(im_client => "mail_agent", im_login => 'ya@ya.ru'), [{im_login => vr_errors('InvalidFormat')}]);
    ok_validation_result(vr(im_client => "jabber", im_login => 'ya@ya.ru'));
    cmp_validation_result(vr(im_client => "jabber", im_login => "12345"), [{im_login => vr_errors('InvalidFormat')}]);
    ok_validation_result(vr(im_client => $_, im_login => 'ya@ya.ru')) for qw/skype msn/;
    ok_validation_result(vr(im_client => $_, im_login => 'log.in_skype')) for qw/skype msn/;
    cmp_validation_result(vr(im_client => $_, im_login => "!^&"), [{im_login => vr_errors('InvalidFormat')}]) for qw/skype msn/;

    #
    # contact_email
    #
    ok_validation_result(vr(contact_email => undef));
    cmp_validation_result(vr(contact_email => 'nomail'), [{contact_email => vr_errors('InvalidFormat')}]);
    cmp_validation_result(vr(contact_email => 'yaya'), [{contact_email => vr_errors('InvalidFormat')}]);
    ok_validation_result(vr(contact_email => 'ya@ya.ru'));
    ok_validation_result(vr(contact_email => 'имя@домен'));
    ok_validation_result(vr(contact_email => '@домен'));
    ok_validation_result(vr(contact_email => 'имя@'));
    ok_validation_result(vr(contact_email => '@'));
    ok_validation_result(vr(contact_email => 'ya+ya@ya.ru'));

    #
    # organization_details: ogrn
    #
    cmp_validation_result(vr(ogrn => $_), [{ogrn => vr_errors('InvalidField')}]) for ('qwerty', '1' x 12, '2' x 14, '3' x 16);

    cmp_validation_result(vr(ogrn => "7027739358778"), [{ogrn => vr_errors('InvalidField')}], 'First number is invalid');
    cmp_validation_result(vr(ogrn => "10277r935t778"), [{ogrn => vr_errors('InvalidField')}], 'Has letters');
    cmp_validation_result(vr(ogrn => "102477455778"), [{ogrn => vr_errors('InvalidField')}], 'Short number');
    cmp_validation_result(vr(ogrn => "10247745577845"), [{ogrn => vr_errors('InvalidField')}], 'Long number (14)');
    cmp_validation_result(vr(ogrn => "10247745577843455"), [{ogrn => vr_errors('InvalidField')}], 'Long number (17)');
    cmp_validation_result(vr(ogrn => "1027739019204"), [{ogrn => vr_errors('InvalidField')}], 'Wrong checksum (OGRN)');
    cmp_validation_result(vr(ogrn => "3045402208000t32"), [{ogrn => vr_errors('InvalidField')}], 'Letter after 11 symbols');
    cmp_validation_result(vr(ogrn => "310253706101022"), [{ogrn => vr_errors('InvalidField')}], 'Wrong checksum (OGRNIP)');

    ok_validation_result(vr(ogrn => "1027739358778"), 'Valid OGRN');
    ok_validation_result(vr(ogrn => "1037723007960"), 'Diff is 10 (but check num is 0)');
    ok_validation_result(vr(ogrn => "304540220800032"), 'Valid OGRNIP');

    #
    # metro
    #
    ok_validation_result(vr(metro => $_)) for (undef, 0);
    cmp_validation_result(vr(metro => 1, city => undef), [{city => vr_errors('ReqField'), metro => vr_errors('InvalidField')}], 'No city defined');

    #
    # TODO: x, y, x1, x2, y1, y2
    #
};

done_testing;
