use qr_tool::*;

fn check(uid: u64, day: u8, is_logged_in: bool) {
    let data_in = Data::new(uid, day, is_logged_in);
    let encoded = encode(&data_in);
    let data_out = decode(&encoded).unwrap();
    assert_eq!(data_in, data_out);
}

#[test]
fn basic() {
    check(0, 1, false);
    check(u64::max_value(), 31, true);
    check(123456789, 23, true);
}

#[test]
#[should_panic]
fn too_big_day_panics() {
    check(0, 32, false);
}

#[test]
fn decode_too_long() {
    let v = [0u8; 6];
    let res = decode(&v).unwrap_err();
    assert!(res.contains("wrong bytes count"));
}

#[test]
fn decode_too_short() {
    let v = [0u8; 4];
    let res = decode(&v).unwrap_err();
    assert!(res.contains("wrong bytes count"));
}

#[test]
fn b64_encode() {
    let data_in = Data::new(123456789, 23, true);
    let encoded = encode_b64(&data_in);
    println!("{}", encoded);
    assert_eq!(encoded, "AAAAAAdbzRU3");
}

#[test]
fn b64_decode() {
    let data_in = Data::new(123456789, 23, true);
    let data_out = decode_b64("AAAAAAdbzRU3").unwrap();
    assert_eq!(data_in, data_out);
}

#[test]
fn b64_decode_bad_string() {
    let res = decode_b64("23456(*&^%$%").unwrap_err();
    assert!(res.contains("Invalid byte"));
}
