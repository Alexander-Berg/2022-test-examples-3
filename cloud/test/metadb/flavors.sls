data:
  dbaas_metadb:
    flavors:
      - name: db1.nano
        id: 32165f32-34c9-4e2d-b150-d07699b73cf4
        cpu_guarantee: 0.5
        cpu_limit: 1
        memory_guarantee: 1073741824
        memory_limit: 2147483648
        network_guarantee: 1048576
        network_limit: 2097152
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: standard
        generation: 1
      - name: db1.micro
        id: f7d18670-4ea8-465f-a07b-a6fde9ed89cb
        cpu_guarantee: 1
        cpu_limit: 1
        memory_guarantee: 2147483648
        memory_limit: 4294967296
        network_guarantee: 1048576
        network_limit: 4194304
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: standard
        generation: 1
      - name: db1.small
        id: 5d06e121-1434-416d-8cd6-2a2e89b30611
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 4294967296
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 8388608
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: standard
        generation: 1
      - name: db1.medium
        id: ab59e8ef-bdb2-4f78-9efe-eaf25348061e
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 8589934592
        memory_limit: 17179869184
        network_guarantee: 1048576
        network_limit: 16777216
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: standard
        generation: 1
      - name: m1.nano
        id: 4ac6abfb-0a81-4a93-883d-74fc9971af39
        cpu_guarantee: 1
        cpu_limit: 1
        memory_guarantee: 8589934592
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 2097152
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: memory-optimized
        generation: 1
      - name: m1.micro
        id: 87a98f03-44ff-4136-a270-be33e1c111ed
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 17179869184
        memory_limit: 17179869184
        network_guarantee: 1048576
        network_limit: 4194304
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: memory-optimized
        generation: 1
      - name: m1.small
        id: f12e9a75-a61c-4f51-a1af-6a9c81466103
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 34359738368
        memory_limit: 34359738368
        network_guarantee: 1048576
        network_limit: 8388608
        io_limit: 5242880
        visible: true
        vtype: porto
        platform_id: '1'
        type: memory-optimized
        generation: 1
      - name: s2.nano
        id: 6c47aceb-8124-414b-a531-ffa3d9e63c7a
        cpu_guarantee: 1
        cpu_limit: 1
        memory_guarantee: 2147483648
        memory_limit: 4294967296
        network_guarantee: 1048576
        network_limit: 2097152
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '2'
        type: standard
        generation: 2
      - name: s2.micro
        id: f5f90c33-c2d6-4421-b542-4bb4816c5d63
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 4294967296
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 4194304
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '2'
        type: standard
        generation: 2
      - name: s2.small
        id: cd4a8957-e80b-4690-a8e4-b446c4fe0171
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 4294967296
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 8388608
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '2'
        type: standard
        generation: 2
      - name: m2.nano
        id: 358fcfdc-b4a6-446a-a529-252fab7b3830
        cpu_guarantee: 1
        cpu_limit: 1
        memory_guarantee: 8589934592
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 2097152
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '2'
        type: memory-optimized
        generation: 2
      - name: m2.micro
        id: 221562a6-3cda-4f91-8b9d-74eaa5da89cd
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 17179869184
        memory_limit: 17179869184
        network_guarantee: 1048576
        network_limit: 4194304
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '2'
        type: memory-optimized
        generation: 2
      - name: m2.small
        id: 26d0e54b-66c2-4c65-b504-4723d5a96018
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 34359738368
        memory_limit: 34359738368
        network_guarantee: 1048576
        network_limit: 8388608
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '2'
        type: memory-optimized
        generation: 2
      - name: s3.nano
        id: f46f9b8f-5add-4a2f-9145-8e7ef8d35574
        cpu_guarantee: 1
        cpu_limit: 1
        memory_guarantee: 2147483648
        memory_limit: 4294967296
        network_guarantee: 1048576
        network_limit: 2097152
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '3'
        type: standard
        generation: 3
      - name: s3.micro
        id: c39d5b8a-b6eb-4950-8c27-c1e1e3bf3b08
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 4294967296
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 4194304
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '3'
        type: standard
        generation: 3
      - name: s3.small
        id: 5c1408bb-3d5c-4c7a-aba1-599d82a72dd0
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 4294967296
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 8388608
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '3'
        type: standard
        generation: 3
      - name: m3.nano
        id: 6d80e607-02cd-4b48-b6fd-d470ddc411ce
        cpu_guarantee: 1
        cpu_limit: 1
        memory_guarantee: 8589934592
        memory_limit: 8589934592
        network_guarantee: 1048576
        network_limit: 2097152
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '3'
        type: memory-optimized
        generation: 3
      - name: m3.micro
        id: cfe5ec42-442c-4b81-8167-e03679492569
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 17179869184
        memory_limit: 17179869184
        network_guarantee: 1048576
        network_limit: 4194304
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '3'
        type: memory-optimized
        generation: 3
      - name: m3.small
        id: aaf3fc2e-144b-4e1b-9326-aa9f44b87110
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 34359738368
        memory_limit: 34359738368
        network_guarantee: 1048576
        network_limit: 8388608
        io_limit: 10485760
        visible: true
        vtype: porto
        platform_id: '3'
        type: memory-optimized
        generation: 3
      - name: c3-c2-m4
        id: 9d9b7dcb-c811-4fba-8ce8-2035884785ae 
        cpu_guarantee: 2
        cpu_limit: 2
        memory_guarantee: 4294967296
        memory_limit: 4294967296
        network_guarantee: 0
        network_limit: 1073741824
        io_limit: 1073741824
        visible: true
        vtype: porto
        platform_id: '3'
        type: cpu-optimized
        generation: 3
      - name: c3-c4-m8
        id: 29522b84-2652-4d47-9d1b-3e6ecd7033c4
        cpu_guarantee: 4
        cpu_limit: 4
        memory_guarantee: 8589934592
        memory_limit: 8589934592
        network_guarantee: 0
        network_limit: 1073741824
        io_limit: 1073741824
        visible: true
        vtype: porto
        platform_id: '3'
        type: cpu-optimized
        generation: 3