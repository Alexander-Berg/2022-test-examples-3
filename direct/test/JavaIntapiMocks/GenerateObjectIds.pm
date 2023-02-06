package Test::JavaIntapiMocks::GenerateObjectIds;

=head1 NAME

    Test::JavaIntapiMocks::GenerateObjectIds - mock походов в JavaIntapi за идентификаторами новых объектов


=head1 DESCRIPTION

    Для тестов используем старую перловую реализацию.

=cut

use Direct::Modern;
use Yandex::DBShards;

sub import {
    my $pkg = shift;

    no warnings qw/redefine once/;
    *JavaIntapi::GenerateObjectIds::call = \&_generate_via_perl;

    $pkg->SUPER::import(@_);
}

sub _generate_via_perl {
    my $self = shift;

    my $signature = $self->_get_signature();
    my $count = $self->count();

    return _is_client_id_needed($signature) ?
        get_new_id_multi($signature, $count, ClientID => $self->client_id())
      : get_new_id_multi($signature, $count);
}

sub _is_client_id_needed {
    my ($signature) = @_;

    return $Yandex::DBShards::SHARD_KEYS{$signature}->{chain_key} ? 1 : 0;
}

1;
