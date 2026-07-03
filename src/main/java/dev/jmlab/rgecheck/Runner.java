// SPDX-License-Identifier: Apache-2.0
package dev.jmlab.rgecheck;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
class Runner implements CommandLineRunner {

	private static final String PINNED_VECTORS_DIGEST = "8603868389a18f8de6f593b03c2c9947bf145c79491f2b095e1da380b6abbc95";

	private final ObjectMapper mapper;

	Runner(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(String... args) throws Exception {
		Path vectorsPath = Path.of(args.length > 0 ? args[0] : "vectors.json");
		Map<String, Object> doc = mapper.readValue(Files.readAllBytes(vectorsPath), Map.class);
		List<Map<String, Object>> vectors = (List<Map<String, Object>>) doc.get("vectors");

		Map<String, String> outcomes = new LinkedHashMap<>();
		for (Map<String, Object> vector : vectors) {
			String axis = (String) vector.get("axis");
			Map<String, Object> inputs = (Map<String, Object>) vector.get("inputs");
			outcomes.put((String) vector.get("vector_id"), Axes.evaluate(axis, inputs));
		}

		Map<String, Object> matrix = new LinkedHashMap<>();
		matrix.put("impl", "jm-lab-spring");
		matrix.put("outcomes", outcomes);
		Path out = Path.of("out", "jm-lab-spring.json");
		Files.createDirectories(out.getParent());
		Files.write(out, mapper.writeValueAsBytes(matrix));

		String nativeDigest = sha256Hex(mapper.writeValueAsBytes(vectors));
		String sortedDigest = sha256Hex(mapper.writeValueAsBytes(sortKeys(vectors)));
		report(vectors, outcomes, nativeDigest, sortedDigest);
	}

	private void report(List<Map<String, Object>> vectors, Map<String, String> outcomes, String nativeDigest,
			String sortedDigest) {
		Map<String, int[]> perAxis = new LinkedHashMap<>();
		int reproduced = 0;
		for (Map<String, Object> vector : vectors) {
			int[] tally = perAxis.computeIfAbsent((String) vector.get("axis"), k -> new int[2]);
			tally[1]++;
			if (outcomes.get(vector.get("vector_id")).equals(vector.get("expected"))) {
				tally[0]++;
				reproduced++;
			}
		}

		System.out.println();
		System.out.println("serializer: " + mapper.getClass().getName());
		System.out.println();
		System.out.println("== per-axis reproduction (mine vs expected, post-hoc diff) ==");
		perAxis.forEach((axis, t) -> System.out.printf("  %-24s %d/%d  %s%n", axis, t[0], t[1],
				t[0] == t[1] ? "pass" : (t[0] == 0 ? "fail" : "partial")));
		System.out.printf("  %-24s %d/%d%n", "TOTAL", reproduced, vectors.size());
		System.out.println();
		System.out.println("== vectors_digest ==");
		System.out.println("  README pinned             : " + PINNED_VECTORS_DIGEST);
		System.out.println("  sorted keys (his recipe)  : " + sortedDigest + "  match="
				+ sortedDigest.equals(PINNED_VECTORS_DIGEST));
		System.out.println("  insertion order (Spring)  : " + nativeDigest + "  differs="
				+ !nativeDigest.equals(PINNED_VECTORS_DIGEST));
	}

	private static Object sortKeys(Object value) {
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> sorted = new TreeMap<>();
			map.forEach((k, v) -> sorted.put((String) k, sortKeys(v)));
			return sorted;
		}
		if (value instanceof List<?> list) {
			List<Object> out = new ArrayList<>(list.size());
			list.forEach(item -> out.add(sortKeys(item)));
			return out;
		}
		return value;
	}

	private static String sha256Hex(byte[] bytes) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
		StringBuilder hex = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return hex.toString();
	}

}
