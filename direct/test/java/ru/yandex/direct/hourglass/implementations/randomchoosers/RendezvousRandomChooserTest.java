package ru.yandex.direct.hourglass.implementations.randomchoosers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.InstanceId;
import ru.yandex.direct.hourglass.client.SchedulerInstance;
import ru.yandex.direct.hourglass.implementations.InstanceIdImpl;
import ru.yandex.direct.hourglass.implementations.MD5Hash;
import ru.yandex.direct.hourglass.updateschedule.SchedulerInstancesRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RendezvousRandomChooserTest {
    private SchedulerInstancesRepository schedulerInstancesRepository = mock(SchedulerInstancesRepository.class);
    private InstanceId instanceId = new InstanceIdImpl("0i");
    private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private MD5Hash md5Hash = mock(MD5Hash.class);

    List<SchedulerInstance> mockedInstances(List<String> names) {
        List<SchedulerInstance> schedulerInstances = new ArrayList<>();

        for (String name : names) {
            SchedulerInstance schedulerInstance = mock(SchedulerInstance.class);
            when(schedulerInstance.getInstanceId()).thenReturn(new InstanceIdImpl(name));
            when(schedulerInstance.getVersion()).thenReturn("version");
            when(schedulerInstance.isActive()).thenReturn(true);

            schedulerInstances.add(schedulerInstance);
        }

        return schedulerInstances;
    }

    @Test
    void currentInstanceHasBiggestHash() {
        List<SchedulerInstance> schedulerInstances = mockedInstances(List.of("0i", "1i", "2i", "3i"));

        RendezvousRandomChooser<Integer> rendezvousRandomChooser =
                new RendezvousRandomChooser<>("version", schedulerInstancesRepository, instanceId, md5Hash, clock);

        when(schedulerInstancesRepository.getSchedulerInstancesInfo()).thenReturn(schedulerInstances);

        when(md5Hash.hash(anyString())).thenAnswer(
                args -> {
                    String arg = args.getArgument(0);

                    String[] parts = arg.split(";");

                    assertThat(parts).hasSize(3);

                    if (parts[2].equals("0i")) {
                        return Long.MAX_VALUE;
                    }

                    return 0L;
                }
        );

        List<Integer> result = rendezvousRandomChooser.choose(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 5);
        assertThat(result).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }


    @Test
    void currentInstanceHasLowestHash() {
        List<SchedulerInstance> schedulerInstances = mockedInstances(List.of("0i", "1i", "2i", "3i"));

        RendezvousRandomChooser<Integer> rendezvousRandomChooser =
                new RendezvousRandomChooser<>("version", schedulerInstancesRepository, instanceId, md5Hash, clock);

        when(schedulerInstancesRepository.getSchedulerInstancesInfo()).thenReturn(schedulerInstances);

        when(md5Hash.hash(anyString())).thenAnswer(
                args -> {
                    String arg = args.getArgument(0);

                    String[] parts = arg.split(";");

                    assertThat(parts).hasSize(3);

                    if (parts[2].equals("0i")) {
                        return Long.MIN_VALUE;
                    }

                    return 0L;
                }
        );

        List<Integer> result = rendezvousRandomChooser.choose(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 5);
        assertThat(result).isEmpty();
    }

    @Test
    void someElementsHaveBiggerHash() {
        List<SchedulerInstance> schedulerInstances = mockedInstances(List.of("0i", "1i", "2i", "3i"));

        RendezvousRandomChooser<Integer> rendezvousRandomChooser =
                new RendezvousRandomChooser<>("version", schedulerInstancesRepository, instanceId, md5Hash, clock);

        when(schedulerInstancesRepository.getSchedulerInstancesInfo()).thenReturn(schedulerInstances);

        when(md5Hash.hash(anyString())).thenAnswer(
                args -> {
                    String arg = args.getArgument(0);

                    String[] parts = arg.split(";");

                    assertThat(parts).hasSize(3);

                    if (parts[2].equals("0i")) {
                        if (parts[1].equals("2") || parts[1].equals("9") || parts[1].equals("4")) {
                            return Long.MAX_VALUE;
                        } else {
                            return Long.MIN_VALUE;
                        }
                    }

                    return 0L;
                }
        );

        List<Integer> result = rendezvousRandomChooser.choose(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 5);
        assertThat(result).containsExactlyInAnyOrder(2, 9, 4);
    }

    @Test
    void realTest() {
        List<String> instancesIds = List.of("32630b48-4285-4711-ad17-a37c1c8e3c37",
                "62659541-fef8-4f99-aa25-6242186a65d6", "774ab80c-3c17-4f22-9d0d-9ed05a22681f",
                "78bbd106-9005-47cd-bd8e-c236ec2bd358", "e2791faf-67bf-40af-9819-51fc138a84a3",
                "f5b3c6a2-d398-48d3-b6d0-690e248fca44");

        List<Long> jobsIds = LongStream.range(5L, 400L).boxed().collect(Collectors.toList());

        List<SchedulerInstance> schedulerInstances = mockedInstances(instancesIds);

        when(schedulerInstancesRepository.getSchedulerInstancesInfo()).thenReturn(schedulerInstances);

        Set<Long> ids = new HashSet<>();

        for (String currentInstance : instancesIds) {
            RendezvousRandomChooser<Long> rendezvousRandomChooser =

                    new RendezvousRandomChooser<>(
                            "version",
                            schedulerInstancesRepository,
                            new InstanceIdImpl(currentInstance), new MD5Hash(), clock);

            List<Long> result2 = rendezvousRandomChooser.choose(jobsIds, 1000);

            ids.addAll(result2);

            System.out.println(result2.size() + " / " + result2);
        }

        assertThat(ids).containsExactlyInAnyOrder(jobsIds.toArray(Long[]::new));
    }


}
