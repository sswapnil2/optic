use optic_diff::SpecEvent;
use insta::assert_debug_snapshot;

#[test]
fn spec_should_deserialize() {
    let events = SpecEvent::from_file(
        std::env::current_dir()
            .unwrap()
            .join("tests/fixtures/ergast-example-spec.json")
            .to_str()
            .unwrap(),
    )
    .expect("ergast spec should deserialize");
    assert_debug_snapshot!(events);
}
