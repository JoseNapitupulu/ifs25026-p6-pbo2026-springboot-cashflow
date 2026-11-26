package org.delcom.ifs25026p6pbo2026springbootcashflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Ifs25026P6Pbo2026SpringbootCashflowApplication {

    public static void main(String[] args) {
        try {
            DotEnv.load();
            SpringApplication.run(Ifs25026P6Pbo2026SpringbootCashflowApplication.class, args);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Gagal membaca konfigurasi .env", ex);
        }
    }
}
