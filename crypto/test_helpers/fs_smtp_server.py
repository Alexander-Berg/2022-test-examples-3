import os
from smtpd import SMTPServer


class FsSmtpServer(SMTPServer):
    def __init__(self, localaddr, remoteaddr, filename_format="{count}.txt", formatter=None, out_dir=""):
        SMTPServer.__init__(self, localaddr, remoteaddr)
        self._out_dir = out_dir
        self._email_count = 0
        self._filename_format = filename_format
        self._formatter = formatter

    def process_message(self, peer, mailfrom, rcpttos, data, **kwargs):
        filename = os.path.join(self._out_dir, self._filename_format.format(peer=peer, mailfrom=mailfrom, rcpttos=rcpttos, count=self._email_count))

        data = self._formatter(data) if self._formatter is not None else data
        with open(filename, 'w') as f:
            f.write(data)

        self._email_count += 1
