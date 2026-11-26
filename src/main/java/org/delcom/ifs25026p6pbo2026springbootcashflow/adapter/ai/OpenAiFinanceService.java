package org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.ai;

import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IFinanceAiService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiFinanceService implements IFinanceAiService {
    private final String endpoint;
    private final String key;
    private final String model;
    private final HttpClient httpClient;

    public OpenAiFinanceService(String endpoint, String key, String model) {
        this.endpoint = endpoint;
        this.key = key;
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    @Override
    public List<AiGeneratedTransaction> generateTransactions(String prompt) {
        if (key == null || key.isBlank() || "key".equalsIgnoreCase(key)) {
            throw new IllegalStateException("Konfigurasi OPENAI_KEY belum diisi");
        }
        String requestBody = Json.stringify(Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Buat daftar transaksi keuangan dari permintaan pengguna. Balas hanya JSON array "
                                        + "dengan setiap objek berisi description (string), amount (bilangan bulat "
                                        + "rupiah positif), dan type (INCOME atau EXPENSE). Jangan gunakan markdown."),
                        Map.of("role", "user", "content", prompt)
                )
        ));
        HttpRequest request = HttpRequest.newBuilder(completionUri())
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!isSuccessfulStatus(response.statusCode())) {
                throw new IllegalStateException("Layanan AI mengembalikan HTTP " + response.statusCode());
            }
            return parseTransactions(extractContent(Json.parse(response.body())));
        } catch (IOException ex) {
            throw new IllegalStateException("Tidak dapat menghubungi layanan AI", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Permintaan AI dihentikan", ex);
        }
    }

    private URI completionUri() {
        String normalized = endpoint == null ? "" : endpoint.trim();
        int end = normalized.length();
        while (end > 0 && normalized.charAt(end - 1) == '/') {
            end--;
        }
        normalized = normalized.substring(0, end);
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Konfigurasi OPENAI_ENDPOINT belum diisi");
        }
        if (!normalized.endsWith("/chat/completions")) {
            normalized += "/chat/completions";
        }
        return URI.create(normalized);
    }

    static boolean isSuccessfulStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static String extractContent(Object response) {
        if (!(response instanceof Map<?, ?> root)
                || !(root.get("choices") instanceof List<?> choices)
                || choices.isEmpty()
                || !(choices.get(0) instanceof Map<?, ?> choice)
                || !(choice.get("message") instanceof Map<?, ?> message)
                || !(message.get("content") instanceof String content)) {
            throw new IllegalStateException("Respons AI tidak memiliki choices[0].message.content");
        }
        String normalizedContent = content.trim();
        if (normalizedContent.startsWith("```")) {
            normalizedContent = normalizedContent.substring(3);
            if (normalizedContent.startsWith("json")) {
                normalizedContent = normalizedContent.substring(4);
            }
            normalizedContent = normalizedContent.stripLeading();
        }
        if (normalizedContent.endsWith("```")) {
            normalizedContent = normalizedContent.substring(0, normalizedContent.length() - 3).stripTrailing();
        }
        return normalizedContent;
    }

    private static List<AiGeneratedTransaction> parseTransactions(String content) {
        Object parsed = Json.parse(content);
        if (!(parsed instanceof List<?> items)) {
            throw new ValidationException("Respons AI harus berupa array transaksi");
        }
        List<AiGeneratedTransaction> transactions = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> values)
                    || !(values.get("description") instanceof String description)
                    || !(values.get("amount") instanceof Number amount)
                    || !(values.get("type") instanceof String type)) {
                throw new ValidationException("Format transaksi AI tidak valid");
            }
            try {
                long parsedAmount = new java.math.BigDecimal(amount.toString()).longValueExact();
                if (description.isBlank() || parsedAmount <= 0) {
                    throw new ValidationException("Keterangan harus diisi dan jumlah harus positif");
                }
                transactions.add(new AiGeneratedTransaction(
                        description, parsedAmount, TransactionType.valueOf(type.toUpperCase())));
            } catch (ArithmeticException | IllegalArgumentException ex) {
                throw new ValidationException("Jumlah atau tipe transaksi AI tidak valid");
            }
        }
        if (transactions.isEmpty()) {
            throw new ValidationException("AI tidak menghasilkan transaksi");
        }
        return List.copyOf(transactions);
    }

    private static final class Json {
        private final String source;
        private int index;

        private Json(String source) {
            this.source = source;
        }

        static Object parse(String source) {
            Json parser = new Json(source);
            Object result = parser.value();
            parser.whitespace();
            if (parser.index != source.length()) {
                throw new IllegalStateException("JSON tidak valid pada posisi " + parser.index);
            }
            return result;
        }

        static String stringify(Object value) {
            if (value instanceof String text) {
                return quote(text);
            }
            if (value instanceof Number || value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof Map<?, ?> map) {
                List<String> entries = new ArrayList<>();
                map.forEach((key, item) -> entries.add(quote(key.toString()) + ":" + stringify(item)));
                return "{" + String.join(",", entries) + "}";
            }
            if (value instanceof Iterable<?> values) {
                List<String> items = new ArrayList<>();
                values.forEach(item -> items.add(stringify(item)));
                return "[" + String.join(",", items) + "]";
            }
            return "null";
        }

        private static String quote(String text) {
            StringBuilder escaped = new StringBuilder("\"");
            for (char character : text.toCharArray()) {
                switch (character) {
                    case '"' -> escaped.append("\\\"");
                    case '\\' -> escaped.append("\\\\");
                    case '\b' -> escaped.append("\\b");
                    case '\f' -> escaped.append("\\f");
                    case '\n' -> escaped.append("\\n");
                    case '\r' -> escaped.append("\\r");
                    case '\t' -> escaped.append("\\t");
                    default -> {
                        if (character < 0x20) {
                            escaped.append(String.format("\\u%04x", (int) character));
                        } else {
                            escaped.append(character);
                        }
                    }
                }
            }
            return escaped.append('"').toString();
        }

        private Object value() {
            whitespace();
            if (index >= source.length()) {
                throw invalidJson();
            }
            return switch (source.charAt(index)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            whitespace();
            if (consume('}')) {
                return result;
            }
            do {
                whitespace();
                String key = string();
                whitespace();
                require(':');
                result.put(key, value());
                whitespace();
            } while (consume(','));
            require('}');
            return result;
        }

        private List<Object> array() {
            index++;
            List<Object> result = new ArrayList<>();
            whitespace();
            if (consume(']')) {
                return result;
            }
            do {
                result.add(value());
                whitespace();
            } while (consume(','));
            require(']');
            return result;
        }

        private String string() {
            require('"');
            StringBuilder result = new StringBuilder();
            while (index < source.length()) {
                char character = source.charAt(index++);
                if (character == '"') {
                    return result.toString();
                }
                if (character != '\\') {
                    result.append(character);
                    continue;
                }
                if (index >= source.length()) {
                    throw invalidJson();
                }
                char escape = source.charAt(index++);
                switch (escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (index + 4 > source.length()) {
                            throw invalidJson();
                        }
                        result.append((char) Integer.parseInt(source.substring(index, index + 4), 16));
                        index += 4;
                    }
                    default -> throw invalidJson();
                }
            }
            throw invalidJson();
        }

        private Number number() {
            int start = index;
            if (consume('-')) {
                // The digits are parsed below.
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            if (consume('.')) {
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            if (start == index) {
                throw invalidJson();
            }
            return new java.math.BigDecimal(source.substring(start, index));
        }

        private Object literal(String expected, Object result) {
            if (!source.startsWith(expected, index)) {
                throw invalidJson();
            }
            index += expected.length();
            return result;
        }

        private void whitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!consume(expected)) {
                throw invalidJson();
            }
        }

        private IllegalStateException invalidJson() {
            return new IllegalStateException("JSON tidak valid pada posisi " + index);
        }
    }
}
