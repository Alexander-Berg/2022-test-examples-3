use assert_cmd::prelude::*;
use predicates::prelude::*;
use std::process::Command;

#[test]
fn no_args() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;

    cmd.assert().failure().stderr(predicate::str::contains(
        "The following required arguments were not provided",
    ));

    Ok(())
}

#[test]
fn encode1() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;
    cmd.arg("1234567890987654321").arg("15").arg("-l");
    cmd.assert().success().stdout("ESIQ9LFsHLEv\n");
    Ok(())
}

#[test]
fn encode2() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;
    cmd.arg("1234567890987654321").arg("15");
    cmd.assert().success().stdout("ESIQ9LFsHLEP\n");
    Ok(())
}

#[test]
fn decode() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;

    cmd.arg("ESIQ9LFsHLEv");

    cmd.assert()
        .success()
        .stdout(predicate::str::contains("ESIQ9LFsHLEv"))
        .stdout(predicate::str::contains("day=15"))
        .stdout(predicate::str::contains("is_logged_in=true"))
        .stdout(predicate::str::contains("1234567890987654321"));

    Ok(())
}

#[test]
fn decode_bad_b64() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;

    cmd.arg("ESIQ9__LFsHLEv");

    cmd.assert()
        .failure()
        .stderr(predicate::str::contains("base64 error: Invalid byte"));

    Ok(())
}

#[test]
fn decode_bad_length1() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;

    cmd.arg("AAAA");

    cmd.assert()
        .failure()
        .stderr(predicate::str::contains("wrong bytes count"));

    Ok(())
}

#[test]
fn decode_bad_length2() -> Result<(), Box<dyn std::error::Error>> {
    let mut cmd = Command::cargo_bin("qr-tool")?;

    cmd.arg("AAAAAAAAAAAAAAAAAAAA");

    cmd.assert()
        .failure()
        .stderr(predicate::str::contains("wrong bytes count"));

    Ok(())
}
