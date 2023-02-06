package ru.yandex.edu;

import java.security.SecureRandom;
import java.math.BigInteger;
import java.util.Random;

import org.junit.Test;

/**
 * Created by agroroza on 17.03.2016.
 */


public final class JavaEdu {
    public static String generateString(Random rng, String characters, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
}