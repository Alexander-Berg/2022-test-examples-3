package API::Test::Reports::FakeWriter;
use Direct::Modern;

=head1 NAME

API::Test::Reports::FakeWriter

=head1 DESCRIPTION

Компонент, совместимый с объектом writer из стандарта PSGI, который ничего не пишет, только
сохраняет всё записанное в массиве в памяти и с помощью метода get_lines может вернуть.

=cut

=head2 new

=cut

sub new {
    my ($class) = @_;
    return bless { lines => [] }, $class;
}

=head2 write

=cut

sub write {
    my ($self, $line) = @_;
    push @{ $self->{lines} }, $line;
}

=head2 get_lines

=cut

sub get_lines {
    my ($self) = @_;
    return $self->{lines};
}

1;
