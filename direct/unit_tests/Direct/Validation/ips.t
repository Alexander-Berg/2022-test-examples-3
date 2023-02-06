#!/usr/bin/env perl

use Direct::Modern;

use base qw/Test::Class/;

use Settings;
use Test::More;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;

sub use_module : Tests( startup => 1 ) {
    use_ok('Direct::Validation::Ips', qw/ validate_disabled_ips /);
}

sub disabled_ips_limit : Tests( 1 ) {
    my @ips = map { '198.169.0.1' } 0 .. 25; # $Direct::Validation::Ips::MAX_IP_QTY + 1

    cmp_validation_result(
        validate_disabled_ips( \@ips ),
        vr_errors('ReachLimit'), # TODO: vr dont match qr/^Размер списка #field# превышает максимально допустимый размер 25$/, wtf?
        'error if size of disabled ips list reach limit'
    );
}

sub invalid_ip_format : Tests( 1 ) {
    my @ips = (
        '1111.2.3.4', '1a.2.3.4', '256.2.3.4', '01.2.3.4',
        '1.2222.3.4', '1.2a.3.4', '1.256.3.4', '1.02.3.4',
        '1.2.3333.4', '1.2.3a.4', '1.2.256.4', '1.2.03.4',
        '1.2.3.4444', '1.2.3.4a', '1.2.3.256', '1.2.3.04',
        '1.2.3.4.5',
    );

    cmp_validation_result(
        validate_disabled_ips( \@ips ),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - неправильный формат IP-адреса$/ } @ips ), # 'InvalidFormat'
        'errors on invalid ip format'
    );
}

sub special_use_ip : Tests( 1 ) {
    my @ips = (
        '0.0.0.0', '127.0.0.1', '192.168.0.1',
    );

    cmp_validation_result(
        validate_disabled_ips( \@ips ),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - нельзя запрещать IP-адреса из частных подсетей$/ } @ips ), # 'BadUsage'
        'errors on special ip v4 addresses'
    );
}

sub unblockable_ip : Tests( 1 ) {
    my @ips = (
        '87.250.224.0', '213.180.192.0', '77.88.0.0',
    );

    cmp_validation_result(
        validate_disabled_ips( \@ips ),
        vr_errors( map { qr/^Элемент [^ ]+ списка #field# - нельзя запрещать IP-адреса из подсети Яндекса \(показы и клики с этих адресов не учитываются\)$/ } @ips ), # 'BadUsage'
        'errors on unblockable ip addresses'
    );
}

sub duplicates_within_disabled_ips : Tests( 1 ) {
    cmp_validation_result(
        validate_disabled_ips(
            [ map { '198.169.0.1' } 1 .. 2 ],
            check_duplicates => 1,
        ),
        vr_errors(qr/^Элемент [^ ]+ списка #field# - повторяющееся значение$/), # warning 'DuplicatedItems'
        'warning on duplicates within disabled ips'
    );
}

__PACKAGE__->runtests();
