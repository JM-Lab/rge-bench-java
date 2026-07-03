# rge-bench-java

An independent, clean-room implementation of the [rge-bench](https://github.com/rge-bench/rge-bench)
reviewer-grade-evidence checker, written on the Spring Boot 4 / Jackson 3 stack used by
[Spring AI Playground](https://github.com/spring-ai-community/spring-ai-playground). It imports nothing
from the kit and implements the eleven axis rules directly from the README contract, as an independent
reproduction of the rge-bench v0 corpus on a non-Python stack.

## What it does

- Reads `vectors.json`, computes each vector's outcome from `inputs` alone via the eleven axis rules
  (`Axes.java`), and writes `out/jm-lab-spring.json` in the kit's contract format.
- Recomputes the `vectors_digest` two ways through the Spring-managed Jackson `ObjectMapper`: sorted
  keys (the kit's pinned recipe) and the stack's native insertion order.

## Result (rge-bench v0)

- All 62 vectors reproduce: every outcome equals the contract's `expected`, and the kit's `checker.py`
  scores `pass` on all eleven axes (including the `coverage_honesty` axis).
- `vectors_digest` reproduces byte-for-byte (`sha256:860386...`) under the kit's sorted-keys
  canonicalization; the same ObjectMapper in insertion order yields a different digest, which is why
  the canonicalization has to be declared next to the digest.

## Discipline

- The eleven axis rules are derived from the README contract only. The checker never reads a vector's
  `expected` to decide an outcome; the comparison against `expected` is a post-hoc diff.
- `Axes.java` was written from the contract before the kit's own `ref_example.py` was read. The kit's
  reference checker was run only afterward, to score and compare.

## Edge probes

`adversarial-edge-vectors.json` holds ten out-of-corpus probes that expose three places where the
contract wording leaves language-specific behavior implicit: present vs empty (`""`), null vs absent,
and numeric semantic equality (`1` vs `1.0`). They do not affect the current 62 vectors; they only
appear once the inputs are pushed outside the corpus.

## Run

    JAVA_HOME=/path/to/jdk-21 ./mvnw -q spring-boot:run

Reads `vectors.json` from the working directory, or pass a path as the first argument. Output goes to
`out/jm-lab-spring.json`, and the per-axis matrix plus both digests print to stdout.

## Attribution

`vectors.json` is a vendored copy of the rge-bench v0 corpus (see `NOTICE`); rge-bench is by
[@Rul1an](https://github.com/Rul1an). All other files are original work.

## License

Dual-licensed to mirror the rge-bench kit it reproduces (see `LICENSE`):

| material | license |
| --- | --- |
| Code (`src/`, `pom.xml`) | Apache-2.0 |
| Docs (`README.md`, `NOTICE`) | CC-BY-4.0 |
| `vectors.json` (rge-bench v0 corpus, vendored unmodified) | CC-BY-4.0, Copyright RGE-Bench authors, attribution required |

Full texts in `LICENSES/`.
