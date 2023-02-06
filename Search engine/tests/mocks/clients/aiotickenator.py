from typing import Tuple, Optional


class Tickenator:
    NOC = 'NOCREQUESTS'
    APPHOST = 'APPHOSTSUPPORT'
    YDB = 'YDBREQUESTS'

    def __init__(self):
        self.queues: dict[str, list[dict]] = {}

    async def get_ticket(self, task_id: str) -> Tuple[bool, Optional[str]]:
        return True, task_id

    async def create_ticket(self, queue, author, subject, description):
        assert len(subject) <= 255
        destination = self.queues.setdefault(queue, [])
        num = len(destination) + 1
        identifier = f'{queue}-{num}'
        destination.append(dict(
            id=identifier,
            author=author,
            subject=subject,
            description=description
        ))
        return identifier

    async def create_spi(self, data: dict):
        raise NotImplementedError

    def find_ticket_by_identifier(self, identifier):
        queue, num = identifier.split('-')
        queue = self.queues.get(queue, [])
        num = int(num)
        if 1 <= num <= len(queue):
            return queue[num - 1]
