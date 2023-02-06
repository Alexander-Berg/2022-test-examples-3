#!/usr/bin/perl
use Direct::Modern;

=head1 NAME

DBQueue/types.t

=head1 DESCRIPTION

Проверяет формат YAML-файла etc/dbqueue-types.yaml

=cut

use Test::Deep;
use Test::More;
use YAML 'LoadFile';

use Yandex::DBQueue::Typemap;
use Yandex::Validate;

use Settings;

plan tests => 1;

my $DB_TYPE_LENGTH = 30;

sub check_id {
    my $id = shift; 
    state %IDS;

    if (!is_valid_id($id)) {
        return (0, "id is not valid number");
    }
    if ($IDS{$id}) {
        return (0, "id isn't unique");
    }

    $IDS{$id}++;

    return 1;
}

sub check_type {
    my $type = shift;
    state %TYPES;

    if ($type !~ qr/^\w+$/) {
        return (0, 'invalid type format, expected: ^\w+$');
    }
    if (length($type) > $DB_TYPE_LENGTH) {
        return (0, "type must not be longer than $DB_TYPE_LENGTH characters");
    }
    if ($TYPES{$type}) {
        return (0, "type isn't unique");
    }

    $TYPES{ $type }++;

    return 1;
}

cmp_deeply(
    YAML::LoadFile( $Yandex::DBQueue::Typemap::SOURCE_FILE_PATH ),
    array_each(
        {
            id => code(\&check_id),
            type => code(\&check_type),
        }
    ),
    "$Yandex::DBQueue::Typemap::SOURCE_FILE_PATH format ok"
);
