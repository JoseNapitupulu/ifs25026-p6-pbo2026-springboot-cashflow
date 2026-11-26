package org.delcom.ifs25026p6pbo2026springbootcashflow;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiFinanceServiceTests {
    private HttpServer server;
    private String lastRequestBody;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesOpenAiCompatibleJsonResponseWithoutCallingExternalAi() throws IOException {
        String content = "[{\"description\":\"Makan \\\"siang\\\"\""
                + ",\"amount\":1.25e4,\"type\":\"expense\",\"metadata\":{\"flag\":true,"
                + "\"nothing\":null,\"values\":[false,-2],\"exponents\":[1E+2,1e-2],"
                + "\"emptyObject\":{},\"emptyArray\":[]}}]";
        OpenAiFinanceService service = serviceForResponse(wrapContent(content), 200);
        var result = service.generateTransactions("makan siang");

        assertEquals(1, result.size());
        assertEquals("Makan \"siang\"", result.getFirst().description());
        assertEquals(12_500, result.getFirst().amount());
        assertEquals(TransactionType.EXPENSE, result.getFirst().type());
    }

    @Test
    void serializesAllJsonStringEscapesAndAcceptsAFullCompletionEndpoint() throws IOException {
        String encodedDescription = "line\\n\\t\\b\\f\\r\\/\\\\\\\"" + "\\u" + "0041";
        String expectedDescription = "line\n\t\b\f\r/\\\"A";
        OpenAiFinanceService service = serviceForResponse(
                wrapContent("[{\"description\":\"ok\",\"amount\":1,\"type\":\"INCOME\"}]"),
                200, true);

        var result = service.generateTransactions("quote \" slash \\ controls \b\f\n\r\t" + (char) 1);

        assertEquals(1, result.size());
        assertTrue(lastRequestBody.contains("\\\""));
        assertTrue(lastRequestBody.contains("\\\\"));
        assertTrue(lastRequestBody.contains("\\b"));
        assertTrue(lastRequestBody.contains("\\f"));
        assertTrue(lastRequestBody.contains("\\n"));
        assertTrue(lastRequestBody.contains("\\r"));
        assertTrue(lastRequestBody.contains("\\t"));
        assertTrue(lastRequestBody.contains("\\u0001"));

        stopServer();
        OpenAiFinanceService escapedResponse = serviceForResponse(
                wrapContent("[{\"description\":\"" + encodedDescription
                        + "\",\"amount\":1,\"type\":\"INCOME\"}]"), 200);
        assertEquals(expectedDescription,
                escapedResponse.generateTransactions("prompt").getFirst().description());
    }

    @Test
    void trimsLongTrailingSlashSuffixFromEndpoint() throws IOException {
        OpenAiFinanceService service = serviceForResponse(
                wrapContent("[{\"description\":\"ok\",\"amount\":1,\"type\":\"INCOME\"}]"),
                200, "/".repeat(100_000));

        assertEquals("ok", service.generateTransactions("prompt").getFirst().description());
    }

    @Test
    void serializesNullJsonValue() throws Exception {
        var stringify = Class.forName(OpenAiFinanceService.class.getName() + "$Json")
                .getDeclaredMethod("stringify", Object.class);
        stringify.setAccessible(true);

        assertEquals("null", stringify.invoke(null, new Object[]{null}));
        assertEquals("true", stringify.invoke(null, true));
    }

    @Test
    void acceptsOnlySuccessfulHttpStatusCodes() throws Exception {
        var isSuccessfulStatus = OpenAiFinanceService.class.getDeclaredMethod("isSuccessfulStatus", int.class);
        isSuccessfulStatus.setAccessible(true);

        assertEquals(false, isSuccessfulStatus.invoke(null, 199));
        assertEquals(true, isSuccessfulStatus.invoke(null, 200));
        assertEquals(true, isSuccessfulStatus.invoke(null, 299));
        assertEquals(false, isSuccessfulStatus.invoke(null, 300));
    }

    @Test
    void acceptsMarkdownWrappedJsonAndWhitespaceInPayload() throws IOException {
        OpenAiFinanceService service = serviceForResponse(
                wrapContent("```json\n [ { \"description\":\"Gaji\", \"amount\":100, \"type\":\"income\" } ] \n```"),
                200);
        var result = service.generateTransactions("gaji");

        assertEquals(1, result.size());
        assertEquals(TransactionType.INCOME, result.getFirst().type());

        stopServer();
        OpenAiFinanceService unlabelledFence = serviceForResponse(
                wrapContent("```\n [ { \"description\":\"Bonus\", \"amount\":50, \"type\":\"income\" } ] \n```"),
                200);
        assertEquals("Bonus", unlabelledFence.generateTransactions("bonus").getFirst().description());
    }

    @Test
    void rejectsMissingAiConfigurationAndInvalidEndpoints() {
        assertThrows(IllegalStateException.class,
                () -> new OpenAiFinanceService("endpoint", null, "model").generateTransactions("test"));
        assertThrows(IllegalStateException.class,
                () -> new OpenAiFinanceService("endpoint", "key", "model").generateTransactions("test"));
        assertThrows(IllegalStateException.class,
                () -> new OpenAiFinanceService(null, "valid-key", "model").generateTransactions("test"));
        assertThrows(IllegalStateException.class,
                () -> new OpenAiFinanceService("  ", "valid-key", "model").generateTransactions("test"));
        assertThrows(IllegalStateException.class,
                () -> new OpenAiFinanceService("endpoint", " ", "model").generateTransactions("test"));
        assertThrows(IllegalArgumentException.class,
                () -> new OpenAiFinanceService("://bad", "valid-key", "model").generateTransactions("test"));
    }

    @Test
    void rejectsHttpFailuresMalformedResponsesAndInvalidTransactions() throws IOException {
        OpenAiFinanceService failedHttp = serviceForResponse("unavailable", 503);
        assertThrows(IllegalStateException.class, () -> failedHttp.generateTransactions("test"));

        for (String body : List.of(
                "[]",
                "{}",
                "{\"choices\":[]}",
                "{\"choices\":[null]}",
                "{\"choices\":[{}]}",
                "{\"choices\":[{\"message\":{}}]}",
                "{\"choices\":[{\"message\":{\"content\":42}}]}"
        )) {
            assertThrows(IllegalStateException.class,
                    () -> serviceForResponse(body, 200).generateTransactions("test"));
            stopServer();
        }

        for (String content : List.of(
                "{}",
                "[]",
                "true",
                "[null]",
                "[{}]",
                "[{\"description\":\"x\"}]",
                "[{\"description\":\"x\",\"amount\":1}]",
                "[{\"description\":\"x\",\"amount\":1,\"type\":null}]",
                "[{\"description\":\"x\",\"amount\":\"1\",\"type\":\"INCOME\"}]",
                "[{\"description\":\" \",\"amount\":1,\"type\":\"INCOME\"}]",
                "[{\"description\":\"x\",\"amount\":0,\"type\":\"INCOME\"}]",
                "[{\"description\":\"x\",\"amount\":1.5,\"type\":\"INCOME\"}]",
                "[{\"description\":\"x\",\"amount\":999999999999999999999999999999,\"type\":\"INCOME\"}]",
                "[{\"description\":\"x\",\"amount\":1,\"type\":\"invalid\"}]"
        )) {
            assertThrows(ValidationException.class,
                    () -> serviceForResponse(wrapContent(content), 200).generateTransactions("test"));
            stopServer();
        }
    }

    @Test
    void reportsConnectionErrorsAndRestoresInterruptedStatus() throws IOException {
        OpenAiFinanceService unavailable = new OpenAiFinanceService(
                "http://127.0.0.1:1", "valid-key", "model");
        assertThrows(IllegalStateException.class, () -> unavailable.generateTransactions("test"));

        OpenAiFinanceService service = serviceForResponse(
                wrapContent("[{\"description\":\"x\",\"amount\":1,\"type\":\"INCOME\"}]"), 200);
        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class, () -> service.generateTransactions("test"));
            org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void rejectsInvalidJsonSyntaxAndTrailingData() throws IOException {
        for (String content : List.of(
                "",
                "x",
                "1",
                "1.25",
                "1e2",
                "[{\"description\":\"x\",\"amount\":1,\"type\":\"INCOME\"}] trailing",
                "[{\"description\":\"x\",\"amount\":1,\"type\":\"INCOME\"",
                "[{\"description\":\"x\",\"amount\":1,\"type\":\"INCOME\",}]",
                "[{\"description\":\"bad\\q\",\"amount\":1,\"type\":\"INCOME\"}]",
                "[{\"description\":\"bad\\u12",
                "[{\"description\":\"x\",\"amount\":1,\"type\":\"INCOME\",\"meta\":truX}]",
                "[{\"description\":\"bad\\u12z4\",\"amount\":1,\"type\":\"INCOME\"}]",
                "[{\"description\":\"bad",
                "[{\"description\":\"bad\\",
                "[{\"description\":\"x\",\"amount\":1e,\"type\":\"INCOME\"}]"
        )) {
            assertThrows(RuntimeException.class,
                    () -> serviceForResponse(wrapContent(content), 200).generateTransactions("test"));
            stopServer();
        }
    }

    private OpenAiFinanceService serviceForResponse(String responseBody, int status) throws IOException {
        return serviceForResponse(responseBody, status, false);
    }

    private OpenAiFinanceService serviceForResponse(
            String responseBody, int status, boolean fullCompletionEndpoint) throws IOException {
        return serviceForResponse(
                responseBody, status, fullCompletionEndpoint ? "/v1/chat/completions" : "");
    }

    private OpenAiFinanceService serviceForResponse(
            String responseBody, int status, String endpointSuffix) throws IOException {
        byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        return new OpenAiFinanceService(
                "http://127.0.0.1:" + server.getAddress().getPort()
                        + endpointSuffix,
                "valid-key", "test-model");
    }

    private static String wrapContent(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
        return "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
    }
}
