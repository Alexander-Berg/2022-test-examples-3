package QBit::WebInterface::Test::Request;
{
  $QBit::WebInterface::Test::Request::VERSION = '0.020';
}

use qbit;

use base qw(QBit::WebInterface::Request);

sub http_header {
    my ($self, $name) = @_;

    my $value = $self->{'headers'}{$name};

    return defined($value) ? $value : '';
}

sub method {shift->{'method'}}

sub uri {
    my ($self) = @_;

    my $result = "/$self->{'path'}/$self->{'cmd'}";
    $result .= '?' . $self->{'query'} if defined($self->{'query'});
}

sub scheme {shift->{'scheme'}}

sub server_name {'Test'}

sub server_port {0}

sub remote_addr {'127.0.0.1'}

sub query_string {shift->{'query'}}

sub _read_from_stdin {
    my ($self, $buffer_ref, $size) = @_;

    return read($self->{'__STDIN__'}, $$buffer_ref, $size);
}

TRUE;
