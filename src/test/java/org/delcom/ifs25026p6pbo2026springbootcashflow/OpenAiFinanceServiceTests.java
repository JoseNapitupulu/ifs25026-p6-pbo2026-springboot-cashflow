package org.delcom.ifs25026p6pbo2026springbootcashflow;

import com.sun.net.httpserver.HttpServer;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenAiFinanceServiceTests {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesOpenAiCompatibleJsonResponseWithoutCallingExternalAi() throws IOException {
        String content = "[{\"description\":\"Makan \\\"siang\\\"\",\"amount\":12500,\"type\":\"EXPENSE\"}]";
        String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"");
        byte[] response = ("{\"choices\":[{\"message\":{\"content\":\"" + escapedContent + "\"}}]}")
                .getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().close();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        OpenAiFinanceService service = new OpenAiFinanceService(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "test-key", "test-model");
        var result = service.generateTransactions("makan siang");

        assertEquals(1, result.size());
        assertEquals("Makan \"siang\"", result.getFirst().description());
        assertEquals(12_500, result.getFirst().amount());
        assertEquals(TransactionType.EXPENSE, result.getFirst().type());
    }
}
