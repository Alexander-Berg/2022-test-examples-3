use Direct::Modern;

use Test::More;
use Test::Deep;
use Test::Exception;
use Yandex::Test::ValidationResult;

use Yandex::Test::Tools qw/generate_string/;

use Settings qw/
    $DISALLOW_BANNER_LETTER_RE
/;

BEGIN {
    use_ok('Direct::Model::Sitelink');
    use_ok('Direct::Model::SitelinksSet');
    use_ok('Direct::Model::Banner');
    use_ok('Direct::Validation::SitelinksSets', qw/
        $ONE_SITELINK_MAX_LENGTH
        $SITELINKS_NUMBER
        $SITELINKS_MAX_LENGTH

        validate_sitelinks_sets
    /);
}

{
    package Test::Model::SitelinksSet;
    use Mouse;
    extends 'Direct::Model::SitelinksSet';
    has 'banners_count' => (is => 'rw', isa => 'Int');
};


subtest 'validate_sitelinks_sets' => sub {
    local *vr = sub {
        validate_sitelinks_sets([Direct::Model::SitelinksSet->new(
            links => [map { Direct::Model::Sitelink->new(%$_) } @_],
        )]);
    };

    #
    # title
    #
    dies_ok { vr({title => undef, description => undef, href => "http://ya.ru/1"}) } 'no undef in title';
    cmp_validation_result(vr({title => $_, description => undef, href => "http://ya.ru/1"}), [{sitelinks => [{title => vr_errors('ReqField')}]}]) for ('', ' ', " \n\t  ");
    cmp_validation_result(vr({title => 'a' x ($ONE_SITELINK_MAX_LENGTH + 1), description => undef, href => "http://ya.ru/1"}), [{sitelinks => [{title => vr_errors('MaxLength')}]}]);
    ok_validation_result(vr({title => $_, description => undef, href => "http://ya.ru/1"}))
        for generate_string(not_re_and => [$DISALLOW_BANNER_LETTER_RE, qr/[!?]/], chunk_len => $ONE_SITELINK_MAX_LENGTH);
    cmp_validation_result(vr({title => $_, description => undef, href => "http://ya.ru/1"}), [{sitelinks => [{title => vr_errors('InvalidChars')}]}])
        for generate_string(re_or => [$DISALLOW_BANNER_LETTER_RE, qr/[!?]/], chunk_len => $ONE_SITELINK_MAX_LENGTH);

    #
    # href
    #
    dies_ok { vr({title => "1", description => undef, href => undef}) } 'no undef in href';
    cmp_validation_result(vr({title => "1", description => undef, href => $_}), [{sitelinks => [{href => vr_errors('ReqField')}]}]) for ('', ' ', " \n\t  ");
    cmp_validation_result(vr({title => "1", description => undef, href => "ya.ru/1"}), [{sitelinks => [{href => vr_errors('InvalidField')}]}]);

    #
    # limit for sitelinks count
    #
    cmp_validation_result(vr(), [vr_errors('LimitExceeded')]);
    ok_validation_result(vr(map { {title => $_, description => undef, href => "http://ya.ru/$_"} } (1 .. $SITELINKS_NUMBER)));
    cmp_validation_result(vr(map { {title => $_, description => undef, href => "http://ya.ru/$_"} } (1 .. ($SITELINKS_NUMBER) + 1)), [vr_errors('LimitExceeded')]);

    #
    # summary limit for length of sitelinks titles
    #
    ok_validation_result(vr(
        (map { {title => $_ x $ONE_SITELINK_MAX_LENGTH, description => undef, href => "http://ya.ru/$_"} } (1 .. int($SITELINKS_MAX_LENGTH / $ONE_SITELINK_MAX_LENGTH))),
        $SITELINKS_MAX_LENGTH % $ONE_SITELINK_MAX_LENGTH ?
            {title => 'x' x ($SITELINKS_MAX_LENGTH % $ONE_SITELINK_MAX_LENGTH), description => undef, href => "http://ya.ru/x"} :
            ()
    ));
    cmp_validation_result(vr(
        (map { {title => $_ x $ONE_SITELINK_MAX_LENGTH, description => undef, href => "http://ya.ru/$_"} } (1 .. int($SITELINKS_MAX_LENGTH / $ONE_SITELINK_MAX_LENGTH))),
        {title => 'x' x ($SITELINKS_MAX_LENGTH % $ONE_SITELINK_MAX_LENGTH + 1), description => undef, href => "http://ya.ru/x"}
    ), [vr_errors('MaxLength')]);

    #
    # duplicate title/href of sitelinks
    #
    cmp_validation_result(vr(map { {title => "title", description => undef, href => "http://ya.ru/$_"} } (1 .. 3)), [vr_errors('DuplicateField')]);
    ok_validation_result(vr(map { {title => $_, description => undef, href => "http://ya.ru/x"} } (1 .. 3)));
    cmp_validation_result(vr(map { {title => "title", description => undef, href => "http://ya.ru/x"} } (1 .. 3)), [vr_errors('DuplicateField')]);
};

done_testing;
