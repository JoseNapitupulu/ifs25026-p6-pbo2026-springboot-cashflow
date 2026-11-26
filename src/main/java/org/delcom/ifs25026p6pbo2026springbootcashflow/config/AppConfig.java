package org.delcom.ifs25026p6pbo2026springbootcashflow.config;

import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25026p6pbo2026springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25026p6pbo2026springbootcashflow.usecase.UserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

@Configuration
public class AppConfig {
    @Bean
    public IUserRepository userRepository(DataSource dataSource) {
        SchemaInitializer.initialize(dataSource);
        return new JdbcUserRepository(dataSource);
    }

    @Bean
    public ITransactionRepository transactionRepository(DataSource dataSource) {
        return new JdbcTransactionRepository(dataSource);
    }

    @Bean
    public FinanceUseCase financeUseCase(ITransactionRepository transactionRepository) {
        return new FinanceUseCase(transactionRepository);
    }

    @Bean
    public AuthUseCase authUseCase(IUserRepository userRepository) {
        return new AuthUseCase(userRepository);
    }

    @Bean
    public UserUseCase userUseCase(IUserRepository userRepository) {
        return new UserUseCase(userRepository);
    }

    @Bean
    public FinanceAiUseCase financeAiUseCase(IFinanceAiService aiService, FinanceUseCase financeUseCase) {
        return new FinanceAiUseCase(aiService, financeUseCase);
    }

    @Bean
    public IFinanceAiService financeAiService(
            @Value("${app.ai.endpoint:}") String endpoint,
            @Value("${app.ai.key:}") String key,
            @Value("${app.ai.model:}") String model
    ) {
        return new OpenAiFinanceService(endpoint, key, model);
    }

    @Bean
    public FinancePresenter financePresenter() {
        return new FinancePresenter();
    }

    @Bean
    public FileStorage fileStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
        return new FileStorage(uploadDir);
    }

    @Bean
    public ApplicationRunner seedRootAdmin(
            AuthUseCase authUseCase,
            @Value("${app.root-username:admin}") String username,
            @Value("${app.root-password:admin}") String password
    ) {
        return args -> authUseCase.ensureRootAdmin(username, password);
    }

    @Bean
    public WebMvcConfigurer uploadResourceConfigurer(FileStorage storage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String location = storage.root().toUri().toString();
                registry.addResourceHandler("/uploads/**").addResourceLocations(location);
            }
        };
    }
}
