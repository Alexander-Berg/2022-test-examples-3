import email


def replace_boundaries(message_as_string):
    boundaries_count = 0
    message = email.message_from_string(message_as_string.decode("utf-8"))

    for part in message.walk():
        if part.get_boundary() is not None:
            part.set_boundary("boundary_{}".format(boundaries_count))
            boundaries_count += 1

    return message.as_string()
