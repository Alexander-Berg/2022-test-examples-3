import socket
import struct


class IpRange(object):
    def __init__(self, ip_range_str):
        left, right = ip_range_str.split("-", 1)

        self.begin = IpRange.ip2long(left.strip())
        self.end = IpRange.ip2long(right.strip())

    def contains(self, ip_str):
        ip = IpRange.ip2long(ip_str)
        return ip >= self.begin and ip <= self.end

    @staticmethod
    def ip2long(ip):
        packedIP = socket.inet_aton(ip)
        return struct.unpack("!L", packedIP)[0]
