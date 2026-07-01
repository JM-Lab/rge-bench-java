// SPDX-License-Identifier: Apache-2.0
package dev.jmlab.rgecheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class Axes {

	private Axes() {
	}

	private static final Map<String, Integer> CEILING = Map.of("producer_reported", 1, "issuer_attested", 2,
			"receiver_receipt", 3, "boundary_observed", 4, "third_party_observed", 5);

	private static final Map<String, Integer> STRENGTH = Map.of("asserted", 1, "asserted_signed", 2,
			"observed_at_receiver", 3, "observed_in_path", 4, "independently_confirmed", 5);

	static String evaluate(String axis, Map<String, Object> in) {
		return switch (axis) {
			case "sufficiency" -> sufficiency(in);
			case "source_class_ceiling" -> sourceClassCeiling(in);
			case "recompute" -> recompute(in);
			case "format_equivalence" -> formatEquivalence(in);
			case "tamper_fail_closed" -> tamperFailClosed(in);
			case "incomplete_visibility" -> incompleteVisibility(in);
			case "delegated_scope" -> delegatedScope(in);
			case "hard_soft_digest" -> hardSoftDigest(in);
			case "retained_replay" -> retainedReplay(in);
			case "mcp_description_code" -> mcpDescriptionCode(in);
			case "coverage_honesty" -> coverageHonesty(in);
			default -> throw new IllegalArgumentException("unknown axis: " + axis);
		};
	}

	private static String sufficiency(Map<String, Object> in) {
		return Boolean.TRUE.equals(in.get("record_valid")) && "complete".equals(in.get("coverage")) ? "sufficient"
				: "incomplete";
	}

	private static String sourceClassCeiling(Map<String, Object> in) {
		Integer ceiling = CEILING.get(in.get("source_class"));
		Integer strength = STRENGTH.get(in.get("claim"));
		if (ceiling == null || strength == null) {
			return "invalid";
		}
		return strength <= ceiling ? "within_ceiling" : "exceeds_ceiling";
	}

	private static String recompute(Map<String, Object> in) {
		return asSet(in.get("declared")).containsAll(asList(in.get("observed"))) ? "match" : "mismatch";
	}

	private static String coverageHonesty(Map<String, Object> in) {
		Object declared = in.get("declared_cases");
		if (declared == null) {
			return "invalid";
		}
		List<?> cases = (List<?>) declared;
		Map<?, ?> results = in.get("case_results") instanceof Map<?, ?> m ? m : Map.of();
		if (cases.stream().anyMatch(c -> "failed".equals(results.get(c)))) {
			return "refuted";
		}
		return cases.stream().allMatch(c -> "passed".equals(results.get(c))) ? "confirmed" : "incomplete";
	}

	private static String formatEquivalence(Map<String, Object> in) {
		return Objects.equals(semantic(in.get("a")), semantic(in.get("b"))) ? "equivalent" : "distinct";
	}

	private static Object semantic(Object side) {
		return ((Map<?, ?>) side).get("semantic");
	}

	private static String tamperFailClosed(Map<String, Object> in) {
		Object stored = in.get("stored_digest");
		return stored != null && stored.equals(in.get("recomputed_digest")) ? "accepted" : "rejected";
	}

	private static String incompleteVisibility(Map<String, Object> in) {
		return "present".equals(in.get("observation")) ? "observed" : "incomplete";
	}

	private static String delegatedScope(Map<String, Object> in) {
		if (!in.containsKey("granted") || !in.containsKey("used")) {
			return "invalid";
		}
		return asSet(in.get("granted")).containsAll(asList(in.get("used"))) ? "within_grant" : "exceeds_grant";
	}

	private static String hardSoftDigest(Map<String, Object> in) {
		Object hardStored = in.get("hard_stored");
		if (hardStored == null || !hardStored.equals(in.get("hard_recomputed"))) {
			return "rejected_hard";
		}
		return Objects.equals(in.get("soft_a"), in.get("soft_b")) ? "soft_equivalent" : "soft_divergent";
	}

	private static String retainedReplay(Map<String, Object> in) {
		if (Boolean.FALSE.equals(in.get("carrier_valid"))) {
			return "rejected_carrier";
		}
		if (Boolean.FALSE.equals(in.get("records_retained"))) {
			return "incomplete";
		}
		return asSet(in.get("replayed")).equals(asSet(in.get("recorded"))) ? "replayed_match" : "replayed_mismatch";
	}

	private static String mcpDescriptionCode(Map<String, Object> in) {
		Set<Object> declared = asSet(in.get("declared_interface"));
		Set<Object> code = asSet(in.get("code_effects"));
		if (!declared.containsAll(code)) {
			return "undeclared_effect";
		}
		if (!code.containsAll(declared)) {
			return "over_declared";
		}
		return "consistent";
	}

	private static List<?> asList(Object value) {
		return value == null ? List.of() : (List<?>) value;
	}

	private static Set<Object> asSet(Object value) {
		return new HashSet<>((Collection<?>) asList(value));
	}

}
