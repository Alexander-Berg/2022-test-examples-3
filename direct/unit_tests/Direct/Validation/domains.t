#!/usr/bin/env perl

use Direct::Modern;

use base qw/Test::Class/;

use Settings;
use Test::More;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;

use Yandex::DBUnitTest qw/:all/;

my %db = (
    ssp_platforms => {
        original_db => PPCDICT,
        rows => [
            { title => 'Valid SSP' },
        ],
    },
);

init_test_dataset(\%db);


sub use_module : Tests( startup => 1 ) {
    use_ok('Direct::Validation::Domains', qw/ validate_disabled_domains /);
}


sub valid_platforms :Tests(1) {
    cmp_validation_result(
        validate_disabled_domains(
            [
                'aa.ru',
                'Valid SSP',
                'bb.com',
                'aa_aa.bb-bb.cc_.dd-',
                'com.ringtonesapp',
            ],
        ),
        {},
        'valid platforms',
    );
}

sub skip_ssp : Tests(1) {
    cmp_validation_result(
        validate_disabled_domains(
            [ 'Valid SSP' ],
            skip_ssp => 1,
        ),
        vr_errors(qr/^Элемент [^ ]+ списка #field# - неправильный формат домена или идентификатора мобильного приложения$/), # 'InvalidFormat'
        'error for valid ssp with skip_ssp'
    );
}

sub disabled_domains_limit : Tests( 2 ) {
    my @too_much_ips_qty = 0 .. ($Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT + 1);

    cmp_validation_result(
        validate_disabled_domains(
            [ map { "domain$_.ru" } @too_much_ips_qty ],
            blacklist_size_limit => $Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT
        ),
        vr_errors(qr/^Размер списка #field# превышает максимально допустимый размер/), # 'ReachLimit'
        'error when size of disabled domains list reach limit'
    );

    # Проверяем, что используются не только дефолтные значения, но и выше
    cmp_validation_result(
        validate_disabled_domains(
            [ map { "domain$_.ru" } @too_much_ips_qty ],
            blacklist_size_limit => ($Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT + 2)
        ),
        {},
        'valid platforms',
    );
}

sub disabled_domains_limit_skip_blacklist_size : Tests( 2 ) {
    my @too_much_ips_qty = 0 .. ($Settings::DEFAULT_GENERAL_BLACKLIST_SIZE_LIMIT + 1);

    cmp_validation_result(
        validate_disabled_domains(
            [ map { "domain$_.ru" } @too_much_ips_qty ],
            skip_blacklist_size_limit => 1
        ),
        {},
        'valid platforms',
    );
}

sub invalid_domain_format : Tests( 1 ) {
    my $max_subdomain_name_length = 63; # from Yandex::IDN::is_valid_domain

    cmp_validation_result(
        validate_disabled_domains(
            [ join( '' => 'www.', ('a' x ( $max_subdomain_name_length + 1 ) ), '.рф' ) ],
        ),
        vr_errors(qr/^Элемент [^ ]+ списка #field# - неправильный формат домена или идентификатора мобильного приложения$/), # 'InvalidFormat'
        'error for invalid domain format'
    );
}

sub domains_forbidden_for_excluding : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([
            'm.yandex.ru', 'www.yandex.ru',
            'm.ya.ru', 'www.ya.ru',
            'm.direct.yandex.ru', 'www.direct.yandex.ru',
            'm.direct.ya.ru', 'www.direct.ya.ru',
            'yandex.ru', 'yandex.ru',
            'ya.ru', 'ya.ru',
            'direct.yandex.ru', 'direct.yandex.ru',
            'direct.ya.ru', 'direct.ya.ru',
            'яндекс.рф',
            'go.mail.ru', 'www.mail.ru', 'mail.ru'
        ]),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - отключать показы на этом домене нельзя$/ } 0 .. 19 ), # 'BadUsage'
        'error for domain forbidden for excluding'
    );
}

