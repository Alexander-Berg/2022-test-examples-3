PATH=/sbin:$PATH

RULE="INPUT ! -d 127.0.0.1/32 -p tcp -m multiport --dports 80,443 -j REJECT --reject-with icmp-port-unreachable"

iptables-save  | grep -q "${RULE}" || iptables -I ${RULE}
