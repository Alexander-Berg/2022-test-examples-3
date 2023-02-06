balancer_accept:
  iptables.delete:
    - source: '10.0.0.0/8'
    - table: filter
    - chain: INPUT
    - jump: REJECT
    - reject-with: icmp-port-unreachable
