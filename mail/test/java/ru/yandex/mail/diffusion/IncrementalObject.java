package ru.yandex.mail.diffusion;

import lombok.Value;
import lombok.With;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

@With
@Value
public class IncrementalObject {
    @With
    @Value
    public static class NonIncremental {
        String string;
        int integer;
    }

    public enum Enumeration {
        ONE,
        TWO
    }

    boolean bool;
    String string;
    int integer;
    Long boxedLong;
    Set<String> set;
    Set<Byte> byteSet;
    NonIncremental pojo;
    OptionalLong optionalLong;
    Optional<Short> optionalShort;
    Enumeration enumeration;
}
