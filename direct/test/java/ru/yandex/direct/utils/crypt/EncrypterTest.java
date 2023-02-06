package ru.yandex.direct.utils.crypt;

import java.math.BigInteger;
import java.util.Arrays;

import javax.crypto.IllegalBlockSizeException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.direct.utils.crypt.Encrypter.genKeyAndIv;
import static ru.yandex.direct.utils.crypt.Encrypter.pack;
import static ru.yandex.direct.utils.crypt.Encrypter.unpack;

public class EncrypterTest {
    private static final String SECRET = "29acb93af5a72e31a2e55c0d41913b6f";
    private Encrypter encrypter;

    @Before
    public void setUp() throws Exception {
        encrypter = new Encrypter(SECRET);
    }

    @Test
    public void decryptText_success() {
        String encrypted = "53616c7465645f5f9fb9f092879ed999e7e6d3a2559710714b48af94963da576";
        String res = encrypter.decryptText(encrypted);
        assertThat(res).isEqualTo("12345678");
    }

    @Test
    public void encryptText_success() {
        // так как при шифровании генерируется рандомная соль, проверяем корректность через расшифровку
        String text = "12345678";
        String encrypted = encrypter.encryptText(text);
        String res = encrypter.decryptText(encrypted);
        assertThat(res).isEqualTo(text);
    }

    @Test
    public void encryptBytes_success() {
        String text = "12345678";
        byte[] res = Encrypter.encryptBytes(text.getBytes(), SECRET.getBytes(), "        ".getBytes());
        assertThat(unpack(res)).isEqualTo("53616c7465645f5f202020202020202001334e8b440b3ebeba755dcbb46a96bd");
    }

    @Test
    public void genKeyAndIv_success() {
        // pass: 3239616362393361663561373265333161326535356330643431393133623666, salt: 9fb9f092879ed999
        var pass = new BigInteger("3239616362393361663561373265333161326535356330643431393133623666", 16).toByteArray();
        var salt = new BigInteger("9fb9f092879ed999", 16).toByteArray();
        if (salt.length > 8) {
            salt = Arrays.copyOfRange(salt, salt.length - 8, salt.length);
        }
        assertThat(pass).hasSize(32);
        assertThat(salt).hasSize(8);

        Encrypter.KeyAndIv keyAndIv = genKeyAndIv(pass, salt);
        assertThat(unpack(keyAndIv.key))
                .as("key")
                .isEqualTo("ec60791ad3b4383fa1f81be7868d892b14b4bf4846d2c766a7a2c46dbd19a7b7");
        assertThat(unpack(keyAndIv.iv))
                .as("iv")
                .isEqualTo("210b94f1746112b48928ecbea76840cc");
    }

    @Test
    public void unpack_success() {
        assertThat(unpack("A!".getBytes())).isEqualTo("4121");
        assertThat(unpack(new BigInteger("1111111111111111", 2).toByteArray())).isEqualTo("ffff");
    }

    @Test
    public void pack_success() {
        assertThat(pack("4121")).satisfies(bytes ->
                assertThat(Arrays.equals(bytes, "A!".getBytes())).isTrue());
        assertThat(pack("ffff")).satisfies(bytes ->
                assertThat(Arrays.equals(bytes, new byte[]{-1 /*0xff*/, -1 /*0xff*/})).isTrue());
    }

    @Test
    public void encryptText_throwExpectedException() {
        // получено из правильного шифра путём удаления последнего байта (двух 16-ричных цифр)
        String badEncrypted = "53616c7465645f5f9fb9f092879ed999e7e6d3a2559710714b48af94963da5";
        assertThatCode(() -> encrypter.decryptText(badEncrypted))
                .isInstanceOf(EncryptionException.class)
                .hasCauseInstanceOf(IllegalBlockSizeException.class);
    }
}
/*
Чтобы получить ключ и начальный вектор для эталона, добавил в perl'овый метод отладочной печати.
Вход, на котором получены значения: 53616c7465645f5f9fb9f092879ed999e7e6d3a2559710714b48af94963da576
perl -I./protected -ME -e 'use Direct::Encrypt; print Direct::Encrypt::decrypt_text
("53616c7465645f5f9fb9f092879ed999e7e6d3a2559710714b48af94963da576")."\n"'
key_len: 32, iv_len: 16
pass: 3239616362393361663561373265333161326535356330643431393133623666, salt: 9fb9f092879ed999
md5(32396163623933616635613732653331613265353563306434313931336236669fb9f092879ed999)=ec60791ad3b4383fa1f81be7868d892b
data: ec60791ad3b4383fa1f81be7868d892b
md5(ec60791ad3b4383fa1f81be7868d892b32396163623933616635613732653331613265353563306434313931336236669fb9f092879ed999)
=14b4bf4846d2c766a7a2c46dbd19a7b7
data: ec60791ad3b4383fa1f81be7868d892b14b4bf4846d2c766a7a2c46dbd19a7b7
md5(14b4bf4846d2c766a7a2c46dbd19a7b732396163623933616635613732653331613265353563306434313931336236669fb9f092879ed999)
=210b94f1746112b48928ecbea76840cc
data: ec60791ad3b4383fa1f81be7868d892b14b4bf4846d2c766a7a2c46dbd19a7b7210b94f1746112b48928ecbea76840cc
key: ec60791ad3b4383fa1f81be7868d892b14b4bf4846d2c766a7a2c46dbd19a7b7
iv: 210b94f1746112b48928ecbea76840cc
12345678

Патченный метод:
sub _salted_key_and_iv {
  my $self = shift;
  my ($pass,$salt)  = @_;

  croak "Salt must be 8 bytes long" unless length $salt == 8;

  my $key_len = $self->{keysize};
  my $iv_len  = $self->{blocksize};
  print STDERR "key_len: $key_len, iv_len: $iv_len\n";

  my $desired_len = $key_len+$iv_len;

  my $data  = '';
  my $d = '';
  print STDERR "pass: ".(unpack "H*", $pass).", salt: ".(unpack "H*", $salt)."\n";
  while (length $data < $desired_len) {
    my $md = md5($d . $pass . $salt);
    print STDERR "md5(".(unpack "H*", ($d . $pass . $salt)).")=".(unpack "H*", $md)."\n";
    $d = $md;
    $data .= $d;
    print STDERR "data: ".(unpack "H*", $data)."\n";
  }
  print STDERR "key: ".(unpack "H*", substr($data,0,$key_len))."\n";
  print STDERR "iv: ".(unpack "H*", substr($data,$key_len,$iv_len))."\n";
  return (substr($data,0,$key_len),substr($data,$key_len,$iv_len));
}
 */