sub domains_forbidden_for_excluding_disable_any_domains_allowed_off : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([
            'm.yandex.ru', 'www.yandex.ru',
            'm.ya.ru', 'www.ya.ru',
            'm.direct.yandex.ru', 'www.direct.yandex.ru',
            'm.direct.ya.ru', 'www.direct.ya.ru',
            'yandex.ru', 'yandex.ru',
            'ya.ru', 'ya.ru',
            'direct.yandex.ru', 'direct.yandex.ru',
            'direct.ya.ru', 'direct.ya.ru',
            'яндекс.рф',
            'go.mail.ru', 'www.mail.ru', 'mail.ru'
        ],
        disable_any_domains_allowed => 0),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - отключать показы на этом домене нельзя$/ } 0 .. 19 ), # 'BadUsage'
        'error for domain forbidden for excluding, any_domains_allowed is off'
    );
}

sub domains_forbidden_for_excluding_disable_any_domains_allowed_on : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([
            'm.yandex.ru', 'www.yandex.ru',
            'm.ya.ru', 'www.ya.ru',
            'm.direct.yandex.ru', 'www.direct.yandex.ru',
            'm.direct.ya.ru', 'www.direct.ya.ru',
            'yandex.ru', 'yandex.ru',
            'ya.ru', 'ya.ru',
            'direct.yandex.ru', 'direct.yandex.ru',
            'direct.ya.ru', 'direct.ya.ru',
            'яндекс.рф',
            'go.mail.ru', 'www.mail.ru', 'mail.ru'
        ],
        disable_any_domains_allowed => 1),
        {},
        'No error for domain forbidden for excluding, any_domains_allowed is on'
    );
}

sub domains_forbidden_for_excluding_disable_number_id_and_short_bundle_id_allowed_off : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([
            '123',
            'id1234',
            'yabro',
            'imiphone',
        ],
            disable_number_id_and_short_bundle_id_allowed => 0),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - неправильный формат домена или идентификатора мобильного приложения$/ } 0 .. 3 ), # 'BadUsage'
        'error for domain forbidden for excluding, excluding_disable_number_id_and_short_bundle_id_allowed is off'
    );
}

sub domains_forbidden_for_excluding_disable_number_id_and_short_bundle_id_allowed_on : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([
            '123',
            'id1234',
            'yabro',
            'imiphone',
        ],
            disable_number_id_and_short_bundle_id_allowed => 1),
        {},
        'No error for domain forbidden for excluding, excluding_disable_number_id_and_short_bundle_id_allowed is on'
    );
}

sub only_third_level_can_be_blocked : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([
            'pp.ru', 'ac.ru', 'boom.ru', 'msk.ru', 'spb.ru', 'nnov.ru',
            'net.ru', 'org.ru', 'com.ru', 'int.ru', 'edu.ru',
        ]),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - для этого домена можно блокировать только третий уровень$/ } 0 .. 10 ), # 'BadUsage'
        'error when try to block not third level'
    );
}

sub duplicates_within_disabled_domains : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains(
            [ map { 'domain.ru' } 1 .. 2 ],
            check_duplicates => 1,
        ),
        vr_errors(qr/^Элемент [^ ]+ списка #field# - повторяющееся значение$/), # warning 'DuplicatedItems'
        'warning on duplicates within disabled domains'
    );
}

=head2 IMHO, эта ошибка не достижима (из-за Yandex::IDN::is_valid_domain)
sub domain_name_too_long : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_domains([ join( '' => ( 'c' x 63 ), '.', ( 'b' x 63 ), '.', ( 'a' x 63 ) , '.xn--', ( 'd' x 60 ) ) ]),
        vr_errors(qr/^Элемент [^ ]+ списка #field# - превышена максимально допустимая длина домена 255$/), # 'MaxLength'
        'error for domain name too long'
    );
}
=cut

__PACKAGE__->runtests();
